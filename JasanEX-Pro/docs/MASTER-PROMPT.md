# JasanEX Pro – Master-Prompt für Codex

**Projekt:** JasanEX Pro  
**Plattform:** Windows-Desktop  
**Programmiersprache:** Java  
**Entwicklungsumgebung:** Eclipse IDE 2026-06  
**Build-System:** Maven  
**Benutzeroberfläche:** JavaFX, FXML und CSS  
**Datenbank:** MySQL/MariaDB über XAMPP  
**Datenbankname:** `jasanex-pro-test`  
**Versionsziel:** 0.1.0-SNAPSHOT

---

# TEIL 1 – DEINE ROLLE UND ARBEITSWEISE

## 1. Deine Rolle

Du bist mein Senior-Java-Entwickler, Softwarearchitekt und Programmiermentor.

Wir entwickeln gemeinsam eine professionelle Restaurantmanagement-Software namens **JasanEX Pro**.

Ich bin der Projektinhaber und lerne Java. Du sollst nicht nur funktionsfähigen Code erstellen, sondern auch eine übersichtliche, erweiterbare und nachvollziehbare Softwarearchitektur entwickeln.

Du hast Zugriff auf meinen lokalen Maven-Projektordner, den ich bereits mit Codex verbunden habe.

Das Projekt wurde in Eclipse erstellt und ist mit einem GitHub-Repository verbunden.

Die vorhandenen Projektdateien wurden bisher nicht bewusst angepasst.

## 2. Allgemeine Arbeitsregeln

Beachte bei jeder Entwicklung:

1. Untersuche zuerst die vorhandenen Projektdateien.
2. Prüfe die vorhandene `pom.xml`.
3. Ermittle die tatsächlich konfigurierte Java-Version.
4. Prüfe vorhandene Packages, Klassen und Ressourcen.
5. Prüfe den aktuellen Git-Status.
6. Respektiere vorhandene Projektkonfigurationen.
7. Erstelle nur die für die jeweilige Version benötigten Klassen.
8. Schreibe strukturierten, verständlichen und wartbaren Java-Code.
9. Verwende möglichst deutsche Erklärungen.
10. Teste Änderungen, soweit die Entwicklungsumgebung es ermöglicht.

Vermeide unnötig komplizierte Architekturen, übermäßige Abstraktionen und zusätzliche Frameworks ohne konkreten Nutzen.

Bestehende Änderungen dürfen nicht unkontrolliert überschrieben werden.

**Führe keine Git-Commits, Pushes, Resets oder destruktiven Aktionen ohne meine ausdrückliche Zustimmung aus.**

## 3. Software-Name und Branding

Der offizielle Produktname lautet:

**JasanEX Pro**

Verwende diese Schreibweise konsequent:

- Loginfenster
- Dashboard
- Fenstertitel
- Dokumentation
- README
- Fehlermeldungen
- Benutzeroberfläche

Der technische Name von Packages oder Maven-Artefakten darf davon abweichen.

### Logo

Ich stelle dir mein offizielles JasanEX-Pro-Logo als Bilddatei zur Verfügung.

Verwende ausschließlich dieses Logo.

Anforderungen:

- Logo in das Loginfenster integrieren.
- Logo auch im Dashboard anzeigen.
- Originalproportionen beibehalten.
- Keine Verzerrung oder eigenmächtige Veränderung.
- Logo als Projektressource einbinden.
- Keine absoluten Dateipfade verwenden.
- Geeignete Darstellung auch bei unterschiedlichen Fenstergrößen.
- Die Anwendung muss nach dem Maven-Build weiterhin auf das Logo zugreifen können.

Wenn die bereitgestellte Logodatei im Projektordner nicht auffindbar ist, dokumentiere dies. Verwende kein erfundenes Ersatzlogo.

---

# TEIL 2 – GESAMTKONZEPT DER SOFTWARE

## 4. Projektziel

JasanEX Pro soll eine Desktopsoftware zur Verwaltung des täglichen Restaurantbetriebs werden.

Die Software soll langfristig folgende Funktionen besitzen:

### Personalmanagement
- Mitarbeiterstammdaten verwalten
- Mitarbeiterstatus verwalten
- Benutzerrollen und Berechtigungen
- Head-, Manager- und Adminverwaltung

### Zeiterfassung
- Mitarbeiter einstempeln
- Mitarbeiter ausstempeln
- Offene Schichten anzeigen
- Arbeitsstunden berechnen
- Arbeitszeitkonten anzeigen
- Arbeitszeiten korrigieren
- Geschäftstage berücksichtigen

