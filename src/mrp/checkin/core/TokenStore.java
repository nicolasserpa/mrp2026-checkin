package mrp.checkin.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class TokenStore {
    private static final String PREFS = "checkin_prefs";
    private static final String KEY_TOKEN = "jwt";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_OPERATOR = "operator";

    public static final String DEFAULT_ENDPOINT = "http://192.168.0.30:8000";

    /** Endpoint do modo dev offline (sem servidor, fixtures locais). */
    public static final String DEV_ENDPOINT = "dev://offline";

    private final Context context;
    private final SharedPreferences prefs;

    public TokenStore(Context context) {
        this.context = context;
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

    public boolean hasEndpoint() {
        return prefs.contains(KEY_ENDPOINT);
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
        if (u.startsWith("dev://")) {
            return u;
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        return u;
    }

    private static boolean isDevEndpoint(String endpoint) {
        return endpoint != null && endpoint.startsWith("dev://");
    }

    private static boolean isDevPackage(String packageName) {
        return packageName != null && packageName.endsWith(".dev");
    }

    /** Modo dev se o endpoint salvo é dev:// OU o package é *.dev (APK dev lado a lado). */
    public boolean isDev() {
        if (isDevEndpoint(getEndpoint())) {
            return true;
        }
        return context != null && isDevPackage(context.getPackageName());
    }

    /** Variante estática (lê prefs + packageName direto do Context). */
    public static boolean isDev(Context context) {
        if (context == null) {
            return false;
        }
        if (isDevPackage(context.getPackageName())) {
            return true;
        }
        String endpoint = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ENDPOINT, "");
        return isDevEndpoint(endpoint);
    }
}