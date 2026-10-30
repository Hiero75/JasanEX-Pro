# Prüfbericht – 30.10.2026

## Erfolgreich ausgeführt

JDK 26.0.1, lokal bereitgestelltes Apache Maven 3.9.11, JavaFX 26.0.1, MariaDB Connector/J 3.5.11. Maven `clean package -Djasanex.uiTest=true` mit ausdrücklich aktivierter lesender Datenbankprüfung: **BUILD SUCCESS**, 10 Tests, 0 Fehler, 0 übersprungen.

| Prüfung | Ergebnis / Grenze |
|---|---|
| Java-Kompilierung und Maven-JAR | Erfolgreich, 14 Java-Quelldateien |
| Passwortprüfung | Korrektes Passwort akzeptiert; falsche, beschädigte und unbekannte Hashes abgewiesen; zufällige Salts geprüft |
| Service-Anmeldung | Mit simuliertem Repository erfolgreich; inaktive/unberechtigte/fehlende Benutzer und falsche Passwörter abgewiesen |
| Fehler beim Stammdatenladen | Kein erfolgreicher Login; Rollback und Verbindungsschließung geprüft |
| Session/Cache | Nach erfolgreichem Testlogin gefüllt, beim Abmelden geleert |
| FXML/CSS/Logo | Beide Ansichten in echter JavaFX-Laufzeit geöffnet, CSS angewandt, Screenshots visuell geprüft |
| Abmelden | Dashboard-Button führt zurück zu leerem Loginformular |
| XAMPP/JDBC | Verbindung zu MariaDB 10.11.16 erfolgreich; Restaurant-, Mitarbeiter- und offene-Schichten-Abfragen ausgeführt |
| Echter Negativlogin | Unbekanntes Konto und vorhandenes inaktives Konto mit ungültigem Testpasswort abgewiesen |

Der UI-Test verwendet ausschließlich flüchtige Testdaten (`QA`, `Testrestaurant · UI-Prüfung`) und umgeht nur innerhalb des Testcodes den Authentifizierungsablauf für die Ansichtsprüfung. Es existiert kein Demo-Login in der ausgelieferten Anwendung. Bilder: `target/ui-check/login.png`, `target/ui-check/dashboard.png`.

## Nicht behauptete Ergebnisse

- Kein erfolgreicher Login gegen ein echtes aktives Datenbankkonto: die vorhandenen Konten sind inaktiv und haben kein nachgewiesenes unterstütztes Passwortformat.
- Kein neuer Testbenutzer angelegt. Das interaktive TestUserTool ist kompiliert; der schreibende Insert wurde nicht ausgeführt.
- Für die echte Negativprüfung wurde pro Testprozess ein absichtlich nicht vorhandener Aktivstatus verwendet. Damit wird kein fachlicher Aktivstatus festgelegt.
- Keine Tabellen-/Spaltenänderungen, keine Mitarbeiteränderungen, keine Git-Commits oder Pushes.
- Eclipse-Import ist dokumentiert und Projektdateien sind abgestimmt; die Eclipse-Oberfläche selbst wurde nicht bedient.

Java 26 meldet bei älteren Maven-internen Bibliotheken Hinweise zu Unsafe/reflektiven Feldzugriffen. Der UI-Test auf dem Test-Classpath meldet zusätzlich JavaFX-Hinweise zum unnamed module/native access. Diese Hinweise verhinderten Build und Tests nicht. Der reguläre JavaFX-Maven-Start enthält die Freigabe für native Bibliotheken.

## Änderungen gegenüber dem Maven-Grundgerüst

Geändert: `pom.xml` (Version, Java, JavaFX, JDBC, Test-/Buildplugins), `.classpath`, `.settings/org.eclipse.jdt.core.prefs`, `.gitignore`, bisherige `App.java` als delegierender Einstieg. Der inhaltslose Archetyp-Test `AppTest` wurde durch fünf fachliche Testklassen ersetzt.

Neu: Packages `app`, `config`, `controller`, `model`, `repository`, `service`, `cache`, `security` unter `de.jasanex.pro`; zwei FXML-Dateien, ein Stylesheet, zwei Original-Logos; lokale Konfigurationsvorlage; README, Architektur, dieser Prüfbericht und archivierter Master-Prompt. Vollständige Klassenaufgaben und Startanleitung stehen in der README.
