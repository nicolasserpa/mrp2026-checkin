package mrp.checkin.scan;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CameraEngine {
    public interface DecodeListener {
        void onDecoded(String text);

        void onCameraError(String message);

        void onTorch(boolean on);

        void onFlashSupport(boolean available);
    }

    private static final int DECODE_INTERVAL_MS = 120;
    private static final int MAX_IMAGES = 2;

    private final Context context;
    private final AutoFitTextureView preview;
    private final DecodeListener listener;
    private final Handler mainHandler;
    private final HandlerThread thread;
    private final Handler handler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader imageReader;
    private DecodeThread decodeThread;
    private Size previewSize;
    private Size readerSize;
    private boolean scanning = true;
    private boolean torchOn;
    private boolean torchSupported;
    private int sensorOrientation;
    private long lastDecodeTs;
    private boolean opened;
    private boolean permissionGranted;
    private boolean flashReported;

    public void setCameraPermission(boolean granted) {
        permissionGranted = granted;
    }

    public CameraEngine(Context context, AutoFitTextureView preview, DecodeListener listener, Handler mainHandler) {
        this.context = context;
        this.preview = preview;
        this.listener = listener;
        this.mainHandler = mainHandler;
        this.thread = new HandlerThread("camera");
        this.thread.start();
        this.handler = new Handler(thread.getLooper());
        attachSurfaceListener();
    }

    public void start() {
        scanning = true;
        if (preview.isAvailable()) {
            openCamera();
        }
    }

    public void stop() {
        scanning = false;
        closeCamera();
    }

    public void pauseScanning() {
        scanning = false;
    }

    public void resumeScanning() {
        scanning = true;
    }

    public void toggleTorch() {
        setTorch(!torchOn);
    }

    public boolean hasFlash() {
        return torchSupported;
    }

    private void reportFlashSupport() {
        if (flashReported) {
            return;
        }
        flashReported = true;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onFlashSupport(torchSupported);
            }
        });
    }

    public void setTorch(boolean on) {
        torchOn = on;
        if (!torchSupported) {
            torchOn = false;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onTorch(false);
                }
            });
            return;
        }
        if (previewBuilder == null || captureSession == null) {
            return;
        }
        try {
            previewBuilder.set(CaptureRequest.FLASH_MODE,
                    on ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
            captureSession.setRepeatingRequest(previewBuilder.build(), null, handler);
            final boolean state = on;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onTorch(state);
                }
            });
        } catch (CameraAccessException e) {
            torchOn = false;
        }
    }

    public void shutdown() {
        closeCamera();
        thread.quitSafely();
    }

    private void attachSurfaceListener() {
        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                if (permissionGranted) {
                    openCamera();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                configureTransform(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });
    }

    private void openCamera() {
        if (!permissionGranted) {
            return;
        }
        if (opened || cameraDevice != null) {
            return;
        }
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) {
            reportError("Câmera indisponível neste aparelho.");
            return;
        }
        opened = true;
        try {
            String cameraId = pickBackCamera(cm);
            CameraCharacteristics cc = cm.getCameraCharacteristics(cameraId);
            Integer so = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation = so == null ? 0 : so;
            torchSupported = Boolean.TRUE.equals(cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            reportFlashSupport();
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                reportError("Câmera sem perfis de captura.");
                return;
            }

            Size[] yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            Size[] textureSizes = map.getOutputSizes(SurfaceTexture.class);
            if (yuvSizes == null || yuvSizes.length == 0
                    || textureSizes == null || textureSizes.length == 0) {
                reportError("Câmera sem perfis de captura.");
                return;
            }
            // Decode: plano Y próximo de 1280x720 (inalterado).
            readerSize = chooseClosest(yuvSizes, new Size(1280, 720));
            // Preview: tamanho + proporção da view (template Camera2Basic).
            setupPreviewOutput(textureSizes, yuvSizes);

            imageReader = ImageReader.newInstance(readerSize.getWidth(), readerSize.getHeight(),
                    ImageFormat.YUV_420_888, MAX_IMAGES);
            imageReader.setOnImageAvailableListener(new imageAvailableListener(), handler);

            decodeThread = new DecodeThread(readerSize.getWidth(), readerSize.getHeight(), new DecodeThread.DecodeResultListener() {
                @Override
                public void onDecoded(final String text) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listeningDecode(text);
                        }
                    });
                }
            });
            decodeThread.start();

            SurfaceTexture st = preview.getSurfaceTexture();
            st.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            applyTransformNowAndAfterLayout();
            final Surface previewSurface = new Surface(st);
            final Surface readerSurface = imageReader.getSurface();

            cm.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice device) {
                    cameraDevice = device;
                    createCaptureSession(previewSurface, readerSurface);
                }

                @Override
                public void onDisconnected(CameraDevice device) {
                    closeCamera();
                }

                @Override
                public void onError(CameraDevice device, int error) {
                    closeCamera();
                    reportError("Erro ao abrir a câmera (" + error + ").");
                }
            }, handler);
        } catch (SecurityException e) {
            opened = false;
            reportError("Permissão de câmera não concedida.");
        } catch (Exception e) {
            opened = false;
            reportError("Não foi possível acessar a câmera: " + e.getMessage());
        }
    }

    private class imageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            try {
                if (!scanning || SystemClock.elapsedRealtime() - lastDecodeTs < DECODE_INTERVAL_MS) {
                    return;
                }
                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int rowStride = plane.getRowStride();
                int width = image.getWidth();
                int height = image.getHeight();
                byte[] y = new byte[width * height];
                if (rowStride == width) {
                    buffer.duplicate().get(y, 0, y.length);
                } else {
                    for (int r = 0; r < height; r++) {
                        buffer.position(r * rowStride);
                        buffer.get(y, r * width, width);
                    }
                }
                decodeThread.enqueue(y);
                lastDecodeTs = SystemClock.elapsedRealtime();
            } finally {
                image.close();
            }
        }
    }

    private void createCaptureSession(Surface previewSurface, Surface readerSurface) {
        List<Surface> outputs = Arrays.asList(previewSurface, readerSurface);
        try {
            cameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewBuilder.addTarget(previewSurface);
                        previewBuilder.addTarget(readerSurface);
                        try {
                            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        } catch (Exception ignored) {
                            // câmera sem autofoco: segue no modo padrão
                        }
                        setTorch(torchOn);
                        session.setRepeatingRequest(previewBuilder.build(), null, handler);
                    } catch (CameraAccessException e) {
                        reportError("Falha ao iniciar a captura.");
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    reportError("Falha ao configurar a câmera.");
                }
            }, handler);
        } catch (CameraAccessException e) {
            reportError("Falha ao criar a sessão de captura.");
        }
    }

    private void closeCamera() {
        opened = false;
        if (decodeThread != null) {
            decodeThread.shutdown();
            decodeThread = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        previewBuilder = null;
    }

    private String pickBackCamera(CameraManager cm) throws CameraAccessException {
        for (String id : cm.getCameraIdList()) {
            CameraCharacteristics cc = cm.getCameraCharacteristics(id);
            Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return cm.getCameraIdList()[0];
    }

    private static Size chooseClosest(Size[] sizes, Size target) {
        Size best = null;
        long bestArea = Long.MAX_VALUE;
        for (Size s : sizes) {
            long area = (long) s.getWidth() * s.getHeight();
            if (area <= bestArea) {
                bestArea = area;
                best = s;
            }
        }
        for (Size s : sizes) {
            if (s.getWidth() <= target.getWidth() && s.getHeight() <= target.getHeight()) {
                long area = (long) s.getWidth() * s.getHeight();
                if (best == null || area > bestArea) {
                    best = s;
                    bestArea = area;
                }
            }
        }
        if (best == null && sizes.length > 0) {
            best = sizes[0];
        }
        return best;
    }

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /** Escolhe o tamanho do preview e a proporção da view (Camera2Basic). */
    private void setupPreviewOutput(Size[] textureSizes, Size[] yuvSizes) {
        Size largest = Collections.max(Arrays.asList(yuvSizes), new CompareSizesByArea());
        int displayRotation = getDisplayRotationEnum();
        boolean swappedDimensions = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                break;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point displaySize = new Point(1080, 1920);
        if (wm != null) {
            wm.getDefaultDisplay().getSize(displaySize);
        }
        int viewW = preview.getWidth();
        int viewH = preview.getHeight();
        if (viewW <= 0 || viewH <= 0) {
            viewW = displaySize.x;
            viewH = displaySize.y;
        }
        int rotatedPreviewWidth = swappedDimensions ? viewH : viewW;
        int rotatedPreviewHeight = swappedDimensions ? viewW : viewH;
        int maxPreviewWidth = swappedDimensions ? displaySize.y : displaySize.x;
        int maxPreviewHeight = swappedDimensions ? displaySize.x : displaySize.y;
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }
        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }
        previewSize = chooseOptimalSize(textureSizes, rotatedPreviewWidth, rotatedPreviewHeight,
                maxPreviewWidth, maxPreviewHeight, largest);
        // Proporção segue o swap sensor/display (Camera2Basic): com swap, o
        // buffer chega girado e a view precisa do aspecto transposto — vale
        // para portrait e landscape sem reabrir a câmera.
        if (swappedDimensions) {
            preview.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
        } else {
            preview.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        }
    }

    /** Menor tamanho suficiente com o aspecto pedido (tolerância ~2%); senão o maior que couber. */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight,
            int maxWidth, int maxHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        long w = aspectRatio.getWidth();
        long h = aspectRatio.getHeight();
        for (Size option : choices) {
            long ow = option.getWidth();
            long oh = option.getHeight();
            if (ow <= maxWidth && oh <= maxHeight
                    && Math.abs(ow * h - oh * w) * 50 <= ow * h) {
                if (ow >= textureViewWidth && oh >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (!notBigEnough.isEmpty()) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        return choices[0];
    }

    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /** Transform fiel ao Camera2Basic: em portrait travado = identidade
     * (o TextureView já compensa o sensor sozinho — matriz manual causava a
     * rotação dupla e o retângulo deslocado). */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (preview == null || previewSize == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        int rotation = getDisplayRotationEnum();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        preview.setTransform(matrix);
    }

    /** Aplica agora e reaplica pós-layout (setAspectRatio é assíncrono). */
    private void applyTransformNowAndAfterLayout() {
        configureTransform(Math.max(preview.getWidth(), 1), Math.max(preview.getHeight(), 1));
        preview.post(new Runnable() {
            @Override
            public void run() {
                configureTransform(preview.getWidth(), preview.getHeight());
            }
        });
    }

    /**
     * Reaplica o transform após rotação (90/270°) sem reabrir a câmera:
     * torch, sessão de captura e estado de scan/retry da activity sobrevivem.
     * Chamado pela activity em onConfigurationChanged.
     */
    public void refreshTransform() {
        preview.requestLayout();
        applyTransformNowAndAfterLayout();
    }

    private int getDisplayRotationEnum() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return Surface.ROTATION_0;
        }
        return wm.getDefaultDisplay().getRotation();
    }

    private void listeningDecode(final String text) {
        if (listener != null) {
            listener.onDecoded(text);
        }
    }

    private void reportError(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onCameraError(message);
                }
            }
        });
    }
}