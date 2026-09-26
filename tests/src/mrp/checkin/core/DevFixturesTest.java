package mrp.checkin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class DevFixturesTest {
    @Test
    public void verifyVehicleForVeicQr() {
        JSONObject v = DevFixtures.verifyFor("DEV-VEIC-042");
        assertTrue(v.optBoolean("valid", false));
        assertEquals("vehicle", v.optString("subject_type", ""));
        assertEquals("Carro DEV 42", v.optJSONObject("vehicle").optString("name", ""));
        assertEquals("Equipe DEV", v.optJSONObject("vehicle").optString("team", ""));
        assertEquals("pending", v.optJSONObject("vehicle").optString("conformity_status", ""));
    }

    @Test
    public void verifyVehicleMatchesCaseInsensitive() {
        assertEquals("vehicle",
                DevFixtures.verifyFor("qr-do-CHASSI-9").optString("subject_type", ""));
        assertEquals("vehicle",
                DevFixtures.verifyFor("meu carro 1").optString("subject_type", ""));
    }

    @Test
    public void verifyMemberForOtherQr() {
        JSONObject v = DevFixtures.verifyFor("DEV-MEMBER-001");
        assertTrue(v.optBoolean("valid", false));
        assertEquals("member", v.optString("subject_type", ""));
        assertEquals("Piloto DEV", v.optJSONObject("member").optString("name", ""));
        assertEquals("Equipe DEV", v.optJSONObject("member").optString("team", ""));
        assertEquals("member", v.optJSONObject("member").optString("role", ""));
    }

    @Test
    public void presenceShape() {
        JSONObject p = DevFixtures.presenceFor("DEV-MEMBER-001");
        assertEquals(false, p.optBoolean("duplicate", true));
        assertEquals("agora (dev)", p.optString("last_seen_at", ""));
    }

    @Test
    public void conformityShape() {
        JSONObject c = DevFixtures.conformityFor("pass");
        assertEquals("pass", c.optString("conformity_status", ""));
        assertEquals("agora (dev)", c.optString("checked_at", ""));
    }

    @Test
    public void sessionsArrayHasTwoDevSessions() {
        JSONArray arr = DevFixtures.sessionsArray();
        assertEquals(2, arr.length());
        assertEquals(1, arr.optJSONObject(0).optInt("id", 0));
        assertEquals("Sessão 1 (dev)", arr.optJSONObject(0).optString("name", ""));
        assertEquals(2, arr.optJSONObject(1).optInt("id", 0));
        assertEquals("Sessão 2 (dev)", arr.optJSONObject(1).optString("name", ""));
    }

    @Test
    public void normalizeKeepsDevEndpoint() {
        assertEquals("dev://offline", TokenStore.normalize("dev://offline"));
        assertEquals(TokenStore.DEV_ENDPOINT, TokenStore.normalize(TokenStore.DEV_ENDPOINT));
    }
}
