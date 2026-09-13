package mrp.checkin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import mrp.checkin.core.TokenStore;
import mrp.checkin.net.ApiClient;
import mrp.checkin.scan.CameraEngine;
import mrp.checkin.scan.ScanFeedback;
import mrp.checkin.ui.ResultSheet;

public class ScanActivity extends Activity {
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_CHECK = 1002;

    private static final int PHASE_SCANNING = 0;
    private static final int PHASE_VERIFYING = 1;
    private static final int PHASE_CONFIRMED = 2;
    private static final int PHASE_ERROR = 3;

    private TokenStore store;
    private ApiClient api;
    private CameraEngine engine;
    private ScanFeedback feedback;
    private Spinner sessionSpinner;
    private Button torchButton;
    private TextView hintView;
    private ResultSheet resultSheet;

    private final ArrayList<Integer> sessionIds = new ArrayList<>();
    private final ArrayList<String> sessionNames = new ArrayList<>();
    private int sessionId = 1;

    private int phase = PHASE_SCANNING;
    private String qrText;
    private JSONObject verifyJson;
    private JSONObject presenceJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new TokenStore(this);
        if (!store.hasToken()) {
            openLogin();
            return;
        }
        api = new ApiClient(store);
        feedback = new ScanFeedback(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = buildUi();
        setContentView(root);

        resultSheet = new ResultSheet(this, new ResultSheet.Actions() {
            @Override
            public void onPresence() {
                registerPresence();
            }

            @Override
            public void onConformity() {
                openConformity();
            }

            @Override
            public void onRescan() {
                resumeScanning();
            }
        });
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(resultSheet.root(), sheetLp);
        resultSheet.showIdle();

        engine = new CameraEngine(this, (android.view.TextureView) root.getChildAt(0),
                new CameraEngine.DecodeListener() {
                    @Override
                    public void onDecoded(String text) {
                        handleDecoded(text);
                    }

                    @Override
                    public void onCameraError(String message) {
                        showCameraError(message);
                    }

                    @Override
                    public void onTorch(boolean on) {
                        torchButton.setText(on ? "Apagar" : "Lanterna");
                    }
                }, new Handler(Looper.getMainLooper()));

        loadSessions();
        requestCameraPermission();
    }

