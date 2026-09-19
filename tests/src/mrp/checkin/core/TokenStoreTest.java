package mrp.checkin.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TokenStoreTest {
    @Test
    public void normalizeTrimsAndPrefixesHttp() {
        assertEquals("http://192.168.0.31:8000",
                TokenStore.normalize(" 192.168.0.31:8000 "));
    }

    @Test
    public void normalizeStripsTrailingSlashes() {
        assertEquals("http://192.168.0.31:8000",
                TokenStore.normalize("http://192.168.0.31:8000///"));
    }

    @Test
    public void normalizeKeepsHttps() {
        assertEquals("https://checkin.example.com",
                TokenStore.normalize("https://checkin.example.com/"));
    }

    @Test
    public void normalizeEmptyBecomesHttpPrefix() {
        assertEquals("http://", TokenStore.normalize(""));
    }
}