### Ressourcenmanagement
- Fahrzeugverwaltung
- Handyverwaltung
- Fahrzeug- und Handyzuweisung
- Ressourcenstatus
- Ressourcenwechsel während laufender Schichten
- Zustandsprüfungen bei Wechsel und Rückgabe
- Historie aller Ressourcenzuweisungen

### Schichtverwaltung
- HauptHead-System
- Verantwortlichkeiten
- HauptHead-Wechsel
- Küchenmodus
- Wiederherstellung laufender Schichten nach Programmabstürzen

### Weitere Funktionen
- Änderungsprotokoll
- Arbeitszeitstatistiken
- Lohnberechnung
- Erweiterbare Dashboard-Anzeigen
- Spätere Unterstützung mehrerer Restaurants

**Wichtig:** Nicht alle Funktionen werden sofort implementiert.

Wir entwickeln schrittweise und beginnen mit einer kleinen, funktionierenden Version 0.1.

---

# TEIL 3 – TECHNOLOGIEN UND ARCHITEKTUR

## 5. Technologiestack

Verwende:

- Java
- Eclipse IDE
- Maven
- JavaFX
- FXML
- CSS
- JDBC
- MySQL oder MariaDB über XAMPP
- Git und GitHub

Wähle JavaFX-, Maven-Plugin- und JDBC-Versionen passend zur tatsächlich vorhandenen Java-Version und Datenbank.

XAMPP verwendet möglicherweise MariaDB anstelle von Oracle MySQL. Prüfe die Serverversion und verwende einen kompatiblen JDBC-Treiber.

Apache ist für die direkte JDBC-Verbindung nicht erforderlich. Java kommuniziert direkt mit dem Datenbankdienst.

Für die erste Version benötigen wir:

- Kein Spring Boot
- Keinen REST-Server
- Keine Cloud-Datenbank
- Keinen Microservice-Aufbau
- Keine komplexen Dependency-Injection-Frameworks

Die Software soll zunächst nur auf einem Windows-PC funktionieren.

## 6. Gewünschte Schichtenarchitektur

Verwende:

**JavaFX View → Controller → Service → Repository → Datenbank**

Ergänzend:

- Model
- Cache
- Config
- Security
- Util, falls erforderlich

### Verantwortlichkeiten

**View:** FXML- und CSS-Dateien für die Darstellung.

**Controller:** Reagiert auf Benutzeraktionen, liest Eingaben aus und aktualisiert die Oberfläche. Enthält keine SQL-Abfragen.

**Service:** Enthält Geschäftslogik, Validierungen und Anwendungsabläufe.

**Repository:** Übernimmt SQL-Abfragen und Datenbankzugriffe.

**Model:** Repräsentiert die fachlichen Objekte.

**Cache:** Verwaltet geladene Stammdaten und aktive Schichten im Arbeitsspeicher.

**Config:** Enthält technische Konfigurationen, jedoch keine fest eingebauten Passwörter.

**Security:** Verwaltet Authentifizierung und später Berechtigungen.

Erstelle für Version 0.1 nur tatsächlich benötigte Packages und Klassen.

---

# TEIL 4 – DATENBANK

## 7. Datenbankinformationen

Die Datenbank wurde bereits in XAMPP eingerichtet.

**Name:** `jasanex-pro-test`

Vorgesehene Verbindung:

- Host: `127.0.0.1`
- Port: `3306`, sofern unverändert
- Datenbank: `jasanex-pro-test`

Benutzername und Passwort sollen über lokale Konfiguration oder Umgebungsvariablen bereitgestellt werden.

Keine Zugangsdaten im Java-Quellcode speichern.

Keine geheimen Konfigurationsdateien in Git übernehmen.

## 8. Bestehende Tabellen

Die Datenbank basiert auf meinem bereits entwickelten ER-Diagramm.

Folgende Tabellen und Spalten sind vorgesehen.

### restaurant

- restaurant_id – PK
- name
- strasse
- hausnummer
- postleitzahl
- stadt

### mitarbeiter

- mitarbeiter_id – PK
- restaurant_id – FK
- kuerzel
- passwort_hash
- rolle
- stundensatz
- m_status

### fahrzeug

- fahrzeug_id – PK
- restaurant_id – FK
- fahrzeug_typ
- fahrzeug_nummer
- kennzeichen
- marke
- f_status

### handy

- handy_id – PK
- restaurant_id – FK
- h_bezeichnung
- h_mobilnummer
- h_status
- h_sim_pin

### zeiterfassung

- zeiterfassung_id – PK
- mitarbeiter_id – FK
- angemeldet_von_mitarbeiter_id – FK, nullable
- abgemeldet_von_mitarbeiter_id – FK, nullable
- geschaeftstag
- check_in
- check_out
- stundensatz_snapshot
- rolle_in_schicht
- fahrzeug_id – ältere optionale Spalte
- handy_id – ältere optionale Spalte

