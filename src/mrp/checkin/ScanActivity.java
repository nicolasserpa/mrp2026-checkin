package mrp.checkin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

import mrp.checkin.core.AuthFlow;
import mrp.checkin.core.CrashReporter;
import mrp.checkin.core.ScanPhase;
import mrp.checkin.core.TokenStore;
import mrp.checkin.core.VerifyParser;
import mrp.checkin.net.ApiClient;
import mrp.checkin.scan.AutoFitTextureView;
import mrp.checkin.scan.CameraEngine;
import mrp.checkin.scan.ScanFeedback;
import mrp.checkin.ui.M3;
import mrp.checkin.ui.ResultSheet;

public class ScanActivity extends Activity {
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_CHECK = 1002;
    private static final int SCAN_STATUS_COLOR = 0xB3000000;

    private static final String PREF_RETRY = "checkin_prefs";
    private static final String KEY_SESSION_ID = "session_id";

    private enum RetryTarget { NONE, VERIFY, PRESENCE }

    private TokenStore store;
    private ApiClient api;
    private AutoFitTextureView textureView;
    private CameraEngine engine;
    private ScanFeedback feedback;
    private Spinner sessionSpinner;
    private Button torchFloat;
    private TextView hintView;
    private ResultSheet resultSheet;
    private LinearLayout headerView;
    private View viewfinder;

    private final ArrayList<Integer> sessionIds = new ArrayList<>();
    private final ArrayList<String> sessionNames = new ArrayList<>();
    private int sessionId = 1;

