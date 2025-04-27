package com.sweetlove.directdetection.Controller;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    private TextView fullname_input, email_input, password_input, signin, family_email_input;
    private Spinner usertype;
    private ImageButton back_btn;
    private Button signup;
    private FirebaseAuth mauth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Bắt đầu khởi tạo RegisterActivity");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        Log.d(TAG, "onCreate: Đã set layout activity_register");

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
        Log.d(TAG, "onCreate: Khởi tạo App Check với Play Integrity");

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Tắt xác minh ứng dụng (reCAPTCHA) trong môi trường kiểm tra
        mauth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        Log.d(TAG, "onCreate: Khởi tạo FirebaseAuth và Firestore, tắt reCAPTCHA cho kiểm tra");

        // Liên kết UI
        fullname_input = findViewById(R.id.name_input);
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        family_email_input = findViewById(R.id.family_email_input);
        usertype = findViewById(R.id.spinner_usertype);
        signin = findViewById(R.id.signin_btn);
        signup = findViewById(R.id.signup_btn);
        back_btn = findViewById(R.id.back_btn);
        Log.d(TAG, "onCreate: Đã liên kết các thành phần UI");

        // Thiết lập Spinner
        String[] userTypes = {"Người dùng", "Người thân"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_selected_item,
                userTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usertype.setAdapter(adapter);
        Log.d(TAG, "onCreate: Đã thiết lập Spinner với các loại người dùng");

        usertype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Spinner: Đã chọn loại người dùng: " + selectedItem);
                family_email_input.setVisibility(selectedItem.equals("Người thân") ? View.VISIBLE : View.GONE);
                Log.d(TAG, "Spinner: " + (selectedItem.equals("Người thân") ? "Hiển thị" : "Ẩn") + " trường family_email_input");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner: Không có loại người dùng nào được chọn");
                family_email_input.setVisibility(View.GONE);
                Log.d(TAG, "Spinner: Ẩn trường family_email_input do không chọn");
            }
        });

        // Sự kiện nút
        signin.setOnClickListener(v -> {
            Log.d(TAG, "SignIn Button: Nhấn nút Sign In");
            Intent login_form = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(login_form);
            Log.d(TAG, "SignIn Button: Chuyển sang LoginActivity");
        });

        back_btn.setOnClickListener(v -> {
            Log.d(TAG, "Back Button: Nhấn nút Back");
            finish();
            Log.d(TAG, "Back Button: Đóng RegisterActivity");
        });

        signup.setOnClickListener(v -> {
            Log.d(TAG, "SignUp Button: Nhấn nút Sign Up");
            registerUser();
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void registerUser() {
        Log.d(TAG, "registerUser: Bắt đầu quá trình đăng ký");
        String email = email_input.getText().toString().trim().toLowerCase();
        String password = password_input.getText().toString().trim();
        String fullname = fullname_input.getText().toString().trim();
        String role = usertype.getSelectedItem().toString();
        String family_email = role.equals("Người thân") ? family_email_input.getText().toString().trim().toLowerCase() : null;

        Log.d(TAG, "registerUser: Thông tin đầu vào - email=" + email + ", fullname=" + fullname + ", role=" + role + ", family_email=" + family_email);

        // Kiểm tra thông tin nhập
        if (email.isEmpty() || password.isEmpty() || fullname.isEmpty()) {
            Log.w(TAG, "registerUser: Lỗi - Thiếu thông tin đầu vào");
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Log.w(TAG, "registerUser: Lỗi - Mật khẩu dưới 6 ký tự");
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (role.equals("Người thân") && (family_email == null || family_email.isEmpty())) {
            Log.w(TAG, "registerUser: Lỗi - Thiếu email người thân");
            Toast.makeText(this, "Vui lòng nhập email người thân hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isNetworkAvailable()) {
            Log.w(TAG, "registerUser: Lỗi - Không có kết nối Internet");
            Toast.makeText(this, "Không có kết nối Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "registerUser: Kiểm tra đầu vào hợp lệ, bắt đầu tạo tài khoản");

        // Tạo tài khoản Firebase Authentication
        try {
            Log.d(TAG, "createUserWithEmail: Gọi Firebase Authentication với email=" + email);
            mauth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser firebaseUser = mauth.getCurrentUser();
                                if (firebaseUser == null) {
                                    Log.e(TAG, "createUserWithEmail: Lỗi - Không lấy được thông tin người dùng");
                                    Toast.makeText(RegisterActivity.this, "Lỗi: Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String userId = firebaseUser.getUid();
                                Log.d(TAG, "createUserWithEmail: UID người dùng: " + userId);

                                Map<String, Object> user = new HashMap<>();
                                user.put("fullname", fullname);
                                user.put("email", email);
                                user.put("role", role);
                                if (role.equals("Người thân")) {
                                    user.put("family_email", family_email);
                                }
                                Log.d(TAG, "createUserWithEmail: Dữ liệu người dùng: " + user.toString());

                                // Lưu thông tin người dùng vào Firestore
                                db.collection("users").document(userId)
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.i(TAG, "Firestore: Lưu thông tin người dùng thành công cho UID: " + userId);
                                            if (role.equals("Người thân")) {
                                                Log.i(TAG, "Firestore: Đăng ký với vai trò Người thân, kiểm tra family_email: " + family_email);
                                                // Kiểm tra sự tồn tại của family_email
                                                db.collection("users")
                                                        .whereEqualTo("email", family_email)
                                                        .get()
                                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                                            if (!queryDocumentSnapshots.isEmpty()) {
                                                                String familyUserId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                                                Log.d(TAG, "Firestore: Tìm thấy familyUserId: " + familyUserId + " cho family_email: " + family_email);
                                                                Map<String, Object> relationship = new HashMap<>();
                                                                relationship.put("userId", familyUserId);
                                                                relationship.put("familyUserId", userId);
                                                                Log.d(TAG, "Firestore: Dữ liệu mối quan hệ: " + relationship.toString());

                                                                // Lưu mối quan hệ vào collection relationships
                                                                db.collection("relationships")
                                                                        .add(relationship)
                                                                        .addOnSuccessListener(documentReference -> {
                                                                            Log.i(TAG, "Firestore: Lưu mối quan hệ thành công, ID: " + documentReference.getId());
                                                                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                                            finish();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Log.e(TAG, "Firestore: Lỗi lưu mối quan hệ: ", e);
                                                                            // Xóa tài khoản Authentication và thông tin người dùng
                                                                            firebaseUser.delete()
                                                                                    .addOnCompleteListener(deleteTask -> {
                                                                                        if (deleteTask.isSuccessful()) {
                                                                                            Log.d(TAG, "Firestore: Đã xóa tài khoản Authentication do lỗi lưu mối quan hệ");
                                                                                        }
                                                                                    });
                                                                            db.collection("users").document(userId).delete();
                                                                            Toast.makeText(RegisterActivity.this, "Lỗi lưu mối quan hệ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                                        });
                                                            } else {
                                                                Log.w(TAG, "Firestore: Không tìm thấy người dùng với email: " + family_email);
                                                                // Xóa tài khoản Authentication và thông tin người dùng
                                                                firebaseUser.delete()
                                                                        .addOnCompleteListener(deleteTask -> {
                                                                            if (deleteTask.isSuccessful()) {
                                                                                Log.d(TAG, "Firestore: Đã xóa tài khoản Authentication do family_email không tồn tại");
                                                                            }
                                                                        });
                                                                db.collection("users").document(userId).delete();
                                                                Toast.makeText(RegisterActivity.this, "Email người thân không tồn tại: " + family_email, Toast.LENGTH_LONG).show();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Firestore: Lỗi kiểm tra family_email: ", e);
                                                            // Xóa tài khoản Authentication và thông tin người dùng
                                                            firebaseUser.delete()
                                                                    .addOnCompleteListener(deleteTask -> {
                                                                        if (deleteTask.isSuccessful()) {
                                                                            Log.d(TAG, "Firestore: Đã xóa tài khoản Authentication do lỗi kiểm tra family_email");
                                                                        }
                                                                    });
                                                            db.collection("users").document(userId).delete();
                                                            Toast.makeText(RegisterActivity.this, "Lỗi kiểm tra email người thân: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                        });
                                            } else {
                                                Log.i(TAG, "Firestore: Đăng ký với vai trò Người dùng bình thường");
                                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Firestore: Lỗi lưu thông tin người dùng: ", e);
                                            firebaseUser.delete()
                                                    .addOnCompleteListener(deleteTask -> {
                                                        if (deleteTask.isSuccessful()) {
                                                            Log.d(TAG, "Firestore: Đã xóa tài khoản Authentication do lỗi Firestore");
                                                        } else {
                                                            Log.e(TAG, "Firestore: Lỗi xóa tài khoản Authentication: ", deleteTask.getException());
                                                        }
                                                    });
                                            Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                Log.e(TAG, "createUserWithEmail: Đăng ký thất bại: ", task.getException());
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                                if (task.getException() instanceof FirebaseAuthException) {
                                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                                    Log.e(TAG, "createUserWithEmail: Mã lỗi: " + errorCode);
                                    switch (errorCode) {
                                        case "ERROR_EMAIL_ALREADY_IN_USE":
                                            errorMessage = "Email đã được sử dụng";
                                            break;
                                        case "ERROR_INVALID_EMAIL":
                                            errorMessage = "Email không hợp lệ";
                                            break;
                                        case "ERROR_WEAK_PASSWORD":
                                            errorMessage = "Mật khẩu quá yếu";
                                            break;
                                        case "ERROR_OPERATION_NOT_ALLOWED":
                                            errorMessage = "Tính năng đăng ký bị vô hiệu hóa";
                                            break;
                                    }
                                }
                                Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Tạo tài khoản thất bại: ", e);
                            Toast.makeText(RegisterActivity.this, "Tạo tài khoản thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "createUserWithEmail: Ngoại lệ không mong muốn: ", e);
            Toast.makeText(RegisterActivity.this, "Lỗi đăng ký: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}