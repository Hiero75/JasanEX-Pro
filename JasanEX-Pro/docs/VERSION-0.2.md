# Version 0.2.0-SNAPSHOT – 30.10.2026

## Umfang

- Stammdatenleiste mit Mitarbeiter-, Fahrzeug- und Handytabellen; Anlegen, Bearbeiten, Löschen nach Bestätigung. Historische Referenzen verhindern Löschung. Keine Restaurantverwaltung in dieser Ausbaustufe.
- Admin-/Managerrechte für Stammdaten; Manager darf keine Admin-/Managerkonten ändern. Keine Lohnwerte für Head. Eigene Berechtigung und letzter aktiver Admin geschützt.
- Aktive Mitarbeiter mit ihrem Passwort einstempeln; Tätigkeit unabhängig von Berechtigungsrolle. Geschäftstag 04:00 Uhr Europe/Berlin, Stundensatz-Snapshot, Verantwortlicher und einmalige offene Schicht.
- Fahrer erhalten passendes verfügbares Fahrzeug und Handy. Ressourcenzuweisungen in separaten Historieneinträgen. Keine Doppelbelegung durch konkurrierende App-Schreibvorgänge.
- Ausstempeln mit Rückgabeprüfung; Mangelnotiz obligatorisch bei Defekt. Checkout, Zuweisungsende und Ressourcenstatus atomar. Geschäftstag wird nicht verändert.
- Kategorienkarten: Schichtleitung/Management, Küche, Autofahrer, Fahrradfahrer, Sonstiges. Aktualisierung nach jedem erfolgreichen Schreiben und manuell über Aktualisieren.
- Aktionen im bestehenden Änderungsprotokoll. Kein vollständiger Alt-/Neuwert-Audit und keine Audit-Ansicht.

## Startwarnungen

Neue Eclipse-Maven-Startdatei `JasanEX Pro starten.launch`. Ziele `compile javafx:run`; JavaFX läuft über den Modulepath. In der POM ist `--enable-native-access=javafx.graphics` gesetzt. Der reguläre `javafx:run`-Start wurde tatsächlich geöffnet: keine JavaFX-Meldung zu unnamed module oder fehlendem native access. Der Testprozess verwendet bewusst den Test-Classpath und kann diese Hinweise weiterhin ausgeben. Maven-interne Hinweise zu Unsafe/reflektiven Feldzugriffen unter JDK 26 bleiben möglich.

## Prüfungen

Maven `package -Djasanex.uiTest=true` mit aktivierter Lese- und zurückgerollter Schreibprüfung: **16 Tests, 0 Fehler, 0 übersprungen, BUILD SUCCESS**.

Die vier neuen SQL-Integrationstests prüfen CRUD, Rechte, Passwortweiterverwendung, Doppelstempelung, Historienlöschschutz, Geschäftstag beim Checkout, Lohndatensnapshot, verantwortliche Mitarbeiter-IDs, Ressourcenzuweisung, Doppelbelegung, Rückgabe und Defektstatus. Test-Commits werden durch einen Connection-Testadapter abgefangen; alle Datensätze werden am Ende zurückgerollt. AUTO_INCREMENT-Werte können durch Tests weiterlaufen. Zum Abschluss: weiterhin keine Zeiterfassungen oder Ressourcenzuweisungen in der lokalen Testdatenbank, keine dauerhaften Testfixtures.

Zwei neue reine Java-Tests prüfen Validierungen und Geschäftstagsgrenze einschließlich Sommer-/Winterzeitwechsel. Der JavaFX-Test lädt Login, Dashboard und Stammdaten, prüft fünf Kategorien und die Rückkehr zum Login; Ansichts-Testdaten bleiben im RAM. Dashboard und Stammdaten-Screenshots wurden visuell geprüft (`target/ui-check`). Der Eclipse-Launch wurde als Projektdatei erstellt; Eclipse selbst wurde nicht automatisiert bedient.

## Grenzen

Gleichzeitige App-Schreibvorgänge werden per Restaurantzeile serialisiert. Die Konkurrenzsituation wurde noch nicht als zeitgleicher Mehrprozess-Test ausgeführt; sequentielle Konfliktfälle werden getestet. Keine neue Tabelle/Spalte, keine produktiven Stammdatenänderungen durch Entwicklungstests. Kein Git-Commit/Push.

Manuelles Einstempeln gilt auch für Head. Automatisches Head-Einstempeln, HauptHead-Wechsel und Küchenmodus benötigen weitere Umsetzung. Bestehende DATETIME-Spalten speichern keine absolute Zeitzone; eine eindeutige spätere Stundenabrechnung über die Zeitumstellung bleibt eine offene Schemaentscheidung. Die Geschäftstagszuordnung selbst ist getestet.
