package mrp.checkin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class VerifyParserTest {
    @Test
    public void nullObjectIsInvalid() {
        VerifyParser.Info info = VerifyParser.parse(null);
        assertFalse(info.valid);
    }

    @Test
    public void parseValidMember() throws Exception {
        JSONObject obj = new JSONObject("{"
                + "\"valid\":true,\"subject_type\":\"member\","
                + "\"member\":{\"name\":\"Ana A.\",\"team\":\"Equipe 1\",\"role\":\"líder\"},"
                + "\"vehicle\":null}");
        VerifyParser.Info info = VerifyParser.parse(obj);
        assertTrue(info.valid);
        assertEquals("member", info.subjectType);
        assertEquals("Ana A.", info.name);
        assertEquals("Equipe 1", info.team);
        assertEquals("líder", info.role);
        assertNull(info.conformityStatus);
    }

    @Test
    public void parseValidVehicle() throws Exception {
        JSONObject obj = new JSONObject("{"
                + "\"valid\":true,\"subject_type\":\"vehicle\","
                + "\"member\":null,"
                + "\"vehicle\":{\"name\":\"Chassi Rato 001\",\"team\":\"Equipe 1\","
                + "\"conformity_status\":\"pass\"}}");
        VerifyParser.Info info = VerifyParser.parse(obj);
        assertTrue(info.valid);
        assertEquals("vehicle", info.subjectType);
        assertEquals("Chassi Rato 001", info.name);
        assertEquals("Equipe 1", info.team);
        assertEquals("pass", info.conformityStatus);
        assertEquals("member", info.role);
    }

    @Test
    public void parseInvalidBodyKeepsDefaults() throws Exception {
        JSONObject obj = new JSONObject("{\"valid\":false}");
        VerifyParser.Info info = VerifyParser.parse(obj);
        assertFalse(info.valid);
        assertEquals("—", info.name);
    }
}