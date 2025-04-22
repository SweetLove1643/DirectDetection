package com.sweetlove.directdetection.Controller;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.sweetlove.directdetection.R;

public class SettingsActivity extends AppCompatActivity {

    private ConstraintLayout vn_language;
    private ConstraintLayout en_language;
    private Toolbar toolbar;
    private SeekBar volume;
    private AudioManager audioManager;
    TextView volumeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        volumeText = findViewById(R.id.volumeText);
        toolbar = findViewById(R.id.toolbar_settings);
        vn_language = findViewById(R.id.vn_language);
        en_language = findViewById(R.id.en_language);
        volume = findViewById(R.id.volume_value);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

//        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

//        volume.setMax(maxVolume);
        volume.setProgress(currentVolume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    volumeText.setText("Âm lượng: " + progress);
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

    }
}