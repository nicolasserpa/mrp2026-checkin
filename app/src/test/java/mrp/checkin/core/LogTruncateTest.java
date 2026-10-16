package mrp.checkin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LogTruncateTest {
    @Test
    public void tailKeepsLastChars() {
        assertEquals("lo", LogTruncate.tail("hello", 2));
        assertEquals("hello", LogTruncate.tail("hello", 10));
        assertEquals("", LogTruncate.tail(null, 10));
        assertEquals("", LogTruncate.tail("", 10));
    }

    @Test
    public void tailBytesKeepsLastBytes() {
        byte[] src = {1, 2, 3, 4, 5};
        byte[] out = LogTruncate.tailBytes(src, 3);
        assertEquals(3, out.length);
        assertEquals(3, out[0]);
        assertEquals(5, out[2]);
    }

    @Test
    public void tailUtf8DoesNotSplitMultiByte() {
        String s = "çã";
        byte[] out = LogTruncate.tailUtf8(s, 3);
        assertTrue(out.length <= 3);
        assertEquals("ã", new String(out, java.nio.charset.StandardCharsets.UTF_8));
    }
}