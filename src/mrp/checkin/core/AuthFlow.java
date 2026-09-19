package mrp.checkin.core;

import android.app.Activity;
import android.content.Intent;

/** Navegação centralizada p/ sessão expirada (401): limpa token e volta ao login. */
public final class AuthFlow {
    private AuthFlow() {
    }

    public static void goLogin(Activity activity, TokenStore store, Class<?> loginActivity) {
        store.clearToken();
        Intent intent = new Intent(activity, loginActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}