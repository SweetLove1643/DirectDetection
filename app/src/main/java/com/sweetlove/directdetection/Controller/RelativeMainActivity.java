package com.sweetlove.directdetection.Controller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sweetlove.directdetection.R;

public class RelativeMainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private final String TAG = this.getClass().getSimpleName().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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