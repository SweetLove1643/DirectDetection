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

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    FirebaseAuth mauth;
    FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mauth.getCurrentUser();

        notificationList = new ArrayList<>(); // list chua thong bao

        db.collection("notification")
                .whereEqualTo("uid_user", user.getUid().toString())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for(DocumentSnapshot doc : queryDocumentSnapshots){
                        String title = doc.get("title").toString();
                        String mess = doc.get("message").toString();
                        String data = doc.get("data").toString();
                        notificationList.add(new Notification(title, mess, data));

                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(this.getClass().getSimpleName().toString(), "Error getting documents: ", e);
                });

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}