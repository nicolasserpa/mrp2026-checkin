package mrp.checkin;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;

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

    private final ArrayList<CheckBox> itemChecks = new ArrayList<>();
    private RadioGroup resultGroup;
    private android.widget.EditText notesField;
    private TextView statusView;
    private LinearLayout formBox;
    private LinearLayout resultBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(40), pad, pad);

        TextView title = new TextView(this);
        title.setText("Conformidade técnica");
        title.setTextSize(22);
        root.addView(title);

        formBox = new LinearLayout(this);
        formBox.setOrientation(LinearLayout.VERTICAL);

        TextView vehName = new TextView(this);
        vehName.setText(vehicleName);
        vehName.setTextSize(24);
        vehName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        formBox.addView(vehName);

        TextView vehTeam = new TextView(this);
        vehTeam.setText("Equipe: " + team);
        vehTeam.setTextSize(16);
        vehTeam.setTextColor(Color.rgb(90, 90, 90));
        formBox.addView(vehTeam);

        TextView current = new TextView(this);
        current.setTextSize(14);
        current.setText("Status atual: " + statusLabel(conformityStatus));
        formBox.addView(current);

        formBox.addView(section(this, "Resultado da inspeção"));
        resultGroup = new RadioGroup(this);
        resultGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton passBtn = new RadioButton(this);
        passBtn.setText("Aprovado");
        resultGroup.addView(passBtn);
        RadioButton failBtn = new RadioButton(this);
        failBtn.setText("Reprovado");
        resultGroup.addView(failBtn);
        formBox.addView(resultGroup);

        formBox.addView(section(this, "Itens conferidos"));
        for (int i = 0; i < ITEM_KEYS.length; i++) {
            CheckBox check = new CheckBox(this);
            check.setText(ITEM_LABELS[i]);
            itemChecks.add(check);
            formBox.addView(check);
        }

        formBox.addView(section(this, "Notas (opcional)"));
        notesField = new android.widget.EditText(this);
        notesField.setTextSize(15);
        notesField.setHint("Observações do fiscal…");
        notesField.setMinLines(2);
        formBox.addView(notesField);

        Button submitButton = new Button(this);
        submitButton.setText("Enviar avaliação");
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit(qrText, sessionId);
            }
        });
        formBox.addView(submitButton);

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTextColor(Color.rgb(200, 0, 0));
        statusView.setPadding(0, dp(8), 0, 0);
        formBox.addView(statusView);

        root.addView(formBox);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setVisibility(View.GONE);
        root.addView(resultBox);

        scroll.addView(root);
        return scroll;
    }

    private void submit(String qrText, int sessionId) {
        int checkedId = resultGroup.getCheckedRadioButtonId();
        RadioButton selected = checkedId == -1 ? null : findViewById(checkedId);
        if (selected == null) {
            statusView.setText("Selecione o resultado: aprovado ou reprovado.");
            statusView.setTextColor(Color.rgb(200, 0, 0));
            return;
        }
        String result = "Reprovado".contentEquals(selected.getText()) ? "fail" : "pass";

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
            statusView.setTextColor(Color.rgb(200, 0, 0));
            return;
        }

        statusView.setText("");
        final RadioButton btn = selected;
        btn.setEnabled(false);

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

        api.post("/api/v1/checkin/conformity", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                btn.setEnabled(true);
                if (result.status == 401) {
                    store.clearToken();
                    finish();
                    return;
                }
                if (!result.ok || result.object == null) {
                    statusView.setText(result.detail() != null ? result.detail() : "Falha ao registrar conformidade.");
                    statusView.setTextColor(Color.rgb(200, 0, 0));
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
        TextView badge = new TextView(this);
        badge.setText(pass ? "Veículo aprovado" : "Veículo reprovado");
        badge.setTextSize(20);
        badge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        badge.setTextColor(pass ? Color.rgb(0, 130, 0) : Color.rgb(200, 0, 0));
        resultBox.addView(badge);

        TextView info = new TextView(this);
        info.setTextSize(16);
        info.setText("Veículo: " + vehicleName + "\nRegistrado em: " + conformity.optString("checked_at", ""));
        info.setPadding(0, dp(10), 0, 0);
        resultBox.addView(info);

        Button backButton = new Button(this);
        backButton.setText("Voltar para a leitura");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        resultBox.addView(backButton);
    }

    private static TextView section(Activity context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(13);
        label.setTextColor(Color.rgb(90, 90, 90));
        label.setPadding(0, dpStatic(context, 12), 0, 0);
        return label;
    }

    private static int dpStatic(Activity context, int n) {
        return (int) (context.getResources().getDisplayMetrics().density * n);
    }

    private int dp(int n) {
        return dpStatic(this, n);
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