package mrp.checkin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regressão da política da lanterna (ScanActivity.torchShouldShow).
 * Trava o comportamento pretendido: a lanterna some COM a ficha aberta
 * (by design) e deve voltar ao fechar / ao retomar a activity — sem
 * depender de emulador (lógica pura, roda na JVM do host).
 */
public class ScanActivityTorchPolicyTest {
    @Test
    public void mostraComFichaFechadaEFlash() {
        assertTrue(ScanActivity.torchShouldShow(false, true));
    }

    @Test
    public void escondeComFichaAberta() {
        assertFalse(ScanActivity.torchShouldShow(true, true));
    }

    @Test
    public void escondeSemFlashMesmoComFichaFechada() {
        assertFalse(ScanActivity.torchShouldShow(false, false));
    }

    @Test
    public void escondeComFichaAbertaESemFlash() {
        assertFalse(ScanActivity.torchShouldShow(true, false));
    }
}
