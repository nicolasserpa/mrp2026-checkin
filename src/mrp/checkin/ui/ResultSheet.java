package mrp.checkin.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ResultSheet {
    public interface Actions {
        void onPresence();

        void onConformity();

        void onRescan();
    }

    private final LinearLayout root;
    private final ScrollView scroller;
    private final LinearLayout content;
    private final TextView badge;
    private final LinearLayout successBlock;
    private final TextView title;
    private final TextView row1Label;
    private final TextView row1Value;
    private final TextView row2Label;
    private final TextView row2Value;
    private final TextView status;
    private final ProgressBar progress;
    private final Button presenceButton;
    private final Button conformityButton;
    private final Button rescanButton;

    public ResultSheet(Context context, Actions actions) {
        root = M3.sheet(context);
        // Inset resolvido (navbar OU zona de gesto OU teclado; lateral em
        // paisagem) — o getSystemWindowInsetBottom() sozinho deixava o botão
        // primário atrás da barra de 3 botões e sob a faixa de "home".
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                M3.SafeArea sa = M3.safeArea(insets);
                v.setPadding(M3.dp(context, 20) + sa.left, M3.dp(context, 20),
                        M3.dp(context, 20) + sa.right, M3.dp(context, 20) + sa.bottom);
                return insets;
            }
        });

        // Conteúdo rolável: em portrait comporta-se como WRAP_CONTENT; em
        // paisagem a activity limita a altura via setMaxHeight() e a ficha
        // rola em vez de estourar a tela.
        scroller = new ScrollView(context);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        root.addView(scroller, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        badge = new TextView(context);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.CENTER_HORIZONTAL;
        badgeLp.bottomMargin = M3.dp(context, 8);
        content.addView(badge, badgeLp);

        successBlock = M3.successBlock(context);
        LinearLayout.LayoutParams successLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        successLp.bottomMargin = M3.dp(context, 14);
        successBlock.setVisibility(View.GONE);
        content.addView(successBlock, successLp);

        title = new TextView(context);
        title.setTextSize(26);
        title.setTextColor(M3.onSurface(context));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, M3.dp(context, 10));
        content.addView(title);

        progress = M3.spinner(context);
        progress.setPadding(0, M3.dp(context, 6), 0, M3.dp(context, 6));
        progress.setVisibility(View.GONE);
        content.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row1Label = M3.label(context, "Equipe");
        content.addView(row1Label);
        row1Value = new TextView(context);
        row1Value.setTextSize(18);
        row1Value.setTextColor(M3.onSurface(context));
        row1Value.setPadding(0, 0, 0, M3.dp(context, 10));
        content.addView(row1Value);

        row2Label = M3.label(context, "Conformidade");
        content.addView(row2Label);
        row2Value = new TextView(context);
        row2Value.setTextSize(18);
        row2Value.setTextColor(M3.onSurface(context));
        row2Value.setPadding(0, 0, 0, M3.dp(context, 4));
        content.addView(row2Value);

        status = new TextView(context);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, M3.dp(context, 14), 0, M3.dp(context, 14));
        content.addView(status);

        presenceButton = M3.filledButton(context, "Registrar presença");
        presenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onPresence();
            }
        });
        content.addView(presenceButton, buttonLp(context, RESULT_BUTTON_GAP_DP));

        conformityButton = M3.filledButton(context, "Conformidade técnica");
        conformityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onConformity();
            }
        });
        content.addView(conformityButton, buttonLp(context, RESULT_BUTTON_GAP_DP));

        rescanButton = M3.tonalButton(context, "Escanear novamente");
        rescanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onRescan();
            }
        });
        content.addView(rescanButton, buttonLp(context, RESULT_BUTTON_GAP_DP));
    }

    /** Espaço vertical entre botões adjacentes da ficha (regressão: sem isso eles encostam). */
    public static final int RESULT_BUTTON_GAP_DP = 12;

    private static LinearLayout.LayoutParams buttonLp(Context context, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = M3.dp(context, topMarginDp);
        return lp;
    }

    public View root() {
        return root;
    }

    /**
     * Limita a altura rolável da ficha (paisagem). <= 0 restaura WRAP_CONTENT.
     */
    public void setMaxHeight(int maxHeightPx) {
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) scroller.getLayoutParams();
        if (maxHeightPx > 0) {
            lp.height = maxHeightPx;
        } else {
            lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        scroller.setLayoutParams(lp);
        scroller.requestLayout();
    }

    /** Texto e estilo do botão reserva (escaneia de novo ou tenta de novo). */
    public void setRescanLabel(String label, boolean primary) {
        Context c = rescanButton.getContext();
        rescanButton.setText(label);
        if (primary) {
            rescanButton.setBackground(M3.ripple(M3.RIPPLE_ON_PRIMARY,
                    M3.rounded(M3.primary(c), M3.SHAPE_PILL, c)));
            rescanButton.setTextColor(M3.onPrimary(c));
        } else {
            rescanButton.setBackground(M3.ripple(M3.RIPPLE_ON_CONTAINER,
                    M3.rounded(M3.secondaryContainer(c), M3.SHAPE_PILL, c)));
            rescanButton.setTextColor(M3.onSecondaryContainer(c));
        }
        rescanButton.requestLayout();
    }

    private void showSuccessBlock(boolean visible) {
        if (visible) {
            successBlock.setVisibility(View.VISIBLE);
            if (M3.motionEnabled(successBlock.getContext())) {
                successBlock.setScaleX(0.6f);
                successBlock.setScaleY(0.6f);
                successBlock.setAlpha(0f);
                successBlock.animate()
                        .scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(240)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
            } else {
                successBlock.setScaleX(1f);
                successBlock.setScaleY(1f);
                successBlock.setAlpha(1f);
            }
        } else {
            successBlock.animate().cancel();
            successBlock.setVisibility(View.GONE);
            successBlock.setScaleX(1f);
            successBlock.setScaleY(1f);
            successBlock.setAlpha(1f);
        }
    }

    public void showParticipant(String name, String team, String role) {
        Context c = root.getContext();
        setBadge("Participante confirmado",
                M3.successContainer(c), M3.onSuccessContainer(c));
        showSuccessBlock(true);
        title.setText(name);
        row1Label.setText("Equipe");
        row1Value.setText(team);
        row2Label.setText("Função");
        row2Value.setText("Líder".equals(role) ? "Líder" : "Integrante");
        showRows(true);
        progress.setVisibility(View.GONE);
        presenceButton.setVisibility(View.VISIBLE);
        conformityButton.setVisibility(View.GONE);
        rescanButton.setVisibility(View.VISIBLE);
        setRescanLabel("Escanear próximo", false);
        clearStatus();
    }

    public void showVehicle(String name, String team, String conformityStatus) {
        Context c = root.getContext();
        setBadge("Veículo confirmado",
                M3.successContainer(c), M3.onSuccessContainer(c));
        showSuccessBlock(true);
        title.setText(name);
        row1Label.setText("Equipe");
        row1Value.setText(team);
        row2Label.setText("Conformidade");
        row2Value.setText(conformityLabel(conformityStatus));
        showRows(true);
        progress.setVisibility(View.GONE);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        setRescanLabel("Escanear próximo", false);
        clearStatus();
    }

    public void setWaiting(String message) {
        Context c = root.getContext();
        setBadge("Verificando", M3.surfaceVariant(c), M3.onSurfaceVariant(c));
        showSuccessBlock(false);
        title.setText("");
        showRows(false);
        progress.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.GONE);
        rescanButton.setVisibility(View.GONE);
    }

    public void showError(String message, String reason) {
        Context c = root.getContext();
        setBadge(reason == null ? "Erro" : reason,
                M3.errorContainer(c), M3.onErrorContainer(c));
        showSuccessBlock(false);
        title.setText("");
        showRows(false);
        progress.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(message);
        status.setTextColor(M3.error(c));
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.GONE);
        rescanButton.setVisibility(View.VISIBLE);
        setRescanLabel("Escanear novamente", false);
    }

    public void setPresenceResult(boolean duplicate, String lastSeenAt) {
        Context c = root.getContext();
        progress.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setTextColor(duplicate ? M3.warn(c) : M3.success(c));
        status.setText(duplicate
                ? "Presença já registrada anteriormente nesta sessão.\n" + lastSeenAt
                : "Presença registrada com sucesso.\n" + lastSeenAt);
        presenceButton.setVisibility(View.GONE);
    }

    public void showIdle() {
        showIdle(null);
    }

    /**
     * Fecha a ficha e avisa quando ela ficou realmente GONE. O callback é
     * essencial: durante o fade-out a ficha ainda está visível, então quem
     * consulta isVisible()logo após showIdle() ainda a enxerga aberta.
     * Sem isso, a lanterna era escondida e nunca voltava.
     */
    public void showIdle(final Runnable onHidden) {
        root.animate().cancel();
        if (isVisible() && M3.motionEnabled(root.getContext())) {
            root.animate().alpha(0f).setDuration(140)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            root.setVisibility(View.GONE);
                            if (onHidden != null) {
                                onHidden.run();
                            }
                        }
                    })
                    .start();
        } else {
            root.setVisibility(View.GONE);
            root.setAlpha(1f);
            if (onHidden != null) {
                onHidden.run();
            }
        }
    }

    public void show() {
        root.setVisibility(View.VISIBLE);
        root.animate().cancel();
        if (M3.motionEnabled(root.getContext())) {
            root.setAlpha(0f);
            root.setTranslationY(M3.dp(root.getContext(), 24));
            root.animate().alpha(1f).translationY(0f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            root.setAlpha(1f);
            root.setTranslationY(0f);
        }
    }

    public boolean isVisible() {
        return root.getVisibility() == View.VISIBLE;
    }

    private void setBadge(String text, int bg, int fg) {
        badge.setText(text);
        badge.setTextColor(fg);
        badge.setTextSize(13);
        badge.setTypeface(android.graphics.Typeface.SANS_SERIF);
        badge.setLetterSpacing(0.05f);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(M3.dp(badge.getContext(), 14), M3.dp(badge.getContext(), 6),
                M3.dp(badge.getContext(), 14), M3.dp(badge.getContext(), 6));
        badge.setBackground(M3.rounded(bg, M3.SHAPE_PILL, badge.getContext()));
    }

    private void showRows(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        row1Label.setVisibility(v);
        row1Value.setVisibility(v);
        row2Label.setVisibility(v);
        row2Value.setVisibility(v);
    }

    private void clearStatus() {
        status.setText("");
        status.setTextColor(Color.TRANSPARENT);
    }

    private static String conformityLabel(String status) {
        if (status == null) {
            return "Não avaliado";
        }
        if ("pass".equals(status)) {
            return "Aprovado";
        }
        if ("fail".equals(status)) {
            return "Reprovado";
        }
        return "Não avaliado";
    }
}
