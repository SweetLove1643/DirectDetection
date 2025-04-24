package com.sweetlove.directdetection.Controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Bundle;
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
import com.sweetlove.directdetection.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class RecognitionActivity extends AppCompatActivity {

    private ImageButton help_btn, action_btn, setting_btn;
    private PreviewView camera_view;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = RecognitionActivity.class.getSimpleName();

    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private String inputName;
    private final int INPUT_WIDTH = 128;
    private final int INPUT_HEIGHT = 128;
    String[] class_names = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
            "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter",
            "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear",
            "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase",
            "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat",
            "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut",
            "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
            "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
            "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
            "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recognition);

        help_btn = findViewById(R.id.helpButton);
        action_btn = findViewById(R.id.actionBtn);
        setting_btn = findViewById(R.id.settingsButton);
        camera_view = findViewById(R.id.imageScan);

        help_btn.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        setting_btn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            ortEnvironment = OrtEnvironment.getEnvironment();
            byte[] modelData = readModelFromAssets("yolo11_128.onnx");
            ortSession = ortEnvironment.createSession(modelData, new OrtSession.SessionOptions());

            Set<String> inputNames = ortSession.getInputNames();
            if (inputNames.isEmpty()) {
                throw new IllegalStateException("Model has no input names");
            }
            inputName = inputNames.iterator().next();
            Log.i(TAG, "Model loaded successfully. Input name: " + inputName);
        } catch (Exception e) {
            Log.e(TAG, "Error loading ONNX model", e);
            Toast.makeText(this, "Failed to load model: " + e.getMessage(), Toast.LENGTH_LONG).show();
            ortSession = null;
        }

        requestCameraPermission();
    }

    private byte[] readModelFromAssets(String modelPath) throws Exception {
        try {
            InputStream inputStream = getAssets().open(modelPath);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            inputStream.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error reading model file: " + modelPath, e);
            throw new Exception("Cannot read model file: " + modelPath, e);
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
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
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy image) {
        if (ortSession == null || inputName == null) {
            Log.e(TAG, "Cannot process image: Model not loaded");
            image.close();
            return;
        }

        Bitmap bitmap = imageToBitmap(image);
        if (bitmap == null) {
            image.close();
            return;
        }

        float[] inputTensor = preprocessImage(bitmap);
        OnnxTensor onnxTensor = null;
        try {
            long[] shape = new long[]{1, 3, INPUT_HEIGHT, INPUT_WIDTH}; // [1, 3, 128, 128]
            FloatBuffer floatBuffer = FloatBuffer.wrap(inputTensor);
            onnxTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape);

            OnnxTensor finalOnnxTensor = onnxTensor;
            OrtSession.Result result = ortSession.run(
                    new HashMap<String, OnnxTensor>() {{
                        put(inputName, finalOnnxTensor);
                    }}
            );

            // Xử lý đầu ra float[][][]
            float[][][] outputTensor = (float[][][]) result.get(0).getValue();
            // Giả định đầu ra có dạng [1, 1, num_classes], trích xuất vector phân loại
            float[] output = outputTensor[0][0]; // Lấy vector [num_classes]

            int predictedClass = argMax(output);
            float confidence = output[predictedClass];

            runOnUiThread(() -> {
                String resultText = "Class: " + predictedClass + ", Confidence: " + confidence;
                String predict = class_names[predictedClass % 79];
                Log.i(TAG, "Dối tượng: " + predict);
                Log.i(TAG, resultText);
                Toast.makeText(RecognitionActivity.this, predict, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error running inference", e);
        } finally {
            if (onnxTensor != null) {
                try {
                    onnxTensor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing OnnxTensor", e);
                }
            }
            image.close();
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
            return Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true);
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    private float[] preprocessImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true);
        float[] inputTensor = new float[1 * 3 * INPUT_WIDTH * INPUT_HEIGHT];
        int[] pixels = new int[INPUT_WIDTH * INPUT_HEIGHT];
        resizedBitmap.getPixels(pixels, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT);

        int index = 0;
        for (int pixel : pixels) {
            inputTensor[index++] = ((pixel >> 16) & 0xFF) / 255.0f; // R
            inputTensor[index++] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
            inputTensor[index++] = (pixel & 0xFF) / 255.0f;         // B
        }
        return inputTensor;
    }

    private int argMax(float[] array) {
        int maxIdx = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        try {
            if (ortSession != null) ortSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing ONNX resources", e);
        }
    }
}