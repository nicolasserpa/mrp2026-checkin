package mrp.checkin;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

import mrp.checkin.core.AuthFlow;
import mrp.checkin.core.DevFixtures;
import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;
import mrp.checkin.ui.M3;

public class CheckActivity extends Activity {
    private static final String[] ITEM_KEYS = {"dimensoes", "ratoeira", "materiais"};
    private static final String[] ITEM_LABELS = {
            "Dimensões (comprimento < 60 cm, largura < 30 cm, altura < 30 cm)",
            "Ratoeira 90 × 165 mm (± 10%)",
            "Materiais sustentáveis (≥ 50% da massa)",
    };

    private TokenStore store;
    private ApiClient api;
    private String vehicleName;
    private String team;
    private boolean submitting;

    private final ArrayList<CheckBox> itemChecks = new ArrayList<>();
    private RadioGroup resultGroup;
    private int passBtnId;
    private int failBtnId;
    private android.widget.EditText notesField;
    private TextView statusView;
    private LinearLayout formBox;
    private LinearLayout resultBox;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        M3.surfaceSystemBars(this);
        store = new TokenStore(this);
        api = new ApiClient(store);

        String qrText = getIntent().getStringExtra("qr_text");
        int sessionId = getIntent().getIntExtra("session_id", 1);
        vehicleName = getIntent().getStringExtra("vehicle_name");
        team = getIntent().getStringExtra("team");
        String conformityStatus = getIntent().getStringExtra("conformity_status");
        if (qrText == null) {
            finish();
            return;
        }

