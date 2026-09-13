package mrp.checkin;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;

public class SettingsActivity extends Activity {
    private TokenStore store;
    private EditText endpointField;
    private TextView healthView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new TokenStore(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(40), pad, pad);

        TextView title = new TextView(this);
        title.setText("Servidor (Nuvem/LAN)");
        title.setTextSize(22);
        root.addView(title);

        endpointField = new EditText(this);
        endpointField.setText(store.getEndpoint());
        endpointField.setTextSize(16);
        root.addView(endpointField);
        TextView hint = new TextView(this);
        hint.setTextSize(13);
        hint.setTextColor(Color.rgb(110, 110, 110));
        hint.setText("IP ou hostname com porta. Ex.: http://192.168.0.10:8000");
        root.addView(hint);

        Button saveButton = new Button(this);
        saveButton.setText("Salvar");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.setEndpoint(endpointField.getText().toString().trim());
                Toast.makeText(SettingsActivity.this, "Endpoint salvo.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(saveButton);

        Button testButton = new Button(this);
        testButton.setText("Testar conexão");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
        root.addView(testButton);

        healthView = new TextView(this);
        healthView.setTextSize(14);
        healthView.setPadding(0, dp(8), 0, 0);
        root.addView(healthView);

        TextView operator = new TextView(this);
        operator.setTextSize(13);
        operator.setTextColor(Color.rgb(110, 110, 110));
        operator.setText("Operador: " + (store.getOperator() != null ? store.getOperator() : "—"));
        root.addView(operator);

        Button logoutButton = new Button(this);
        logoutButton.setText("Sair (trocar de usuário)");
        logoutButton.setBackgroundColor(Color.argb(255, 240, 240, 240));
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.clearToken();
                finish();
            }
        });
        root.addView(logoutButton);

        Button backButton = new Button(this);
        backButton.setText("Voltar");
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        root.addView(backButton);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void testConnection() {
        store.setEndpoint(endpointField.getText().toString().trim());
        healthView.setText("Testando…");
        new ApiClient(store).get("/api/v1/health", false, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.ok && result.object != null) {
                    String status = result.object.optString("status", "?");
                    String db = result.object.optString("db", "?");
                    healthView.setText("Conectado (" + status + ", db=" + db + ").");
                    healthView.setTextColor(Color.rgb(0, 130, 0));
                } else {
                    healthView.setText("Falha de conexão. Verifique IP/porta e rede.");
                    healthView.setTextColor(Color.rgb(200, 0, 0));
                }
            }
        });
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}