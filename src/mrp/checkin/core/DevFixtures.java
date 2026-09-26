package mrp.checkin.core;

import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Fixtures do modo dev offline (sem servidor). Lógica pura (sem Android),
 * testável no JVM do host. Formato compatível com {@link VerifyParser} e com
 * as telas de presença/conformidade.
 */
public final class DevFixtures {
    private DevFixtures() {
    }

    /**
     * Fabrica a resposta de /api/v1/checkin/verify.
     * QR contendo "veic", "chassi" ou "carro" (case-insensitive) vira veículo;
     * qualquer outro QR vira membro.
     */
    public static JSONObject verifyFor(String qr) {
        String q = qr == null ? "" : qr.toLowerCase(Locale.ROOT);
        boolean vehicle = q.contains("veic") || q.contains("chassi") || q.contains("carro");
        JSONObject out = new JSONObject();
        try {
            out.put("valid", true);
            if (vehicle) {
                out.put("subject_type", "vehicle");
                JSONObject v = new JSONObject();
                v.put("name", "Carro DEV 42");
                v.put("team", "Equipe DEV");
                v.put("conformity_status", "pending");
                out.put("vehicle", v);
            } else {
                out.put("subject_type", "member");
                JSONObject m = new JSONObject();
                m.put("name", "Piloto DEV");
                m.put("team", "Equipe DEV");
                m.put("role", "member");
                out.put("member", m);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Fabrica a resposta de /api/v1/checkin/presence. */
    public static JSONObject presenceFor(String qr) {
        JSONObject out = new JSONObject();
        try {
            out.put("duplicate", false);
            out.put("last_seen_at", "agora (dev)");
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Fabrica a resposta de /api/v1/checkin/conformity. */
    public static JSONObject conformityFor(String result) {
        JSONObject out = new JSONObject();
        try {
            out.put("conformity_status", result);
            out.put("checked_at", "agora (dev)");
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Lista fixa de sessões do modo dev (sem rede). */
    public static JSONArray sessionsArray() {
        JSONArray arr = new JSONArray();
        try {
            JSONObject s1 = new JSONObject();
            s1.put("id", 1);
            s1.put("name", "Sessão 1 (dev)");
            arr.put(s1);
            JSONObject s2 = new JSONObject();
            s2.put("id", 2);
            s2.put("name", "Sessão 2 (dev)");
            arr.put(s2);
        } catch (Exception ignored) {
        }
        return arr;
    }
}
