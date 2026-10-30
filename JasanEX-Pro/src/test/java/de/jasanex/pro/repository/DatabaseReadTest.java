package de.jasanex.pro.repository;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.service.AuthService;

/** Explizit aktivierbare, ausschließlich lesende Prüfung gegen lokale XAMPP-Daten. */
@EnabledIfEnvironmentVariable(named="JASANEX_DB_TEST", matches="true")
class DatabaseReadTest {
    @Test void readsSchemaAndRejectsUnknownLogin() throws Exception {
        DatabaseConfig config = DatabaseConfig.load();
        var repository = new MitarbeiterRepository();
        try (var connection = config.connect()) {
            connection.setReadOnly(true);
            var restaurant = repository.restaurant(connection, config.restaurantId());
            assertTrue(restaurant.id() > 0);
            assertNotNull(repository.loadMitarbeiter(connection, restaurant.id()));
            assertNotNull(repository.loadOffeneSchichten(connection, restaurant.id()));
            assertTrue(repository.findLogin(connection, restaurant.id(), "__nonexistent_test_user__").isEmpty());
            try (var statement = connection.prepareStatement("SELECT kuerzel FROM mitarbeiter WHERE restaurant_id=? AND m_status='INAKTIV' LIMIT 1")) {
                statement.setInt(1, restaurant.id());
                try (var rows = statement.executeQuery()) {
                    if (rows.next()) {
                        String kuerzel = rows.getString(1);
                        assertThrows(AuthService.LoginException.class, () -> new AuthService(config).login(kuerzel, "invalid-test-password".toCharArray()));
                    }
                }
            }
        }
        char[] password = "invalid-test-password".toCharArray();
        assertThrows(AuthService.LoginException.class, () -> new AuthService(config).login("__nonexistent_test_user__", password));
        char[] empty = new char[password.length]; assertTrue(Arrays.equals(empty, password));
    }
}
