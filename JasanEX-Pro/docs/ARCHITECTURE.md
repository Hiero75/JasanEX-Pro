# JasanEX Pro – Architektur und fachlicher Rahmen

## Ergänzung: Implementiert in 0.2

Aktueller Umfang und Prüfungen: [VERSION-0.2.md](VERSION-0.2.md). Mitarbeiter-, Fahrzeug- und Handy-CRUD sowie manuelles Ein-/Ausstempeln mit Ressourcenzuweisung und Rückgabeprüfung sind jetzt implementiert. Die folgenden Abschnitte zu 0.1 beschreiben den historischen Ausgangspunkt; entsprechende geplante Prozesse sind mit dieser Ergänzung teilweise umgesetzt.

`BetriebService` prüft die aktuelle Datenbankrolle und den Aktivstatus der Sitzung bei jedem Zugriff erneut. Schreibvorgänge sperren zunächst die Restaurantzeile (`SELECT … FOR UPDATE`), prüfen anschließend ihre Voraussetzungen und bestätigen SQL plus Aktionsprotokoll in einer Transaktion. Alle schreibenden App-Instanzen verwenden diese Reihenfolge; direkte Änderungen außerhalb der App müssen dieselben Invarianten respektieren. Lese-Snapshots nutzen REPEATABLE READ. Nach Commit lädt der Controller den Snapshot neu, dann werden Cache und Ansicht ersetzt. Schlägt das Neuladen fehl, fordert die UI Aktualisierung und verhindert weitere Aktionen auf veraltetem Stand.

Bei HEAD enthält der Snapshot keine Personal-Stundensätze. Berechtigungsrollen und Tätigkeiten sind getrennt. Unbekannte alte Schichttätigkeiten bleiben unter Sonstiges sichtbar. Historische Referenzen bleiben durch die vorhandenen Fremdschlüssel erhalten. Sonstiges-Begründungen werden ohne neue Spalte als Aktionsgrund im vorhandenen Änderungsprotokoll gespeichert. Passwörter und SIM-PINs erscheinen nicht im Protokoll. Laufende Ressourcenwechsel und HauptHead-Persistenz bleiben offen.

## Implementiert in 0.1

Windows, Java 26, Maven, JavaFX 26.0.1 mit FXML/CSS, JDBC über MariaDB Connector/J 3.5.11. Kein Spring, REST-Server, Cloud-Dienst oder DI-Framework. Bestehende Maven-Koordinaten `de.JansanEX:JasanEX-Pro` bleiben erhalten.

Abhängigkeiten: **View → Controller → Service → Repository → Datenbank**. Model, Config, Security und Cache ergänzen diese Schichten. Main erstellt und verwaltet Session/Cache. Keine SQL-Anweisungen in Views oder Controllern. Nicht benötigte Fahrzeug-/Handyservices werden erst mit ihren Funktionen ergänzt.

Online-Authentifizierung vor Zugriff auf Unternehmensdaten. Erst nach gültigem Passwort, aktivem Status und erlaubter Rolle werden restaurantgebundene Mitarbeiterstammdaten und offene Zeiterfassungen geladen. Datenbankarbeit läuft außerhalb des JavaFX Application Thread. Datenbankverbindungen sind zeitlich begrenzt und werden geschlossen. Der Login-Ladevorgang verwendet einen konsistenten Lesetransaktionszustand. Bei Fehlern entsteht keine Session mit teilweise geladenem Cache.

Die Datenbank ist maßgeblich. Cache besteht aus einer Map von Mitarbeiter-ID zu unveränderlichem Mitarbeiter-Record und einer Liste offener Schichten. Keine Passworthashes oder Löhne im Stammdatencache. Kein dauerhaftes lokales Abbild. Abmelden und Programmende entfernen die Session und Cache-Referenzen, beenden jedoch keine Schichten. JVM-Strings lassen sich nicht garantiert aus dem Speicher löschen; Passwortfelder werden geleert und verarbeitete char-Arrays überschrieben.

Dashboard ist eine Grundlage mit Kürzel, Rolle, Restaurant, Uhr, Geschäftstag und beim Login geladenen Anzahlen. Es gibt keine laufende Datenbanksynchronisierung. Die Uhr und der angezeigte Geschäftstag aktualisieren sich; die Anzahlen sind ausdrücklich ein Login-Snapshot. Eine HauptHead-Zuständigkeit wird nicht suggeriert. Die UI verwendet unveränderte Original-Logos mit erhaltenen Proportionen.

## Datenmodell und Grenzen

Vorhandene Tabellen: restaurant, mitarbeiter, fahrzeug, handy, zeiterfassung, ressourcen_zuweisung, checkout_pruefung, aenderungsprotokoll; View v_aktive_ressourcen. Schema wurde lesend geprüft, nicht geändert. Die geplanten Spalten aus dem Master-Prompt sind vorhanden. Das vollständige ursprüngliche Anforderungsdokument wird unter `docs/MASTER-PROMPT.md` archiviert, damit auch Detailregeln verfügbar bleiben.

Mitarbeiterkürzel sind nur restaurantintern eindeutig. Bei mehreren Restaurants ist eine konfigurierte Restaurant-ID erforderlich. Eine mehrdeutige Mitarbeiterabfrage wird abgewiesen. Die realen Statuswerte sind nicht durch Java-Enums ersetzt; der bestätigte Aktivstatus wird exakt konfiguriert. Rollen werden von realen Datenbankwerten auf ADMIN/MANAGER/HEAD abgebildet; unbekannte Rollen sind nicht zugelassen.