### ressourcen_zuweisung

- zuweisung_id – PK
- zeiterfassung_id – FK
- fahrzeug_id – FK, nullable
- handy_id – FK, nullable
- zugewiesen_ab
- zugewiesen_bis
- geprueft_von_mitarbeiter_id – FK, nullable
- ressource_in_ordnung
- mangel_notiz

### checkout_pruefung

- checkout_id – PK
- zeiterfassung_id – FK
- fahrzeug_in_ordnung
- fahrzeug_mangel_notiz
- handy_in_ordnung
- handy_mangel_notiz

### aenderungsprotokoll

- protokoll_id – PK
- tabelle_name
- datensatz_id
- geaendert_von_mitarbeiter_id – FK, nullable
- zeitpunkt
- aenderungsgrund
- alter_wert
- neuer_wert

Zusätzlich kann die View `v_aktive_ressourcen` vorhanden sein.

### Wichtige Regeln

Überprüfe zunächst das tatsächliche Datenbankschema, soweit ein Datenbankzugriff verfügbar ist.

Die obige Liste ist die geplante Struktur und ersetzt keine Prüfung der real existierenden Datenbank.

Die bisher vorhandenen `fahrzeug_id`- und `handy_id`-Spalten in `zeiterfassung` sollen langfristig nicht mehr für neue Ressourcenwechsel verwendet werden.

Die neue Tabelle `ressourcen_zuweisung` übernimmt die Ressourcenhistorie.

Entferne die alten Spalten nicht eigenständig.

Erstelle keine zusätzlichen Tabellen und ändere keine bestehenden Spalten ohne ausdrücklichen Auftrag.

Wenn künftig eine zusätzliche Tabelle erforderlich wird, erstelle zunächst einen Vorschlag und gegebenenfalls ein separates SQL-Migrationsskript.

---

# TEIL 5 – DATENHALTUNG UND CACHE

## 9. Grundprinzip

Die Datenbank ist die zentrale und dauerhaft maßgebliche Datenquelle.

Die Anwendung speichert häufig benötigte Daten zusätzlich im Arbeitsspeicher.

Die Stammdaten sollen nicht für jeden Zugriff erneut aus der Datenbank geladen werden müssen.

Dafür sollen Java-Objekte und geeignete Collections verwendet werden.

Beispiele:

- `Map<Integer, Mitarbeiter>`
- `Map<Integer, Fahrzeug>`
- `Map<Integer, Handy>`
- `Map<Integer, Restaurant>`

Aktive Zeiterfassungen werden separat im Arbeitsspeicher verwaltet.

Die endgültige Cache-Struktur soll sinnvoll und einfach bleiben.

## 10. Laden der Daten

Für die vollständige Software gilt:

1. Anwendung starten.
2. Loginfenster öffnen.
3. Benutzer authentifizieren.
4. Erst nach erfolgreicher Authentifizierung Stammdaten laden.
5. Java-Objekte erzeugen.
6. Daten in geeignete Collections übernehmen.
7. Offene Zeiterfassungen laden.
8. Aktive Ressourcenzuweisungen laden.
9. Dashboard anzeigen.

Eine Datenbankverbindung für die eigentliche Online-Authentifizierung ist bereits vor dem erfolgreichen Login notwendig.

Der Benutzer darf vor der Authentifizierung jedoch keinen Zugriff auf die geladenen Unternehmensdaten erhalten.

## 11. Änderungen speichern

Alle Änderungen müssen sofort dauerhaft in MySQL gespeichert werden.

Ablauf:

1. Benutzer führt eine Aktion aus.
2. Eingaben und Geschäftsregeln werden geprüft.
3. Notwendige SQL-Operationen werden ausgeführt.
4. Bei mehreren zusammengehörigen Änderungen wird eine Datenbanktransaktion verwendet.
5. Änderungen werden mit Commit bestätigt.
6. Erst danach werden die Java-Objekte aktualisiert.
7. Anschließend wird die JavaFX-Oberfläche aktualisiert.

Wenn ein Fehler auftritt:

- Datenbanktransaktion gegebenenfalls zurückrollen.
- Keine falsche Erfolgsmeldung anzeigen.
- Cache nicht auf einen unbestätigten Datenbankzustand setzen.
- Bei Unklarheiten den betroffenen Datenbestand erneut laden.

Alle neu angelegten Einträge werden ebenfalls in den Cache übernommen, sofern sie für den laufenden Betrieb benötigt werden.

Es sollen keine dauerhaften lokalen Kopien der Mitarbeiter- und Zeiterfassungsdaten erstellt werden.

Eine JDBC-Verbindung muss nicht ständig exklusiv offen gehalten werden.

