package mrp.checkin.net;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mrp.checkin.core.TokenStore;

public final class ApiClient {
    public interface Callback {
        void onResult(Result result);
    }

    public static final class Result {
        public final boolean ok;
        public final int status;
        public final JSONObject object;
        public final JSONArray array;

        Result(boolean ok, int status, JSONObject object, JSONArray array) {
            this.ok = ok;
            this.status = status;
            this.object = object;
            this.array = array;
        }

        public String detail() {
            if (object != null) {
                Object d = object.opt("detail");
                if (d instanceof String) {
                    return (String) d;
                }
            }
            return null;
        }
    }

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final TokenStore store;

    public ApiClient(TokenStore store) {
        this.store = store;
    }

    public void post(String path, JSONObject payload, boolean auth, Callback callback) {
        request("POST", path, payload, auth, callback);
    }

    public void get(String path, boolean auth, Callback callback) {
        request("GET", path, null, auth, callback);
    }

    private void request(String method, final String path, final JSONObject payload,
                         final boolean auth, final Callback callback) {
        final String base = store.getEndpoint();
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                final Result result = execute(method, base, path, payload, auth);
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(result);
                    }
                });
            }
        });
    }

    private Result execute(String method, String base, String path, JSONObject payload, boolean auth) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(EndpointResolver.join(base, path));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            if (auth) {
                String token = store.getToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }
            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            }
            conn.setRequestProperty("Accept", "application/json");

            if (payload != null) {
                byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                try {
                    dos.write(out);
                } finally {
                    dos.close();
                }
            }

            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String text = readAll(is);
            JSONObject object = null;
            JSONArray array = null;
            if (text != null && !text.isEmpty()) {
                try {
                    object = new JSONObject(text);
                } catch (Exception ignored) {
                    try {
                        array = new JSONArray(text);
                    } catch (Exception ignored2) {
                    }
                }
            }
            return new Result(status >= 200 && status < 300, status, object, array);
        } catch (Exception e) {
            return new Result(false, 0, null, null);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        } finally {
            r.close();
        }
    }
}