    private ScanPhase phase = ScanPhase.SCANNING;
    private RetryTarget retryTarget = RetryTarget.NONE;
    private String qrText;
    private JSONObject verifyJson;
    private JSONObject presenceJson;
    private int loadSessionsAttempt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashReporter.maybeShow(this);
        store = new TokenStore(this);
        if (!store.hasToken()) {
            openLogin();
            return;
        }
        api = new ApiClient(store);
        feedback = new ScanFeedback(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(SCAN_STATUS_COLOR);
        getWindow().setNavigationBarColor(M3.SURFACE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }

        SharedPreferences prefs = getSharedPreferences(PREF_RETRY, MODE_PRIVATE);
        sessionId = prefs.getInt(KEY_SESSION_ID, 1);

        FrameLayout root = buildUi();
        setContentView(root);

        resultSheet = new ResultSheet(this, new ResultSheet.Actions() {
            @Override
            public void onPresence() {
                submitPresence();
            }

            @Override
            public void onConformity() {
                openConformity();
            }

            @Override
            public void onRescan() {
                handleRescanAction();
            }
        });
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(resultSheet.root(), sheetLp);
        resultSheet.showIdle();
        updateTorchVisibility();

        engine = new CameraEngine(this, textureView,
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
                        styleTorch(on);
                    }

                    @Override
                    public void onFlashSupport(boolean available) {
                        torchFloat.setEnabled(available);
                        torchFloat.setAlpha(available ? 1f : 0.4f);
                        torchFloat.setVisibility(available ? View.VISIBLE : View.GONE);
                    }
                }, new Handler(Looper.getMainLooper()));

        loadSessions();
        requestCameraPermission();
    }

    private FrameLayout buildUi() {
        FrameLayout root = new FrameLayout(this);

        textureView = new AutoFitTextureView(this);
        root.addView(textureView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        headerView = new LinearLayout(this);
        headerView.setOrientation(LinearLayout.HORIZONTAL);
        headerView.setGravity(Gravity.CENTER_VERTICAL);
        headerView.setPadding(dp(10), dp(16), dp(10), dp(10));
        headerView.setBackgroundColor(SCAN_STATUS_COLOR);

        TextView sessionLabel = M3.label(this, "Sessão");
        sessionLabel.setTextColor(Color.WHITE);
        sessionLabel.setGravity(Gravity.CENTER_VERTICAL);
        sessionLabel.setPadding(0, 0, dp(8), 0);
        headerView.addView(sessionLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

        sessionSpinner = new Spinner(this);
        sessionSpinner.setAdapter(darkSpinnerAdapter());
        sessionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < sessionIds.size()) {
                    sessionId = sessionIds.get(position);
                    getSharedPreferences(PREF_RETRY, MODE_PRIVATE)
                            .edit().putInt(KEY_SESSION_ID, sessionId).apply();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        headerView.addView(sessionSpinner, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        Button settingsButton = compactHeaderButton(M3.textButton(this, "Ajustes"), Color.WHITE);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanActivity.this, SettingsActivity.class));
            }
        });
        headerView.addView(settingsButton);

        Button logoutButton = compactHeaderButton(M3.textButton(this, "Sair"), Color.WHITE);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthFlow.goLogin(ScanActivity.this, store, MainActivity.class);
            }
        });
        headerView.addView(logoutButton);

        root.addView(headerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        hintView = new TextView(this);
        hintView.setText("Aponte a câmera para o QR da credencial ou do chassi.");
        hintView.setTextSize(14);
        hintView.setTextColor(Color.WHITE);
        hintView.setGravity(Gravity.CENTER);
        hintView.setPadding(dp(14), dp(8), dp(14), dp(8));
        hintView.setBackground(M3.rounded(M3.SCRIM, M3.SHAPE_PILL, this));
        root.addView(hintView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        ((FrameLayout.LayoutParams) hintView.getLayoutParams()).topMargin = dp(120);

        viewfinder = new View(this);
        android.graphics.drawable.GradientDrawable finderFg =
                M3.rounded(Color.TRANSPARENT, 14, this);
        finderFg.setStroke(dp(3), Color.WHITE);
        viewfinder.setBackground(finderFg);
        root.addView(viewfinder, new FrameLayout.LayoutParams(dp(240), dp(240),
                Gravity.CENTER));

        torchFloat = M3.scrimButton(this, "Lanterna", Color.WHITE);
        torchFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                engine.toggleTorch();
            }
        });
        FrameLayout.LayoutParams torchLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        torchLp.rightMargin = dp(16);
        torchLp.bottomMargin = dp(96);
        root.addView(torchFloat, torchLp);

        final int baseHeaderTopPad = dp(16);
        final int baseHintTopMargin = dp(120);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int topInset = insets.getSystemWindowInsetTop();
                headerView.setPadding(dp(10), baseHeaderTopPad + topInset, dp(10), dp(10));
                ((FrameLayout.LayoutParams) hintView.getLayoutParams()).topMargin =
                        baseHintTopMargin + topInset;
                return insets;
            }
        });

        return root;
    }

    private ArrayAdapter<String> darkSpinnerAdapter() {
        return new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, sessionNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(15);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                return tv;
            }
        };
    }

    private Button compactHeaderButton(Button button, int textColor) {
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setMinHeight(dp(48));
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(M3.ripple(M3.RIPPLE_ON_CONTAINER,
                M3.rounded(Color.TRANSPARENT, M3.SHAPE_PILL, this)));
        return button;
    }

    private void styleTorch(boolean on) {
        if (on) {
            torchFloat.setTextColor(M3.ON_PRIMARY_CONTAINER);
            torchFloat.setBackground(M3.ripple(M3.RIPPLE_ON_CONTAINER,
                    M3.rounded(M3.PRIMARY_CONTAINER, M3.SHAPE_PILL, this)));
            torchFloat.setText("Lanterna acesa");
        } else {
            torchFloat.setTextColor(Color.WHITE);
            torchFloat.setBackground(M3.ripple(M3.RIPPLE_ON_PRIMARY,
                    M3.rounded(M3.SCRIM_SOFT, M3.SHAPE_PILL, this)));
            torchFloat.setText("Lanterna");
        }
        torchFloat.requestLayout();
    }

    private void updateTorchVisibility() {
        boolean sheetVisible = resultSheet != null && resultSheet.isVisible();
        torchFloat.setVisibility(sheetVisible ? View.GONE : View.VISIBLE);
    }

    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            engine.setCameraPermission(true);
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
            engine.setCameraPermission(true);
            engine.start();
        } else {
            onCameraPermissionDenied();
        }
    }

    private void onCameraPermissionDenied() {
        phase = ScanPhase.ERROR;
        resultSheet.showError("Permissão de câmera negada. Conceda em Ajustes do sistema.",
                "Câmera indisponível");
        updateTorchVisibility();
        new AlertDialog.Builder(this)
                .setTitle("Câmera indisponível")
                .setMessage("A permissão de câmera foi negada. Conceda a permissão de câmera "
                        + "em Ajustes do sistema para escanear QRs.")
                .setPositiveButton("Abrir Ajustes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)));
                    }
                })
                .setNegativeButton("Entendi", null)
                .show();
    }

    private void loadSessions() {
        api.get("/api/v1/sessions?status=open", true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.errorKind() == ApiClient.ErrorKind.AUTH) {
                    AuthFlow.goLogin(ScanActivity.this, store, MainActivity.class);
                    return;
                }
                if (!result.ok) {
                    if (result.retryable() && loadSessionsAttempt < 2) {
                        loadSessionsAttempt++;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadSessions();
                            }
                        }, 1500);
                        return;
                    }
                    if (!sessionIds.isEmpty()) {
                        return;
                    }
                    Toast.makeText(ScanActivity.this,
                            "Não foi possível carregar as sessões. Verifique o servidor.",
                            Toast.LENGTH_LONG).show();
                    sessionIds.add(sessionId);
                    sessionNames.add("Sessão " + sessionId + " (padrão)");
                    sessionSpinner.setAdapter(darkSpinnerAdapter());
                    return;
                }
                loadSessionsAttempt = 0;
                sessionIds.clear();
                sessionNames.clear();
                if (result.array != null) {
                    for (int i = 0; i < result.array.length(); i++) {
                        JSONObject s = result.array.optJSONObject(i);
                        if (s != null) {
                            sessionIds.add(s.optInt("id", 1));
                            sessionNames.add(s.optString("name", "Sessão"));
                        }
                    }
                }
                if (sessionIds.isEmpty()) {
                    sessionIds.add(sessionId);
                    sessionNames.add("Sessão " + sessionId + " (padrão)");
                }
                sessionSpinner.setAdapter(darkSpinnerAdapter());
                int currentIndex = sessionIds.indexOf(sessionId);
                if (currentIndex >= 0) {
                    sessionSpinner.setSelection(currentIndex);
                }
            }
        });
    }

    private void handleDecoded(String text) {
        if (!phase.allowsDecode()) {
            return;
        }
        qrText = text;
        presenceJson = null;
        engine.pauseScanning();
        phase = ScanPhase.VERIFYING;
        retryTarget = RetryTarget.NONE;
        resultSheet.setWaiting("Verificando QR…");
        resultSheet.show();
        updateTorchVisibility();
        verify();
    }

    private void verify() {
        if (qrText == null) {
            return;
        }
        phase = ScanPhase.VERIFYING;
        resultSheet.setWaiting("Verificando QR…");
        JSONObject body = new JSONObject();
        try {
            body.put("qr_text", qrText);
            body.put("session_id", sessionId);
        } catch (Exception ignored) {
        }
        api.post("/api/v1/checkin/verify", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.errorKind() == ApiClient.ErrorKind.AUTH) {
                    AuthFlow.goLogin(ScanActivity.this, store, MainActivity.class);
                    return;
                }
                if (result.ok && result.object != null
                        && result.object.optBoolean("valid", false)) {
                    verifyJson = result.object;
                    feedback.success();
                    phase = ScanPhase.CONFIRMED;
                    retryTarget = RetryTarget.NONE;
                    renderConfirmed();
                    updateTorchVisibility();
                    return;
                }
                if (phase != ScanPhase.VERIFYING) {
                    return;
                }
                feedback.warn();
                phase = ScanPhase.ERROR;
                showVerifyError(result);
            }
        });
    }

    private void showVerifyError(ApiClient.Result result) {
        ApiClient.ErrorKind kind = result.errorKind();
        if (kind == ApiClient.ErrorKind.NETWORK) {
            retryTarget = RetryTarget.VERIFY;
            resultSheet.showError("Servidor inacessível. Sua rede ou o servidor pode estar fora. "
                    + "Tente novamente sem reescanear.", "Sem conexão");
            resultSheet.setRescanLabel("Tentar novamente", true);
        } else if (kind == ApiClient.ErrorKind.THROTTLE) {
            retryTarget = RetryTarget.VERIFY;
            resultSheet.showError("Muitas tentativas em sequência. Aguarde instantes e tente "
                    + "novamente.", "Aguarde");
            resultSheet.setRescanLabel("Tentar novamente", true);
        } else {
            retryTarget = RetryTarget.NONE;
            String detail = result.detail();
            resultSheet.showError(detail != null ? detail : "QR inválido ou adulterado.",
                    "QR inválido");
        }
        hintView.setText("Aponte a câmera para o QR da credencial ou do chassi.");
        updateTorchVisibility();
    }

    private void renderConfirmed() {
        if (verifyJson == null) {
            return;
        }
        VerifyParser.Info info = VerifyParser.parse(verifyJson);
        if ("vehicle".equals(info.subjectType)) {
            resultSheet.showVehicle(info.name, info.team, info.conformityStatus);
        } else {
            resultSheet.showParticipant(info.name, info.team, info.role);
        }
        if (presenceJson != null) {
            resultSheet.setPresenceResult(presenceJson.optBoolean("duplicate", false),
                    presenceJson.optString("last_seen_at", ""));
        }
        updateTorchVisibility();
    }

    private void submitPresence() {
        if (qrText == null) {
            return;
        }
        phase = ScanPhase.VERIFYING;
        resultSheet.setWaiting("Registrando presença…");
        JSONObject body = new JSONObject();
        try {
            body.put("qr_text", qrText);
            body.put("session_id", sessionId);
        } catch (Exception ignored) {
        }
        api.post("/api/v1/checkin/presence", body, true, new ApiClient.Callback() {
            @Override
            public void onResult(ApiClient.Result result) {
                if (result.errorKind() == ApiClient.ErrorKind.AUTH) {
                    AuthFlow.goLogin(ScanActivity.this, store, MainActivity.class);
                    return;
                }
                if (result.ok && result.object != null) {
                    presenceJson = result.object;
                    feedback.success();
                    phase = ScanPhase.CONFIRMED;
                    retryTarget = RetryTarget.NONE;
                    renderConfirmed();
                    return;
                }
                if (phase != ScanPhase.VERIFYING) {
                    return;
                }
                feedback.warn();
                phase = ScanPhase.ERROR;
                retryTarget = result.retryable() ? RetryTarget.PRESENCE : RetryTarget.NONE;
                if (result.errorKind() == ApiClient.ErrorKind.NETWORK) {
                    resultSheet.showError("Servidor inacessível ao registrar a presença. "
                            + "Tente novamente.", "Sem conexão");
                } else if (result.errorKind() == ApiClient.ErrorKind.THROTTLE) {
                    resultSheet.showError("Muitas tentativas em sequência. Aguarde um instante.",
                            "Aguarde");
                } else {
                    String detail = result.detail();
                    resultSheet.showError(detail != null ? detail
                            : "Falha ao registrar presença.", "Falha no registro");
                }
                resultSheet.setRescanLabel(retryTarget == RetryTarget.PRESENCE
                        ? "Tentar novamente" : "Escanear novamente", retryTarget == RetryTarget.PRESENCE);
                updateTorchVisibility();
            }
        });
    }

    private void handleRescanAction() {
        if (phase == ScanPhase.ERROR && retryTarget == RetryTarget.VERIFY && qrText != null) {
            verify();
            return;
        }
        if (phase == ScanPhase.ERROR && retryTarget == RetryTarget.PRESENCE && qrText != null) {
            submitPresence();
            return;
        }
        resumeScanning();
    }

    private void openConformity() {
        if (verifyJson == null || qrText == null) {
            return;
        }
        VerifyParser.Info info = VerifyParser.parse(verifyJson);
        Intent intent = new Intent(this, CheckActivity.class);
        intent.putExtra("qr_text", qrText);
        intent.putExtra("session_id", sessionId);
        intent.putExtra("vehicle_name", info.name);
        intent.putExtra("team", info.team);
        intent.putExtra("conformity_status", info.conformityStatus == null
                ? "" : info.conformityStatus);
        startActivityForResult(intent, REQUEST_CHECK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK && phase == ScanPhase.CONFIRMED) {
            renderConfirmed();
            updateTorchVisibility();
        }
    }

    @Override
    public void onBackPressed() {
        if (resultSheet != null && resultSheet.isVisible()) {
            resultSheet.showIdle();
            resumeScanning();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Sair da leitura?")
                .setMessage("Você vai sair do escaneamento de QRs.")
                .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Continuar", null)
                .show();
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
            engine.setCameraPermission(true);
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
        phase = ScanPhase.SCANNING;
        qrText = null;
        verifyJson = null;
        presenceJson = null;
        retryTarget = RetryTarget.NONE;
        resultSheet.showIdle();
        hintView.setText("Aponte a câmera para o QR da credencial ou do chassi.");
        engine.resumeScanning();
        updateTorchVisibility();
    }

    private void showCameraError(String message) {
        phase = ScanPhase.ERROR;
        resultSheet.showError(message, "Câmera indisponível");
        updateTorchVisibility();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openLogin() {
        AuthFlow.goLogin(this, store, MainActivity.class);
    }

    private int dp(int n) {
        return (int) (getResources().getDisplayMetrics().density * n);
    }
}