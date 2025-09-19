import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class Pawtect {
    private static final byte[] KEY = new byte[32];
    static {
        for (int i = 0; i < 32; i++) {
            KEY[i] = (byte) ((i % 2 == 0) ? 19 : 55);
        }
    }
    public static String sign(String body) throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0xAC831369);
        buf.put((byte) 0x00);
        buf.put(DigestUtils.sha256(body));
        buf.putShort((short) 0);
        buf.putInt(1);
        byte[] hb = "backend.wplace.live".getBytes(StandardCharsets.UTF_8);
        buf.putInt(hb.length);
        buf.put(hb);
        byte[] plaintext = new byte[buf.position()];
        buf.flip();
        buf.get(plaintext);
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[24];
        random.nextBytes(nonce);
        XChaCha20Poly1305 cipher = new XChaCha20Poly1305(KEY);
        byte[] ciphertext = cipher.encrypt(nonce, plaintext);
        String ctB64 = Base64.getEncoder().encodeToString(ciphertext);
        String nonceB64 = Base64.getEncoder().encodeToString(nonce);
        return ctB64 + "." + nonceB64;
    }
}
