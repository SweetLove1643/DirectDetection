package com.sweetlove.directdetection.Controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Bắt đầu khởi tạo LoginActivity");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate: Đã set layout activity_login");

        // Khởi tạo ActivityResultLauncher cho Google Sign-In
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                Log.d(TAG, "onActivityResult: Kết quả Google Sign-In, resultCode=" + result.getResultCode());
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "onActivityResult: Result OK");
                    Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                        if (signInAccount != null) {
                            Log.d(TAG, "onActivityResult: Tài khoản Google - email=" + signInAccount.getEmail());
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            mauth.signInWithCredential(authCredential)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "signInWithCredential:success");
                                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                if (user != null) {
                                                    Log.d(TAG, "handleGoogleSignInResult: User signed in: " + user.getEmail());
                                                    checkUserRole(user);
                                                } else {
                                                    Log.w(TAG, "handleGoogleSignInResult: Sign-in failed");
                                                }
//                                                saveUserToFirestore(user);
//                                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công với Google", Toast.LENGTH_SHORT).show();
//                                                Intent home_form = new Intent(LoginActivity.this, RecognitionActivity.class);
//                                                startActivity(home_form);
//                                                finish();
                                            } else {
                                                Log.e(TAG, "signInWithCredential: Đăng nhập thất bại: ", task.getException());
                                                Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            Log.w(TAG, "onActivityResult: Không lấy được tài khoản Google");
                            Toast.makeText(LoginActivity.this, "Không lấy được tài khoản Google", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "onActivityResult: Google sign in failed, statusCode=" + e.getStatusCode(), e);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "onActivityResult: Kết quả không OK, resultCode=" + result.getResultCode());
                }
            }
        });

        // Khởi tạo Firebase
        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Tắt xác minh ứng dụng (reCAPTCHA) trong môi trường kiểm tra
        mauth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        Log.d(TAG, "onCreate: Khởi tạo FirebaseAuth và Firestore, tắt reCAPTCHA cho kiểm tra");

        // Liên kết UI
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        forgetpassword = findViewById(R.id.forgotpassword_textview);
        signup = findViewById(R.id.signup_btn);
        rememberme = findViewById(R.id.rememberme_checkbox);
        login_btn = findViewById(R.id.login_btn);
        back_btn = findViewById(R.id.back_btn);
        fb_login = findViewById(R.id.login_fb_btn);
        gg_login = findViewById(R.id.login_gg_btn);
        Log.d(TAG, "onCreate: Đã liên kết các thành phần UI");

        // Khởi tạo Google Sign-In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, options);
        Log.d(TAG, "onCreate: Khởi tạo GoogleSignInClient");

        // Sự kiện nút
        signup.setOnClickListener(v -> {
            Log.d(TAG, "SignUp Button: Nhấn nút Sign Up");
            Intent register_form = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(register_form);
            Log.d(TAG, "SignUp Button: Chuyển sang RegisterActivity");
        });

        back_btn.setOnClickListener(v -> {
            Log.d(TAG, "Back Button: Nhấn nút Back");
            finish();
            Log.d(TAG, "Back Button: Đóng LoginActivity");
        });

        forgetpassword.setOnClickListener(v -> {
            Log.d(TAG, "ForgetPassword: Nhấn quên mật khẩu");
            Senforgotpassword();
        });

        login_btn.setOnClickListener(v -> {
            Log.d(TAG, "Login Button: Nhấn nút Login");
            String email = email_input.getText().toString().trim();
            String password = password_input.getText().toString().trim();
            loginWithEmail(email, password);
        });

        gg_login.setOnClickListener(v -> {
            Log.d(TAG, "Google Login Button: Nhấn nút Google Login");
            startGoogleSignIn();
        });

        // Tự động đăng nhập nếu có thông tin lưu
        new android.os.Handler().post(() -> {
            Log.d(TAG, "Handler: Kiểm tra thông tin đăng nhập đã lưu");
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String email_saved = preferences.getString("email", null);
            String password_saved = preferences.getString("password", null);
            boolean rememberMe = preferences.getBoolean("rememberMe", false);
            Log.d(TAG, "Handler: rememberMe=" + rememberMe + ", email_saved=" + email_saved);
            if (rememberMe && email_saved != null && password_saved != null) {
                Log.d(TAG, "Handler: Thử tự động đăng nhập với email=" + email_saved);
                try {
                    loginWithEmail(email_saved, password_saved);
                } catch (Exception e) {
                    Log.e(TAG, "Handler: Tự động đăng nhập thất bại: ", e);
                    Toast.makeText(LoginActivity.this, "Tự động đăng nhập thất bại, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                    clearSavedCredentials();
                }
            } else {
                Log.d(TAG, "Handler: Không có thông tin đăng nhập đã lưu hoặc rememberMe=false");
            }
        });
    }

    private void Senforgotpassword() {
        Log.d(TAG, "Senforgotpassword: Hiển thị dialog quên mật khẩu");
        final EditText emailEditText = new EditText(this);
        emailEditText.setHint("Nhập email của bạn");
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Quên mật khẩu")
                .setView(emailEditText)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String email = emailEditText.getText().toString().trim();
                    Log.d(TAG, "Senforgotpassword: Email nhập vào=" + email);
                    if (isValidEmail(email)) {
                        Log.d(TAG, "Senforgotpassword: Gửi email đặt lại mật khẩu");
                        mauth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Senforgotpassword: Gửi email đặt lại mật khẩu thành công");
                                        Toast.makeText(LoginActivity.this, "Email đặt lại mật khẩu đã được gửi", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.e(TAG, "Senforgotpassword: Gửi email thất bại: ", task.getException());
                                        Toast.makeText(LoginActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Log.w(TAG, "Senforgotpassword: Email không hợp lệ");
                        Toast.makeText(LoginActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    Log.d(TAG, "Senforgotpassword: Hủy dialog");
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private boolean isValidEmail(String email) {
        boolean isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        Log.d(TAG, "isValidEmail: Email=" + email + ", isValid=" + isValid);
        return isValid;
    }

    private void startGoogleSignIn() {
        Log.d(TAG, "startGoogleSignIn: Bắt đầu đăng nhập Google");
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "startGoogleSignIn: Đã sign out Google client");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
            Log.d(TAG, "startGoogleSignIn: Đã launch intent Google Sign-In");
        });
    }

    private void checkUserRole(FirebaseUser user) {
        Log.d(TAG, "checkUserRole: Checking role for user: " + user.getUid());
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                        String role =  documentSnapshot.getString("role");
                        Log.d(TAG, "checkUserRole: User has role: " + role);
                        if("Người dùng".equals(role)){
                            Log.d(TAG, "Người dùng " );
                            // Người dùng đã có vai trò, chuyển đến màn hình chính
                            startActivity(new Intent(this, RecognitionActivity.class));
                        }else{
                            Log.d(TAG, "Người thân " );

                            startActivity(new Intent(this, RelativeMainActivity.class));
                        }

                    } else {
                        Log.d(TAG, "checkUserRole: No role found, redirecting to RoleSelectionActivity");
                        // Chưa có vai trò, chuyển đến chọn vai trò
                        startActivity(new Intent(this, ChooseRoleActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkUserRole: Error checking role: ", e);
                    Toast.makeText(this, "Lỗi khi kiểm tra vai trò: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        Log.d(TAG, "saveUserToFirestore: Lưu thông tin người dùng, UID=" + user.getUid());
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullname", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("role", "Người dùng");

        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "saveUserToFirestore: Lưu thông tin người dùng thành công"))
                .addOnFailureListener(e -> Log.e(TAG, "saveUserToFirestore: Lỗi lưu Firestore: ", e));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        Log.d(TAG, "isNetworkAvailable: Kết nối mạng=" + isAvailable);
        return isAvailable;
    }

    private void loginWithEmail(@Nullable String email, @Nullable String password) {
        Log.d(TAG, "loginWithEmail: Bắt đầu đăng nhập với email=" + email + ", password=" + (password != null ? "[đã nhập]" : null));
        if (email == null || password == null) {
            Log.w(TAG, "loginWithEmail: Email hoặc password là null");
            Toast.makeText(this, "Email hoặc mật khẩu không hợp lệ", Toast.LENGTH_SHORT).show();
            clearSavedCredentials();
            return;
        }

        email = email.trim().toLowerCase();
        Log.d(TAG, "loginWithEmail: Email sau chuẩn hóa=" + email);

        if (rememberme.isChecked()) {
            Log.d(TAG, "loginWithEmail: Lưu thông tin đăng nhập vào SharedPreferences");
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("email", email);
            editor.putString("password", password);
            editor.putBoolean("rememberMe", true);
            editor.apply();
            Log.d(TAG, "loginWithEmail: Đã lưu email và password vào SharedPreferences");
        }

        // Kiểm tra thông tin nhập
        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "loginWithEmail: Email hoặc password rỗng");
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            clearSavedCredentials();
            return;
        }

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Log.w(TAG, "loginWithEmail: Không có kết nối Internet");
            Toast.makeText(this, "Không có kết nối Internet", Toast.LENGTH_SHORT).show();
            clearSavedCredentials();
            return;
        }

        Log.i(TAG, "loginWithEmail: Kiểm tra đầu vào hợp lệ, bắt đầu đăng nhập Firebase");

        // Đăng nhập với Firebase Authentication
        try {
            Log.d(TAG, "signInWithEmail: Gọi Firebase Authentication với email=" + email);
            mauth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mauth.getCurrentUser();
                                if (user == null) {
                                    Log.e(TAG, "signInWithEmail: Lỗi - Không lấy được thông tin người dùng");
                                    Toast.makeText(LoginActivity.this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                                    clearSavedCredentials();
                                    return;
                                }

                                String userId = user.getUid();
                                Log.d(TAG, "signInWithEmail: UID người dùng=" + userId);

                                // Lấy thông tin người dùng từ Firestore
                                Log.d(TAG, "signInWithEmail: Truy vấn Firestore collection users, document=" + userId);
                                db.collection("users").document(userId)
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                DocumentSnapshot document = task1.getResult();
                                                if (document.exists()) {
                                                    String role = document.getString("role");
                                                    Log.d(TAG, "signInWithEmail: Firestore - Role=" + role);
                                                    if (role != null) {
                                                        if (role.equals("Người dùng")) {
                                                            Log.d(TAG, "signInWithEmail: Chuyển hướng đến RecognitionActivity");
                                                            startActivity(new Intent(LoginActivity.this, RecognitionActivity.class));
                                                            finish();
                                                        } else if (role.equals("Người thân")) {
                                                            Log.d(TAG, "signInWithEmail: Chuyển hướng đến RelativeMainActivity");
                                                            startActivity(new Intent(LoginActivity.this, RelativeMainActivity.class));
                                                            finish();
                                                        } else {
                                                            Log.w(TAG, "signInWithEmail: Role không hợp lệ: " + role);
                                                            Toast.makeText(LoginActivity.this, "Vai trò người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        Log.w(TAG, "signInWithEmail: Không tìm thấy trường role trong document");
                                                        Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin vai trò", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Log.w(TAG, "signInWithEmail: Document không tồn tại trong users/" + userId);
                                                    Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Log.e(TAG, "signInWithEmail: Lỗi truy vấn Firestore: ", task1.getException());
                                                Toast.makeText(LoginActivity.this, "Lỗi truy vấn dữ liệu: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                                String errorCode = "";
                                if (task.getException() instanceof FirebaseAuthException) {
                                    errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                                    Log.e(TAG, "signInWithEmail: Mã lỗi: " + errorCode);
                                    switch (errorCode) {
                                        case "ERROR_INVALID_EMAIL":
                                            errorMessage = "Email không hợp lệ";
                                            break;
                                        case "ERROR_WRONG_PASSWORD":
                                            errorMessage = "Mật khẩu không đúng";
                                            break;
                                        case "ERROR_USER_NOT_FOUND":
                                            errorMessage = "Tài khoản không tồn tại";
                                            break;
                                        case "ERROR_USER_DISABLED":
                                            errorMessage = "Tài khoản đã bị vô hiệu hóa";
                                            break;
                                        case "ERROR_TOO_MANY_REQUESTS":
                                            errorMessage = "Quá nhiều yêu cầu, vui lòng thử lại sau";
                                            break;
                                    }
                                }
                                Log.e(TAG, "signInWithEmail: Đăng nhập thất bại, errorCode=" + errorCode + ", message=" + errorMessage);
                                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                                clearSavedCredentials();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "signInWithEmail: Ngoại lệ không mong muốn: ", e);
            Toast.makeText(LoginActivity.this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            clearSavedCredentials();
        }
    }

    private void clearSavedCredentials() {
        Log.d(TAG, "clearSavedCredentials: Xóa thông tin đăng nhập đã lưu");
        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "clearSavedCredentials: Đã xóa SharedPreferences");
    }
}