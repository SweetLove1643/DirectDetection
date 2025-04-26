package com.sweetlove.directdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.sweetlove.directdetection.Controller.LoginActivity;
import com.sweetlove.directdetection.Controller.RegisterActivity;

public class MainActivity extends AppCompatActivity {
    private Button signin_btn;
    private TextView create_account;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        signin_btn = findViewById(R.id.signin_btn);
        create_account = findViewById(R.id.create_account);

        signin_btn.setOnClickListener(v -> {
            Intent login_form = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(login_form);
        });
        create_account.setOnClickListener(v -> {
            Intent register_form = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(register_form);
        });

    }
}