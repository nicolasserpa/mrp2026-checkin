package mrp.checkin.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ResultSheet {
    public interface Actions {
        void onPresence();

        void onConformity();

        void onRescan();
    }

    private final LinearLayout root;
    private final TextView badge;
    private final TextView title;
    private final TextView row1Label;
    private final TextView row1Value;
    private final TextView row2Label;
    private final TextView row2Value;
    private final TextView status;
    private final Button presenceButton;
    private final Button conformityButton;
    private final Button rescanButton;

    public ResultSheet(Context context, Actions actions) {
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));
        root.setBackgroundColor(Color.WHITE);

        badge = new TextView(context);
        badge.setTextSize(15);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setPadding(0, 0, 0, dp(context, 4));
        root.addView(badge);

        title = new TextView(context);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(context, 10));
        root.addView(title);

        row1Label = sectionLabel(context, "Equipe");
        root.addView(row1Label);
        row1Value = new TextView(context);
        row1Value.setTextSize(18);
        root.addView(row1Value);

        row2Label = sectionLabel(context, "Conformidade");
        root.addView(row2Label);
        row2Value = new TextView(context);
        row2Value.setTextSize(18);
        root.addView(row2Value);

        status = new TextView(context);
        status.setTextSize(17);
        status.setTypeface(null, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(context, 14), 0, dp(context, 14));
        root.addView(status);

        presenceButton = new Button(context);
        presenceButton.setText("Registrar presença");
        presenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onPresence();
            }
        });
        root.addView(presenceButton);

        conformityButton = new Button(context);
        conformityButton.setText("Conformidade técnica");
        conformityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.onConformity();
            }
        });
        root.addView(conformityButton);

        rescanButton = new Button(context);
        rescanButton.setText("Escanear novamente");
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

    public void showParticipant(String name, String team, String role) {
        badge.setText("Participante confirmado");
        badge.setTextColor(Color.rgb(0, 130, 0));
        title.setText(name);
        row1Label.setText("Equipe");
        row1Value.setText(team);
        row2Label.setText("Função");
        row2Value.setText("Líder".equals(role) ? "Líder" : "Integrante");
        row2Label.setVisibility(View.VISIBLE);
        row2Value.setVisibility(View.VISIBLE);
        presenceButton.setVisibility(View.VISIBLE);
        conformityButton.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText("");
    }

    public void showVehicle(String name, String team, String conformityStatus) {
        badge.setText("Veículo confirmado");
        badge.setTextColor(Color.rgb(0, 130, 0));
        title.setText(name);
        row1Label.setText("Equipe");
        row1Value.setText(team);
        row2Label.setText("Conformidade");
        row2Value.setText(conformityLabel(conformityStatus));
        row2Label.setVisibility(View.VISIBLE);
        row2Value.setVisibility(View.VISIBLE);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
    }

    public void setWaiting(String message) {
        badge.setText("Verificando QR…");
        badge.setTextColor(Color.rgb(0, 0, 0));
        title.setText("");
        row1Label.setVisibility(View.GONE);
        row1Value.setVisibility(View.GONE);
        row2Label.setVisibility(View.GONE);
        row2Value.setVisibility(View.GONE);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(message);
        status.setTextColor(Color.rgb(0, 0, 0));
        rescanButton.setVisibility(View.GONE);
    }

    public void showError(String message, String reason) {
        badge.setText(reason == null ? "Erro" : reason);
        badge.setTextColor(Color.rgb(200, 0, 0));
        title.setText("");
        row1Label.setVisibility(View.GONE);
        row1Value.setVisibility(View.GONE);
        row2Label.setVisibility(View.GONE);
        row2Value.setVisibility(View.GONE);
        presenceButton.setVisibility(View.GONE);
        conformityButton.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(message);
        status.setTextColor(Color.rgb(200, 0, 0));
        rescanButton.setVisibility(View.VISIBLE);
    }

    public void setPresenceResult(boolean duplicate, String lastSeenAt) {
        status.setVisibility(View.VISIBLE);
        status.setTextColor(duplicate ? Color.rgb(180, 140, 0) : Color.rgb(0, 130, 0));
        status.setText(duplicate
                ? "Presença já registrada anteriormente nesta sessão.\n" + lastSeenAt
                : "Presença registrada com sucesso.\n" + lastSeenAt);
        presenceButton.setVisibility(View.GONE);
    }

    public void showIdle() {
        root.setVisibility(View.GONE);
    }

    public void show() {
        root.setVisibility(View.VISIBLE);
    }

    public boolean isVisible() {
        return root.getVisibility() == View.VISIBLE;
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

    private static TextView sectionLabel(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(12);
        label.setTextColor(Color.rgb(90, 90, 90));
        label.setPadding(0, dp(context, 6), 0, 0);
        return label;
    }

    private static int dp(Context context, int n) {
        return (int) (context.getResources().getDisplayMetrics().density * n);
    }
}