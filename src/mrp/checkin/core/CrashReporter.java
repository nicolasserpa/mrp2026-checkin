package mrp.checkin.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;

import java.util.HashSet;
import java.util.Set;

/**
 * Mostra o conteúdo de crash.log numa única caixa por execução do processo
 * (dedupe entre MainActivity e ScanActivity) e limpa o log ao fechar.
 */
public final class CrashReporter {
    private static final int MESSAGE_LIMIT = 1200;
    private static final Set<String> SHOWN = new HashSet<>();

    private CrashReporter() {
    }

    public static void maybeShow(final Activity activity) {
        if (CrashSink.logFile(activity).length() == 0) {
            return;
        }
        String body = LogTruncate.tail(CrashSink.read(activity), MESSAGE_LIMIT);
        if (!SHOWN.add(String.valueOf(body.hashCode()))) {
            CrashSink.clear(activity);
            return;
        }
        final String trace = body;
        new AlertDialog.Builder(activity)
                .setTitle("O app fechou inesperadamente na última execução.")
                .setMessage(trace)
                .setPositiveButton("Copiar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager cm = (ClipboardManager)
                                activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("crash", trace));
                        }
                    }
                })
                .setNegativeButton("Entendi", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CrashSink.clear(activity);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        CrashSink.clear(activity);
                    }
                })
                .show();
    }
}