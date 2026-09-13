package mrp.checkin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;

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
        store = new TokenStore(this);
        if (store.hasToken()) {
            openScan();
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
        root.setPadding(pad, dp(60), pad, pad);

        TextView title = new TextView(this);
        title.setText("MRP2026 Check-In");
        title.setTextSize(28);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setTextSize(15);
        subtitle.setText("Acesso de staff / fiscal — credenciamento e conformidade");
        subtitle.setTextColor(Color.rgb(90, 90, 90));
        root.addView(subtitle);

        endpointView = new TextView(this);
        endpointView.setTextSize(13);
        endpointView.setText("Servidor: " + store.getEndpoint());
        endpointView.setTextColor(Color.rgb(110, 110, 110));
        root.addView(endpointView);

        root.addView(spacer(this, dp(24)));

        usernameField = input(this, "Usuário");
        root.addView(usernameField);

        passwordField = input(this, "Senha");
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordField);

        statusView = new TextView(this);
        statusView.setTextSize(14);
        statusView.setTextColor(Color.rgb(200, 0, 0));
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(8), 0, 0);
        root.addView(statusView);

        loginButton = new Button(this);
        loginButton.setText("Entrar");
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
        root.addView(loginButton);

        Button settingsButton = new Button(this);
        settingsButton.setText("Ajustar servidor (Nuvem/LAN)");
        settingsButton.setBackgroundColor(Color.argb(255, 240, 240, 240));
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        root.addView(settingsButton);

        scroll.addView(root);
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
                } else {
                    String detail = result.detail();
                    statusView.setText(detail != null
                            ? detail
                            : "Credenciais inválidas ou servidor inacessível. Verifique o servidor em Ajustes.");
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

    private static EditText input(Activity context, String hint) {
        EditText field = new EditText(context);
        field.setHint(hint);
        field.setTextSize(17);
        field.setSingleLine(true);
        return field;
    }

    private static View spacer(Activity context, int height) {
        View view = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        view.setLayoutParams(lp);
        return view;
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}