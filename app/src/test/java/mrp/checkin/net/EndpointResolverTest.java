package mrp.checkin.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EndpointResolverTest {
    @Test
    public void joinSlash() {
        assertEquals("http://192.168.0.30:8000/api/v1/health",
                EndpointResolver.join("http://192.168.0.30:8000", "/api/v1/health"));
    }

    @Test
    public void joinWithoutSlashAddsIt() {
        assertEquals("http://a:8000/x", EndpointResolver.join("http://a:8000", "x"));
    }

    @Test
    public void joinStripsTrailingSlashes() {
        assertEquals("http://a/b", EndpointResolver.join("http://a///", "/b"));
    }

    @Test
    public void joinNullBase() {
        assertEquals("/x", EndpointResolver.join(null, "/x"));
    }
}