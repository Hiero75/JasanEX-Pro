package de.jasanex.pro.model;
/** Stammdaten ohne Passwort-Hash und ohne Lohndaten. */
public record Mitarbeiter(int id, int restaurantId, String kuerzel, String rolle, String status) { }
