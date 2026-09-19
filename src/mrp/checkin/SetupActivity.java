package mrp.checkin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ServerScanner;
import mrp.checkin.ui.M3;

/**
 * Tela de primeiro uso (e de "redescobrir servidor"): localiza a API na LAN via
 * scan da sub-rede ou aceita endpoint manual. Nunca deixa o app assumir um IP
 * default hardcoded sem o usuário confirmar.
 */
public class SetupActivity extends Activity {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();

    private TokenStore store;
    private TextView statusView;
    private LinearLayout resultsView;
    private Button scanButton;
    private EditText endpointField;
    private final AtomicReference<ServerScanner> scannerRef =
            new AtomicReference<ServerScanner>(null);

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
        M3.edgeToEdgeTop(scroll, 44);

        root.addView(M3.headline(this, "Servidor da API"));

        TextView sub = M3.label(this, "Este aparelho precisa apontar para o servidor "
                + "do evento. Encontre o servidor na rede ou digite o endereço.");
        sub.setAllCaps(false);
        sub.setLetterSpacing(0f);
        sub.setTextSize(14);
        sub.setPadding(0, 0, 0, dp(4));
        root.addView(sub);

        root.addView(M3.spacer(this, 16));

        LinearLayout scanCard = M3.card(this);
        scanCard.addView(M3.label(this, "Encontrar na rede"));

        scanButton = M3.filledButton(this, "Buscar servidor na rede");
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
        scanCard.addView(scanButton);

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setTextColor(M3.ON_SURFACE_VARIANT);
        statusView.setPadding(0, dp(8), 0, 0);
        scanCard.addView(statusView);

        resultsView = new LinearLayout(this);
        resultsView.setOrientation(LinearLayout.VERTICAL);
        scanCard.addView(resultsView);
        root.addView(scanCard);

        root.addView(M3.spacer(this, 16));

        LinearLayout manualCard = M3.card(this);
        manualCard.addView(M3.label(this, "Digitar endereço"));
        endpointField = M3.outlinedInput(this, "http://192.168.0.30:8000");
        endpointField.setText(store.getEndpoint());
        endpointField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        endpointField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveAndGo();
                    return true;
                }
                return false;
            }
        });
        manualCard.addView(endpointField);

        TextView hint = M3.label(this, "IP ou hostname com porta. Ex.: http://192.168.0.30:8000");
        hint.setAllCaps(false);
        hint.setLetterSpacing(0f);
        hint.setPadding(0, dp(4), 0, 0);
        manualCard.addView(hint);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button testButton = M3.tonalButton(this, "Testar e salvar");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndGo();
            }
        });
        actions.addView(testButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        manualCard.addView(actions);

        root.addView(manualCard);

        root.addView(M3.spacer(this, 16));

        Button backButton = M3.outlinedButton(this, "Voltar ao login");
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

    @Override
    protected void onDestroy() {
        ServerScanner s = scannerRef.get();
        if (s != null) {
            s.stop();
        }
        super.onDestroy();
    }

    private void startScan() {
        ServerScanner current = scannerRef.get();
        if (current != null && current.isRunning()) {
            return;
        }
        scanButton.setEnabled(false);
        statusView.setTextColor(M3.ON_SURFACE_VARIANT);
        statusView.setText("Preparando…");
        resultsView.removeAllViews();

        final int targetPort = currentPort();

        WORK.execute(new Runnable() {
            @Override
            public void run() {
                final ServerScanner s = new ServerScanner();
                if (!scannerRef.compareAndSet(null, s)) {
                    return;
                }
                try {
                    runScan(s, targetPort);
                } finally {
                    scannerRef.compareAndSet(s, null);
                }
            }
        });
    }

    private void runScan(ServerScanner s, int targetPort) {
        final String local = ServerScanner.localIpv4();
        final List<String> hosts = ServerScanner.hostsInNetwork(local);
        if (local == null || hosts.isEmpty()) {
            fail("Sem Wi-Fi/rede local. Digite o endereço acima.");
            return;
        }
        final String prefix = hosts.get(0).substring(0,
                hosts.get(0).lastIndexOf('.') + 1) + "x";
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                statusView.setText("Varrendo " + prefix + " (porta "
                        + targetPort + ")…");
            }
        });
        s.scan(hosts, new ServerScanner.HostChecker() {
            @Override
            public boolean isServer(String host) {
                return isApi(host, targetPort);
            }
        }, new ServerScanner.Listener() {
            @Override
            public void onProgress(final int checked, final int total) {
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        statusView.setText("Verificando… " + checked + "/" + total);
                    }
                });
            }

            @Override
            public void onFound(final String host) {
                final String endpoint = "http://" + host + ":" + targetPort;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        store.setEndpoint(endpoint);
                        scanButton.setEnabled(true);
                        statusView.setTextColor(M3.SUCCESS);
                        statusView.setText("Servidor encontrado: " + host);
                        addResult(host, endpoint);
                    }
                });
            }

            @Override
            public void onNothing() {
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        scanButton.setEnabled(true);
                        statusView.setTextColor(M3.WARN);
                        statusView.setText("Nenhum servidor encontrado. "
                                + "Confirme o endereço acima.");
                    }
                });
            }
        });
    }

    private void fail(String message) {
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                scanButton.setEnabled(true);
                statusView.setTextColor(M3.ERROR);
                statusView.setText(message);
            }
        });
    }

    private int currentPort() {
        String base = store.getEndpoint();
        String rest;
        if (base.startsWith("https://")) {
            rest = base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            rest = base.substring("http://".length());
        } else {
            rest = base;
        }
        int colon = rest.lastIndexOf(':');
        if (colon >= 0) {
            String after = rest.substring(colon + 1);
            int slash = after.indexOf('/');
            if (slash >= 0) {
                after = after.substring(0, slash);
            }
            try {
                return Integer.parseInt(after);
            } catch (NumberFormatException ignored) {
            }
        }
        return 8000;
    }

    private void addResult(final String host, final String endpoint) {
        Button b = M3.tonalButton(this, "Usar " + host);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.setEndpoint(endpoint);
                goLogin();
            }
        });
        resultsView.addView(b);
    }

    private void saveAndGo() {
        String url = endpointField.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Informe um endereço de servidor (IP ou host).",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        store.setEndpoint(url);
        goLogin();
    }

    private void goLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** GET http://host:port/api/v1/health com timeout curto (probe de varredura). */
    private boolean isApi(String host, int port) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + host + ":" + port + "/api/v1/health");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestProperty("Accept", "application/json");
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                return false;
            }
            java.io.InputStream is = conn.getInputStream();
            try {
                byte[] buf = new byte[256];
                int n = is.read(buf);
                String body = n > 0 ? new String(buf, 0, n, "UTF-8") : "";
                return body.contains("\"status\"");
            } finally {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}