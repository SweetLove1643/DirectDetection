package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth mauth;
    FirebaseFirestore db;
    Button logout_btn;
    private String TAG = this.getClass().getSimpleName().toString();
    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mauth.getCurrentUser();
        if(currentUser != null){
            Log.i(TAG, "Đã có dữ liệu người dùng");
            Intent recognition = new Intent(getApplicationContext(), RecognitionActivity.class);
            startActivity(recognition);
        }
    }

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
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
            finish();
        });


    }
}