    private FrameLayout buildUi() {
        FrameLayout root = new FrameLayout(this);

        android.view.TextureView texture = new android.view.TextureView(this);
        root.addView(texture, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(10), dp(16), dp(10), dp(10));
        header.setBackgroundColor(Color.argb(235, 255, 255, 255));

        TextView sessionLabel = new TextView(this);
        sessionLabel.setText("Sessão:");
        sessionLabel.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(sessionLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

        sessionSpinner = new Spinner(this);
        sessionSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sessionNames));
        sessionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sessionIds.size()) {
                    sessionId = sessionIds.get(position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        header.addView(sessionSpinner, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        torchButton = new Button(this);
        torchButton.setText("Lanterna");
        torchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                engine.toggleTorch();
            }
        });
        header.addView(torchButton);

        Button settingsButton = new Button(this);
        settingsButton.setText("Ajustes");
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanActivity.this, SettingsActivity.class));
            }
        });
        header.addView(settingsButton);

        Button logoutButton = new Button(this);
        logoutButton.setText("Sair");
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
        header.addView(logoutButton);

        root.addView(header, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        hintView = new TextView(this);
        hintView.setText("Aponte a câmera para o QR da credencial ou do chassi.");
        hintView.setTextSize(14);
        hintView.setTextColor(Color.WHITE);
        hintView.setBackgroundColor(Color.argb(150, 0, 0, 0));
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(dp(10), dp(6), dp(10), dp(6));
        root.addView(hintView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        ((FrameLayout.LayoutParams) hintView.getLayoutParams()).topMargin = dp(120);

        return root;
    }

    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            engine.start();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            engine.start();
        } else {
            showCameraError("Permissão de câmera negada. Conceda em Ajustes do sistema.");
        }
    }

    private void loadSessions() {
        api.get("/api/v1/sessions?status=open", true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.status == 401) {
                    logout();
                    return;
                }
                sessionIds.clear();
                sessionNames.clear();
                if (result.ok && result.array != null) {
                    for (int i = 0; i < result.array.length(); i++) {
                        JSONObject s = result.array.optJSONObject(i);
                        if (s != null) {
                            sessionIds.add(s.optInt("id", 1));
                            sessionNames.add(s.optString("name", "Sessão"));
                        }
                    }
                }
                if (sessionIds.isEmpty()) {
                    sessionIds.add(1);
                    sessionNames.add("Sessão 1 (padrão)");
                }
                sessionSpinner.setAdapter(new ArrayAdapter<>(ScanActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, sessionNames));
            }
        });
    }

    private void handleDecoded(String text) {
        if (phase != PHASE_SCANNING) {
            return;
        }
        qrText = text;
        presenceJson = null;
        engine.pauseScanning();
        phase = PHASE_VERIFYING;
        resultSheet.setWaiting("Verificando QR…");
        resultSheet.show();

        JSONObject body = new JSONObject();
        try {
            body.put("qr_text", text);
            body.put("session_id", sessionId);
        } catch (Exception ignored) {
        }
        api.post("/api/v1/checkin/verify", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.status == 401) {
                    logout();
                    return;
                }
                if (!result.ok || result.object == null || !result.object.optBoolean("valid", false)) {
                    feedback.warn();
                    phase = PHASE_ERROR;
                    renderError(result.detail() != null ? result.detail() : "QR inválido ou adulterado.");
                    return;
                }
                verifyJson = result.object;
                feedback.success();
                phase = PHASE_CONFIRMED;
                renderConfirmed();
            }
        });
    }

    private void renderConfirmed() {
        if (verifyJson == null) {
            return;
        }
        String subjectType = verifyJson.optString("subject_type", "member");
        JSONObject member = verifyJson.optJSONObject("member");
        JSONObject vehicle = verifyJson.optJSONObject("vehicle");
        if ("vehicle".equals(subjectType) && vehicle != null) {
            resultSheet.showVehicle(
                    vehicle.optString("name", "—"),
                    vehicle.optString("team", "—"),
                    vehicle.optString("conformity_status", null));
        } else if (member != null) {
            resultSheet.showParticipant(
                    member.optString("name", "—"),
                    member.optString("team", "—"),
                    member.optString("role", "member"));
        }
        if (presenceJson != null) {
            boolean duplicate = presenceJson.optBoolean("duplicate", false);
            String lastSeen = presenceJson.optString("last_seen_at", "");
            resultSheet.setPresenceResult(duplicate, lastSeen);
        }
    }

    private void registerPresence() {
        if (qrText == null) {
            return;
        }
        resultSheet.setWaiting("Registrando presença…");
        phase = PHASE_VERIFYING;
        JSONObject body = new JSONObject();
        try {
            body.put("qr_text", qrText);
            body.put("session_id", sessionId);
        } catch (Exception ignored) {
        }
        api.post("/api/v1/checkin/presence", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.status == 401) {
                    logout();
                    return;
                }
                if (!result.ok || result.object == null) {
                    feedback.warn();
                    phase = PHASE_ERROR;
                    renderError(result.detail() != null ? result.detail() : "Falha ao registrar presença.");
                    return;
                }
                presenceJson = result.object;
                feedback.success();
                phase = PHASE_CONFIRMED;
                renderConfirmed();
            }
        });
    }

    private void openConformity() {
        if (verifyJson == null || qrText == null) {
            return;
        }
        JSONObject vehicle = verifyJson.optJSONObject("vehicle");
        Intent intent = new Intent(this, CheckActivity.class);
        intent.putExtra("qr_text", qrText);
        intent.putExtra("session_id", sessionId);
        intent.putExtra("vehicle_name", vehicle != null ? vehicle.optString("name", "—") : "—");
        intent.putExtra("team", vehicle != null ? vehicle.optString("team", "—") : "—");
        intent.putExtra("conformity_status",
                vehicle != null ? vehicle.optString("conformity_status", "") : "");
        startActivityForResult(intent, REQUEST_CHECK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK) {
            resumeScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        engine.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            engine.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.shutdown();
        feedback.release();
    }

    private void resumeScanning() {
        phase = PHASE_SCANNING;
        qrText = null;
        verifyJson = null;
        presenceJson = null;
        resultSheet.showIdle();
        hintView.setText("Aponte a câmera para o QR da credencial ou do chassi.");
        engine.resumeScanning();
    }

    private void renderError(String message) {
        hintView.setText(message);
        resultSheet.showError(message, null);
    }

    private void showCameraError(String message) {
        phase = PHASE_ERROR;
        resultSheet.showError(message, "Câmera indisponível");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void logout() {
        store.clearToken();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}