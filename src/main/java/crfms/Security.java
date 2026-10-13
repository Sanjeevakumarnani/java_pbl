package crfms;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2 password hashing helpers. */
public final class Security {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Security() { }
    public static String newSalt() { byte[] b = new byte[16]; RANDOM.nextBytes(b); return Base64.getEncoder().encodeToString(b); }
    public static String hash(String password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 120_000, 256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        catch (Exception ex) { throw new IllegalStateException("Password hashing unavailable", ex); }
        finally { spec.clearPassword(); }
    }
    public static boolean verify(String password, String salt, String expected) {
        try { return MessageDigest.isEqual(Base64.getDecoder().decode(hash(password, salt)), Base64.getDecoder().decode(expected)); }
        catch (RuntimeException ex) { return false; }
    }
}