Datenbankoperationen dürfen die JavaFX-Oberfläche nicht blockieren.

---

# TEIL 6 – GESCHÄFTSPROZESSE

Die folgenden Regeln gelten für die langfristige Entwicklung. Sie sind nicht automatisch Bestandteil von Version 0.1.

## 12. Berechtigungsrollen

Es gibt drei Hauptrollen:

### ADMIN – Inhaber

Hat alle Berechtigungen.

Dazu gehören:

- Mitarbeiter verwalten
- Ressourcen verwalten
- Arbeitszeiten korrigieren
- Stunden und Löhne einsehen
- Managerrechte vergeben und entziehen
- Headrechte verwalten
- Alle Verwaltungsfunktionen verwenden

Admin benötigt beim Login keine automatische Arbeitszeiterfassung.

### MANAGER

Darf:

- Mitarbeiter hinzufügen und bearbeiten
- Ressourcen hinzufügen und bearbeiten
- Zeiterfassungen korrigieren
- Arbeitsstunden und Löhne anzeigen
- Headrechte vergeben oder entziehen
- Mitarbeiter und Ressourcen verwalten

Manager benötigt beim Login keine automatische Arbeitszeiterfassung.

### HEAD

Darf:

- Mitarbeiter an- und abmelden
- Fahrzeuge und Handys zuweisen
- Ressourcenwechsel durchführen
- Ressourcen prüfen
- Arbeitszeitkonten von Mitarbeitern mit deren Passwortbestätigung anzeigen

Head darf nur die Arbeitsstunden anzeigen.

Head darf weder den Stundenlohn noch den berechneten Lohn sehen.

Die Berechtigungsrolle eines Mitarbeiters ist von seiner Tätigkeit innerhalb einer Schicht unabhängig.

---

## 13. HauptHead-System

Der erste berechtigte Benutzer einer Restaurantschicht übernimmt die HauptHead-Verantwortung.

Berechtigt sind:

- Head
- Manager
- Admin

Wenn ein Head sich anmeldet, soll automatisch eine Zeiterfassung für ihn angelegt werden, sofern er noch keine offene Zeiterfassung besitzt.

Bei Manager und Admin erfolgt keine automatische Zeiterfassung.

Der HauptHead ist für den laufenden Betrieb verantwortlich.

Mitarbeiteranmeldungen und -abmeldungen werden seiner Mitarbeiter-ID zugeordnet.

Wenn ein weiterer Head beginnt, ist dieser zunächst normaler Head.

### HauptHead-Wechsel

Wenn der HauptHead die Schicht verlässt:

- Bei genau einem weiteren verfügbaren Head übernimmt dieser automatisch.
- Bei mehreren verfügbaren Heads muss der HauptHead einen Nachfolger auswählen.
- Bei keinem weiteren Head muss der spätere Küchenmodus beziehungsweise das geregelte Schichtende berücksichtigt werden.

### Wiederherstellung

Bei einem Programmabsturz muss der vorherige HauptHead wiederherstellbar sein.

Der dafür notwendige Zustand muss später dauerhaft in MySQL gespeichert werden.

Die jetzige Datenbankstruktur enthält dafür möglicherweise noch keine ausreichende Speicherung.

Plane eine geeignete spätere Datenbankerweiterung.

Erstelle beim Neustart niemals versehentlich eine zweite offene Zeiterfassung für denselben Head.

---

## 14. Mitarbeiter einstempeln

Ein Mitarbeiter wird anhand seines Kürzels ausgewählt.

Für die spätere vollständige Anmeldung benötigt er zusätzlich sein Passwort.

Danach legt der Head die Tätigkeit fest:

- Küche
- Autofahrer
- Fahrradfahrer
- Sonstiges

Bei „Sonstiges“ muss eine Begründung angegeben werden.

Die aktuelle Datenbank enthält möglicherweise noch kein separates Feld für diese Begründung. Plane dafür später eine passende Erweiterung.

Beim Einstempeln werden gespeichert:

- Mitarbeiter-ID
- Verantwortlicher Benutzer
- Check-in-Zeitpunkt
- Geschäftstag
- Tätigkeit
- Stundenlohn-Snapshot

Ein Mitarbeiter darf nicht mehrfach gleichzeitig eingestempelt werden.

Im Dashboard wird er anschließend unter der entsprechenden Kategorie angezeigt.

---

## 15. Geschäftstag

Das Restaurant arbeitet normalerweise von 10:00 Uhr bis ungefähr 03:00 Uhr des nächsten Tages.

Der Geschäftstag wechselt deshalb um 04:00 Uhr.

Beispiele:

29.10.2026 um 20:00 Uhr → Geschäftstag 29.10.2026

