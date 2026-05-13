package security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

public final class ContentCrypto {

    private ContentCrypto() {}

    public static void encryptFile(Path src, Path dst, SecretKey key) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        try (InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst)) {
            out.write(iv);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                byte[] chunk = cipher.update(buf, 0, n);
                if (chunk != null) out.write(chunk);
            }
            out.write(cipher.doFinal());
        }
    }

    public static void decryptFile(Path src, Path dst, SecretKey key) throws Exception {
        try (InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst)) {
            byte[] iv = new byte[16];
            int read = 0;
            while (read < 16) {
                int r = in.read(iv, read, 16 - read);
                if (r == -1) throw new IllegalArgumentException("File too short to contain IV.");
                read += r;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                byte[] chunk = cipher.update(buf, 0, n);
                if (chunk != null) out.write(chunk);
            }
            out.write(cipher.doFinal());
        }
    }
}
