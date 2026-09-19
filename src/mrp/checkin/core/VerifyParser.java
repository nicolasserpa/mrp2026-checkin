package mrp.checkin.core;

import org.json.JSONObject;

/**
 * Interpretação da resposta de /api/v1/checkin/verify. Lógica pura (sem Android),
 * testável com o org.json de verdade no classpath dos testes.
 */
public final class VerifyParser {
    private VerifyParser() {
    }

    public static final class Info {
        public final boolean valid;
        public final String subjectType;
        public final String name;
        public final String team;
        public final String role;
        public final String conformityStatus;

        private Info(boolean valid, String subjectType, String name, String team,
                     String role, String conformityStatus) {
            this.valid = valid;
            this.subjectType = subjectType;
            this.name = name;
            this.team = team;
            this.role = role;
            this.conformityStatus = conformityStatus;
        }
    }

    public static Info parse(JSONObject obj) {
        if (obj == null) {
            return empty();
        }
        boolean valid = obj.optBoolean("valid", false);
        String subjectType = obj.optString("subject_type", "member");
        JSONObject vehicle = obj.optJSONObject("vehicle");
        JSONObject member = obj.optJSONObject("member");
        String name = "—";
        String team = "—";
        String role = "member";
        String conformityStatus = null;
        if ("vehicle".equals(subjectType) && vehicle != null) {
            name = vehicle.optString("name", "—");
            team = vehicle.optString("team", "—");
            conformityStatus = vehicle.optString("conformity_status", null);
        } else if (member != null) {
            name = member.optString("name", "—");
            team = member.optString("team", "—");
            role = member.optString("role", "member");
        }
        return new Info(valid, subjectType, name, team, role, conformityStatus);
    }

    private static Info empty() {
        return new Info(false, "member", "—", "—", "member", null);
    }
}