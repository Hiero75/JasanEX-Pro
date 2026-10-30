package de.jasanex.pro.security;

import java.io.Console;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Arrays;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.Role;
import de.jasanex.pro.repository.MitarbeiterRepository;

/** Nur durch ausdrücklichen CLI-Aufruf. Keine Schemaänderung, kein UPDATE. */
public final class TestUserTool {
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Bitte in einem externen Terminal starten; das Passwort wird verdeckt abgefragt."); return;
        }
        try {
            DatabaseConfig config = DatabaseConfig.load();
            String status = config.activeStatus();
            String kuerzel = console.readLine("Neues Kürzel (1–2 Zeichen): ");
            if (kuerzel == null || !kuerzel.matches("[\\p{L}0-9]{1,2}")) throw new IllegalArgumentException();
            Role role = Role.valueOf(console.readLine("Rolle (HEAD / MANAGER / ADMIN): ").trim());
            BigDecimal wage = new BigDecimal(console.readLine("Test-Stundensatz (Dezimalpunkt): "));
            if (wage.signum() < 0 || wage.scale() > 2 || wage.compareTo(new BigDecimal("99999999.99")) > 0) throw new IllegalArgumentException();
            char[] password = console.readPassword("Neues Passwort (mindestens 12 Zeichen): ");
            char[] repeat = console.readPassword("Passwort wiederholen: ");
            String hash;
            try {
                if (password == null || repeat == null || password.length < 12 || !Arrays.equals(password, repeat)) throw new IllegalArgumentException();
                hash = new PasswordHasher().hash(password);
            } finally {
                if (password != null) Arrays.fill(password, '\0');
                if (repeat != null) Arrays.fill(repeat, '\0');
            }
            try (Connection connection = config.connect()) {
                connection.setAutoCommit(false);
                try {
                    var restaurant = new MitarbeiterRepository().restaurant(connection, config.restaurantId());
                    console.printf("Neuer Testbenutzer %s / %s, Restaurant-ID %d, Aktivstatus %s.%n", kuerzel, role, restaurant.id(), status);
                    if (!"ANLEGEN".equals(console.readLine("Zum Einfügen ANLEGEN eingeben: "))) { connection.rollback(); return; }
                    try (PreparedStatement check = connection.prepareStatement("SELECT mitarbeiter_id FROM mitarbeiter WHERE restaurant_id=? AND kuerzel=? FOR UPDATE")) {
                        check.setInt(1, restaurant.id()); check.setString(2, kuerzel);
                        try (ResultSet rows = check.executeQuery()) { if (rows.next()) throw new IllegalStateException("Kürzel bereits vergeben."); }
                    }
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO mitarbeiter (restaurant_id,kuerzel,passwort_hash,rolle,stundensatz,m_status) VALUES (?,?,?,?,?,?)")) {
                        insert.setInt(1, restaurant.id()); insert.setString(2, kuerzel); insert.setString(3, hash);
                        insert.setString(4, config.roleValue(role)); insert.setBigDecimal(5, wage); insert.setString(6, status);
                        insert.executeUpdate();
                    }
                    connection.commit(); console.printf("Testbenutzer angelegt.%n");
                } catch (Exception e) { connection.rollback(); throw e; }
            }
        } catch (Exception e) {
            System.err.println("Nicht angelegt. Bitte Eingaben, eindeutiges Kürzel, Datenbank und Konfiguration prüfen.");
        }
    }
}
