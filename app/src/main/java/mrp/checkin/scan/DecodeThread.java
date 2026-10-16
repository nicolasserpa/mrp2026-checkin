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

    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(3);
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
        if (queue.size() >= 3) {
            return;
        }
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
        byte[] data = yData;
        int curW = width;
        int curH = height;
        for (int rot = 0; rot < 4; rot++) {
            LuminanceSource source = new PlanarYUVLuminanceSource(
                    data, curW, curH, 0, 0, curW, curH, false);
            try {
                Result r = reader.decode(new BinaryBitmap(new HybridBinarizer(source)));
                return r.getText();
            } catch (Exception ignored) {
            } finally {
                reader.reset();
            }
            data = rotateYPlane(data, curW, curH);
            int tmp = curW;
            curW = curH;
            curH = tmp;
        }
        return null;
    }

    /** Rotaciona o plano Y (escala de cinza) 90° no sentido anti-horário. */
    static byte[] rotateYPlane(byte[] src, int w, int h) {
        byte[] dst = new byte[w * h];
        for (int r = 0; r < h; r++) {
            int base = r * w;
            for (int c = 0; c < w; c++) {
                dst[(w - 1 - c) * h + r] = src[base + c];
            }
        }
        return dst;
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}