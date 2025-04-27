package com.sweetlove.directdetection.Controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sweetlove.directdetection.Controller.Notification;
import com.sweetlove.directdetection.Controller.NotificationAdapter;
import com.sweetlove.directdetection.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Dữ liệu mẫu
        notificationList = new ArrayList<>();
        notificationList.add(new Notification("Cảnh báo nguy hiểm", "Phát hiện chuyển động bất thường lúc 22:00", "2025-04-27"));
        notificationList.add(new Notification("Cảnh báo an toàn", "Cửa chính mở lúc 21:30", "2025-04-27"));

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}