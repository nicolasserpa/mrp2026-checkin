package mrp.checkin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import mrp.checkin.ui.M3;

/**
 * Regressão: botões interativos atrás da navbar / zona de gesto "home".
 *
 * A lanterna (Scan), o botão primário da ficha e os últimos botões das telas
 * de formulário usavam inset fixo (0, ou getSystemWindowInsetBottom(), ou
 * bottomMargin 96dp) — o suficiente para 3 botões em portrait, insuficiente
 * com navegação por gestos e insuficiente em paisagem (navbar na lateral).
 *
 * Trava a matemática de M3 (funções puras, sem Android): base =
 * max(navigationBars, systemGestures, tappableElement, ime) + gap. O
 * tappableElement é o que garante que o controle não caia na faixa de gesto —
 * e o mesmo cálculo serve para navbar de 3 botões, sem bifurcar.
 */
public class BottomInsetTest {

    // --- (a) só gesto: navBar=0, gesto e tappable = 48px -----------------
    @Test
    public void soGestoUsaTappableEGestoMaisOGap() {
        assertEquals(48 + 12, M3.computeBottomInset(0, 48, 48, 12));
    }

    // --- (b) navbar de 3 botões: navBar=144, sem gesto ---------------------
    @Test
    public void navDeTresBotoesUsaABarraInteiraMaisOGap() {
        assertEquals(144 + 12, M3.computeBottomInset(144, 0, 0, 12));
    }

    // --- (c) tappable maior que navBar: vence o tappable ------------------
    @Test
    public void tappableMaiorQueNavBarVence() {
        assertEquals(96, M3.computeBottomInset(48, 0, 96, 0));
    }

    // --- (d) teclado aberto: o IME entra na conta --------------------------
    @Test
    public void tecladoAbertoCobre() {
        assertEquals(1200 + 8, M3.computeBottomInset(144, 48, 48, 1200, 8));
    }

    @Test
    public void tecladoMaiorQueBarraVenceMasBarraContinuaValendoComImePequeno() {
        // IME em paisagem pode ser menor que a navbar: nunca perdemos a barra.
        assertEquals(144, M3.computeBottomInset(144, 0, 0, 90, 0));
    }

    // --- (e) nada de barra: zero ------------------------------------------
    @Test
    public void zeroQuandoNaoHaNada() {
        assertEquals(0, M3.computeBottomInset(0, 0, 0, 0));
        assertEquals(0, M3.computeSideInset(0, 0, 0));
        assertEquals(0, M3.computeTopInset(0, 0));
    }

    @Test
    public void semGapEComBarraRetornaABarraCrua() {
        assertEquals(144, M3.computeBottomInset(144, 0, 0, 0));
    }

    // --- a variante de 4 args delega com ime=0 -----------------------------
    @Test
    public void sobrecargaDeQuatroArgumentosIgnoraIme() {
        assertEquals(M3.computeBottomInset(144, 48, 48, 16),
                M3.computeBottomInset(144, 48, 48, 0, 16));
    }

    // --- entradas degeneradas nunca viram padding negativo -----------------
    @Test
    public void valoresNegativosOuNulosNaoGeramPaddingNegativo() {
        assertEquals(0, M3.computeBottomInset(-10, -10, -10, -10, -10));
        assertEquals(0, M3.computeSideInset(-5, -5, -5));
        assertEquals(0, M3.computeTopInset(-5, -5));
    }

    // --- laterais em paisagem: navbar na lateral / cutout / gesto de borda --
    @Test
    public void lateralUsaNavbarGestureECutout() {
        assertEquals(132, M3.computeSideInset(132, 0, 0));
        assertEquals(48, M3.computeSideInset(0, 48, 0));
        assertEquals(210, M3.computeSideInset(132, 48, 210));
    }

    @Test
    public void lateralSimetricaPegaOMaiorDosDoisLados() {
        // navbar de 3 botões na direita em paisagem: 132 à direita, 0 à esquerda.
        assertEquals(132, M3.computeSideInset(0, 0, 0, 132, 0, 0));
        // notch só à esquerda.
        assertEquals(96, M3.computeSideInset(0, 0, 96, 0, 0, 0));
        // o pior dos dois lados.
        assertEquals(132, M3.computeSideInset(0, 0, 40, 132, 0, 0));
    }

    @Test
    public void lateralDeConteudoIgnoraZonaDeGesto() {
        // Controle encostado na borda: a faixa de gesto lateral conta (um toque
        // ali seria interceptado pelo gesto de voltar).
        assertEquals(48, M3.computeSideInset(0, 48, 0, 0, 48, 0));
        // Conteúdo de tela cheia: a mesma faixa não entra (safeArea passa 0 nos
        // gestos ao pedir a lateral de conteúdo) — senão login/ajustes/check
        // ficariam estreitos em portrait em quase todo aparelho com gesto.
        assertEquals(0, M3.computeSideInset(0, 0, 0, 0, 0, 0));
        // A navbar na lateral continua valendo para conteúdo em paisagem.
        assertEquals(132, M3.computeSideInset(0, 0, 0, 132, 0, 0));
    }

    @Test
    public void topoUsaStatusBarOuCutout() {
        assertEquals(72, M3.computeTopInset(72, 0));
        assertEquals(96, M3.computeTopInset(72, 96));
    }

    // --- conversão dp->px pura (usada pelos wrappers de inset) -------------
    @Test
    public void dpPxArredondaParaBaixo() {
        assertEquals(24, M3.dpPx(2f, 12));
        assertEquals(30, M3.dpPx(2.5f, 12));
        assertEquals(0, M3.dpPx(2f, 0));
    }

    // --- trava de wiring: o bug era o caller, não a matemática -------------
    @Test
    public void fichaUsaInsetResolvidoEONaoSystemWindowInset() throws Exception {
        String src = read("src/mrp/checkin/ui/ResultSheet.java");
        assertTrue("a ficha precisa do inset resolvido",
                src.contains("M3.safeArea(insets)"));
        assertTrue("a ficha não pode voltar a ler o inset cru (não cobre a "
                        + "zona de gesto)",
                !src.contains("insets.getSystemWindowInsetBottom()"));
    }

    @Test
    public void lanternaSomaInsetNaMargemInferior() throws Exception {
        String src = read("src/mrp/checkin/ScanActivity.java");
        assertTrue("margem inferior da lanterna tem de somar o inset resolvido",
                src.contains("lp.bottomMargin = dp(FLOAT_BASE_MARGIN_DP) + bottomInset;"));
        assertTrue("layoutHint() precisa ser reavaliado com os insets novos",
                src.contains("layoutFloatingControls();"));
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
