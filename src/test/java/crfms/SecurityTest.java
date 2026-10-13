package crfms;

/** Small no-dependency tests for password hashing. */
public final class SecurityTest {
    private SecurityTest() { }
    public static void main(String[] args) {
        String password = "Example123";
        String salt = Security.newSalt();
        String hash = Security.hash(password, salt);
        check(!hash.equals(password), "password must not be stored in plain text");
        check(Security.verify(password, salt, hash), "correct password must verify");
        check(!Security.verify("Wrong123", salt, hash), "wrong password must fail");
        check(!Security.newSalt().equals(salt), "salts must be random");
        System.out.println("SecurityTest: 4 checks passed.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
