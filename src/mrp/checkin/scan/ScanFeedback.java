package mrp.checkin.scan;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class ScanFeedback {
    private final Vibrator vibrator;
    private ToneGenerator tone;

    public ScanFeedback(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
        } catch (Exception e) {
            tone = null;
        }
    }

    public void success() {
        vibrate(60);
        beep(ToneGenerator.TONE_PROP_BEEP, 120);
    }

    public void warn() {
        vibrate(180);
        beep(ToneGenerator.TONE_PROP_BEEP2, 300);
    }

    public void release() {
        if (tone != null) {
            tone.release();
            tone = null;
        }
    }

    private void vibrate(long ms) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }

    private void beep(int toneType, int ms) {
        if (tone == null) {
            return;
        }
        try {
            tone.startTone(toneType, ms);
        } catch (Exception ignored) {
        }
    }
}