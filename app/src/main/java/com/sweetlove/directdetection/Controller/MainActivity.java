package com.sweetlove.directdetection.Controller;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.sweetlove.directdetection.R;
import com.sweetlove.directdetection.Utils.LanguageManager;

public class MainActivity extends AppCompatActivity {
    private LanguageManager languageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo và áp dụng ngôn ngữ
        languageManager = LanguageManager.getInstance(this);
        languageManager.applyLanguage(this);
    }
} 