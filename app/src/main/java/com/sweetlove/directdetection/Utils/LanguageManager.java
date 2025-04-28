package com.sweetlove.directdetection.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LanguageManager {
    private static final String LANGUAGE_KEY = "app_language";
    private static LanguageManager instance;
    private final SharedPreferences preferences;

    private LanguageManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setLanguage(Context context, String languageCode) {
        // Lưu ngôn ngữ mới vào SharedPreferences
        preferences.edit().putString(LANGUAGE_KEY, languageCode).apply();

        // Cập nhật locale
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public String getCurrentLanguage() {
        return preferences.getString(LANGUAGE_KEY, "vi"); // Mặc định là tiếng Việt
    }

    public void applyLanguage(Context context) {
        String language = getCurrentLanguage();
        setLanguage(context, language);
    }
} 