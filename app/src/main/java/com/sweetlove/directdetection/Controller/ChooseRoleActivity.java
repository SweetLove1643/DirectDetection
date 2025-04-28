package com.sweetlove.directdetection.Controller;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

import com.sweetlove.directdetection.R;

public class ChooseRoleActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelectionActivity";
    private RadioGroup roleRadioGroup;
    private RadioButton radioUser, radioRelative;
    private TextInputLayout relativeEmailLayout;
    private TextInputEditText relativeEmailInput;
    private Button confirmButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_role);
        Log.d(TAG, "onCreate: Starting RoleSelectionActivity");

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Gắn kết các view
        roleRadioGroup = findViewById(R.id.role_radio_group);
        radioUser = findViewById(R.id.radio_user);
        radioRelative = findViewById(R.id.radio_relative);
        relativeEmailLayout = findViewById(R.id.relative_email_layout);
        relativeEmailInput = findViewById(R.id.relative_email_input);
        confirmButton = findViewById(R.id.confirm_button);

        // Ẩn trường email ban đầu
        relativeEmailLayout.setVisibility(View.GONE);
        Log.d(TAG, "onCreate: Relative email input hidden by default");

        // Xử lý chọn vai trò
        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_relative) {
                relativeEmailLayout.setVisibility(View.VISIBLE);
                Log.d(TAG, "onCheckedChanged: Relative role selected, showing email input");
            } else {
                relativeEmailLayout.setVisibility(View.GONE);
                relativeEmailInput.setText("");
                Log.d(TAG, "onCheckedChanged: User role selected, hiding email input");
            }
        });

        // Xử lý nút xác nhận
        confirmButton.setOnClickListener(v -> saveRole());
    }

    private void saveRole() {
        Log.d(TAG, "saveRole: Attempting to save role");
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "saveRole: No authenticated user");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String role = radioUser.isChecked() ? "user" : "relative";
        String relativeEmail;

        if (radioRelative.isChecked()) {
            relativeEmail = relativeEmailInput.getText() != null ? relativeEmailInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(relativeEmail)) {
                relativeEmailLayout.setError(getString(R.string.email_required));
                Log.w(TAG, "saveRole: Relative email is empty");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(relativeEmail).matches()) {
                relativeEmailLayout.setError(getString(R.string.invalid_email));
                Log.w(TAG, "saveRole: Invalid email format: " + relativeEmail);
                return;
            }
        } else {
            relativeEmail = null;
        }

        // Lưu vào Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("role", role);
        if (relativeEmail != null) {
            userData.put("relativeEmail", relativeEmail);
        }

        Log.d(TAG, "saveRole: Saving user data to Firestore: " + userData);
        db.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "saveRole: Role saved successfully for user: " + user.getUid());
                    Toast.makeText(this, R.string.role_saved, Toast.LENGTH_SHORT).show();

                    // Nếu là người thân, lưu mối quan hệ vào collection relationships
                    if (role.equals("relative")) {
                        saveRelationship(user.getUid(), relativeEmail);
                    } else {
                        // Chuyển đến màn hình chính
                        startActivity(new Intent(this, RecognitionActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveRole: Error saving role: ", e);
                    Toast.makeText(this, String.format(getString(R.string.error_saving_role), e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void saveRelationship(String userId, String relativeEmail) {
        Log.d(TAG, "saveRelationship: Saving relationship for user: " + userId + ", relativeEmail: " + relativeEmail);
        // Tìm UID của người dùng có email người thân
        db.collection("users")
                .whereEqualTo("email", relativeEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "saveRelationship: No user found with email: " + relativeEmail);
                        Toast.makeText(this, "Không tìm thấy người dùng với email: " + relativeEmail, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String relativeUid = queryDocumentSnapshots.getDocuments().get(0).getString("uid");
                    Map<String, Object> relationship = new HashMap<>();
                    relationship.put("userId", relativeUid); // Người thân liên kết với user chính
                    relationship.put("familyUserId", userId); // UID của người thân

                    db.collection("relationships")
                            .document()
                            .set(relationship)
                            .addOnSuccessListener(aVoid -> {
                                Log.i(TAG, "saveRelationship: Relationship saved successfully");
                                Toast.makeText(this, "Mối quan hệ đã được lưu", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, RelativeMainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "saveRelationship: Error saving relationship: ", e);
                                Toast.makeText(this, "Lỗi khi lưu mối quan hệ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveRelationship: Error finding user with email: " + relativeEmail, e);
                    Toast.makeText(this, "Lỗi khi tìm người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}