package de.jasanex.pro.security;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();
    @Test void verifiesPasswordsAndUsesRandomSalts() {
        String first = hasher.hash("Testpasswort-123!".toCharArray());
        String second = hasher.hash("Testpasswort-123!".toCharArray());
        assertNotEquals(first, second);
        assertTrue(hasher.verify("Testpasswort-123!".toCharArray(), first));
        assertFalse(hasher.verify("falsch".toCharArray(), first));
    }
    @Test void rejectsUnsupportedAndMalformedHashes() {
        for (String value : new String[] {"klartext", "$2y$invalid", "pbkdf2-sha256$1$AA==$AA==", "pbkdf2-sha256$999999999$AA==$AA==", "pbkdf2-sha256$600000$?$?"})
            assertFalse(hasher.verify("pass".toCharArray(), value));
        assertFalse(hasher.verify("pass".toCharArray(), null));
    }
}
