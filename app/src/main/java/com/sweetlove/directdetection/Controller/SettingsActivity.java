package com.sweetlove.directdetection.Controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.sweetlove.directdetection.R;
import com.sweetlove.directdetection.Utils.LanguageManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ConstraintLayout vn_language;
    private ConstraintLayout en_language;
    private Toolbar toolbar;
    private SeekBar volume;
    private AudioManager audioManager;
    private TextView volumeText;
    private Button logoutBtn;
    private LanguageManager languageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Khởi tạo LanguageManager
        languageManager = LanguageManager.getInstance(this);

        // 1. View binding
        toolbar = findViewById(R.id.toolbar_settings);
        vn_language = findViewById(R.id.vn_language);
        en_language = findViewById(R.id.en_language);
        volume = findViewById(R.id.volume_value);
        volumeText = findViewById(R.id.volumeText);
        logoutBtn = findViewById(R.id.logout_btn);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 2. Toolbar back
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 3. Click chọn ngôn ngữ
        vn_language.setOnClickListener(v -> {
            languageManager.setLanguage(this, "vi");
            recreate();
        });
        en_language.setOnClickListener(v -> {
            languageManager.setLanguage(this, "en");
            recreate();
        });

        // Hiển thị tick cho ngôn ngữ hiện tại
        updateLanguageSelection();

        // 4. Thiết lập âm lượng
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume.setProgress(currentVolume);
        volumeText.setText(getString(R.string.volume) + ": " + currentVolume);
        
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    volumeText.setText(getString(R.string.volume) + ": " + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 5. Đăng xuất
        logoutBtn.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().clear().apply();
            finishAffinity();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLanguageSelection();
    }

    private void updateLanguageSelection() {
        String currentLang = languageManager.getCurrentLanguage();
        
        // Tìm ImageView trong mỗi ConstraintLayout
        ImageView vnCheck = findViewById(R.id.icon_vn_check);
        ImageView enCheck = findViewById(R.id.icon_en_check);
        
        if (vnCheck != null && enCheck != null) {
            vnCheck.setVisibility(currentLang.equals("vi") ? View.VISIBLE : View.GONE);
            enCheck.setVisibility(currentLang.equals("en") ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