## Geplante Geschäftsprozesse

- **Personal:** Stammdaten, Status, Rollen und Berechtigungen. ADMIN darf alles; MANAGER darf Mitarbeiter/Ressourcen, Korrekturen, Löhne und Headrechte verwalten. HEAD darf ein-/ausstempeln, Ressourcen zuweisen/prüfen und nach Mitarbeiter-Passwortbestätigung dessen Arbeitsstunden sehen, niemals Stundenlohn oder berechneten Lohn.
- **Tätigkeit vs. Rolle:** Küche, Autofahrer, Fahrradfahrer oder Sonstiges sind Schichttätigkeiten unabhängig von Berechtigungen. Sonstiges benötigt eine Begründung; ein geeignetes Feld ist noch zu planen.
- **Einstempeln:** Mitarbeiter anhand Kürzel und Passwort bestätigen; verantwortliche Person, Check-in, Geschäftstag, Tätigkeit und Stundenlohnsnapshot speichern. Pro Mitarbeiter höchstens eine offene Zeiterfassung. Parallelität muss datenbankseitig/transaktional abgesichert werden.
- **Geschäftstag:** Restaurantzeitzone Europe/Berlin, Wechsel 04:00 Uhr. Check-in 30.10.2026 02:30 gehört zum 29.10.; 04:30 zum 30.10. Geschäftstag beim Check-out nicht neu berechnen, auch bei Arbeit über 04:00 Uhr hinaus. Für spätere Dauerberechnung einschließlich Sommer-/Winterzeit ist die Speicherung absoluter Zeitpunkte zu entscheiden; vorhandenes DATETIME enthält keine Zone.
- **HauptHead:** erster berechtigter Head/Manager/Admin übernimmt. HEAD startet automatisch eine Zeiterfassung, sofern noch keine offen ist; ADMIN/MANAGER nicht. Mitarbeiteran-/abmeldungen werden dem Verantwortlichen zugeordnet. Beim Wechsel übernimmt genau ein weiterer Head automatisch; bei mehreren erfolgt Auswahl; ohne Nachfolger Küchenmodus oder geregeltes Schichtende. Persistente Zuständigkeit ist noch nicht ausreichend im Schema abgebildet.
- **Ressourcen:** Autofahrer erhalten Auto und Handy, Fahrradfahrer Fahrrad und Handy. Verfügbar/in Benutzung/defekt/gesperrt sind fachliche Zustände, deren reale DB-Werte noch abzugleichen sind. Belegung aus offenen ressourcen_zuweisung-Datensätzen bestimmen; keine Doppelzuweisung. Jeder Eintrag enthält entweder Fahrzeug oder Handy.
- **Ressourcenwechsel:** alte Ressource prüfen und Zustand/Mängel dokumentieren, neue Verfügbarkeit prüfen, alte Zuweisung beenden und neue anlegen – atomar in einer Transaktion. Gleiche zeiterfassung_id; niemals neue Arbeitszeiterfassung durch Ressourcenwechsel. Alte optionale fahrzeug_id/handy_id in zeiterfassung nicht für neue Wechsel verwenden und nicht eigenmächtig entfernen.
- **Checkout:** Ressourcenprüfung und Mängel dokumentieren, Schicht und Zuweisungen konsistent beenden. Änderungen erst nach Datenbankcommit in Cache und UI übernehmen; bei Fehler rollback, keine falsche Erfolgsmeldung und bei unklarer Commitlage neu laden.
- **Küchenmodus:** Verwaltungsfunktionen sperren; Küchenmitarbeiter können sich ohne Passwort selbst abmelden. Eigene ID als abgemeldet_von_mitarbeiter_id; sichtbare Personenbestätigung gegen versehentliche Fremdabmeldung. Detailfluss offen.
- **Arbeitszeitkonten:** frei wählbarer Zeitraum. HEAD nur Stunden nach Mitarbeiterpasswort; ADMIN/MANAGER auch Lohn aus stundensatz_snapshot. Soll-/Überstundenregeln noch offen.
- **Wiederherstellung:** Stammdaten, check_out IS NULL, zugewiesen_bis IS NULL, HauptHead-Zustand und Ressourcenstatus aus MySQL wiederherstellen. Absturz/Beenden darf Schichten nicht schließen. Später alle betrieblichen Änderungen sofort dauerhaft speichern.
- **Audit und Erweiterungen:** Änderungsprotokoll, Statistiken, Löhne, mehrere Restaurants, mögliche API für weitere PCs. Direkter JDBC-Zugriff ist zunächst nur lokal vorgesehen.

## Offene Entscheidungen vor Erweiterungen

1. Bestätigter aktiver Mitarbeiterstatus sowie tatsächliche Admin-/Managerwerte.
2. Bisheriges Hashformat und ausdrücklich beauftragte sichere Passwortmigration.
3. Persistenter HauptHead-/Küchenmoduszustand und Schichttätigkeitsbegründung.
4. Eindeutigkeitsregeln und Parallelitätsschutz für offene Schichten/Ressourcen.
5. Sommerzeitfeste Zeitpunktspeicherung, Rundung, Sollstunden und Überstunden.
6. Rechte eines künftigen dedizierten Datenbankbenutzers und Architektur bei mehreren PCs.

Neue Tabellen/Spalten nur nach ausdrücklich beauftragter Migration, zunächst als separates Vorschlagsskript. Keine eigenmächtigen Änderungen, keine automatischen Testdaten, keine Commits/Pushes/Resets ohne Auftrag.
