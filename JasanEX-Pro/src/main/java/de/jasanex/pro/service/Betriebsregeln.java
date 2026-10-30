package de.jasanex.pro.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class Betriebsregeln {
    public static final ZoneId ZEITZONE = ZoneId.of("Europe/Berlin");
    public static final List<String> TAETIGKEITEN = List.of("Head", "Küche", "Autofahrer", "Fahrradfahrer", "Sonstiges");
    private Betriebsregeln() { }

    public static LocalDate geschaeftstag(ZonedDateTime zeit) {
        ZonedDateTime lokal = zeit.withZoneSameInstant(ZEITZONE);
        return lokal.toLocalTime().isBefore(LocalTime.of(4, 0))
                ? lokal.toLocalDate().minusDays(1) : lokal.toLocalDate();
    }
    public static String text(String wert, String feld, int max, boolean erforderlich) {
        String result = wert == null ? "" : wert.trim();
        if ((erforderlich && result.isEmpty()) || result.length() > max)
            throw new IllegalArgumentException(feld + ": " + (erforderlich ? "1 bis " : "höchstens ") + max + " Zeichen erforderlich.");
        return result;
    }
    public static void stundensatz(BigDecimal wert) {
        if (wert == null || wert.signum() < 0 || wert.scale() > 2 || wert.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Stundensatz: positiver Betrag oder 0, maximal zwei Nachkommastellen.");
    }
    public static boolean fahrer(String taetigkeit) {
        return "Autofahrer".equals(taetigkeit) || "Fahrradfahrer".equals(taetigkeit);
    }
    public static String kategorie(String taetigkeit) {
        if (TAETIGKEITEN.contains(taetigkeit)) return taetigkeit;
        return "Sonstiges";
    }
}
