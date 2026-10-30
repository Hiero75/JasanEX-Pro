# JasanEX Pro

Windows-Desktopsoftware für Restaurantmanagement. Version **0.2.0-SNAPSHOT** bietet Login, Mitarbeiter-/Fahrzeug-/Handyverwaltung, Ein- und Ausstempeln, Ressourcenzuweisung mit Rückgabeprüfung und Kategorienkarten für offene Schichten. Das Dashboard orientiert sich am bereitgestellten HTML-Entwurf: dunkelblaue Kopfzeile, helle Arbeitsfläche, kompakte Karten, goldene Rollenmarkierung und ein rechtes Navigationsmenü. Das Login ist dunkel gestaltet. Beide Original-Logos liegen unverändert im Classpath.

## Voraussetzungen und Start in Eclipse

1. XAMPP öffnen und den Datenbankdienst „MySQL“ starten. Apache ist für JDBC nicht erforderlich.
2. Eclipse: **File → Import → Maven → Existing Maven Projects**; den Unterordner `JasanEX-Pro` mit der `pom.xml` auswählen.
3. **Preferences → Java → Installed JREs**: JDK 26 auswählen. Das bestehende Eclipse-Projekt war auf Java 8 eingestellt, installiert ist JDK 26.0.1; Maven und Eclipse sind jetzt auf Java 26 abgestimmt.
4. Rechtsklick auf das Projekt → **Maven → Update Project**.
5. `config/database.properties.example` nach `config/database.properties` kopieren und lokal ausfüllen. Diese Datei wird nicht versioniert.
6. Nach **Refresh (F5)** die Datei `JasanEX Pro starten.launch` rechts anklicken → **Run As → JasanEX Pro starten** (Maven Build). Alternativ **Run As → Maven build…**, Goals `compile javafx:run`. Arbeitsverzeichnis ist der Ordner mit der `pom.xml`. Nicht den bisherigen Java-Application-Start von `App.java` verwenden.

### Rote Konsolenwarnungen aus Eclipse

