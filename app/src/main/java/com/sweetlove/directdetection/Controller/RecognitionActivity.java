package com.sweetlove.directdetection.Controller;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sweetlove.directdetection.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecognitionActivity extends AppCompatActivity {

    private ImageButton help_btn, action_btn, setting_btn;
    private PreviewView camera_view;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = RecognitionActivity.class.getSimpleName();
    private WebSocketClient webSocketClient;
    private String clientId;
    private long lastFrameTime = 0;
    private static final long FRAME_INTERVAL_MS = 2000; // Gửi frame mỗi 2000ms
    private static final long RECONNECT_INTERVAL_MS = 5000; // Thử kết nối lại sau 5s
    private static final long SPEAK_INTERVAL_MS = 3000; // Chỉ phát âm mỗi 3s
    private Handler reconnectHandler;
    private Runnable reconnectRunnable;
    private TextToSpeech textToSpeech;
    private String lastSpokenClass = "";
    private long lastSpeakTime = 0;
    private String IP_ADDRESS = "192.168.1.115";
    private final Map<String, String> classTranslations = new HashMap<>();
    private FirebaseAuth mauth;
    private FirebaseFirestore db;
    private ExecutorService executorService;

    private String[] warning = {
//            "người",
//            "điện thoại di động"
    };

    @Override
    protected void onStart() {
        super.onStart();

        try{
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String email_saved = preferences.getString("email", null);
            String password_saved = preferences.getString("password", null);
            if (email_saved == null && password_saved != null) {
                Log.w(TAG, "Logout Recognition");
                finish();
            }

        } catch (Exception e) {
            finish();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recognition);

        mauth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        help_btn = findViewById(R.id.helpButton);
        action_btn = findViewById(R.id.actionBtn);
        setting_btn = findViewById(R.id.settingsButton);
        camera_view = findViewById(R.id.imageScan);

        help_btn.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        setting_btn.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        reconnectHandler = new Handler(Looper.getMainLooper());
        cameraExecutor = Executors.newSingleThreadExecutor();
        executorService = Executors.newSingleThreadExecutor(); // Khởi tạo ExecutorService
        Log.d(TAG, "onCreate: ExecutorService initialized for camera and email");


        try{
            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String email_saved = preferences.getString("email", null);
            String password_saved = preferences.getString("password", null);
            if (email_saved == null && password_saved != null) {
                Log.w(TAG, "Logout Recognition");
                finish();
            }

        } catch (Exception e) {
            finish();
            throw new RuntimeException(e);
        }

        // Khởi tạo bản dịch 80 nhãn COCO
        initializeClassTranslations();

        // Khởi tạo TextToSpeech với ngôn ngữ tiếng Việt
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {

                SharedPreferences preferences = getSharedPreferences("app_language", MODE_PRIVATE);
                String language = preferences.getString("app_language", "vi");
                if(language == "vi"){
                    int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TextToSpeech: Tiếng Việt không được hỗ trợ");
                        Toast.makeText(this, "Tiếng Việt không được hỗ trợ trên thiết bị này", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.i(TAG, "TextToSpeech initialized successfully with Vietnamese");
                    }
                }else{
                    int result = textToSpeech.setLanguage(new Locale("en", "EN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TextToSpeech: English not support");
                        Toast.makeText(this, "English not support in phone", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.i(TAG, "TextToSpeech initialized successfully with English");
                    }
                }


            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
                Toast.makeText(this, "Không thể khởi tạo Text-to-Speech", Toast.LENGTH_SHORT).show();
            }
        });

        // Khởi tạo runnable để thử kết nối lại
        reconnectRunnable = () -> connectWebSocket();

        // Kết nối tới server WebSocket
        connectWebSocket();

        requestCameraPermission();
    }

    private void initializeClassTranslations() {
        classTranslations.put("person", "người");
        classTranslations.put("bicycle", "xe đạp");
        classTranslations.put("car", "xe hơi");
        classTranslations.put("motorcycle", "xe máy");
        classTranslations.put("airplane", "máy bay");
        classTranslations.put("bus", "xe buýt");
        classTranslations.put("train", "tàu hỏa");
        classTranslations.put("truck", "xe tải");
        classTranslations.put("boat", "thuyền");
        classTranslations.put("traffic light", "đèn giao thông");
        classTranslations.put("fire hydrant", "vòi cứu hỏa");
        classTranslations.put("stop sign", "biển dừng");
        classTranslations.put("parking meter", "đồng hồ đỗ xe");
        classTranslations.put("bench", "ghế dài");
        classTranslations.put("bird", "chim");
        classTranslations.put("cat", "mèo");
        classTranslations.put("dog", "chó");
        classTranslations.put("horse", "ngựa");
        classTranslations.put("sheep", "cừu");
        classTranslations.put("cow", "bò");
        classTranslations.put("elephant", "voi");
        classTranslations.put("bear", "gấu");
        classTranslations.put("zebra", "ngựa vằn");
        classTranslations.put("giraffe", "hươu cao cổ");
        classTranslations.put("backpack", "ba lô");
        classTranslations.put("umbrella", "ô");
        classTranslations.put("handbag", "túi xách");
        classTranslations.put("tie", "cà vạt");
        classTranslations.put("suitcase", "vali");
        classTranslations.put("frisbee", "đĩa bay");
        classTranslations.put("skis", "ván trượt tuyết");
        classTranslations.put("snowboard", "ván trượt tuyết đơn");
        classTranslations.put("sports ball", "bóng thể thao");
        classTranslations.put("kite", "diều");
        classTranslations.put("baseball bat", "gậy bóng chày");
        classTranslations.put("baseball glove", "găng tay bóng chày");
        classTranslations.put("skateboard", "ván trượt");
        classTranslations.put("surfboard", "ván lướt sóng");
        classTranslations.put("tennis racket", "vợt tennis");
        classTranslations.put("bottle", "chai");
        classTranslations.put("wine glass", "ly rượu");
        classTranslations.put("cup", "cốc");
        classTranslations.put("fork", "nĩa");
        classTranslations.put("knife", "dao");
        classTranslations.put("spoon", "thìa");
        classTranslations.put("bowl", "bát");
        classTranslations.put("banana", "chuối");
        classTranslations.put("apple", "táo");
        classTranslations.put("sandwich", "bánh sandwich");
        classTranslations.put("orange", "cam");
        classTranslations.put("broccoli", "bông cải xanh");
        classTranslations.put("carrot", "cà rốt");
        classTranslations.put("hot dog", "xúc xích kẹp bánh");
        classTranslations.put("pizza", "pizza");
        classTranslations.put("donut", "bánh rán vòng");
        classTranslations.put("cake", "bánh ngọt");
        classTranslations.put("chair", "ghế");
        classTranslations.put("couch", "ghế sofa");
        classTranslations.put("potted plant", "cây trồng chậu");
        classTranslations.put("bed", "giường");
        classTranslations.put("dining table", "bàn ăn");
        classTranslations.put("toilet", "bồn cầu");
        classTranslations.put("tv", "tivi");
        classTranslations.put("laptop", "máy tính xách tay");
        classTranslations.put("mouse", "chuột máy tính");
        classTranslations.put("remote", "điều khiển từ xa");
        classTranslations.put("keyboard", "bàn phím");
        classTranslations.put("cell phone", "điện thoại di động");
        classTranslations.put("microwave", "lò vi sóng");
        classTranslations.put("oven", "lò nướng");
        classTranslations.put("toaster", "máy nướng bánh mì");
        classTranslations.put("sink", "bồn rửa");
        classTranslations.put("refrigerator", "tủ lạnh");
        classTranslations.put("book", "sách");
        classTranslations.put("clock", "đồng hồ");
        classTranslations.put("vase", "lọ hoa");
        classTranslations.put("scissors", "kéo");
        classTranslations.put("teddy bear", "gấu bông");
        classTranslations.put("hair drier", "máy sấy tóc");
        classTranslations.put("toothbrush", "bàn chải đánh răng");
    }

    private String translateToVietnamese(String className) {
        return classTranslations.getOrDefault(className, className);
    }

    private void connectWebSocket() {
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


                    Log.i(TAG, "WebSocket connected");
                    runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "WebSocket connected", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onMessage(String message) {
                    runOnUiThread(() -> handleMessage(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket closed: " + reason + ", code: " + code);
                    runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "WebSocket closed: " + reason, Toast.LENGTH_SHORT).show());
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error: " + ex.getMessage());
                    runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "WebSocket error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                    scheduleReconnect();
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "WebSocket connection error: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "Failed to connect to server: " + e.getMessage(), Toast.LENGTH_LONG).show());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_INTERVAL_MS);
    }

    private void handleMessage(String message) {
        try {
            JSONObject data = new JSONObject(message);

            if (data.has("client_id")) {
                clientId = data.getString("client_id");
                Log.i(TAG, "Received client_id: " + clientId);
            } else if (data.has("detections")) {
                JSONArray detections = data.getJSONArray("detections");
                StringBuilder resultText = new StringBuilder();
                for (int i = 0; i < Math.min(detections.length(), 1); i++) {
                    JSONObject detection = detections.getJSONObject(i);
                    String className = detection.getString("class");
                    double confidence = detection.getDouble("confidence");

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    String language = preferences.getString("app_language", "vi");
                    Log.d(TAG, "handleMessage: Current language: " + language);

                    String vietnameseClass = language.equals("vi") ? translateToVietnamese(className) : className;

                    resultText.append("Class: ").append(vietnameseClass)
                            .append(", Confidence: ").append(String.format("%.2f", confidence))
                            .append("\n");

                    // Cảnh báo về người thân
                    if(Arrays.asList(warning).contains(vietnameseClass)){
                        notifi_to_firestore("Cảnh báo nguy hiểm", "Phát hiện nguy hiểm: " + vietnameseClass);
                        send_relativeid_to_server();
                        send_relative_to_email(vietnameseClass);
                    }


                    // Phát âm tên class bằng tiếng Việt nếu khác với class trước đó và đủ thời gian
                    if (!className.equals(lastSpokenClass) && textToSpeech != null && System.currentTimeMillis() - lastSpeakTime > SPEAK_INTERVAL_MS) {
                        lastSpokenClass = className;
                        lastSpeakTime = System.currentTimeMillis();
                        textToSpeech.speak(vietnameseClass, TextToSpeech.QUEUE_FLUSH, null, null);
                        Log.i(TAG, "Speaking: " + vietnameseClass);
                    }
                }
                if (!resultText.toString().isEmpty()) {
                    Log.i(TAG, resultText.toString());
                    Toast.makeText(this, resultText.toString(), Toast.LENGTH_SHORT).show();
                }
            } else if (data.has("error")) {
                Log.e(TAG, data.toString());
                String error = data.getString("error");
                Log.e(TAG, "Server error: " + error);
                Toast.makeText(this, "Server error: " + error, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message: " + e.getMessage());
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(camera_view.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::processFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processFrame(ImageProxy image) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime < FRAME_INTERVAL_MS) {
            image.close();
            return;
        }
        lastFrameTime = currentTime;

        Bitmap bitmap = imageToBitmap(image);
        if (bitmap != null && webSocketClient != null && webSocketClient.isOpen()) {
            sendFrameToServer(bitmap);
        } else {
            Log.w(TAG, "Skipping frame: WebSocket not open");
        }
        image.close();
    }

    private void sendFrameToServer(Bitmap bitmap) {
        try {
            // Chuyển Bitmap thành base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            Log.i(TAG, "Sending frame: " + encodedImage.length() + " bytes");

            // Gửi frame qua WebSocket
            JSONObject data = new JSONObject();
            data.put("frame", encodedImage);
            webSocketClient.send(data.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending frame: " + e.getMessage());
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageToBitmap(ImageProxy image) {
        if (image.getImage() == null || image.getImage().getFormat() != ImageFormat.YUV_420_888) {
            return null;
        }

        try {
            ByteBuffer yBuffer = image.getImage().getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getImage().getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getImage().getPlanes()[2].getBuffer();

            int width = image.getWidth();
            int height = image.getImage().getHeight();
            int[] rgb = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int yIndex = y * width + x;
                    int uvIndex = (y / 2) * (width / 2) + (x / 2);

                    int Y = yBuffer.get(yIndex) & 0xFF;
                    int U = uBuffer.get(uvIndex) & 0xFF;
                    int V = vBuffer.get(uvIndex) & 0xFF;

                    int R = (int) (Y + 1.402 * (V - 128));
                    int G = (int) (Y - 0.344136 * (U - 128) - 0.714136 * (V - 128));
                    int B = (int) (Y + 1.772 * (U - 128));

                    R = Math.max(0, Math.min(255, R));
                    G = Math.max(0, Math.min(255, G));
                    B = Math.max(0, Math.min(255, B));

                    rgb[y * width + x] = (0xFF << 24) | (R << 16) | (G << 8) | B;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
            return Bitmap.createScaledBitmap(bitmap, 640, 640, true); // Resize cho YOLO
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        reconnectHandler.removeCallbacks(reconnectRunnable);
    }

    private void notifi_to_firestore(String Title, String Message){
        try{
            Date dateee = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String date = sdf.format(dateee);

            FirebaseUser user = mauth.getCurrentUser();
            String UID = user.getUid().toString();
            String new_idDoc = UUID.randomUUID().toString();

            Map<String, Object> notice = new HashMap<>();
            notice.put("uid_user", UID);
            notice.put("title", Title);
            notice.put("message", Message);
            notice.put("time", date);


            db.collection("notification")
                    .document(new_idDoc)
                    .set(notice)
                    .addOnSuccessListener(documentReference -> {
                        Log.i(TAG, "Đã lưu cảnh báo vào Firestore");
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "Thất bại khi lưu cảnh báo vào Firestore");
                    });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
    private void send_relativeid_to_server() throws JSONException {
        FirebaseUser user = mauth.getCurrentUser();

        Date dateee = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String date = sdf.format(dateee);


        db.collection("relationships")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> list_relative = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String relative_id = doc.get("familyUserId").toString();
                        list_relative.add(relative_id);
                    }

                    JSONObject json = new JSONObject();


                    try {
                        json.put("warning", new JSONArray(list_relative));
                        json.put("title", "Cảnh báo nguy hiểm");
                        json.put("message", "Phát hiện nguy hiểm");
                        json.put("date", date);

                        webSocketClient.send(json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching relationships", e);
                });
    }

    private void send_relative_to_email(String detectedClass){
        try{
            FirebaseUser user = mauth.getCurrentUser();
            db.collection("relationships")
                    .whereEqualTo("userId", user.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots){
                            String relative_id = doc.get("familyUserId").toString();
                            Log.i(TAG, "Lay duoc id nguoi than: " + relative_id);

                            // lay email dua vao id
                            db.collection("users")
                                    .document(relative_id)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshotss -> {
                                            String relative_email = queryDocumentSnapshotss.get("email").toString();
                                            Log.i(TAG, "Lay duoc email nguoi than: " + relative_email);
                                            // gui email
                                        // Chạy sendMail trong luồng background
                                        executorService.execute(() -> {
                                            try {
                                                Log.d(TAG, "send_relative_to_email: Starting email sending in background thread");
                                                GMailSender sender = new GMailSender("phanuan028@gmail.com", "thnd nnqx gfcb igly");
                                                sender.sendMail(
                                                        "Cảnh báo nguy hiểm",
                                                        "Phát hiện nguy hiểm: " + detectedClass,
                                                        "phanuan028@gmail.com",
                                                        relative_email
                                                );
                                                Log.d(TAG, "send_relative_to_email: Email sent successfully to " + relative_email);
                                                runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "Email sent to " + relative_email, Toast.LENGTH_SHORT).show());
                                            } catch (Exception e) {
                                                Log.e(TAG, "send_relative_to_email: Error sending email to " + relative_email + ": ", e);
                                                runOnUiThread(() -> Toast.makeText(RecognitionActivity.this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                            }
                                        });

                                    })
                                    .addOnFailureListener(ee ->{
                                        Log.e(TAG, "on send email: khong lay duoc relationships" + ee.getMessage());
                                    });
                        }
                    })
                    .addOnFailureListener(e ->{
                        Log.e(TAG, "on send email: khong lay duoc relationships" + e.getMessage());
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}