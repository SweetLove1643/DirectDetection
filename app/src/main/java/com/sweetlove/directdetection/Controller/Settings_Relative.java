package com.sweetlove.directdetection.Controller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.sweetlove.directdetection.R;

public class Settings_Relative extends Fragment {

    private EditText fullname_input, emailInput;
    private Button saveButton;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_EMAIL = "email";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        fullname_input = view.findViewById(R.id.fullname);
        emailInput = view.findViewById(R.id.email_input);
        saveButton = view.findViewById(R.id.save_button);

        preferences = requireActivity().getSharedPreferences(PREF_NAME, requireActivity().MODE_PRIVATE);

        fullname_input.setText(preferences.getString(KEY_PHONE_NUMBER, ""));
        emailInput.setText(preferences.getString(KEY_EMAIL, ""));

        saveButton.setOnClickListener(v -> {
            String phoneNumber = fullname_input.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            if (phoneNumber.isEmpty() && email.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập họ tên hoặc email", Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(KEY_PHONE_NUMBER, phoneNumber);
                editor.putString(KEY_EMAIL, email);
                editor.apply();
                Toast.makeText(getContext(), "Đã lưu thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}