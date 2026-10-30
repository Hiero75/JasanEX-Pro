package de.jasanex.pro.model;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record OffeneSchicht(int id, int mitarbeiterId, LocalDate geschaeftstag,
        LocalDateTime checkIn, String taetigkeit) { }