Rot kennzeichnet die Fehlerausgabe (stderr), nicht automatisch einen Programmabsturz. `Unsupported JavaFX configuration: classes were loaded from unnamed module` bedeutet, dass JavaFX über den Classpath statt den Modulepath geladen wurde. `System::load` / `--enable-native-access` betrifft das Laden der nativen Grafikbibliotheken. Der Maven-Start lädt JavaFX auf dem Modulepath und erteilt mit `--enable-native-access=javafx.graphics` die passende Freigabe; beide JavaFX-Warnungen sind bei diesem Startweg geprüft verschwunden. Siehe [OpenJFX-Startanleitung](https://openjfx.io/openjfx-docs/) und [native Zugriffsfreigabe](https://openjfx.io/highlights/24/).

Nur `--enable-native-access=ALL-UNNAMED` bei **Run Configurations → Java Application → Arguments → VM arguments** einzutragen beseitigt beim alten Classpath-Start die zweite Warnung, nicht die erste. Der bereitgestellte Maven-Start ist deshalb vorzuziehen. Hinweise zu `sun.misc.Unsafe` oder reflektiven Feldzugriffen können weiterhin aus Maven-internen Bibliotheken unter JDK 26 kommen; sie sind keine SQL-/Anmeldefehler. Keine Warnungen pauschal abschalten.

Alternativ mit installiertem Maven im Projektordner:

```powershell
mvn clean test
mvn javafx:run
```

In dieser Arbeitskopie liegt für die Prüfung zusätzlich Maven in `.tools/apache-maven-3.9.11`. Ohne globales Maven kann `mvn` durch `& ./.tools/apache-maven-3.9.11/bin/mvn.cmd` ersetzt werden. Dieser Werkzeugordner ist nicht Teil von Git. Ein normales JAR ist noch kein eigenständiger Windows-Installer; Start erfolgt über Maven mit seinen JavaFX-Abhängigkeiten.

## Datenbankkonfiguration

Standardziel: `127.0.0.1:3306/jasanex-pro-test`. Zugangsdaten stehen ausschließlich in der ignorierten lokalen Konfiguration oder in Umgebungsvariablen. `JASANEX_CONFIG` kann auf eine alternative UTF-8-Konfigurationsdatei zeigen.

| Property | Umgebungsvariable |
|---|---|
| `db.host`, `db.port`, `db.name` | `JASANEX_DB_HOST`, `JASANEX_DB_PORT`, `JASANEX_DB_NAME` |
| `db.user`, `db.password` | `JASANEX_DB_USER`, `JASANEX_DB_PASSWORD` |
| `restaurant.id` | `JASANEX_RESTAURANT_ID` |
| `auth.activeStatus` | `JASANEX_ACTIVE_STATUS` |
| `auth.role.admin`, `.manager`, `.head` | `JASANEX_ROLE_ADMIN`, `_MANAGER`, `_HEAD` |

Umgebungsvariablen haben Vorrang. Bei genau einem Restaurant wird dessen ID ermittelt; bei mehreren muss `restaurant.id` gesetzt sein. Doppelte Kürzel innerhalb eines Restaurants werden abgewiesen.

**Bestätigter lokaler Befund:** MariaDB 10.11.16; Aktivstatus `AKTIV` und Ressourcenstatus `VERFUEGBAR` sind im realen Tabellenschema als Defaults bestätigt. `INAKTIV` ist vorhanden. Ein Admin-Testkonto wurde auf ausdrücklichen Auftrag eingerichtet und erfolgreich authentifiziert. Seine Zugangsdaten werden nicht in Git dokumentiert. Die lokale Konfiguration ist bereits eingerichtet und bleibt ignoriert. Bei einem neuen Checkout eigene Konfiguration erstellen. Rollenwerte für berechtigte Konten bleiben konfigurierbar (`Admin`, `Manager`, `Head`). Die Erweiterung benötigt keine Schemaänderung.

## Bedienung der neuen Funktionen

1. Als Admin oder Manager anmelden und oben **Stammdaten** öffnen. Tabs: **Mitarbeiter**, **Fahrzeuge**, **Handys**. `Neu anlegen`, `Bearbeiten`, `Löschen` betreffen den gewählten Tab/Datensatz.
2. Mitarbeiter benötigen ein Kürzel mit maximal zwei Zeichen, Rolle, Status, Stundensatz und beim Anlegen ein mindestens zwölfstelliges Passwort. Beim Bearbeiten bedeutet ein leeres Passwortfeld „bisherigen Hash behalten“. So können alte Testkonten ausdrücklich auf ein unterstütztes Passwort umgestellt werden.
3. HEAD hat keinen Zugriff auf Stammdatenverwaltung oder Stundensätze. MANAGER kann keine Admin-/Managerkonten ändern oder solche Rollen vergeben. Eigenes Konto und letzter aktiver Admin sind vor Löschung bzw. Selbst-Deaktivierung geschützt.
4. **+ Mitarbeiter einstempeln**: aktiven Mitarbeiter wählen, dessen Passwort eingeben und Tätigkeit bestimmen. Tätigkeiten: Head, Küche, Autofahrer, Fahrradfahrer, Sonstiges. Sonstiges verlangt eine Begründung; sie wird im bestehenden Änderungsprotokoll gespeichert.
5. Autofahrer benötigen ein verfügbares Auto und Handy, Fahrradfahrer ein Fahrrad und Handy. Die Zuweisungen werden mit der Zeiterfassung atomar gespeichert. Die Verfügbarkeit ergibt sich aus Status und offenen Zuweisungen, einschließlich älterer optionaler Ressourcenfelder.
6. Offene Schichten erscheinen unter ihrer **Schichttätigkeit**, unabhängig von der dauerhaften Mitarbeiterrolle. Auf der Karte **Ausstempeln** wählen. Bei Ressourcen den Zustand bestätigen; Mängel beschreiben. Defekte Ressourcen sind anschließend nicht erneut zuweisbar.
7. **Abmelden** oben beendet nur die Programmsitzung. Offene Arbeitsschichten bleiben erhalten und werden beim nächsten Login geladen. **Aktualisieren** lädt zwischenzeitliche externe Änderungen; nach jeder eigenen Aktion wird automatisch neu geladen.

Verknüpfte Historie verhindert endgültiges Löschen. Mitarbeiter stattdessen nach dem Ausstempeln deaktivieren; Fahrzeuge/Handys sperren oder als defekt markieren. Zugewiesene Ressourcen können nicht während ihrer Nutzung bearbeitet werden. Es gibt keinen automatischen kaskadierenden Historienverlust.

## Testbenutzer ausdrücklich selbst anlegen

Die Passwortprüfung verwendet PBKDF2-HMAC-SHA256 mit 600.000 Iterationen, 16 zufälligen Salt-Bytes und 256-Bit-Ausgabe. Format: `pbkdf2-sha256$Iterationen$Base64Salt$Base64Hash`. Unbekannte, beschädigte oder Klartextwerte werden abgelehnt; bestehende Hash-Verfahren werden nicht automatisch migriert.

Nach Konfiguration des bestätigten Aktivstatus und der Zugangsdaten in einem **externen PowerShell-Terminal** im Projektordner:

```powershell
mvn compile dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
$jasanexClasspath = 'target/classes;' + (Get-Content target/classpath.txt -Raw).Trim()
java -cp $jasanexClasspath de.jasanex.pro.security.TestUserTool
```

Das Werkzeug fragt Kürzel (maximal zwei Zeichen laut Schema), Rolle, Test-Stundensatz und zweimal verdeckt das Passwort ab. Mindestlänge: zwölf Zeichen. Erst nach Eingabe von `ANLEGEN` wird genau ein neuer Mitarbeiter eingefügt. Vorhandene Kürzel werden nicht überschrieben. Keine Testpasswörter als Kommandozeilenargumente oder in Git speichern. In Eclipse fehlt bei gewöhnlichen Java-Starts `System.console()`; deshalb das externe Terminal verwenden. Das Werkzeug wurde hier **nicht zur Anlage eines Benutzers ausgeführt**.

## Aufbau und Java-Konzepte

| Package / Klassen | Aufgabe |
|---|---|
| `app.Main` | JavaFX-Lebenszyklus, Wechsel der Ansichten, gemeinsame Session und Cache |
| `controller.LoginController`, `DashboardController` | Eingaben und UI; JavaFX-Task für Datenbankarbeit; Uhr und Abmelden |
| `service.AuthService` | Eingabeprüfung, Passwort-/Status-/Rollenprüfung, anschließendes Laden |
| `repository.MitarbeiterRepository` | PreparedStatements, Restaurantwahl, Mitarbeiter und offene Schichten |
| `model.Mitarbeiter`, `OffeneSchicht`, `Role` | Unveränderliche Records für Daten; Enum für erlaubte Anwendungsrollen |
| `config.DatabaseConfig` | Lokale Konfiguration und kurzlebige JDBC-Verbindungen |
| `security.PasswordHasher`, `SessionManager`, `TestUserTool` | Passwortschutz, Sitzung, ausdrücklich gestartete Testanlage |
| `cache.StammdatenCache` | Unveränderliche Map und Liste nur im RAM, beim Abmelden geleert |
| `controller.StammdatenController` | Tabellen, Eingabedialoge und Löschbestätigung |
| `service.BetriebService`, `Betriebsregeln` | Rechte, Transaktionen, Ein-/Ausstempeln, Ressourcenprüfung und Geschäftstag |
| `repository.BetriebRepository` | SQL für Stammdaten, Zeiterfassung, Ressourcenhistorie und Aktionsprotokoll |
| `model.Betriebsdaten` | Unveränderliche Eingaben und Ansichtsdatensätze, keine Passworthashes in Snapshots |

FXML beschreibt die Oberfläche, CSS ihre Gestaltung. Der Controller kennt keine SQL-Abfragen. Ein Service bündelt einen fachlichen Ablauf. Das Repository kapselt die Datenbank. `try-with-resources` schließt Verbindungen, Statements und ResultSets auch bei Fehlern. Ein `record` ist ein kompakter unveränderlicher Datenträger. Eine `Map<Integer, Mitarbeiter>` erlaubt den direkten Zugriff über die Mitarbeiter-ID.

Die Anmeldung läuft in einem Hintergrundthread: Restaurant eindeutig bestimmen → Mitarbeiter über PreparedStatement suchen → Passwort, Status und Rolle prüfen → Mitarbeiter ohne Passwort-/Lohndaten und offene Schichten laden → Lesetransaktion abschließen → Session und Cache auf dem JavaFX-Thread übernehmen → Dashboard anzeigen. Schlägt ein Schritt fehl, wird kein erfolgreicher Login veröffentlicht. HEAD erhält keine Lohnwerte. Abmelden leert Session und Cache; Arbeitsschichten in der Datenbank bleiben unverändert.

Ressourcen liegen unter `src/main/resources/de/jasanex/pro/{view,css,images}` und werden über den Classpath geladen. `de.JansanEX.JasanEX_Pro.App` bleibt als kompatibler Einstieg und delegiert an `Main`. Die bestehenden Maven-Koordinaten sind beibehalten.

## Tests

- `mvn test`: Passwortprüfung, Salt-Zufälligkeit, ungültige Hashes, Konfiguration, erfolgreiche/abgewiesene Anmeldung, Ladefehler, Session-/Cache-Löschung. Service-Tests verwenden eine simulierte JDBC-Verbindung und Repository-Testdaten, keine echten Mitarbeiteränderungen.
- `mvn test "-Djasanex.uiTest=true"`: zusätzlicher JavaFX-Test in einer Desktopsitzung; beide FXML-Ansichten laden, Dashboard mit ausschließlich flüchtigen Testdaten anzeigen, Abmelden auslösen. Bilder werden unter `target/ui-check` erzeugt.
- `JASANEX_DB_TEST=true` plus lokale DB-Konfiguration: zusätzliche ausschließlich lesende Prüfung gegen XAMPP. Prüft echte Abfragen und abgewiesene Logins. Die Datenbank wird nicht befüllt.
- `JASANEX_WRITE_TEST=true`: vier Integrationstests für CRUD, Rechte, Schichten, Rückgabe, Historien- und Doppelbelegungsschutz. Alle Testdatensätze laufen in einer äußeren Transaktion; Commit wird nur im Testadapter abgefangen, am Ende wird zurückgerollt. AUTO_INCREMENT-Zähler können dabei Lücken erhalten. Kein Schema wird geändert. Zusätzlich sind Geschäftstagsgrenze und Sommer-/Winterzeitwechsel getestet.

Die aktuellen Ergebnisse stehen in `docs/VERSION-0.2.md`, der historische Erstbericht in `docs/VALIDATION.md`. Es gibt keinen Demo-Login in der Anwendung.

## Häufige Fehler

- **Aktivstatus nicht konfiguriert:** `auth.activeStatus` nach fachlicher Bestätigung setzen. Nicht `INAKTIV` als aktiven Status verwenden.
- **Verbindung fehlgeschlagen:** XAMPP-Datenbankdienst, Port, Datenbankname und lokale Berechtigungen prüfen.
- **Anmeldung abgelehnt:** Kürzel, Restaurant, Status, Rolle und Hashformat prüfen. Benutzeroberfläche verrät nicht, welcher Teil falsch war.
- **JavaFX-Komponenten fehlen:** über `javafx:run` starten und Maven-Projekt aktualisieren.
- **Invalid target release 26:** Maven verwendet ein älteres JDK; JDK 26 in Eclipse/`JAVA_HOME` wählen.
- **Konfiguration wird nicht gefunden:** Arbeitsverzeichnis auf Maven-Projektordner setzen oder `JASANEX_CONFIG` verwenden.

## Noch nicht enthalten / Nächste Schritte

Noch kein automatisches HEAD-Einstempeln beim Programmlogin, HauptHead-Wechsel, Küchenmodus, Ressourcenwechsel während laufender Schichten, Zeitkorrekturen, Lohnberechnung, vollständige Audit-Ansicht, Updates oder Installer. Neue Aktionen werden bereits ohne Passwörter/PINs im vorhandenen Änderungsprotokoll vermerkt; ein vollständiger Alt-/Neuwertvergleich ist noch nicht implementiert.

Als Nächstes HauptHead-Persistenz und Ressourcenwechsel planen. Die vorhandenen DATETIME-Felder haben keine Zeitzone; der Geschäftstag wird korrekt nach Berliner Ortszeit bestimmt, eine eindeutige Dauerauswertung über die doppelte Stunde beim Winterzeitwechsel erfordert eine spätere Entscheidung zur Zeitpunktspeicherung. Langfristige Regeln und offene Schemaentscheidungen stehen in `docs/ARCHITECTURE.md`.

Technische Referenzen: [OpenJFX mit Maven](https://openjfx.io/openjfx-docs/), [JavaFX Maven Plugin](https://github.com/openjfx/javafx-maven-plugin), [MariaDB Connector/J](https://github.com/mariadb-corporation/mariadb-connector-j).