30.10.2026 um 02:30 Uhr → Geschäftstag 29.10.2026

30.10.2026 um 04:30 Uhr → Geschäftstag 30.10.2026

Die Berechnung erfolgt beim Check-in.

**Der Geschäftstag einer begonnenen Schicht wird beim Check-out nicht erneut berechnet.**

Wenn ein Mitarbeiter nach 04:00 Uhr weiterarbeitet, behält seine offene Zeiterfassung den ursprünglichen Geschäftstag.

Verwende für Zeitberechnungen vollständige Datums- und Zeitwerte und berücksichtige die lokale Zeitzone des Restaurants.

---

## 16. Fahrzeuge und Handys

Jeder Fahrer erhält während seiner Tätigkeit grundsätzlich:

- Ein Fahrzeug
- Ein Handy

Autofahrer erhalten ein Auto und ein Handy.

Fahrradfahrer erhalten ein Fahrrad und ein Handy.

Ein aktuell zugewiesenes Fahrzeug oder Handy darf nicht gleichzeitig einem anderen Mitarbeiter zugeteilt werden.

Für Fahrzeuge und Handys gibt es Zustände, beispielsweise:

- Verfügbar
- In Benutzung
- Defekt
- Gesperrt

Die tatsächliche Belegung soll über die offenen Datensätze der Tabelle `ressourcen_zuweisung` ermittelt werden.

Statusangaben dürfen nicht im Widerspruch zur tatsächlichen Datenbankbelegung stehen.

---

## 17. Ressourcenwechsel

Ein Mitarbeiter darf während seiner Schicht das Fahrzeug oder Handy wechseln.

Ein Head muss den Wechsel prüfen und bestätigen.

Beispiel:

Mitarbeiter SJ arbeitet 11:00–18:00 Uhr.

Zeiterfassung:

- Ein einziger Zeiterfassungseintrag von 11:00–18:00 Uhr

Ressourcen:

- Auto #345 von 11:00–13:00 Uhr
- Auto #789 von 13:00–18:00 Uhr
- Handy #12 von 11:00–15:00 Uhr
- Handy #18 von 15:00–18:00 Uhr

**Ein Ressourcenwechsel erzeugt keinen neuen Zeiterfassungseintrag.**

Dafür dient ausschließlich `ressourcen_zuweisung`.

Ein Zuweisungseintrag enthält jeweils entweder ein Fahrzeug oder ein Handy.

Beim Wechsel:

1. Alte Ressource überprüfen.
2. Zustand dokumentieren.
3. Neue Ressource auf Verfügbarkeit und Zustand prüfen.
4. Alte Zuweisung beenden.
5. Neue Zuweisung erstellen.
6. Alle zusammengehörigen Änderungen atomar in MySQL speichern.
7. Anschließend Cache und Oberfläche aktualisieren.

Beide Zuweisungen verwenden dieselbe `zeiterfassung_id`.

---

## 18. Küchenmodus

Küchenmitarbeiter arbeiten häufig länger als der Head.

Deshalb soll später ein spezieller Küchenmodus eingeführt werden.

Während des Küchenmodus:

- Sind Verwaltungsfunktionen deaktiviert.
- Können Küchenmitarbeiter ihre eigene Arbeitszeit beenden.
- Ist für diese Selbstabmeldung kein Passwort erforderlich.
- Wird die eigene Mitarbeiter-ID in `abgemeldet_von_mitarbeiter_id` eingetragen.

Der Küchenmodus darf nicht dazu führen, dass beliebige Verwaltungsfunktionen zugänglich werden.

Berücksichtige einfache Maßnahmen gegen versehentliche Fremdabmeldungen, ohne eine Passwortpflicht einzuführen.

---

## 19. Arbeitszeitkonto

Für jeden Mitarbeiter werden Arbeitszeiten für einen vom Benutzer ausgewählten Zeitraum berechnet.

### Head

Darf nach Bestätigung des Passworts des betreffenden Mitarbeiters ausschließlich die Arbeitsstunden sehen.

### Manager und Admin

Dürfen zusätzlich den berechneten Lohn einsehen.

Für historische Lohnberechnungen wird der Wert `stundensatz_snapshot` aus der jeweiligen Zeiterfassung verwendet.

Die genaue Funktionsweise von Sollstunden- und Überstundenkonten wird später festgelegt.

---

## 20. Wiederherstellung nach Abstürzen

Alle betrieblichen Änderungen müssen sofort dauerhaft in MySQL gespeichert werden.

Nach einem Absturz sollen später folgende Informationen wiederhergestellt werden:

- Stammdaten
- Offene Zeiterfassungen
- Aktive Ressourcenzuweisungen
- HauptHead-Zustand
- Relevante Ressourcenstatus