        setContentView(buildUi(conformityStatus, qrText, sessionId));
    }

    private View buildUi(final String conformityStatus, final String qrText, final int sessionId) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(M3.surface(this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(20), pad, dp(24));
        // Topo 20dp + status bar; rodapé soma navbar/gesto/teclado para o
        // "Enviar avaliação" ficar tocável acima da barra.
        M3.edgeToEdge(scroll, 20, 0, 0);

        root.addView(M3.headline(this, "Conformidade técnica"));
        TextView sub = M3.label(this, "Inspeção manual de segurança da equipe fiscal.");
        sub.setAllCaps(false);
        sub.setLetterSpacing(0f);
        sub.setPadding(0, 0, 0, dp(4));
        root.addView(sub);

        root.addView(M3.spacer(this, 16));

        LinearLayout vehicleCard = M3.card(this);
        TextView vehName = M3.title(this, vehicleName);
        vehName.setPadding(0, 0, 0, dp(4));
        vehicleCard.addView(vehName);
        TextView vehTeam = M3.body(this, "Equipe: " + team);
        vehTeam.setTextColor(M3.onSurfaceVariant(this));
        vehTeam.setTextSize(15);
        vehTeam.setPadding(0, 0, 0, dp(10));
        vehicleCard.addView(vehTeam);
        vehicleCard.addView(M3.pill(this, "Status atual: " + statusLabel(conformityStatus),
                statusContainer(conformityStatus), statusForeground(conformityStatus)));
        root.addView(vehicleCard);

        root.addView(M3.spacer(this, 16));

        formBox = new LinearLayout(this);
        formBox.setOrientation(LinearLayout.VERTICAL);

        LinearLayout resultCard = M3.card(this);
        resultCard.addView(M3.label(this, "Resultado da inspeção"));
        resultCard.addView(M3.spacer(this, 8));
        resultGroup = new RadioGroup(this);
        resultGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton passBtn = radioRow("Aprovado");
        passBtnId = View.generateViewId();
        passBtn.setId(passBtnId);
        resultGroup.addView(passBtn);
        RadioButton failBtn = radioRow("Reprovado");
        failBtnId = View.generateViewId();
        failBtn.setId(failBtnId);
        failBtn.setPadding(0, dp(8), 0, 0);
        resultGroup.addView(failBtn);
        resultCard.addView(resultGroup);
        formBox.addView(resultCard);

        formBox.addView(M3.spacer(this, 16));

        LinearLayout itemsCard = M3.card(this);
        itemsCard.addView(M3.label(this, "Itens conferidos"));
        itemsCard.addView(M3.spacer(this, 6));
        for (int i = 0; i < ITEM_KEYS.length; i++) {
            CheckBox check = new CheckBox(this);
            check.setText(ITEM_LABELS[i]);
            check.setTextSize(15);
            check.setTextColor(M3.onSurface(this));
            check.setButtonTintList(M3.checkTint(this));
            check.setMinHeight(dp(48));
            check.setGravity(Gravity.CENTER_VERTICAL);
            check.setPadding(0, dp(4), 0, dp(4));
            itemChecks.add(check);
            itemsCard.addView(check);
        }
        formBox.addView(itemsCard);

        formBox.addView(M3.spacer(this, 16));

        LinearLayout notesCard = M3.card(this);
        notesCard.addView(M3.label(this, "Notas (opcional)"));
        notesCard.addView(M3.spacer(this, 10));
        notesField = M3.outlinedTextArea(this, "Observações do fiscal…");
        notesCard.addView(notesField);
        formBox.addView(notesCard);

        root.addView(formBox);

        root.addView(M3.spacer(this, 8));

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTextColor(M3.error(this));
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusView);

        Button submitButton = M3.filledButton(this, "Enviar avaliação");
        this.submitButton = submitButton;
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit(qrText, sessionId);
            }
        });
        root.addView(submitButton);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setGravity(Gravity.CENTER_HORIZONTAL);
        resultBox.setPadding(0, dp(24), 0, 0);
        resultBox.setVisibility(View.GONE);
        root.addView(resultBox);

        scroll.addView(root);
        return scroll;
    }

    private RadioButton radioRow(String text) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTextSize(16);
        rb.setTextColor(M3.onSurface(this));
        rb.setButtonTintList(M3.checkTint(this));
        rb.setMinHeight(dp(48));
        rb.setGravity(Gravity.CENTER_VERTICAL);
        return rb;
    }

    private void submit(String qrText, int sessionId) {
        if (submitting) {
            return;
        }
        int checkedId = resultGroup.getCheckedRadioButtonId();
        RadioButton selected = checkedId == -1 ? null : findViewById(checkedId);
        if (selected == null) {
            statusView.setText("Selecione o resultado: aprovado ou reprovado.");
            statusView.setTextColor(M3.error(this));
            return;
        }
        String result = checkedId == failBtnId ? "fail" : "pass";

        JSONObject items = new JSONObject();
        for (int i = 0; i < itemChecks.size() && i < ITEM_KEYS.length; i++) {
            if (itemChecks.get(i).isChecked()) {
                try {
                    items.put(ITEM_KEYS[i], true);
                } catch (Exception ignored) {
                }
            }
        }
        if ("pass".equals(result) && items.length() == 0) {
            statusView.setText("Para aprovar, marque ao menos um item conferido.");
            statusView.setTextColor(M3.error(this));
            return;
        }

        submitting = true;
        submitButton.setEnabled(false);
        submitButton.setText("Enviando…");
        statusView.setText("");

        JSONObject body = new JSONObject();
        try {
            body.put("qr_text", qrText);
            body.put("session_id", sessionId);
            body.put("result", result);
            body.put("items", items);
            String notes = notesField.getText().toString().trim();
            if (!notes.isEmpty()) {
                body.put("notes", notes);
            }
        } catch (Exception ignored) {
        }

        if (store.isDev()) {
            submitting = false;
            submitButton.setEnabled(true);
            submitButton.setText("Enviar avaliação");
            renderDone(DevFixtures.conformityFor(result));
            return;
        }

        api.post("/api/v1/checkin/conformity", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                submitting = false;
                submitButton.setEnabled(true);
                submitButton.setText("Enviar avaliação");
                if (result.errorKind() == ApiClient.ErrorKind.AUTH) {
                    AuthFlow.goLogin(CheckActivity.this, store, MainActivity.class);
                    return;
                }
                if (!result.ok || result.object == null) {
                    if (result.errorKind() == ApiClient.ErrorKind.NETWORK) {
                        statusView.setText("Servidor inacessível. Verifique a rede e tente "
                                + "novamente.");
                    } else if (result.errorKind() == ApiClient.ErrorKind.THROTTLE) {
                        statusView.setText("Muitas tentativas em sequência. Aguarde um instante.");
                    } else {
                        statusView.setText(result.detail() != null ? result.detail()
                                : "Falha ao registrar conformidade.");
                    }
                    statusView.setTextColor(M3.error(CheckActivity.this));
                    return;
                }
                renderDone(result.object);
            }
        });
    }

    private void renderDone(JSONObject conformity) {
        formBox.setVisibility(View.GONE);
        resultBox.setVisibility(View.VISIBLE);

        boolean pass = "pass".equals(conformity.optString("conformity_status", ""));
        int bg = pass ? M3.successContainer(this) : M3.errorContainer(this);
        int fg = pass ? M3.onSuccessContainer(this) : M3.onErrorContainer(this);

        TextView badgePill = M3.pill(this, pass ? "Veículo aprovado" : "Veículo reprovado", bg, fg);
        resultBox.addView(badgePill);

        TextView info = M3.body(this, "Veículo: " + vehicleName
                + "\nRegistrado em: " + conformity.optString("checked_at", ""));
        info.setTextSize(15);
        info.setGravity(Gravity.CENTER);
        info.setPadding(M3.dp(this, 16), M3.dp(this, 12), M3.dp(this, 16), M3.dp(this, 24));
        resultBox.addView(info);

        Button backButton = M3.filledButton(this, pass
                ? "Voltar para a leitura" : "Voltar para o escaneamento");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        resultBox.addView(backButton);
    }

    private int statusContainer(String status) {
        if ("pass".equals(status)) {
            return M3.successContainer(this);
        }
        if ("fail".equals(status)) {
            return M3.errorContainer(this);
        }
        return M3.surfaceVariant(this);
    }

    private int statusForeground(String status) {
        if ("pass".equals(status)) {
            return M3.onSuccessContainer(this);
        }
        if ("fail".equals(status)) {
            return M3.onErrorContainer(this);
        }
        return M3.onSurfaceVariant(this);
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }

    private static String statusLabel(String status) {
        if (status == null || status.isEmpty()) {
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