package de.jasanex.pro.repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import de.jasanex.pro.model.Betriebsdaten.*;

/** SQL für Stammdaten und Schichten. Transaktionsgrenzen bestimmt der Service. */
public final class BetriebRepository {
    public record Konto(Personal personal, String hash) { }

    public void lockRestaurant(Connection c, int restaurant) throws SQLException {
        // Ein gemeinsames Sperrobjekt pro Restaurant verhindert Doppelbelegungen auch bei zwei App-Instanzen.
        try (var s = statement(c, "SELECT restaurant_id FROM restaurant WHERE restaurant_id=? FOR UPDATE", restaurant);
             var r = s.executeQuery()) {
            if (!r.next()) throw new IllegalArgumentException("Restaurant nicht gefunden.");
        }
    }
    public Konto konto(Connection c, int restaurant, int id) throws SQLException {
        try (var s = statement(c, "SELECT mitarbeiter_id,kuerzel,rolle,m_status,stundensatz,passwort_hash FROM mitarbeiter WHERE restaurant_id=? AND mitarbeiter_id=?", restaurant, id);
             var r = s.executeQuery()) {
            if (!r.next()) throw new IllegalArgumentException("Mitarbeiter nicht gefunden. Bitte aktualisieren.");
            return new Konto(personal(r), r.getString("passwort_hash"));
        }
    }
    public List<Personal> personal(Connection c, int restaurant) throws SQLException {
        List<Personal> result = new ArrayList<>();
        try (var s = statement(c, "SELECT mitarbeiter_id,kuerzel,rolle,m_status,stundensatz FROM mitarbeiter WHERE restaurant_id=? ORDER BY kuerzel", restaurant);
             var r = s.executeQuery()) { while (r.next()) result.add(personal(r)); }
        return List.copyOf(result);
    }
    private Personal personal(ResultSet r) throws SQLException {
        return new Personal(r.getInt("mitarbeiter_id"), r.getString("kuerzel"), r.getString("rolle"), r.getString("m_status"), r.getBigDecimal("stundensatz"));
    }
    public List<Fahrzeug> fahrzeuge(Connection c, int restaurant) throws SQLException {
        List<Fahrzeug> result = new ArrayList<>();
        try (var s = statement(c, """
                SELECT f.*, (EXISTS(SELECT 1 FROM ressourcen_zuweisung z WHERE z.fahrzeug_id=f.fahrzeug_id AND z.zugewiesen_bis IS NULL)
                OR EXISTS(SELECT 1 FROM zeiterfassung t WHERE t.fahrzeug_id=f.fahrzeug_id AND t.check_out IS NULL)) AS belegt
                FROM fahrzeug f WHERE f.restaurant_id=? ORDER BY f.fahrzeug_nummer
                """, restaurant); var r = s.executeQuery()) {
            while (r.next()) result.add(new Fahrzeug(r.getInt("fahrzeug_id"), r.getString("fahrzeug_typ"), r.getString("fahrzeug_nummer"),
                    r.getString("kennzeichen"), r.getString("marke"), r.getString("f_status"), r.getBoolean("belegt")));
        }
        return List.copyOf(result);
    }
    public List<Handy> handys(Connection c, int restaurant) throws SQLException {
        List<Handy> result = new ArrayList<>();
        try (var s = statement(c, """
                SELECT h.handy_id,h.h_bezeichnung,h.h_mobilnummer,h.h_status,
                (EXISTS(SELECT 1 FROM ressourcen_zuweisung z WHERE z.handy_id=h.handy_id AND z.zugewiesen_bis IS NULL)
                OR EXISTS(SELECT 1 FROM zeiterfassung t WHERE t.handy_id=h.handy_id AND t.check_out IS NULL)) AS belegt
                FROM handy h WHERE h.restaurant_id=? ORDER BY h.h_bezeichnung
                """, restaurant); var r = s.executeQuery()) {
            while (r.next()) result.add(new Handy(r.getInt("handy_id"), r.getString("h_bezeichnung"), r.getString("h_mobilnummer"), r.getString("h_status"), r.getBoolean("belegt")));
        }
        return List.copyOf(result);
    }
    public List<Schicht> schichten(Connection c, int restaurant) throws SQLException {
        List<Schicht> result = new ArrayList<>();
        try (var s = statement(c, """
                SELECT z.zeiterfassung_id,z.mitarbeiter_id,m.kuerzel,m.rolle,z.rolle_in_schicht,z.check_in,
                COALESCE((SELECT MAX(r.fahrzeug_id) FROM ressourcen_zuweisung r WHERE r.zeiterfassung_id=z.zeiterfassung_id AND r.zugewiesen_bis IS NULL),z.fahrzeug_id) AS fid,
                COALESCE((SELECT MAX(r.handy_id) FROM ressourcen_zuweisung r WHERE r.zeiterfassung_id=z.zeiterfassung_id AND r.zugewiesen_bis IS NULL),z.handy_id) AS hid
                FROM zeiterfassung z JOIN mitarbeiter m ON m.mitarbeiter_id=z.mitarbeiter_id
                WHERE m.restaurant_id=? AND z.check_out IS NULL ORDER BY z.check_in,m.kuerzel
                """, restaurant); var r = s.executeQuery()) {
            Map<Integer, Fahrzeug> vehicles = new HashMap<>(); fahrzeuge(c, restaurant).forEach(f -> vehicles.put(f.id(),f));
            Map<Integer, Handy> phones = new HashMap<>(); handys(c, restaurant).forEach(h -> phones.put(h.id(),h));
            while (r.next()) {
                Integer fid = r.getObject("fid", Integer.class), hid = r.getObject("hid", Integer.class);
                result.add(new Schicht(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5),
                        r.getObject(6, LocalDateTime.class), fid, vehicles.containsKey(fid) ? vehicles.get(fid).toString() : "",
                        hid, phones.containsKey(hid) ? phones.get(hid).toString() : ""));
            }
        }
        return List.copyOf(result);
    }
    public int savePersonal(Connection c, int restaurant, PersonalEingabe input, String hash) throws SQLException {
        if (input.id() == null) return insert(c, "INSERT INTO mitarbeiter (restaurant_id,kuerzel,rolle,m_status,stundensatz,passwort_hash) VALUES (?,?,?,?,?,?)",
                restaurant,input.kuerzel(),input.rolle(),input.status(),input.stundensatz(),hash);
        update(c, "UPDATE mitarbeiter SET kuerzel=?,rolle=?,m_status=?,stundensatz=? WHERE restaurant_id=? AND mitarbeiter_id=?",
                input.kuerzel(),input.rolle(),input.status(),input.stundensatz(),restaurant,input.id());
        if (hash != null) update(c, "UPDATE mitarbeiter SET passwort_hash=? WHERE restaurant_id=? AND mitarbeiter_id=?",hash,restaurant,input.id());
        return input.id();
    }
    public int saveFahrzeug(Connection c, int restaurant, FahrzeugEingabe input) throws SQLException {
        if (input.id() == null) return insert(c, "INSERT INTO fahrzeug (restaurant_id,fahrzeug_typ,fahrzeug_nummer,kennzeichen,marke,f_status) VALUES (?,?,?,?,?,?)",
                restaurant,input.typ(),input.nummer(),input.kennzeichen(),input.marke(),input.status());
        update(c, "UPDATE fahrzeug SET fahrzeug_typ=?,fahrzeug_nummer=?,kennzeichen=?,marke=?,f_status=? WHERE restaurant_id=? AND fahrzeug_id=?",
                input.typ(),input.nummer(),input.kennzeichen(),input.marke(),input.status(),restaurant,input.id());
        return input.id();
    }
    public int saveHandy(Connection c, int restaurant, HandyEingabe input) throws SQLException {
        if (input.id() == null) return insert(c, "INSERT INTO handy (restaurant_id,h_bezeichnung,h_mobilnummer,h_status,h_sim_pin) VALUES (?,?,?,?,?)",
                restaurant,input.bezeichnung(),input.mobilnummer(),input.status(),input.neuePin());
        update(c, "UPDATE handy SET h_bezeichnung=?,h_mobilnummer=?,h_status=? WHERE restaurant_id=? AND handy_id=?",
                input.bezeichnung(),input.mobilnummer(),input.status(),restaurant,input.id());
        if (input.neuePin() != null && !input.neuePin().isEmpty())
            update(c, "UPDATE handy SET h_sim_pin=? WHERE restaurant_id=? AND handy_id=?",input.neuePin(),restaurant,input.id());
        return input.id();
    }
    public void delete(Connection c, StammdatenArt art, int restaurant, int id) throws SQLException {
        String table = table(art);
        update(c, "DELETE FROM " + table + " WHERE restaurant_id=? AND " + table + "_id=?",restaurant,id);
    }
    public static String table(StammdatenArt art) {
        return switch (art) { case MITARBEITER -> "mitarbeiter"; case FAHRZEUG -> "fahrzeug"; case HANDY -> "handy"; };
    }
    public int checkIn(Connection c, int employee, int actor, LocalDate day, LocalDateTime now, java.math.BigDecimal wage, String activity) throws SQLException {
        return insert(c, "INSERT INTO zeiterfassung (mitarbeiter_id,angemeldet_von_mitarbeiter_id,geschaeftstag,check_in,stundensatz_snapshot,rolle_in_schicht) VALUES (?,?,?,?,?,?)",
                employee,actor,day,now,wage,activity);
    }
    public void assign(Connection c, int shift, Integer vehicle, Integer phone, LocalDateTime now) throws SQLException {
        insert(c, "INSERT INTO ressourcen_zuweisung (zeiterfassung_id,fahrzeug_id,handy_id,zugewiesen_ab) VALUES (?,?,?,?)",shift,vehicle,phone,now);
    }
    public void validateCheckoutResources(Connection c, int restaurant, Schicht shift) throws SQLException {
        if (shift.fahrzeugId()!=null && fahrzeuge(c,restaurant).stream().noneMatch(f -> f.id()==shift.fahrzeugId()))
            throw new IllegalArgumentException("Fahrzeug gehört nicht zum Restaurant. Datenbestand prüfen.");
        if (shift.handyId()!=null && handys(c,restaurant).stream().noneMatch(h -> h.id()==shift.handyId()))
            throw new IllegalArgumentException("Handy gehört nicht zum Restaurant. Datenbestand prüfen.");
        try (var s=statement(c,"SELECT COUNT(fahrzeug_id),COUNT(handy_id) FROM ressourcen_zuweisung WHERE zeiterfassung_id=? AND zugewiesen_bis IS NULL",shift.id());
             var r=s.executeQuery()) {
            r.next();
            if(r.getInt(1)>1 || r.getInt(2)>1) throw new IllegalArgumentException("Mehrere gleichzeitige Ressourcenzuweisungen gefunden. Bitte Datenbestand prüfen.");
        }
    }
    public void checkOut(Connection c, Schicht shift, int actor, LocalDateTime now, Pruefung vehicle, Pruefung phone) throws SQLException {
        // Alte optionale Ressourcen werden ebenfalls geprüft; neue Zuordnungen ausschließlich über Historientabelle.
        update(c, "UPDATE zeiterfassung SET check_out=?,abgemeldet_von_mitarbeiter_id=? WHERE zeiterfassung_id=? AND check_out IS NULL",now,actor,shift.id());
        try (var s = statement(c, "SELECT zuweisung_id,fahrzeug_id,handy_id FROM ressourcen_zuweisung WHERE zeiterfassung_id=? AND zugewiesen_bis IS NULL FOR UPDATE",shift.id());
             var r = s.executeQuery()) {
            while (r.next()) {
                Pruefung p = r.getObject("fahrzeug_id") != null ? vehicle : phone;
                update(c, "UPDATE ressourcen_zuweisung SET zugewiesen_bis=?,geprueft_von_mitarbeiter_id=?,ressource_in_ordnung=?,mangel_notiz=? WHERE zuweisung_id=?",
                        now,actor,p.inOrdnung(),p.mangel(),r.getInt("zuweisung_id"));
            }
        }
        if (shift.fahrzeugId() != null || shift.handyId() != null) {
            insert(c, "INSERT INTO checkout_pruefung (zeiterfassung_id,fahrzeug_in_ordnung,fahrzeug_mangel_notiz,handy_in_ordnung,handy_mangel_notiz) VALUES (?,?,?,?,?)",
                    shift.id(),vehicle == null ? null : vehicle.inOrdnung(),vehicle == null ? null : vehicle.mangel(),
                    phone == null ? null : phone.inOrdnung(),phone == null ? null : phone.mangel());
        }
        if (shift.fahrzeugId() != null && !vehicle.inOrdnung()) update(c, "UPDATE fahrzeug SET f_status='DEFEKT' WHERE fahrzeug_id=?",shift.fahrzeugId());
        if (shift.handyId() != null && !phone.inOrdnung()) update(c, "UPDATE handy SET h_status='DEFEKT' WHERE handy_id=?",shift.handyId());
    }
    public void audit(Connection c, String table, int id, int actor, String reason) throws SQLException {
        insert(c, "INSERT INTO aenderungsprotokoll (tabelle_name,datensatz_id,geaendert_von_mitarbeiter_id,aenderungsgrund) VALUES (?,?,?,?)",table,id,actor,reason);
    }
    private PreparedStatement statement(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql);
        try { for (int i=0; i<args.length; i++) s.setObject(i+1,args[i]); return s; }
        catch (SQLException e) { s.close(); throw e; }
    }
    private void update(Connection c, String sql, Object... args) throws SQLException {
        try (var s = statement(c,sql,args)) {
            if (s.executeUpdate() != 1) throw new IllegalArgumentException("Datensatz nicht mehr vorhanden oder bereits geändert. Bitte aktualisieren.");
        }
    }
    private int insert(Connection c, String sql, Object... args) throws SQLException {
        try (var s = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for (int i=0; i<args.length; i++) s.setObject(i+1,args[i]);
            if (s.executeUpdate() != 1) throw new SQLException("Insert fehlgeschlagen.");
            try (var keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("ID fehlt."); return keys.getInt(1); }
        }
    }
}
