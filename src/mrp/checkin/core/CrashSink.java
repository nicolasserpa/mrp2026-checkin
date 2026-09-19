package mrp.checkin.core;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Crash logger local (sem PII): anexa thread+stacktrace em crash.log dentro de
 * filesDir, com limite de 64KB truncando do início (mantém o final mais recente).
 * Não envia nada para a rede — é só para diagnóstico no aparelho do fiscal.
 */
public final class CrashSink {
    private static final int MAX_BYTES = 64 * 1024;
    private static final int READ_CHARS = 3000;

    private CrashSink() {
    }

    public static File logFile(Context context) {
        return new File(context.getFilesDir(), "crash.log");
    }

    public static void install(Context context) {
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("=== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(new Date()) + " ===");
                pw.println("thread: " + thread.getName());
                throwable.printStackTrace(pw);
                pw.flush();
                try {
                    appendTruncated(logFile(app), sw.toString());
                } catch (Exception ignored) {
                    // logging de crash nunca deve quebrar o fluxo de kill
                }
                if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
        });
    }

    public static String read(Context context) {
        File file = logFile(context);
        if (!file.exists() || file.length() == 0) {
            return "";
        }
        try {
            String text = new String(readAll(file), "UTF-8");
            return LogTruncate.tail(text, READ_CHARS);
        } catch (Exception e) {
            return "";
        }
    }

    public static void clear(Context context) {
        File file = logFile(context);
        if (file.exists()) {
            file.delete();
        }
    }

    private static void appendTruncated(File file, String entry) throws Exception {
        byte[] entryBytes = entry.getBytes("UTF-8");
        byte[] existing = file.exists() ? readAll(file) : new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(existing, 0, existing.length);
        out.write('\n');
        out.write('\n');
        out.write(entryBytes, 0, entryBytes.length);
        byte[] merged = out.toByteArray();
        if (merged.length > MAX_BYTES) {
            int skip = merged.length - MAX_BYTES;
            byte[] trimmed = new byte[MAX_BYTES];
            System.arraycopy(merged, skip, trimmed, 0, MAX_BYTES);
            merged = trimmed;
        }
        FileOutputStream fos = new FileOutputStream(file, false);
        try {
            fos.write(merged);
        } finally {
            fos.close();
        }
    }

    private static byte[] readAll(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}