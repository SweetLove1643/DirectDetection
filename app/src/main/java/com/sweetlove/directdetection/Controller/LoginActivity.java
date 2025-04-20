package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

public class LoginActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName().toString();

    private TextView email_input, password_input, forgetpassword, signup;
    private ImageButton back_btn;
    private CheckBox rememberme;
    private Button login_btn;
    private ImageView fb_login, gg_login;
//    private static final int RC_SIGN_IN = 123;
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mauth;
    private FirebaseFirestore db;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mauth.getCurrentUser();
        if(currentUser != null){
            Intent home_form = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(home_form);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);

        forgetpassword = findViewById(R.id.forgotpassword_textview);
        signup = findViewById(R.id.signup_btn);

        rememberme = findViewById(R.id.rememberme_checkbox);

        login_btn = findViewById(R.id.login_btn);
        back_btn = findViewById(R.id.back_btn);

        fb_login = findViewById(R.id.login_fb_btn);
        gg_login = findViewById(R.id.login_gg_btn);


        signup.setOnClickListener(v -> {
            Intent register_form = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(register_form);
        });
        back_btn.setOnClickListener(v -> finish());

        login_btn.setOnClickListener(v -> loginWithEmail());

        gg_login.setOnClickListener(v -> loginWithGoogle());


    }

    private void loginWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mauth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mauth.getCurrentUser().getUid();
                        db.collection("users").document(userId)
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        DocumentSnapshot document = task1.getResult();
                                        if (!document.exists()) {
                                            // Điều hướng để nhập thông tin bổ sung
                                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                            intent.putExtra("email", acct.getEmail());
                                            intent.putExtra("fullname", acct.getDisplayName());
                                            startActivity(intent);
                                        } else {
                                            String role = document.getString("role");
                                            if (role.equals("Người dùng")) {
                                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                            } else if (role.equals("Người thân")) {
                                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                            }
                                            finish();
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Lỗi: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(LoginActivity.this, "Xác thực thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithEmail(){
        String email = email_input.getText().toString().trim();
        String password = password_input.getText().toString().trim();
        Log.i(TAG, email + password);

        // Kiểm tra thông tin nhập
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email và password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đăng nhập với Firebase Authentication
        mauth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mauth.getCurrentUser();

                            String userId = mauth.getCurrentUser().getUid();
                            // Lấy thông tin người dùng từ Firestore
                            db.collection("users").document(userId)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            DocumentSnapshot document = task1.getResult();
                                            if (document.exists()) {
                                                String role = document.getString("role");
                                                if (role.equals("Người dùng")) {
                                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                                } else if (role.equals("Người thân")) {
                                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                                }
                                                finish();
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Lỗi: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Log.w(TAG, "Đăng nhập thất bại: " + task.getException().getMessage());
                            Toast.makeText(LoginActivity.this, "Sai thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
