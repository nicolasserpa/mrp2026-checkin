package mrp.checkin.scan;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
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
import java.util.Arrays;
import java.util.List;

public final class CameraEngine {
    public interface DecodeListener {
        void onDecoded(String text);

        void onCameraError(String message);

        void onTorch(boolean on);
    }

    private static final int DECODE_INTERVAL_MS = 120;
    private static final int MAX_IMAGES = 2;

    private final Context context;
    private final TextureView preview;
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

    public CameraEngine(Context context, TextureView preview, DecodeListener listener, Handler mainHandler) {
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
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                configureTransform();
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
        if (opened || cameraDevice != null) {
            return;
        }
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) {
            reportError("Câmera indisponível neste aparelho.");
            return;
        }
        try {
            String cameraId = pickBackCamera(cm);
            CameraCharacteristics cc = cm.getCameraCharacteristics(cameraId);
            sensorOrientation = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            torchSupported = Boolean.TRUE.equals(cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                reportError("Câmera sem perfis de captura.");
                return;
            }

            Size[] yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            Size[] textureSizes = map.getOutputSizes(SurfaceTexture.class);
            readerSize = chooseClosest(yuvSizes, new Size(1280, 720));
            previewSize = chooseClosest(textureSizes, readerSize);

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
            configureTransform();
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
            opened = true;
        } catch (Exception e) {
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

    private void configureTransform() {
        if (previewSize == null || preview.getWidth() == 0 || preview.getHeight() == 0) {
            return;
        }
        int rotation = getDisplayRotation();
        int viewW = preview.getWidth();
        int viewH = preview.getHeight();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewW, viewH);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max((float) viewH / previewSize.getWidth(),
                (float) viewW / previewSize.getHeight());
        matrix.postScale(scale, scale, centerX, centerY);
        matrix.postRotate((sensorOrientation - rotation + 360) % 360, centerX, centerY);
        preview.setTransform(matrix);
    }

    private int getDisplayRotation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
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