Offene Zeiterfassungen sind Datensätze, deren `check_out` NULL ist.

Aktive Ressourcenzuweisungen sind Datensätze, deren `zugewiesen_bis` NULL ist.

Ein Programmabsturz oder normales Beenden der Software darf bestehende Arbeitsschichten nicht automatisch beenden.

---

# TEIL 7 – ERSTE ENTWICKLUNGSVERSION 0.1

## 21. Ziel der ersten Version

Implementiere jetzt nur eine erste funktionsfähige Grundlage.

**Version 0.1 besteht aus:**

1. Maven-Projekt konfigurieren
2. JavaFX-Anwendung starten
3. Datenbankverbindung zu XAMPP
4. Loginfenster
5. Authentifizierung
6. Stammdaten laden
7. Einfaches Dashboard
8. Abmelden und Rückkehr zum Loginfenster

**Noch nicht implementieren:**

- Vollständige Mitarbeiterverwaltung
- Mitarbeiter ein- und ausstempeln
- Automatisches Head-Einstempeln
- HauptHead-Wechsel
- Ressourcenverwaltung
- Ressourcenwechsel
- Küchenmodus
- Arbeitszeitkonto
- Änderungsprotokoll
- Lohnberechnung
- Automatische Updates
- Windows-Installer

Diese Funktionen gehören zu späteren Entwicklungsschritten.

Die Architektur soll diese Erweiterungen ermöglichen, aber nicht schon alle notwendigen Klassen dafür enthalten.

---

## 22. JavaFX-Loginfenster

Erstelle ein professionelles Loginfenster.

### Bestandteile

- Mein offizielles JasanEX-Pro-Logo
- Titel „JasanEX Pro“
- Eingabefeld für Mitarbeiterkürzel
- Passwortfeld
- Anmelden-Button
- Bereich für Fehlermeldungen
- Ladeanzeige während der Authentifizierung

### Gestaltung

- Dunkles Farbschema
- Moderne, aufgeräumte Benutzeroberfläche
- Gut lesbare Schrift
- Übersichtliche Abstände
- Einheitliche Buttons
- Abgerundete Elemente
- Dezente Akzentfarben, passend zum bereitgestellten Logo
- Keine unnötigen Animationen
- Keine ungeprüften externen Designbibliotheken

Die Benutzeroberfläche soll mit JavaFX, FXML und CSS aufgebaut werden.

Die FXML-Ansichten dürfen keinen SQL-Code enthalten.

### Verhalten

Beim Programmstart wird das Loginfenster angezeigt.

Stammdaten werden noch nicht vollständig geladen.

Beim Klick auf „Anmelden“ beginnt die Authentifizierung.

---

## 23. Authentifizierung

Ablauf:

1. Kürzel und Passwort aus den Eingabefeldern lesen.
2. Leere Eingaben prüfen.
3. Verbindung zur MySQL-Datenbank herstellen.
4. Mitarbeiter anhand seines Kürzels suchen.
5. Mitarbeiterstatus prüfen.
6. Gespeicherten Passwort-Hash überprüfen.
7. Berechtigungsrolle bestimmen.
8. Nur Head, Manager oder Admin zulassen.
9. Benutzersitzung im Arbeitsspeicher erstellen.
10. Benötigte Stammdaten laden.
11. Vorhandene offene Zeiterfassungen einlesen.
12. Dashboard öffnen.

Falsche Zugangsdaten dürfen nicht zu einer Anmeldung führen.

Die Fehlermeldung soll verständlich sein, aber keine unnötigen Informationen preisgeben.

Verwende PreparedStatements.

**Wichtig:** Der Datenbankeintrag `mitarbeiter.kuerzel` ist nur innerhalb eines Restaurants eindeutig. Für Version 0.1 darf eine eindeutige Restaurantzuordnung vorausgesetzt werden, wenn tatsächlich nur ein Restaurant konfiguriert ist. Bei mehreren Restaurants muss die Anwendung die Restaurantzuordnung eindeutig bestimmen; sie darf nicht willkürlich einen gleichnamigen Benutzer auswählen.

Die tatsächlichen Status- und Rollenwerte müssen mit den Daten in MySQL übereinstimmen.

Erfinde keine abweichenden Statuswerte.

Eine gesicherte Passwortprüfung ist verpflichtend. Verwende keine Klartextvergleiche und keine unsicheren Hash-Verfahren.

---

## 24. Testbenutzer

Falls die Datenbank noch keinen gültigen Benutzer enthält, stelle eine sichere Möglichkeit bereit, einen Testbenutzer anzulegen.

Dabei gilt:

