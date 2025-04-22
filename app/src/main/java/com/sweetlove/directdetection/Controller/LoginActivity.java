package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName().toString();

    private TextView email_input, password_input, forgetpassword, signup;
    private ImageButton back_btn;
    private CheckBox rememberme;
    private Button login_btn;
    private ImageView fb_login, gg_login;

    private FirebaseAuth mauth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> activityResultLauncher;

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

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Log.d(TAG, "onActivityResult triggered");
                int resultCode = result.getResultCode();
                Log.d(TAG, "Google Sign-In result code: " + resultCode);
                if(result.getResultCode() == RESULT_OK){
                    Log.d(TAG, "Result OK");
                    Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try{
                        GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                        if (signInAccount != null) {
                            Log.d(TAG, "signInAccount: " + signInAccount.getEmail());
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            mauth.signInWithCredential(authCredential)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if(task.isSuccessful()){
                                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                saveUserToFirestore(user);
                                                Toast.makeText(LoginActivity.this,"Đăng nhập thành công với Google", Toast.LENGTH_SHORT).show();
                                                Intent home_form = new Intent(LoginActivity.this, HomeActivity.class);
                                                startActivity(home_form);

                                            }else{
                                                Toast.makeText(LoginActivity.this,"Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "Không lấy được tài khoản Google", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed", e);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);

        forgetpassword = findViewById(R.id.forgotpassword_textview);
        signup = findViewById(R.id.signup_btn);

        rememberme = findViewById(R.id.rememberme_checkbox);

        login_btn = findViewById(R.id.login_btn);
        back_btn = findViewById(R.id.back_btn);

        fb_login = findViewById(R.id.login_fb_btn);
        gg_login = findViewById(R.id.login_gg_btn);

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, options);


        signup.setOnClickListener(v -> {
            Intent register_form = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(register_form);
        });
        back_btn.setOnClickListener(v -> finish());



        login_btn.setOnClickListener(v -> {
            String email = email_input.getText().toString().trim();
            String password = password_input.getText().toString().trim();
            loginWithEmail(email, password);
        });



        gg_login.setOnClickListener(v -> startGoogleSignIn());

        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email_saved = preferences.getString("email", null);
        String password_saved = preferences.getString("password", null);
        boolean rememberMe = preferences.getBoolean("rememberMe", false);
        if (rememberMe) {
            // Tiến hành đăng nhập tự động bằng thông tin đã lưu
            loginWithEmail(email_saved, password_saved);
        }


    }

    private void startGoogleSignIn(){
        Log.d(TAG, "startGoogleSignIn clicked");
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullname", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("role", "Người dùng");

        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Lưu thông tin người dùng thành công"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu Firestore: ", e));
    }

    private void loginWithEmail(@Nullable String email, @Nullable String password){
        Log.i(TAG, email + password);
        if(rememberme.isChecked()){
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            // Lưu email và mật khẩu khi người dùng chọn "Nhớ mật khẩu"
            editor.putString("email", email);
            editor.putString("password", password);
            editor.putBoolean("rememberMe", true);  // Đánh dấu là "Nhớ mật khẩu"
            editor.apply();
        }

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
