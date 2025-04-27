package com.sweetlove.directdetection.Controller;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName().toString();
    private TextInputEditText fullNameEditText, emailEditText;
    private Button saveButton, logout_btn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullNameEditText = view.findViewById(R.id.fullNameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        saveButton = view.findViewById(R.id.saveButton);
        logout_btn = view.findViewById(R.id.logout_btn);

        // Load thông tin người dùng
        loadUserInfo();

        saveButton.setOnClickListener(v -> saveUserInfo());
        logout_btn.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "loadUserInfo: UID=" + user.getUid());
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullname");
                            String email = documentSnapshot.getString("email");
                            fullNameEditText.setText(fullName);
                            emailEditText.setText(email);
                            Log.d(TAG, "loadUserInfo: Loaded fullname=" + fullName + ", email=" + email);
                        } else {
                            Log.w(TAG, "loadUserInfo: Document không tồn tại");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "loadUserInfo: Lỗi tải thông tin: ", e));
        } else {
            Log.w(TAG, "loadUserInfo: Người dùng chưa đăng nhập");
        }
    }

    private void saveUserInfo() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "saveUserInfo: Lưu thông tin cho UID=" + user.getUid());

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(!documentSnapshot.getData().isEmpty()){
                            String role = documentSnapshot.getString("role");

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fullname", fullName);
                            userData.put("email", email);
                            userData.put("role", role);

                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "saveUserInfo: Lưu thông tin thành công");
                                        Toast.makeText(getContext(), "Lưu thông tin thành công", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "saveUserInfo: Lỗi lưu thông tin: ", e);
                                        Toast.makeText(getContext(), "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                        else{
                            Log.w(TAG, "Dữ liệu role cũ không tồn tại");
                        }
                    });
        } else {
            Log.w(TAG, "saveUserInfo: Người dùng chưa đăng nhập");
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout(){
        mAuth.signOut();
        Log.d(TAG, "clearSavedCredentials: Xóa thông tin đăng nhập đã lưu");
        SharedPreferences preferences = getContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "clearSavedCredentials: Đã xóa SharedPreferences");
        requireActivity().finish();
    }
}