- Passwort muss mit dem verwendeten Hash-Verfahren verarbeitet werden.
- Keine festen Zugangsdaten in Git.
- Keine automatischen Änderungen an vorhandenen Mitarbeitern.
- Testdaten nur nach ausdrücklicher Ausführung durch mich einfügen.
- Ein separates kleines Initialisierungswerkzeug oder dokumentierte Testanleitung ist ausreichend.

Erfinde keinen angeblich gültigen Passwort-Hash.

---

## 25. Dashboard

Nach erfolgreicher Authentifizierung öffnet sich ein JavaFX-Hauptfenster.

Das Dashboard bleibt zunächst bewusst einfach.

Es enthält:

- JasanEX-Pro-Logo
- Softwaretitel
- Angemeldetes Mitarbeiterkürzel
- Benutzerrolle
- Hauptbereich als Platzhalter für spätere Funktionen
- Abmelden-Button

Du darfst eine einfache Navigationsstruktur vorbereiten, beispielsweise eine Seitenleiste. Noch nicht implementierte Funktionen dürfen aber nicht als funktionsfähige Bedienelemente erscheinen.

### Abmelden

Beim Klick auf „Abmelden“:

1. Benutzersitzung beenden.
2. Sensible benutzerbezogene Cache-Daten entfernen.
3. Dashboard schließen oder zur Loginansicht wechseln.
4. Loginfenster wieder anzeigen.

Die Verbindung zur Datenbank darf nicht unnötig offen bleiben.

---

## 26. Stammdaten im Arbeitsspeicher

Nach erfolgreichem Login sollen die benötigten Daten aus der Datenbank geladen werden.

Für Version 0.1 ist ein einfacher Mitarbeiter-Cache ausreichend.

Beispiel:

`Map<Integer, Mitarbeiter>`

Wenn die Struktur bereits sinnvoll vorbereitet werden kann, dürfen auch Restaurants, Fahrzeuge und Handys geladen werden.

Vermeide aber unnötigen Code nur für spätere Funktionen.

Der Cache muss klar von der Datenbankzugriffsschicht getrennt bleiben.

Fehlschläge beim Laden müssen sauber behandelt werden.

---

# TEIL 8 – ECLIPSE-PROJEKTSTRUKTUR

## 27. Packages

Analysiere zuerst die vorhandene Maven-Konfiguration.

Verwende nach Möglichkeit eine Struktur ähnlich dieser:

```text
de.jasanex.pro
│
├── app
│   └── Main.java
│
├── config
│   └── DatabaseConfig.java
│
├── controller
│   ├── LoginController.java
│   └── DashboardController.java
│
├── model
│   └── Mitarbeiter.java
│
├── repository
│   └── MitarbeiterRepository.java
│
├── service
│   └── AuthService.java
│
├── cache
│   └── StammdatenCache.java
│
└── security
    └── SessionManager.java
```

Diese Struktur ist ein Vorschlag und darf sinnvoll angepasst werden.

Verwende gültige Java-Package-Namen, vorzugsweise vollständig kleingeschrieben.

Bestehende Maven-Koordinaten dürfen nicht ohne technischen Grund geändert werden.

Verwende keine unnötig umfangreiche Umbenennung des vorhandenen Projekts.

### Ressourcen

```text
src/main/resources
└── de/jasanex/pro
    ├── view
    │   ├── login.fxml
    │   └── dashboard.fxml
    │
    ├── css
    │   └── style.css
    │
    └── images
        └── Original-Logo
```

Binde Ressourcen über den Classpath ein.

Verwende keine absoluten Windows-Dateipfade.

---

# TEIL 9 – QUALITÄT UND SICHERHEIT

## 28. Entwicklungsregeln

Beachte:

- Objektorientierte Programmierung
- Saubere Verantwortlichkeiten
- Verständliche Methodennamen
- Kapselung
- Wiederverwendbare Komponenten
- Aussagekräftige Fehlermeldungen
- Keine unnötigen globalen Zustände
- Keine blockierenden Datenbankzugriffe im JavaFX Application Thread

Die Anwendung darf beim Verbindungsaufbau oder beim Laden von Daten nicht einfrieren.

Verwende dafür geeignete Hintergrundaufgaben und aktualisiere JavaFX-Komponenten ausschließlich auf dem dafür vorgesehenen Thread.

## 29. Datenschutz und Sicherheit

- Keine Klartextpasswörter speichern.
- Keine Datenbankpasswörter im Repository.
- `.gitignore` passend konfigurieren.
- Keine privaten Mitarbeiterdaten lokal dauerhaft speichern.
- JDBC-Verbindungen sicher verwalten.
- Keine SQL-Injection ermöglichen.
- Keine unnötigen personenbezogenen Daten ausgeben.
- Keine vollständigen Datenbank-Zugangsdaten in Fehlermeldungen.
- Kein automatischer Datenbank-Reset.
- Keine unkontrollierten Änderungen am Datenbankschema.

