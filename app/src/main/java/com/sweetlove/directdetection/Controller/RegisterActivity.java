package com.sweetlove.directdetection.Controller;

import android.content.Intent;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName().toString();
    private TextView fullname_input, email_input, password_input, forgotpassword, signin, family_email_input;
    private Spinner usertype;
    private ImageButton back_btn;
    private Button signup;
    private CheckBox rememberme;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullname_input = findViewById(R.id.name_input);
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        family_email_input = findViewById(R.id.family_email_input);

        usertype = findViewById(R.id.spinner_usertype);

        signin = findViewById(R.id.signin_btn);

        signup = findViewById(R.id.signup_btn);
        back_btn = findViewById(R.id.back_btn);

        String[] userTypes = {"Người dùng", "Người thân"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_selected_item,
                userTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usertype.setAdapter(adapter);
        usertype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                if (selectedItem.equals("Người thân")) {
                    family_email_input.setVisibility(View.VISIBLE);
                } else {
                    family_email_input.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                family_email_input.setVisibility(View.GONE);
            }
        });


        signin.setOnClickListener(v -> {
            Intent login_form = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(login_form);
        });

        back_btn.setOnClickListener(v -> finish());

        signup.setOnClickListener(v -> registerUser());


    }

    private void registerUser(){
        String email = email_input.getText().toString().trim();
        String password = password_input.getText().toString().trim();
        String fullname = fullname_input.getText().toString().trim();
        String role = usertype.getSelectedItem().toString();
        String family_email = role.equals("Người thân") ? family_email_input.getText().toString().trim() : null;

        // Kiểm tra thông tin nhập
        if (email.isEmpty() || password.isEmpty() || fullname.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, usertype.getSelectedItem().toString() + email + password);

        if (role.equals("Người thân") && (family_email == null || family_email.isEmpty())) {
            Toast.makeText(this, "Vui lòng nhập email của người thân", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tài khoản Firebase Authentication
        mauth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser firebaseUser = mauth.getCurrentUser();
                            if (firebaseUser == null) {
                                Toast.makeText(RegisterActivity.this, "Lỗi: Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String userId = mauth.getCurrentUser().getUid();
                            Map<String, Object> user = new HashMap<>();
                            user.put("fullname", fullname);
                            user.put("email", email);
                            user.put("role", role);

                            // Gửi email xác minh (nếu cần)
                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Email xác minh đã được gửi", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e(TAG, "Lỗi gửi email xác minh: ", verifyTask.getException());
                                        }
                                    });

                            // Lưu thông tin người dùng vào Firestore
                            db.collection("users").document(userId)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        if (role.equals("Người thân")) {
                                            // Tìm UID của người thân dựa trên email
                                            db.collection("users")
                                                    .whereEqualTo("email", family_email)
                                                    .get()
                                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                                        if (!queryDocumentSnapshots.isEmpty()) {
                                                            String familyUserId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                                            Map<String, Object> relationship = new HashMap<>();
                                                            relationship.put("userId", familyUserId);
                                                            relationship.put("familyUserId", userId);
                                                            db.collection("relationships").add(relationship);
                                                        } else {
                                                            Toast.makeText(RegisterActivity.this, "Không tìm thấy người dùng với email này", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    })
                                    .addOnFailureListener(e -> {
                                        // Xóa tài khoản Authentication nếu Firestore thất bại
                                        firebaseUser.delete()
                                                .addOnCompleteListener(deleteTask -> {
                                                    if (deleteTask.isSuccessful()) {
                                                        Log.d(TAG, "Đã xóa tài khoản Authentication do lỗi Firestore");
                                                    }
                                                });
                                        Log.e(TAG, "Lỗi lưu Firestore: ", e);
                                        Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            Log.e(TAG, "Đăng ký thất bại: ", task.getException());
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}