package de.jasanex.pro.repository;

import java.sql.*;
import java.util.*;
import de.jasanex.pro.model.*;

/** Ausschließlich lesende Abfragen für die Anwendung. Alle Ergebnisse restaurantgebunden. */
public class MitarbeiterRepository {
    public record Restaurant(int id, String name) { }
    public record LoginDatensatz(Mitarbeiter mitarbeiter, String hash) { }
    public Restaurant restaurant(Connection connection, Integer configuredId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(configuredId == null
                ? "SELECT restaurant_id,name FROM restaurant LIMIT 2"
                : "SELECT restaurant_id,name FROM restaurant WHERE restaurant_id=?")) {
            if (configuredId != null) statement.setInt(1, configuredId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalStateException("Kein konfiguriertes Restaurant gefunden.");
                Restaurant result = new Restaurant(rows.getInt(1), rows.getString(2));
                if (rows.next()) throw new IllegalStateException("Mehrere Restaurants vorhanden. Bitte restaurant.id konfigurieren.");
                return result;
            }
        }
    }
    public Optional<LoginDatensatz> findLogin(Connection connection, int restaurantId, String kuerzel) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT mitarbeiter_id,restaurant_id,kuerzel,rolle,m_status,passwort_hash FROM mitarbeiter WHERE restaurant_id=? AND kuerzel=? LIMIT 2")) {
            statement.setInt(1, restaurantId); statement.setString(2, kuerzel);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                LoginDatensatz result = new LoginDatensatz(read(rows), rows.getString("passwort_hash"));
                if (rows.next()) return Optional.empty(); // Nie einen mehrdeutigen Benutzer wählen.
                return Optional.of(result);
            }
        }
    }
    public Map<Integer, Mitarbeiter> loadMitarbeiter(Connection connection, int restaurantId) throws SQLException {
        Map<Integer, Mitarbeiter> result = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT mitarbeiter_id,restaurant_id,kuerzel,rolle,m_status FROM mitarbeiter WHERE restaurant_id=?")) {
            statement.setInt(1, restaurantId);
            try (ResultSet rows = statement.executeQuery()) { while (rows.next()) { Mitarbeiter m = read(rows); result.put(m.id(), m); } }
        }
        return Map.copyOf(result);
    }
    public List<OffeneSchicht> loadOffeneSchichten(Connection connection, int restaurantId) throws SQLException {
        List<OffeneSchicht> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT z.zeiterfassung_id,z.mitarbeiter_id,z.geschaeftstag,z.check_in,z.rolle_in_schicht FROM zeiterfassung z JOIN mitarbeiter m ON m.mitarbeiter_id=z.mitarbeiter_id WHERE m.restaurant_id=? AND z.check_out IS NULL")) {
            statement.setInt(1, restaurantId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new OffeneSchicht(rows.getInt(1), rows.getInt(2),
                        rows.getObject(3, java.time.LocalDate.class), rows.getObject(4, java.time.LocalDateTime.class), rows.getString(5)));
            }
        }
        return List.copyOf(result);
    }
    private Mitarbeiter read(ResultSet row) throws SQLException {
        return new Mitarbeiter(row.getInt("mitarbeiter_id"), row.getInt("restaurant_id"),
                row.getString("kuerzel"), row.getString("rolle"), row.getString("m_status"));
    }
}
