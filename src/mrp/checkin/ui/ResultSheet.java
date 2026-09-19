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
import android.widget.TextView;

public final class ResultSheet {
    public interface Actions {
        void onPresence();

        void onConformity();

        void onRescan();
    }

    private final LinearLayout root;
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
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(M3.dp(context, 20), M3.dp(context, 20),
                        M3.dp(context, 20), M3.dp(context, 20) + bottom);
                return insets;
            }
        });

        badge = new TextView(context);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.CENTER_HORIZONTAL;
        badgeLp.bottomMargin = M3.dp(context, 8);
        root.addView(badge, badgeLp);

        successBlock = M3.successBlock(context);
        LinearLayout.LayoutParams successLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        successLp.bottomMargin = M3.dp(context, 14);
        successBlock.setVisibility(View.GONE);
        root.addView(successBlock, successLp);

        title = new TextView(context);
        title.setTextSize(26);
        title.setTextColor(M3.ON_SURFACE);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, M3.dp(context, 10));
        root.addView(title);

        progress = M3.spinner(context);
        progress.setPadding(0, M3.dp(context, 6), 0, M3.dp(context, 6));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        row1Label = M3.label(context, "Equipe");
        root.addView(row1Label);
        row1Value = new TextView(context);
        row1Value.setTextSize(18);
        row1Value.setTextColor(M3.ON_SURFACE);
        row1Value.setPadding(0, 0, 0, M3.dp(context, 8));
        root.addView(row1Value);

        row2Label = M3.label(context, "Conformidade");
        root.addView(row2Label);
        row2Value = new TextView(context);
        row2Value.setTextSize(18);
        row2Value.setTextColor(M3.ON_SURFACE);
        root.addView(row2Value);

        status = new TextView(context);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, M3.dp(context, 14), 0, M3.dp(context, 14));
        root.addView(status);

        presenceButton = M3.filledButton(context, "Registrar presença");
        presenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onPresence();
            }
        });
        root.addView(presenceButton);

        conformityButton = M3.filledButton(context, "Conformidade técnica");
        conformityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onConformity();
            }
        });
        root.addView(conformityButton);

        rescanButton = M3.tonalButton(context, "Escanear novamente");
        rescanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onRescan();
            }
        });
        root.addView(rescanButton);
    }

    public View root() {
        return root;
    }

    /** Texto e estilo do botão reserva (escaneia de novo ou tenta de novo). */
    public void setRescanLabel(String label, boolean primary) {
        rescanButton.setText(label);
        if (primary) {
            rescanButton.setBackground(M3.ripple(M3.RIPPLE_ON_PRIMARY,
                    M3.rounded(M3.PRIMARY, M3.SHAPE_PILL, rescanButton.getContext())));
            rescanButton.setTextColor(M3.ON_PRIMARY);
        } else {
            rescanButton.setBackground(M3.ripple(M3.RIPPLE_ON_CONTAINER,
                    M3.rounded(M3.SECONDARY_CONTAINER, M3.SHAPE_PILL,
                            rescanButton.getContext())));
            rescanButton.setTextColor(M3.ON_SECONDARY_CONTAINER);
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
        setBadge("Participante confirmado", M3.SUCCESS_CONTAINER, M3.ON_SUCCESS_CONTAINER);
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
        setBadge("Veículo confirmado", M3.SUCCESS_CONTAINER, M3.ON_SUCCESS_CONTAINER);
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
        setBadge("Verificando", M3.SURFACE_VARIANT, M3.ON_SURFACE_VARIANT);
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
        setBadge(reason == null ? "Erro" : reason, M3.ERROR_CONTAINER, M3.ON_ERROR_CONTAINER);
        showSuccessBlock(false);
        title.setText("");
        showRows(false);
        progress.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(message);
        status.setTextColor(M3.ERROR);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.GONE);
        rescanButton.setVisibility(View.VISIBLE);
        setRescanLabel("Escanear novamente", false);
    }

    public void setPresenceResult(boolean duplicate, String lastSeenAt) {
        progress.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setTextColor(duplicate ? M3.WARN : M3.SUCCESS);
        status.setText(duplicate
                ? "Presença já registrada anteriormente nesta sessão.\n" + lastSeenAt
                : "Presença registrada com sucesso.\n" + lastSeenAt);
        presenceButton.setVisibility(View.GONE);
    }

    public void showIdle() {
        root.animate().cancel();
        if (isVisible() && M3.motionEnabled(root.getContext())) {
            root.animate().alpha(0f).setDuration(140)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            root.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            root.setVisibility(View.GONE);
            root.setAlpha(1f);
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