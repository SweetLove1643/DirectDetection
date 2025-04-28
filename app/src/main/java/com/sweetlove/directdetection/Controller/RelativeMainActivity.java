package com.sweetlove.directdetection.Controller;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sweetlove.directdetection.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class RelativeMainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private static final String TAG = "RelativeMainActivity";
    private WebSocketClient webSocketClient;
    private String IP_ADDRESS = "192.168.1.115";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Starting initialization of RelativeMainActivity");
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "onCreate: FirebaseAuth initialized, user=" + (currentUser != null ? currentUser.getUid() : "null"));

        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_relative);
            Log.d(TAG, "onCreate: Layout activity_relative set successfully");

            // Thiết lập Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "onCreate: Toolbar set as ActionBar");
            } else {
                Log.w(TAG, "onCreate: Toolbar (R.id.toolbar) not found");
            }

            // Thiết lập điều hướng
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    NavigationUI.setupWithNavController(bottomNav, navController);
                    Log.d(TAG, "onCreate: BottomNavigationView synchronized with NavController");

                    bottomNav.setOnItemSelectedListener(item -> {
                        int itemId = item.getItemId();
                        Log.d(TAG, "BottomNavigation: Item selected, itemId=" + itemId);
                        if (itemId == R.id.notificationFragment) {
                            Log.d(TAG, "BottomNavigation: Navigating to NotificationFragment");
                            navController.navigate(R.id.notificationFragment);
                            return true;
                        } else if (itemId == R.id.settingsFragment) {
                            Log.d(TAG, "BottomNavigation: Navigating to SettingsFragment");
                            navController.navigate(R.id.settingsFragment);
                            return true;
                        } else {
                            Log.w(TAG, "BottomNavigation: Unknown item selected, itemId=" + itemId);
                            return false;
                        }
                    });
                } else {
                    Log.w(TAG, "onCreate: BottomNavigationView (R.id.bottom_navigation) not found");
                }
            } else {
                Log.e(TAG, "onCreate: NavHostFragment (R.id.nav_host_fragment) not found");
                Toast.makeText(this, "Error: NavHostFragment not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Thiết lập Notification Channel
            Log.d(TAG, "onCreate: Creating Notification Channel 'warning'");
            NotificationChannel channel = new NotificationChannel("warning", "Warning", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Receive updates about relatives");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "onCreate: Notification Channel 'warning' created");

            // Thiết lập WebSocket
            Log.d(TAG, "onCreate: Initializing WebSocket connection");
            initializeWebSocket();

        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error initializing RelativeMainActivity: ", e);
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeWebSocket() {
        Log.d(TAG, "initializeWebSocket: Starting WebSocket initialization");
        try {
            URI uri = new URI("ws://" + IP_ADDRESS + ":8080/api/detection");
            Log.d(TAG, "initializeWebSocket: WebSocket URI created: " + uri.toString());

            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "WebSocket: Connection opened, status=" + handshakedata.getHttpStatus() + ", message=" + handshakedata.getHttpStatusMessage());
                    try {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            JSONObject message = new JSONObject();
                            message.put("UID", user.getUid());
                            Log.d(TAG, "WebSocket: Sending UID message: " + message.toString());
                            this.send(message.toString());
                        } else {
                            Log.w(TAG, "WebSocket: No user logged in, cannot send UID");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "WebSocket: Error creating UID message: ", e);
                        throw new RuntimeException(e);
                    }
                    runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "WebSocket connected", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "WebSocket: Received message: " + message);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            if (message.trim().startsWith("{") && message.trim().endsWith("}")) {
                                Log.d(TAG, "WebSocket: Message is valid JSON, parsing...");
                                JSONObject msgJson = new JSONObject(message);
                                String title = msgJson.optString("title", "Unknown");
                                String content = msgJson.optString("message", "No message");
                                String date = msgJson.optString("date", "No date");
                                Log.d(TAG, "WebSocket: Parsed JSON - title=" + title + ", message=" + content + ", date=" + date);

                                NotificationCompat.Builder builder = new NotificationCompat.Builder(RelativeMainActivity.this, "warning")
                                        .setSmallIcon(R.drawable.ic_notification)
                                        .setContentTitle("Update from: " + title)
                                        .setContentText(content + " " + date)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                Log.d(TAG, "WebSocket: Notification builder created");

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(RelativeMainActivity.this);
                                Log.d(TAG, "WebSocket: Checking POST_NOTIFICATIONS permission");
                                if (ContextCompat.checkSelfPermission(RelativeMainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    Log.w(TAG, "WebSocket: POST_NOTIFICATIONS permission not granted, requesting...");
                                    ActivityCompat.requestPermissions(RelativeMainActivity.this,
                                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                                } else {
                                    Log.d(TAG, "WebSocket: POST_NOTIFICATIONS permission granted");
                                    NotificationChannel channel = notificationManager.getNotificationChannel("warning");
                                    if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
                                        Log.d(TAG, "WebSocket: Sending notification with ID=1");
                                        notificationManager.notify(1, builder.build());
                                    } else {
                                        Log.w(TAG, "WebSocket: Notification channel 'warning' disabled or not found");
                                    }
                                }
                            } else {
                                Log.w(TAG, "WebSocket: Message is not valid JSON: " + message);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "WebSocket: Error parsing message JSON: ", e);
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket: Connection closed, code=" + code + ", reason=" + reason + ", remote=" + remote);
                    runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "WebSocket closed: " + reason, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket: Error occurred: ", ex);
                    runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "WebSocket error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                }
            };

            Log.d(TAG, "initializeWebSocket: Connecting to WebSocket");
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "initializeWebSocket: Invalid URI: ", e);
            runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "Invalid WebSocket URI: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void requestSmsPermission() {
        Log.d(TAG, "requestSmsPermission: Checking READ_SMS permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestSmsPermission: READ_SMS permission not granted, requesting...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
            Log.d(TAG, "requestSmsPermission: READ_SMS permission already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Received permission result, requestCode=" + requestCode);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: READ_SMS permission granted");
            } else {
                Log.w(TAG, "onRequestPermissionsResult: READ_SMS permission denied");
                Toast.makeText(this, "READ_SMS permission denied, some features may not work.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: POST_NOTIFICATIONS permission granted");
            } else {
                Log.w(TAG, "onRequestPermissionsResult: POST_NOTIFICATIONS permission denied");
                Toast.makeText(this, "POST_NOTIFICATIONS permission denied, notifications will not be shown.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "onRequestPermissionsResult: Unknown request code: " + requestCode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity is being destroyed");
        if (webSocketClient != null) {
            Log.d(TAG, "onDestroy: Closing WebSocket connection");
            webSocketClient.close();
            webSocketClient = null;
        }
    }
}