package security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordSecurity {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordSecurity() {}

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BYTES);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String storedValue) {
        if (password == null || storedValue == null || storedValue.isEmpty()) {
            return false;
        }

        if (!storedValue.startsWith("pbkdf2$")) {
            return constantTimeEquals(password, storedValue);
        }

        try {
            String[] parts = storedValue.split("\\$");
            if (parts.length != 4) return false;

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length);
            return constantTimeEquals(actual, expected);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean isLegacyPlaintext(String storedValue) {
        return storedValue != null && !storedValue.startsWith("pbkdf2$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBytes * 8);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] first = a.getBytes(StandardCharsets.UTF_8);
        byte[] second = b.getBytes(StandardCharsets.UTF_8);
        return constantTimeEquals(first, second);
    }
}