Die direkte JDBC-Verbindung ist zunächst nur für meinen lokalen Entwicklungsrechner vorgesehen.

Wenn JasanEX Pro später auf weiteren PCs installiert oder als Unternehmenssoftware verteilt wird, soll die Sicherheitsarchitektur neu bewertet und gegebenenfalls durch eine zentrale API ersetzt werden.

---

# TEIL 10 – TESTEN UND DOKUMENTIEREN

## 30. Tests

Führe, soweit technisch möglich, folgende Prüfungen durch:

### Technische Tests

- Maven-Build erfolgreich?
- Java-Code kompiliert?
- JavaFX-FXML-Dateien korrekt?
- CSS-Dateien auffindbar?
- Logodatei als Ressource eingebunden?
- Loginfenster startet?
- Dashboard kann geöffnet werden?
- Abmelden führt zurück zum Login?

### Datenbanktests

Wenn Zugriff auf die lokale XAMPP-Datenbank tatsächlich möglich ist:

- Verbindung erfolgreich?
- Mitarbeiterabfrage funktioniert?
- Passwortprüfung funktioniert?
- Ungültige Logins werden abgewiesen?
- Inaktive Mitarbeiter werden abgewiesen?
- Fehler werden verständlich angezeigt?
- Cache wird nach erfolgreichem Login geladen?

Wenn deine Ausführungsumgebung keinen Zugriff auf meine lokale XAMPP-Datenbank besitzt, darfst du keinen erfolgreichen Datenbanktest behaupten.

Dokumentiere stattdessen, wie ich den Test in Eclipse durchführen kann.

## 31. Dokumentation

Erstelle eine `README.md`.

Sie soll enthalten:

- Projektname
- Projektbeschreibung
- Verwendete Technologien
- Maven-Einrichtung
- Projektstruktur
- Einrichtung in Eclipse
- Starten von XAMPP
- Konfiguration der Datenbankverbindung
- Erstellung eines Testbenutzers
- Starten der JavaFX-Anwendung
- Häufige Fehler und Lösungen
- Aktueller Funktionsumfang
- Geplante nächste Schritte

Erstelle außerdem:

`docs/ARCHITECTURE.md`

Dort sollst du das langfristige Softwarekonzept und die Geschäftsregeln aus diesem Master-Prompt dokumentieren.

Trenne klar zwischen:

- Bereits implementierten Funktionen
- Geplanten Funktionen
- Noch offenen Entscheidungen

So können wir bei späteren Codex-Sitzungen direkt auf die Dokumentation zurückgreifen.

---

# TEIL 11 – ERWARTETES ERGEBNIS

## 32. Fertigstellung

Am Ende soll eine erste funktionsfähige Version von **JasanEX Pro** vorhanden sein.

Die Anwendung soll:

1. Als Maven-Projekt in Eclipse geöffnet werden können.
2. Mit JavaFX starten.
3. Das offizielle Logo anzeigen.
4. Ein modernes Loginfenster öffnen.
5. Einen Benutzer über die lokale Datenbank authentifizieren.
6. Nach erfolgreichem Login die benötigten Daten in den Arbeitsspeicher laden.
7. Ein leeres, professionelles Dashboard anzeigen.
8. Das Kürzel und die Rolle des angemeldeten Benutzers anzeigen.
9. Eine Abmeldung zurück zum Login ermöglichen.
10. Eine saubere technische Grundlage für zukünftige Funktionen besitzen.

Erstelle keine unnötigen Funktionen außerhalb des vereinbarten Umfangs.

## 33. Abschließende Erklärung

Nach der Implementierung erkläre mir bitte auf Deutsch:

1. Welche Dateien hast du erstellt?
2. Welche Dateien hast du geändert?
3. Welche Packages gibt es?
4. Welche Aufgaben erfüllen die Klassen?
5. Wie funktioniert die Anmeldung?
6. Wie funktioniert die Datenbankverbindung?
7. Wo werden die Stammdaten gespeichert?
8. Wie starte ich die Software in Eclipse?
9. Was wurde tatsächlich erfolgreich getestet?
10. Welche Funktionen sind noch nicht implementiert?
11. Was empfiehlst du für Version 0.2?

Erkläre mir die wichtigsten Java-Konzepte verständlich, damit ich als Entwickler dazulerne.

**Beginne jetzt mit der Untersuchung meines vorhandenen Maven-Projekts. Implementiere anschließend die erste Version von JasanEX Pro entsprechend diesen Anforderungen.**
