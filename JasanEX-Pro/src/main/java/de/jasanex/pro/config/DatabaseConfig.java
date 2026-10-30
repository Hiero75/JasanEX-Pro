package de.jasanex.pro.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import de.jasanex.pro.model.Role;

/** Lokale Konfiguration; Umgebungsvariablen haben Vorrang. */
public final class DatabaseConfig {
    private final Properties values;
    private final Map<String, String> environment;
    public DatabaseConfig(Properties values, Map<String, String> environment) {
        this.values = new Properties(); this.values.putAll(values);
        this.environment = Map.copyOf(environment);
    }
    public static DatabaseConfig load() throws IOException {
        Properties values = new Properties();
        Path path = Path.of(System.getenv().getOrDefault("JASANEX_CONFIG", "config/database.properties"));
        if (Files.exists(path)) try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        return new DatabaseConfig(values, System.getenv());
    }
    private String get(String key, String env, String fallback) {
        return environment.getOrDefault(env, values.getProperty(key, fallback));
    }
    public Connection connect() throws SQLException {
        String user = get("db.user", "JASANEX_DB_USER", "");
        if (user.isBlank()) throw new IllegalStateException("Bitte den Datenbankbenutzer lokal konfigurieren.");
        String host = get("db.host", "JASANEX_DB_HOST", "127.0.0.1");
        String port = get("db.port", "JASANEX_DB_PORT", "3306");
        String name = get("db.name", "JASANEX_DB_NAME", "jasanex-pro-test");
        if (!host.matches("[a-zA-Z0-9.-]+") || !port.matches("[0-9]{1,5}") || !name.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalStateException("Ungültige Datenbankkonfiguration.");
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", get("db.password", "JASANEX_DB_PASSWORD", ""));
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + name, properties);
    }
    public Integer restaurantId() {
        String id = get("restaurant.id", "JASANEX_RESTAURANT_ID", "");
        if (id.isBlank()) return null;
        try { int parsed = Integer.parseInt(id); if (parsed > 0) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new IllegalStateException("Ungültige Restaurant-ID.");
    }
    public String activeStatus() {
        String status = get("auth.activeStatus", "JASANEX_ACTIVE_STATUS", "");
        if (status.isBlank()) throw new IllegalStateException("Bitte den bestätigten aktiven Mitarbeiterstatus konfigurieren.");
        return status;
    }
    public String roleValue(Role role) {
        return get("auth.role." + role.name().toLowerCase(java.util.Locale.ROOT), "JASANEX_ROLE_" + role.name(),
                switch (role) { case ADMIN -> "Admin"; case MANAGER -> "Manager"; case HEAD -> "Head"; });
    }
    public Role role(String value) {
        for (Role role : Role.values()) if (roleValue(role).equals(value)) return role;
        return null;
    }
}
