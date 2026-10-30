package de.jasanex.pro.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.*;
import de.jasanex.pro.repository.MitarbeiterRepository;
import de.jasanex.pro.security.PasswordHasher;

public final class AuthService {
    public record LoginResult(Mitarbeiter user, Role role, String restaurant,
            Map<Integer, Mitarbeiter> mitarbeiter, List<OffeneSchicht> schichten) { }
    public static final class LoginException extends Exception {
        public LoginException() { super("Anmeldung nicht möglich. Bitte Zugangsdaten und Berechtigung prüfen."); }
    }
    private final DatabaseConfig config;
    private final MitarbeiterRepository repository;
    private final PasswordHasher hasher;
    private final String dummyHash;
    public AuthService(DatabaseConfig config) { this(config, new MitarbeiterRepository(), new PasswordHasher()); }
    public AuthService(DatabaseConfig config, MitarbeiterRepository repository, PasswordHasher hasher) {
        this.config = config; this.repository = repository; this.hasher = hasher;
        dummyHash = hasher.hash(UUID.randomUUID().toString().toCharArray());
    }
    public LoginResult login(String kuerzel, char[] password) throws SQLException, LoginException {
        try {
            if (kuerzel == null || kuerzel.isBlank() || password.length == 0) throw new LoginException();
            String active = config.activeStatus();
            try (Connection connection = config.connect()) {
                connection.setReadOnly(true);
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                connection.setAutoCommit(false);
                try {
                    var restaurant = repository.restaurant(connection, config.restaurantId());
                    var candidate = repository.findLogin(connection, restaurant.id(), kuerzel.trim());
                    boolean verified = hasher.verify(password, candidate.map(MitarbeiterRepository.LoginDatensatz::hash).orElse(dummyHash));
                    if (candidate.isEmpty()) throw new LoginException();
                    Mitarbeiter user = candidate.get().mitarbeiter();
                    Role role = config.role(user.rolle());
                    if (!verified || !active.equals(user.status()) || role == null) throw new LoginException();
                    var staff = repository.loadMitarbeiter(connection, restaurant.id());
                    var shifts = repository.loadOffeneSchichten(connection, restaurant.id());
                    connection.commit();
                    return new LoginResult(user, role, restaurant.name(), staff, shifts);
                } catch (SQLException | LoginException | RuntimeException e) {
                    try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                    throw e;
                }
            }
        } finally { Arrays.fill(password, '\0'); }
    }
}
