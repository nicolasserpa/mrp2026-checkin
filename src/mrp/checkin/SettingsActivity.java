package mrp.checkin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import mrp.checkin.core.AuthFlow;
import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;
import mrp.checkin.ui.M3;

public class SettingsActivity extends Activity {
    private TokenStore store;
    private EditText endpointField;
    private TextView healthView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        M3.surfaceSystemBars(this);
        store = new TokenStore(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(20), pad, dp(24));
        M3.edgeToEdgeTop(scroll, 20);

        root.addView(M3.headline(this, "Ajustes"));

        TextView sub = M3.label(this, "Servidor da API e sessão do operador.");
        sub.setAllCaps(false);
        sub.setLetterSpacing(0f);
        sub.setTextSize(14);
        sub.setPadding(0, 0, 0, dp(4));
        root.addView(sub);

        root.addView(M3.spacer(this, 16));

        LinearLayout endpointCard = M3.card(this);
        endpointCard.addView(M3.label(this, "Endpoint da API"));
        endpointField = M3.outlinedInput(this, "http://192.168.0.30:8000");
        endpointField.setText(store.getEndpoint());
        endpointField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        endpointField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save();
                    return true;
                }
                return false;
            }
        });
        endpointCard.addView(endpointField);

        endpointCard.addView(M3.spacer(this, 8));

        TextView hint = M3.label(this, "IP ou hostname com porta. Ex.: http://192.168.0.30:8000");
        hint.setAllCaps(false);
        hint.setLetterSpacing(0f);
        hint.setPadding(0, dp(4), 0, 0);
        endpointCard.addView(hint);

        healthView = new TextView(this);
        healthView.setTextSize(13);
        healthView.setTextColor(M3.ON_SURFACE_VARIANT);
        healthView.setPadding(0, dp(8), 0, 0);
        endpointCard.addView(healthView);

        endpointCard.addView(M3.spacer(this, 8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button testButton = M3.tonalButton(this, "Testar conexão");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
        actions.addView(testButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actionSpacer(actions, 12);
        Button saveButton = M3.filledButton(this, "Salvar");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        actions.addView(saveButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        endpointCard.addView(actions);

        root.addView(endpointCard);

        root.addView(M3.spacer(this, 16));

        LinearLayout sessionCard = M3.card(this);
        sessionCard.addView(M3.label(this, "Operador"));
        TextView operator = M3.body(this, store.getOperator() != null ? store.getOperator() : "—");
        operator.setPadding(0, dp(2), 0, dp(8));
        sessionCard.addView(operator);

        Button logoutButton = M3.errorButton(this, "Sair (trocar de usuário)");
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Sair da sessão?")
                        .setMessage("O token será apagado deste aparelho e você voltará "
                                + "para a tela de login.")
                        .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AuthFlow.goLogin(SettingsActivity.this, store,
                                        MainActivity.class);
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        sessionCard.addView(logoutButton);
        root.addView(sessionCard);

        root.addView(M3.spacer(this, 16));

        LinearLayout scanCard = M3.card(this);
        scanCard.addView(M3.label(this, "Redescobrir servidor"));
        TextView scanInfo = M3.label(this, "Encontrar automaticamente o servidor na rede local "
                + "ou ajustar o endereço manualmente.");
        scanInfo.setAllCaps(false);
        scanInfo.setLetterSpacing(0f);
        scanInfo.setTextSize(14);
        scanInfo.setPadding(0, 0, 0, dp(8));
        scanCard.addView(scanInfo);

        Button discoverButton = M3.tonalButton(this, "Buscar na rede (Setup)");
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, SetupActivity.class);
                startActivity(intent);
            }
        });
        scanCard.addView(discoverButton);

        root.addView(scanCard);

        root.addView(M3.spacer(this, 16));

        Button backButton = M3.outlinedButton(this, "Voltar");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        root.addView(backButton);

        root.addView(M3.spacer(this, 12));

        TextView version = M3.label(this, "Versão " + versionName());
        version.setAllCaps(false);
        version.setLetterSpacing(0f);
        version.setGravity(Gravity.CENTER);
        version.setTextColor(M3.ON_SURFACE_VARIANT);
        root.addView(version);

        scroll.addView(root);
        setContentView(scroll);
    }

    private String versionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private void actionSpacer(LinearLayout parent, int width) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(dp(width), 1));
    }

    private void save() {
        String url = endpointField.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Informe um endereço de servidor (IP ou host).",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        store.setEndpoint(url);
        Toast.makeText(SettingsActivity.this, "Endpoint salvo.", Toast.LENGTH_SHORT).show();
    }

    private void testConnection() {
        String url = endpointField.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Informe um endereço de servidor (IP ou host).",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        store.setEndpoint(url);
        healthView.setText("Testando…");
        healthView.setTextColor(M3.ON_SURFACE_VARIANT);
        new ApiClient(store).get("/api/v1/health", false, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.ok && result.object != null) {
                    String status = result.object.optString("status", "?");
                    String db = result.object.optString("db", "?");
                    healthView.setText("Conectado (" + status + ", db=" + db + ").");
                    healthView.setTextColor(M3.SUCCESS);
                } else if (result.errorKind() == ApiClient.ErrorKind.NETWORK) {
                    healthView.setText("Servidor inacessível. Confira IP/porta e Wi-Fi.");
                    healthView.setTextColor(M3.ERROR);
                } else {
                    healthView.setText("Resposta inesperada do servidor. Confira o endereço.");
                    healthView.setTextColor(M3.ERROR);
                }
            }
        });
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}