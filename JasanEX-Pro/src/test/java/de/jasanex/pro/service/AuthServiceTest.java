package de.jasanex.pro.service;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.*;
import de.jasanex.pro.repository.MitarbeiterRepository;
import de.jasanex.pro.security.*;
import de.jasanex.pro.cache.StammdatenCache;

class AuthServiceTest {
    private final PasswordHasher hasher = new PasswordHasher();
    private String hash;
    private Driver driver;
    private boolean closed, committed, rolledBack, loaded;
    @BeforeEach void setup() throws Exception {
        hash = hasher.hash("test-password-123".toCharArray());
        driver = new Driver() {
            public boolean acceptsURL(String url) { return url.startsWith("jdbc:mariadb://test.invalid"); }
            public Connection connect(String url, Properties info) {
                if (!acceptsURL(url)) return null;
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (p,m,a) -> {
                    switch (m.getName()) {
                        case "close" -> closed = true;
                        case "commit" -> committed = true;
                        case "rollback" -> rolledBack = true;
                        default -> { }
                    }
                    return null;
                });
            }
            public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; } public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; } public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(driver);
    }
    @AfterEach void cleanup() throws Exception { DriverManager.deregisterDriver(driver); }
    private AuthService service(String role, String status, boolean failLoad, boolean missing) {
        Properties values = new Properties(); values.setProperty("db.host", "test.invalid"); values.setProperty("db.user", "test"); values.setProperty("auth.activeStatus", "TEST_ACTIVE");
        Mitarbeiter user = new Mitarbeiter(1, 7, "TT", role, status);
        var repository = new MitarbeiterRepository() {
            @Override public Restaurant restaurant(Connection c, Integer id) { return new Restaurant(7, "Test"); }
            @Override public Optional<LoginDatensatz> findLogin(Connection c, int id, String kuerzel) { return missing ? Optional.empty() : Optional.of(new LoginDatensatz(user, hash)); }
            @Override public Map<Integer, Mitarbeiter> loadMitarbeiter(Connection c, int id) throws SQLException {
                loaded = true; if (failLoad) throw new SQLException("Test"); return Map.of(1, user);
            }
            @Override public List<OffeneSchicht> loadOffeneSchichten(Connection c, int id) { return List.of(); }
        };
        return new AuthService(new DatabaseConfig(values, Map.of()), repository, hasher);
    }
    @Test void authenticatesLoadsAndClearsSession() throws Exception {
        char[] password = "test-password-123".toCharArray();
        var result = service("Head", "TEST_ACTIVE", false, false).login("TT", password);
        assertTrue(closed); assertTrue(committed); assertTrue(loaded);
        assertEquals('\0', password[0]);
        var session = new SessionManager(); var cache = new StammdatenCache();
        session.start(result); cache.replace(result); assertEquals(1, cache.mitarbeiter().size());
        session.clear(); cache.clear(); assertFalse(session.isActive()); assertTrue(cache.mitarbeiter().isEmpty());
    }
    @Test void rejectsInactiveAndUnauthorizedUsersBeforeLoading() {
        assertThrows(AuthService.LoginException.class, () -> service("Head", "INAKTIV", false, false).login("TT", "test-password-123".toCharArray()));
        assertThrows(AuthService.LoginException.class, () -> service("Fahrer Auto", "TEST_ACTIVE", false, false).login("TT", "test-password-123".toCharArray()));
        assertFalse(loaded); assertTrue(rolledBack); assertTrue(closed);
    }
    @Test void rejectsWrongPasswordAndMissingUser() {
        assertThrows(AuthService.LoginException.class, () -> service("Head", "TEST_ACTIVE", false, false).login("TT", "falsch".toCharArray()));
        assertThrows(AuthService.LoginException.class, () -> service("Head", "TEST_ACTIVE", false, true).login("TT", "test-password-123".toCharArray()));
        assertFalse(loaded); assertFalse(committed);
    }
    @Test void failsAtomicallyWhenCacheLoadFails() {
        assertThrows(SQLException.class, () -> service("Head", "TEST_ACTIVE", true, false).login("TT", "test-password-123".toCharArray()));
        assertFalse(committed); assertTrue(rolledBack); assertTrue(closed);
    }
}
