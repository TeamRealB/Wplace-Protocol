import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.macs.Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class XChaCha20Poly1305 {
    private final byte[] key;

    public XChaCha20Poly1305(byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("Key must be 32 bytes");
        }
        this.key = Arrays.copyOf(key, 32);
    }

    public byte[] encrypt(byte[] nonce24, byte[] plaintext) throws Exception {
        if (nonce24.length != 24) {
            throw new IllegalArgumentException("Nonce must be 24 bytes for XChaCha20");
        }
        byte[] subKey = hchacha20(key, Arrays.copyOfRange(nonce24, 0, 16));
        byte[] nonce12 = new byte[12];
        nonce12[0] = 0;
        nonce12[1] = 0;
        nonce12[2] = 0;
        nonce12[3] = 0;
        System.arraycopy(nonce24, 16, nonce12, 4, 8);
        ChaCha7539Engine chacha = new ChaCha7539Engine();
        chacha.init(true, new ParametersWithIV(new KeyParameter(subKey), nonce12));
        byte[] polyKey = new byte[64];
        chacha.processBytes(polyKey, 0, polyKey.length, polyKey, 0);
        byte[] ciphertext = new byte[plaintext.length];
        chacha.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        Poly1305 poly1305 = new Poly1305();
        poly1305.init(new KeyParameter(Arrays.copyOf(polyKey, 32)));
        int aadLen = 0;
        poly1305.update(ciphertext, 0, ciphertext.length);
        int rem = ciphertext.length % 16;
        if (rem != 0) {
            poly1305.update(new byte[16 - rem], 0, 16 - rem);
        }
        poly1305.update(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(aadLen).array(), 0, 8);
        poly1305.update(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(ciphertext.length).array(), 0, 8);
        byte[] tag = new byte[16];
        poly1305.doFinal(tag, 0);
        ByteBuffer out = ByteBuffer.allocate(ciphertext.length + 16);
        out.put(ciphertext);
        out.put(tag);
        return out.array();
    }

    private static byte[] hchacha20(byte[] key, byte[] nonce16) {
        int[] state = new int[16];

        byte[] sigma = "expand 32-byte k".getBytes();
        state[0] = toIntLE(sigma, 0);
        state[1] = toIntLE(sigma, 4);
        state[2] = toIntLE(sigma, 8);
        state[3] = toIntLE(sigma, 12);
        for (int i = 0; i < 8; i++) {
            state[4 + i] = toIntLE(key, i * 4);
        }
        state[12] = toIntLE(nonce16, 0);
        state[13] = toIntLE(nonce16, 4);
        state[14] = toIntLE(nonce16, 8);
        state[15] = toIntLE(nonce16, 12);
        for (int i = 0; i < 10; i++) {
            quarterRound(state, 0, 4, 8, 12);
            quarterRound(state, 1, 5, 9, 13);
            quarterRound(state, 2, 6, 10, 14);
            quarterRound(state, 3, 7, 11, 15);
            quarterRound(state, 0, 5, 10, 15);
            quarterRound(state, 1, 6, 11, 12);
            quarterRound(state, 2, 7, 8, 13);
            quarterRound(state, 3, 4, 9, 14);
        }
        ByteBuffer out = ByteBuffer.allocate(32).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        out.putInt(state[0]);
        out.putInt(state[1]);
        out.putInt(state[2]);
        out.putInt(state[3]);
        out.putInt(state[12]);
        out.putInt(state[13]);
        out.putInt(state[14]);
        out.putInt(state[15]);
        return out.array();
    }

    private static void quarterRound(int[] s, int a, int b, int c, int d) {
        s[a] += s[b]; s[d] = rotl(s[d] ^ s[a], 16);
        s[c] += s[d]; s[b] = rotl(s[b] ^ s[c], 12);
        s[a] += s[b]; s[d] = rotl(s[d] ^ s[a], 8);
        s[c] += s[d]; s[b] = rotl(s[b] ^ s[c], 7);
    }

    private static int rotl(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static int toIntLE(byte[] bs, int off) {
        return (bs[off] & 0xff) |
               ((bs[off+1] & 0xff) << 8) |
               ((bs[off+2] & 0xff) << 16) |
               ((bs[off+3] & 0xff) << 24);
    }
}
