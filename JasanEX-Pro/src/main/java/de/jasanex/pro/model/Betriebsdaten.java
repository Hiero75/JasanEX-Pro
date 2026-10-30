package de.jasanex.pro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Daten für Betriebsansicht und geschützte Stammdatenverwaltung. Keine Passworthashes. */
public final class Betriebsdaten {
    private Betriebsdaten() { }

    public record Personal(int id, String kuerzel, String rolle, String status, BigDecimal stundensatz) {
        @Override public String toString() { return kuerzel + " · " + rolle; }
    }
    public record Fahrzeug(int id, String typ, String nummer, String kennzeichen, String marke,
                           String status, boolean belegt) {
        @Override public String toString() { return typ + " " + nummer + (kennzeichen == null ? "" : " · " + kennzeichen); }
    }
    public record Handy(int id, String bezeichnung, String mobilnummer, String status, boolean belegt) {
        @Override public String toString() { return bezeichnung; }
    }
    public record Schicht(int id, int mitarbeiterId, String kuerzel, String rolle, String taetigkeit,
                          LocalDateTime checkIn, Integer fahrzeugId, String fahrzeug,
                          Integer handyId, String handy) { }
    public record Snapshot(Map<Integer, Mitarbeiter> mitarbeiter, List<OffeneSchicht> offeneSchichten,
                           List<Personal> personal, List<Fahrzeug> fahrzeuge, List<Handy> handys,
                           List<Schicht> schichten) { }
    public record PersonalEingabe(Integer id, String kuerzel, String rolle, String status,
                                  BigDecimal stundensatz, char[] passwort) { }
    public record FahrzeugEingabe(Integer id, String typ, String nummer, String kennzeichen,
                                  String marke, String status) { }
    public record HandyEingabe(Integer id, String bezeichnung, String mobilnummer, String status,
                               String neuePin) { }
    public record CheckIn(int mitarbeiterId, String taetigkeit, String begruendung,
                          char[] passwort, Integer fahrzeugId, Integer handyId) { }
    public record Pruefung(boolean inOrdnung, String mangel) { }
    public record CheckOut(int schichtId, Pruefung fahrzeug, Pruefung handy) { }
    public enum StammdatenArt { MITARBEITER, FAHRZEUG, HANDY }
}
