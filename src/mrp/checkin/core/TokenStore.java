package mrp.checkin.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class TokenStore {
    private static final String PREFS = "checkin_prefs";
    private static final String KEY_TOKEN = "jwt";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_OPERATOR = "operator";

    public static final String DEFAULT_ENDPOINT = "http://192.168.0.10:8000";

    private final SharedPreferences prefs;

    public TokenStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean hasToken() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_OPERATOR).apply();
    }

    public String getOperator() {
        return prefs.getString(KEY_OPERATOR, null);
    }

    public void setOperator(String name) {
        prefs.edit().putString(KEY_OPERATOR, name).apply();
    }

    public String getEndpoint() {
        return prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT);
    }

    public void setEndpoint(String url) {
        prefs.edit().putString(KEY_ENDPOINT, normalize(url)).apply();
    }

    public static String normalize(String url) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        return u;
    }
}