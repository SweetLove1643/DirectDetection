package com.sweetlove.directdetection.Controller;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Starting initialization of NotificationFragment");
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        Log.d(TAG, "onCreateView: Layout fragment_notification inflated");

        recyclerView = view.findViewById(R.id.notificationRecyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            Log.d(TAG, "onCreateView: RecyclerView initialized with LinearLayoutManager");
        } else {
            Log.w(TAG, "onCreateView: RecyclerView (R.id.notificationRecyclerView) not found");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "onCreateView: Firebase user authenticated, UID=" + user.getUid());
        } else {
            Log.w(TAG, "onCreateView: No authenticated user found");
        }

        notificationList = new ArrayList<>();
        Log.d(TAG, "onCreateView: Notification list initialized, size=" + notificationList.size());

        if (user != null) {
            Log.d(TAG, "onCreateView: Querying Firestore collection 'notification' for UID=" + user.getUid());

            db.collection("relationships")
                    .whereEqualTo("familyUserId", user.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Log.d(TAG, "Truy cập relationships");
                        String relative_uid = queryDocumentSnapshots.getDocuments().get(0).get("userId").toString();

                        db.collection("notification")
                                .whereEqualTo("uid_user", relative_uid)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshotss -> {
                                    Log.d(TAG, "Firestore: Query successful, document count=" + queryDocumentSnapshotss.size());
                                    for (DocumentSnapshot doc : queryDocumentSnapshotss) {
                                        try {
                                            String title = doc.getString("title");
                                            String message = doc.getString("message");
                                            String date = doc.getString("time");
                                            if (title != null && message != null && date != null) {
                                                notificationList.add(new Notification(title, message, date));
                                                Log.d(TAG, "Firestore: Added notification - title=" + title + ", message=" + message + ", date=" + date);
                                            } else {
                                                Log.w(TAG, "Firestore: Invalid document data, skipping - title=" + title + ", message=" + message + ", date=" + date);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Firestore: Error processing document " + doc.getId() + ": ", e);
                                        }
                                    }
                                    Log.d(TAG, "Firestore: Notification list updated, size=" + notificationList.size());
                                    adapter.notifyDataSetChanged();
                                    Log.d(TAG, "Firestore: Adapter notified of data change");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore: Error querying collection 'notification': ", e);
                                });

                    });

        } else {
            Log.w(TAG, "onCreateView: Cannot query Firestore, user is null");
        }

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "onCreateView: NotificationAdapter set for RecyclerView");

        return view;
    }
}