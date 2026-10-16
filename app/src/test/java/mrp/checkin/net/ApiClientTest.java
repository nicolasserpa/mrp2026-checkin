package mrp.checkin.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class ApiClientTest {
    private ApiClient.Result result(boolean ok, int status) {
        return new ApiClient.Result(ok, status, null, null);
    }

    @Test
    public void successIsNone() {
        ApiClient.Result r = result(true, 200);
        assertEquals(ApiClient.ErrorKind.NONE, r.errorKind());
        assertFalse(r.retryable());
    }

    @Test
    public void statusZeroIsNetwork() {
        ApiClient.Result r = result(false, 0);
        assertEquals(ApiClient.ErrorKind.NETWORK, r.errorKind());
        assertTrue(r.retryable());
    }

    @Test
    public void unauthorizedIsAuth() {
        ApiClient.Result r = result(false, 401);
        assertEquals(ApiClient.ErrorKind.AUTH, r.errorKind());
        assertFalse(r.retryable());
    }

    @Test
    public void tooManyIsThrottle() {
        ApiClient.Result r = result(false, 429);
        assertEquals(ApiClient.ErrorKind.THROTTLE, r.errorKind());
        assertTrue(r.retryable());
    }

    @Test
    public void fourHundredIsInvalid() {
        ApiClient.Result r = result(false, 400);
        assertEquals(ApiClient.ErrorKind.INVALID, r.errorKind());
        assertFalse(r.retryable());
    }

    @Test
    public void serverErrorIsServer() {
        ApiClient.Result r = result(false, 503);
        assertEquals(ApiClient.ErrorKind.SERVER, r.errorKind());
        assertTrue(r.retryable());
    }

    @Test
    public void detailReadsFromObject() throws Exception {
        JSONObject obj = new JSONObject("{\"detail\":\"QR inválido\"}");
        ApiClient.Result r = new ApiClient.Result(true, 200, obj, null);
        assertEquals("QR inválido", r.detail());
    }
}