package com.sweetlove.directdetection.Controller;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextView fullname_input, email_input, password_input, forgotpassword, signin, family_email_input;
    private Spinner usertype;
    private Spinner roleSpinner;
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
            finish();
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

        forgotpassword = findViewById(R.id.forgotpassword_btn);
        signin = findViewById(R.id.signin_btn);

        signup = findViewById(R.id.signup_btn);
        back_btn = findViewById(R.id.back_btn);
        rememberme = findViewById(R.id.rememberme_checkbox);

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
        String role = roleSpinner.getSelectedItem().toString();
        String family_email = role.equals("Người thân") ? family_email_input.getText().toString().trim() : null;

        // Kiểm tra thông tin nhập
        if (email.isEmpty() || password.isEmpty() || fullname.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (role.equals("Người thân") && (family_email == null || family_email.isEmpty())) {
            Toast.makeText(this, "Vui lòng nhập email của người thân", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo tài khoản Firebase Authentication
        mauth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mauth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("fullname", fullname);
                        user.put("email", email);
                        user.put("role", role);

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
                                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}