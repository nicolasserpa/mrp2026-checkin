package mrp.checkin.core;

import java.nio.charset.StandardCharsets;

/** Corta texto/bytes mantendo o trecho final mais recente. Lógica pura e testável. */
public final class LogTruncate {
    private LogTruncate() {
    }

    public static final int MAX_BYTES = 64 * 1024;

    public static String tail(String text, int maxChars) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(text.length() - maxChars);
    }

    public static byte[] tailBytes(byte[] bytes, int maxBytes) {
        if (bytes == null || bytes.length == 0) {
            return new byte[0];
        }
        if (bytes.length <= maxBytes) {
            return bytes;
        }
        byte[] out = new byte[maxBytes];
        System.arraycopy(bytes, bytes.length - maxBytes, out, 0, maxBytes);
        return out;
    }

    /** Unicode-safe UTF-8: corta em fronteira de caractere (não no meio de um multibyte). */
    public static byte[] tailUtf8(String text, int maxBytes) {
        String s = text == null ? "" : text;
        byte[] all = s.getBytes(StandardCharsets.UTF_8);
        if (all.length <= maxBytes) {
            return all;
        }
        int cut = all.length - maxBytes;
        while (cut < all.length && (all[cut] & 0xC0) == 0x80) {
            cut++;
        }
        if (cut >= all.length) {
            // A janela inteira caiu no meio de um caractere multibyte: descarta.
            return new byte[0];
        }
        byte[] out = new byte[all.length - cut];
        System.arraycopy(all, cut, out, 0, out.length);
        return out;
    }
}