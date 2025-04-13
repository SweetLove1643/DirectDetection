package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

public class LoginController extends AppCompatActivity {

    private TextView usernameInput, passwordInput, forgetpassword, signup;
    private ImageButton back_btn;
    private CheckBox rememberme;
    private Button login_btn;
    private ImageView fb_login, gg_login;
    private static final int RC_SIGN_IN = 123;

    private FirebaseAuth mauth;
    private FirebaseFirestore db;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mauth.getCurrentUser();
        if(currentUser != null){
            Intent home_form = new Intent(getApplicationContext(), HomeController.class);
            startActivity(home_form);
            finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_controller);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        forgetpassword = findViewById(R.id.forgotpassword_textview);
        signup = findViewById(R.id.signup_btn);

        rememberme = findViewById(R.id.rememberme_checkbox);

        login_btn = findViewById(R.id.login_btn);
        back_btn = findViewById(R.id.back_btn);

        fb_login = findViewById(R.id.login_fb_btn);
        gg_login = findViewById(R.id.login_gg_btn);


        signup.setOnClickListener(v -> {
            Intent register_form = new Intent(LoginController.this, RegisterController.class);
            startActivity(register_form);
            finish();
        });
        back_btn.setOnClickListener(v -> finish());
    }

    private void loginWithUsername(){
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }


    }
}
