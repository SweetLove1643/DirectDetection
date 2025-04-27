package com.sweetlove.directdetection.Controller;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sweetlove.directdetection.R;

import java.util.ArrayList;
import java.util.List;

public class Home_Relative extends Fragment {

    private RecyclerView notificationList;
    private NotificationAdapter adapter;
    private List<Notification> notifications;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        notificationList = view.findViewById(R.id.notification_list);
        notificationList.setLayoutManager(new LinearLayoutManager(getContext()));
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications, notification -> {
            Bundle bundle = new Bundle();
            bundle.putString("content", notification.getContent());
            bundle.putString("time", notification.getTime());
            Navigation.findNavController(view).navigate(R.id.action_home_to_detail, bundle);
        });
        notificationList.setAdapter(adapter);

        loadNotificationsFromSms();

        return view;
    }

    private void loadNotificationsFromSms() {
        notifications.clear();
        ContentResolver contentResolver = requireActivity().getContentResolver();
        Cursor cursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                if (body.startsWith("Cảnh báo: Phát hiện")) {
                    String time = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    notifications.add(new Notification(body, time));
                }
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}