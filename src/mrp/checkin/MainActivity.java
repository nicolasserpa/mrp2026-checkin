package mrp.checkin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import mrp.checkin.core.CrashReporter;
import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;
import mrp.checkin.ui.M3;

public class MainActivity extends Activity {
    private TokenStore store;
    private ApiClient api;
    private EditText usernameField;
    private EditText passwordField;
    private TextView statusView;
    private Button loginButton;
    private TextView endpointView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        M3.surfaceSystemBars(this);
        store = new TokenStore(this);
        CrashReporter.maybeShow(this);
        if (store.hasToken()) {
            openScan();
            return;
        }
        if (!store.hasEndpoint()) {
            openSetup();
            return;
        }
        api = new ApiClient(store);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(20), pad, dp(24));
        M3.edgeToEdgeTop(scroll, 44);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = M3.title(this, "MRP2026");
        title.setTextSize(34);
        title.setGravity(Gravity.CENTER);
        brand.addView(title);

        TextView subtitle = M3.label(this, "Check-in · Credenciamento & conformidade");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setAllCaps(false);
        subtitle.setLetterSpacing(0f);
        brand.addView(subtitle);
        root.addView(brand);

        root.addView(M3.spacer(this, 24));

        endpointView = M3.pill(this,
                "Servidor: " + store.getEndpoint(), M3.SURFACE_VARIANT, M3.ON_SURFACE_VARIANT);
        endpointView.setGravity(Gravity.CENTER);
        root.addView(endpointView);

        root.addView(M3.spacer(this, 28));

        root.addView(M3.label(this, "Usuário"));
        usernameField = M3.outlinedInput(this, "Seu usuário de staff");
        usernameField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        usernameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    passwordField.requestFocus();
                    return true;
                }
                return false;
            }
        });
        root.addView(usernameField);

        root.addView(M3.spacer(this, 12));

        root.addView(M3.label(this, "Senha"));
        LinearLayout passwordRow = M3.outlinedPassword(this, "Sua senha");
        root.addView(passwordRow);
        passwordField = M3.passwordInner(passwordRow);
        passwordField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin();
                    return true;
                }
                return false;
            }
        });

        root.addView(M3.spacer(this, 16));

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTextColor(M3.ERROR);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(8), 0, 0);
        root.addView(statusView);

        root.addView(M3.spacer(this, 8));

        loginButton = M3.filledButton(this, "Entrar");
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
        root.addView(loginButton);

        root.addView(M3.spacer(this, 12));

        Button settingsButton = M3.tonalButton(this, "Ajustar servidor (Nuvem/LAN)");
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        root.addView(settingsButton);

        scroll.addView(root);
        usernameField.requestFocus();
        return scroll;
    }

    private void doLogin() {
        final String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            statusView.setText("Informe usuário e senha.");
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("Entrando…");
        statusView.setText("");
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception ignored) {
        }
        api.post("/api/v1/auth/staff/login", body, false, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                loginButton.setEnabled(true);
                loginButton.setText("Entrar");
                if (result.ok && result.object != null && result.object.has("token")) {
                    store.setToken(result.object.optString("token"));
                    store.setOperator(username);
                    openScan();
                } else if (result.errorKind() == ApiClient.ErrorKind.NETWORK) {
                    statusView.setText("Servidor inacessível. Verifique IP/porta e a rede "
                            + "em Ajustes, e tente novamente.");
                } else if (result.errorKind() == ApiClient.ErrorKind.THROTTLE) {
                    statusView.setText("Muitas tentativas em sequência. Aguarde um instante.");
                } else {
                    String detail = result.detail();
                    statusView.setText(detail != null
                            ? detail
                            : "Usuário ou senha inválidos.");
                }
            }
        });
    }

    private void openScan() {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openSetup() {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}