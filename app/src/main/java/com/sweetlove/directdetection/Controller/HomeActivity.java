package com.sweetlove.directdetection.Controller;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth mauth;
    FirebaseFirestore db;
    Button logout_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        logout_btn = findViewById(R.id.logout_btn);

        logout_btn.setOnClickListener(v -> {
            mauth.signOut();
            finish();
        });


    }
}