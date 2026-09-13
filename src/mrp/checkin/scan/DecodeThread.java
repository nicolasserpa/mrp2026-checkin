package mrp.checkin.scan;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class DecodeThread extends Thread {
    public interface DecodeResultListener {
        void onDecoded(String text);
    }

    private static final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    static {
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, Collections.<BarcodeFormat>singletonList(BarcodeFormat.QR_CODE));
    }

    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private final int width;
    private final int height;
    private final DecodeResultListener listener;
    private final MultiFormatReader reader = new MultiFormatReader();
    private volatile boolean running = true;

    public DecodeThread(int width, int height, DecodeResultListener listener) {
        super("qr-decoder");
        this.width = width;
        this.height = height;
        this.listener = listener;
    }

    public void enqueue(byte[] yPlane) {
        queue.offer(yPlane);
    }

    @Override
    public void run() {
        reader.setHints(HINTS);
        while (running) {
            byte[] frame;
            try {
                frame = queue.take();
            } catch (InterruptedException e) {
                break;
            }
            String text = decodeFrame(frame);
            if (text != null && listener != null) {
                listener.onDecoded(text);
            }
        }
    }

    private String decodeFrame(byte[] yData) {
        for (int rot = 0; rot < 4; rot++) {
            LuminanceSource source = new PlanarYUVLuminanceSource(
                    yData, width, height, 0, 0, width, height, false);
            for (int i = 0; i < rot; i++) {
                source = source.rotateCounterClockwise();
            }
            try {
                Result r = reader.decode(new BinaryBitmap(new HybridBinarizer(source)));
                return r.getText();
            } catch (Exception ignored) {
            } finally {
                reader.reset();
            }
        }
        return null;
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}