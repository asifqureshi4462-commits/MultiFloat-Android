package com.example.miniapps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.example.R;
import com.example.model.ToolType;
import com.example.util.ClipboardHelper;
import com.example.util.PermissionHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScannerController extends BaseMiniAppController {

    private PreviewView previewView;
    private View panelPermission;
    private TextView tvPermMessage;
    private Button btnGrantOrOpen;
    private TextView tvResult;
    private Button btnOpenLink;

    private String lastScanned = "";
    private ProcessCameraProvider cameraProvider;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final MultiFormatReader qrReader = new MultiFormatReader();

    public QrScannerController(Context context, HostCallback hostCallback) {
        super(context, hostCallback);
    }

    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.miniapp_qr_scanner, parent, false);

        previewView = view.findViewById(R.id.preview_qr_camera);
        panelPermission = view.findViewById(R.id.panel_qr_permission);
        tvPermMessage = view.findViewById(R.id.tv_qr_perm_message);
        btnGrantOrOpen = view.findViewById(R.id.btn_qr_grant_or_open);
        tvResult = view.findViewById(R.id.tv_qr_result);
        btnOpenLink = view.findViewById(R.id.btn_qr_open_link);

        view.findViewById(R.id.btn_qr_copy).setOnClickListener(v -> {
            if (!lastScanned.isEmpty()) {
                ClipboardHelper.copyText(context, "QR Result", lastScanned, true);
            }
        });

        btnOpenLink.setOnClickListener(v -> {
            if (lastScanned.startsWith("http://") || lastScanned.startsWith("https://")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(lastScanned));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        });

        refreshCameraState();
        return view;
    }

    public void refreshCameraState() {
        if (!PermissionHelper.hasCameraPermission(context)) {
            panelPermission.setVisibility(View.VISIBLE);
            tvPermMessage.setText("Camera permission is needed to scan QR codes.");
            btnGrantOrOpen.setText("Grant Camera Permission");
            btnGrantOrOpen.setOnClickListener(v -> {
                if (hostCallback != null) hostCallback.onRequestCameraPermission();
            });
            return;
        }

        if (!(context instanceof LifecycleOwner)) {
            // Inside floating overlay Service: Android blocks background Services from opening Camera
            panelPermission.setVisibility(View.VISIBLE);
            tvPermMessage.setText("Android security requires opening the foreground scanner window to access the camera.");
            btnGrantOrOpen.setText("Open Live QR Scanner");
            btnGrantOrOpen.setOnClickListener(v -> {
                if (hostCallback != null) hostCallback.onOpenFullScreenTool(ToolType.QR_SCANNER);
            });
            return;
        }

        panelPermission.setVisibility(View.GONE);
        startCameraX((LifecycleOwner) context);
    }

    private void startCameraX(LifecycleOwner owner) {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                panelPermission.setVisibility(View.VISIBLE);
                tvPermMessage.setText("Camera unavailable on this device: " + e.getMessage());
                btnGrantOrOpen.setVisibility(View.GONE);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void analyzeImage(ImageProxy image) {
        try {
            if (image.getPlanes().length == 0) return;
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data, image.getWidth(), image.getHeight(), 0, 0,
                    image.getWidth(), image.getHeight(), false
            );
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = qrReader.decodeWithState(bitmap);
            if (result != null && result.getText() != null && !result.getText().equals(lastScanned)) {
                final String text = result.getText();
                lastScanned = text;
                tvResult.post(() -> {
                    tvResult.setText(text);
                    boolean isUrl = text.startsWith("http://") || text.startsWith("https://");
                    btnOpenLink.setVisibility(isUrl ? View.VISIBLE : View.GONE);
                });
            }
        } catch (Exception ignored) {
            // No QR code in frame
        } finally {
            qrReader.reset();
            image.close();
        }
    }

    @Override
    public void onDetached() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        cameraExecutor.shutdown();
        super.onDetached();
    }
}
