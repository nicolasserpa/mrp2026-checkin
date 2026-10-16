package mrp.checkin;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Regressão do bug da lanterna que sumia ao tocar "Escanear próximo".
 *
 * Causa: resumeScanning() consultava a ficha (isVisible) imediatamente
 * depois de showIdle(), mas a ficha só fica GONE no fim do fade de 140ms.
 * A política era avaliada com a ficha ainda "aberta", a lanterna era
 * escondida, e nada a reavaliava depois — porque CameraEngine.resumeScanning()
 * não re-emite onFlashSupport (guardado por flashReported).
 *
 * Esta trava é de FONTE (não de pixel): exige que a reavaliação da lanterna
 * aconteça dentro do callback de showIdle, e não em chamada síncrona.
 */
public class TorchWiringSourceTest {
    private static final String SCAN_ACTIVITY = "src/mrp/checkin/ScanActivity.java";
    private static final String RESULT_SHEET = "src/mrp/checkin/ui/ResultSheet.java";

    @Test
    public void resumeScanningReavaliaLanternaNoCallbackDaFicha()
            throws Exception {
        String src = read(SCAN_ACTIVITY);
        int start = src.indexOf("private void resumeScanning()");
        assertTrue("resumeScanning() não encontrado em " + SCAN_ACTIVITY, start >= 0);
        int end = src.indexOf("private void showCameraError", start);
        assertTrue("limite de resumeScanning() não encontrado", end > start);
        String block = src.substring(start, end);

        int showIdleCb = block.indexOf("resultSheet.showIdle(new Runnable()");
        int reeval = block.indexOf("updateTorchVisibility()");
        assertTrue("resumeScanning() deve fechar a ficha com callback", showIdleCb >= 0);
        assertTrue("resumeScanning() deve reavaliar a lanterna", reeval >= 0);
        assertTrue("updateTorchVisibility() tem de vir DENTRO do callback de "
                        + "showIdle (senão a ficha ainda consta visível)",
                reeval > showIdleCb);
    }

    @Test
    public void resultSheetAvisaQuandoFicouOculta() throws Exception {
        String src = read(RESULT_SHEET);
        assertTrue("showIdle(Runnable) ausente", src.contains("public void showIdle(final Runnable onHidden)"));
        assertTrue("callback precisa rodar quando a ficha vira GONE",
                src.contains("root.setVisibility(View.GONE);\n                            if (onHidden != null) {"));
    }

    private static String read(String path) throws Exception {
        File f = new File(path);
        assertTrue("arquivo não encontrado (rode a partir da raiz do repo): " + path,
                f.isFile());
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}
