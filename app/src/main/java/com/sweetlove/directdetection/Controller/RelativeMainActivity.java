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
    private final String TAG = this.getClass().getSimpleName().toString();
    private WebSocketClient webSocketClient;
    private String IP_ADDRESS = "192.168.0.104";
    private FirebaseAuth mauth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mauth = FirebaseAuth.getInstance();

        Log.d(TAG, "onCreate: Bắt đầu khởi tạo RelativeMainActivity");
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_relative);
            Log.d(TAG, "onCreate: Đã set layout activity_relative");

            // Thiết lập Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "onCreate: Đã thiết lập Toolbar làm ActionBar");
            } else {
                Log.w(TAG, "onCreate: Không tìm thấy Toolbar (R.id.toolbar)");
            }

            // Thiết lập điều hướng
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    // Đồng bộ BottomNavigationView với NavController
                    NavigationUI.setupWithNavController(bottomNav, navController);
                    Log.d(TAG, "onCreate: Đã thiết lập BottomNavigationView với NavController");

                    // Thêm sự kiện click cho BottomNavigationView
                    bottomNav.setOnItemSelectedListener(item -> {
                        int itemId = item.getItemId();
                        Log.d(TAG, "BottomNavigation: Item được chọn, itemId=" + itemId);

                        // Điều hướng đến fragment tương ứng
                        if (itemId == R.id.notificationFragment) {
                            Log.d(TAG, "BottomNavigation: Chuyển đến NotificationFragment");
                            navController.navigate(R.id.notificationFragment);
                            return true;
                        } else if (itemId == R.id.settingsFragment) {
                            Log.d(TAG, "BottomNavigation: Chuyển đến SettingsFragment");
                            navController.navigate(R.id.settingsFragment);
                            return true;
                        } else {
                            Log.w(TAG, "BottomNavigation: Item không xác định, itemId=" + itemId);
                            return false;
                        }
                    });
                } else {
                    Log.w(TAG, "onCreate: Không tìm thấy BottomNavigationView (R.id.bottom_navigation)");
                }
            } else {
                Log.e(TAG, "onCreate: Không tìm thấy NavHostFragment (R.id.nav_host_fragment)");
                Toast.makeText(this, "Lỗi: Không tìm thấy NavHostFragment", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Tạm vô hiệu hóa yêu cầu quyền SMS để kiểm tra crash
            // Log.d(TAG, "onCreate: Gọi requestSmsPermission");
            // requestSmsPermission();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Lỗi khởi tạo RelativeMainActivity: ", e);
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        NotificationChannel Tech = new NotificationChannel("warning", "Warning", NotificationManager.IMPORTANCE_DEFAULT);

        Tech.setDescription("Nhận tin tức về người thân");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(Tech);


        try {
            URI uri = new URI("ws://" + IP_ADDRESS + ":8080/api/detection");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    try {
                        FirebaseUser user = mauth.getCurrentUser();
                        JSONObject mess = new JSONObject();
                        mess.put("UID", user.getUid().toString());
                        this.send(mess.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onMessage(String message) {
                    new Handler(Looper.getMainLooper()).post(() ->{
                        try {
                            Log.i(this.getClass().getSimpleName(), "message: " + message);
                            if(message.trim().startsWith("{") && message.trim().endsWith("}")){
                                JSONObject msg_json = new JSONObject(message);
                                String title = msg_json.getString("title");
                                String mess = msg_json.getString("message");
                                String data = msg_json.getString("date");

                                NotificationCompat.Builder builder = new  NotificationCompat.Builder(RelativeMainActivity.this, mess);
                                builder.setSmallIcon(R.color.white);
                                builder.setContentTitle("Tin tức từ: " + title);
                                builder.setContentText(mess + " " + data);

                                NotificationManagerCompat notiMan = NotificationManagerCompat.from(RelativeMainActivity.this);

                                ActivityCompat.requestPermissions(RelativeMainActivity.this,
                                        new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
                                if(ActivityCompat.checkSelfPermission(RelativeMainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) == 0){
                                    NotificationChannel activeChannel = notiMan.getNotificationChannel("warning");
                                    if(activeChannel != null && activeChannel.getImportance() != NotificationManager.IMPORTANCE_NONE){
                                        notiMan.notify(1, builder.build());
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket closed: " + reason + ", code: " + code);
                    runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "WebSocket closed: " + reason, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error: " + ex.getMessage());
                    runOnUiThread(() -> Toast.makeText(RelativeMainActivity.this, "WebSocket error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                }
            };

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestSmsPermission() {
        Log.d(TAG, "requestSmsPermission: Kiểm tra quyền READ_SMS");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestSmsPermission: Quyền READ_SMS chưa được cấp, yêu cầu quyền");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
            Log.d(TAG, "requestSmsPermission: Quyền READ_SMS đã được cấp");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Nhận kết quả yêu cầu quyền, requestCode=" + requestCode);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Quyền READ_SMS được cấp");
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Quyền READ_SMS bị từ chối");
//                 Thông báo cho người dùng
                 Toast.makeText(this, "Quyền đọc SMS bị từ chối, một số tính năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "onRequestPermissionsResult: Mã yêu cầu không khớp, requestCode=" + requestCode);
        }
    }

}