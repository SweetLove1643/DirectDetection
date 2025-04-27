package com.sweetlove.directdetection.Controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ConstraintLayout vn_language;
    private ConstraintLayout en_language;

    private Toolbar toolbar;
    private SeekBar volume;
    private AudioManager audioManager;
    private TextView volumeText;
    private Button logout_btn;
    FirebaseAuth mauth;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        volumeText = findViewById(R.id.volumeText);
        toolbar = findViewById(R.id.toolbar_settings);
        vn_language = findViewById(R.id.vn_language);
        en_language = findViewById(R.id.en_language);
        logout_btn = findViewById(R.id.logout_btn);
        volume = findViewById(R.id.volume_value);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeText.setText(getString(R.string.volume) + ": " + currentVolume);
        volume.setProgress(currentVolume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    volumeText.setText(getString(R.string.volume) + ": " + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Xử lý chuyển đổi ngôn ngữ
        vn_language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocale("vi");
            }
        });

        en_language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocale("en");
            }
        });
        logout_btn.setOnClickListener(v -> {
            logout();
        });
    }

    private void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        
        // Khởi động lại activity để áp dụng ngôn ngữ mới
        Intent refresh = new Intent(this, SettingsActivity.class);
        startActivity(refresh);
        finish();
    }
    private void logout(){
        mauth.signOut();

        SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        finish();
    }
}