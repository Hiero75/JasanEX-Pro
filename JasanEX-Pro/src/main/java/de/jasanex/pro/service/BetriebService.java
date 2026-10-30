package de.jasanex.pro.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.Role;
import de.jasanex.pro.model.Betriebsdaten.*;
import de.jasanex.pro.repository.*;
import de.jasanex.pro.security.PasswordHasher;
import de.jasanex.pro.security.SessionManager.Session;

/** Autorisierung und atomare Betriebsaktionen. Keine UI- oder Cache-Mutation vor Commit. */
public final class BetriebService {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    @FunctionalInterface private interface Work<T> { T run(Connection c) throws SQLException; }
    private final DatabaseConfig config;
    private final Session session;
    private final Connections connections;
    private final Clock clock;
    private final BetriebRepository repository = new BetriebRepository();
    private final MitarbeiterRepository staff = new MitarbeiterRepository();

    public BetriebService(DatabaseConfig config, Session session) {
        this(config,session,config::connect,Clock.system(Betriebsregeln.ZEITZONE));
    }
    public BetriebService(DatabaseConfig config, Session session, Connections connections, Clock clock) {
        this.config=config; this.session=session; this.connections=connections; this.clock=clock;
    }
    public boolean verwaltung() { return session.role() == Role.ADMIN || session.role() == Role.MANAGER; }
    public List<String> rollen() {
        List<String> result = new ArrayList<>(List.of(config.roleValue(Role.HEAD), "Küche", "Fahrer Auto", "Fahrer Fahrrad", "Mitarbeiter"));
        if (session.role() == Role.ADMIN) { result.add(config.roleValue(Role.MANAGER)); result.add(config.roleValue(Role.ADMIN)); }
        return List.copyOf(result);
    }
    public String aktivStatus() { return config.activeStatus(); }
    private int restaurant() { return session.user().restaurantId(); }
    private int actor() { return session.user().id(); }

    public Snapshot load() throws SQLException {
        return transaction(false, c -> snapshot(c));
    }
    private Snapshot snapshot(Connection c) throws SQLException {
        return new Snapshot(staff.loadMitarbeiter(c,restaurant()),staff.loadOffeneSchichten(c,restaurant()),
                verwaltung() ? repository.personal(c,restaurant()) : List.of(),
                repository.fahrzeuge(c,restaurant()),repository.handys(c,restaurant()),repository.schichten(c,restaurant()));
    }
    private <T> T transaction(boolean write, Work<T> work) throws SQLException {
        try (Connection c = connections.open()) {
            c.setTransactionIsolation(write ? Connection.TRANSACTION_READ_COMMITTED : Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            try {
                if (write) repository.lockRestaurant(c,restaurant());
                var user = repository.konto(c,restaurant(),actor()).personal();
                if (!config.activeStatus().equals(user.status()) || config.role(user.rolle()) != session.role())
                    throw new IllegalArgumentException("Berechtigung geändert. Bitte erneut anmelden.");
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23"))
                    throw new IllegalArgumentException("Speichern/Löschen nicht möglich: Kürzel oder Nummer schon vergeben, oder der Datensatz besitzt verknüpfte Historie. Bei Mitarbeitern stattdessen INAKTIV verwenden.");
                throw e;
            }
        }
    }
    private void requireVerwaltung() {
        if (!verwaltung()) throw new IllegalArgumentException("Stammdaten dürfen nur Admin und Manager verwalten.");
    }
    private void protectPersonal(Personal old, String newRole, String newStatus) {
        if (session.role() != Role.ADMIN && (config.role(old.rolle()) == Role.ADMIN || config.role(old.rolle()) == Role.MANAGER))
            throw new IllegalArgumentException("Manager dürfen Admin- und Managerkonten nicht verändern.");
        if (old.id() == actor() && (!old.rolle().equals(newRole) || !config.activeStatus().equals(newStatus)))
            throw new IllegalArgumentException("Eigene Rolle und eigener Aktivstatus können hier nicht geändert werden.");
    }
    /** Gemeinsame Vorprüfung für Formular und Service, ohne Datenbankzugriff. */
    public void validatePersonal(PersonalEingabe input) {
            requireVerwaltung();
            String kuerzel = Betriebsregeln.text(input.kuerzel(),"Kürzel",2,true);
            if (!kuerzel.matches("[\\p{L}0-9]{1,2}")) throw new IllegalArgumentException("Kürzel: ein bis zwei Buchstaben oder Ziffern.");
            Betriebsregeln.stundensatz(input.stundensatz());
            if (input.status()==null || !List.of(config.activeStatus(),"INAKTIV").contains(input.status())) throw new IllegalArgumentException("Ungültiger Mitarbeiterstatus.");
            if (input.rolle()==null || !rollen().contains(input.rolle())) throw new IllegalArgumentException("Diese Rolle darf nicht vergeben werden.");
            boolean hasPassword = input.passwort() != null && input.passwort().length > 0;
            if ((input.id() == null || hasPassword) && (!hasPassword || input.passwort().length < 12))
                throw new IllegalArgumentException("Neues Passwort: mindestens zwölf Zeichen.");
    }
    public void savePersonal(PersonalEingabe input) throws SQLException {
        try {
            validatePersonal(input);
            String kuerzel=input.kuerzel().trim();
            boolean hasPassword=input.passwort()!=null && input.passwort().length>0;
            String hash = hasPassword ? new PasswordHasher().hash(input.passwort()) : null;
            transaction(true,c -> {
                if (input.id() != null) {
                    var old = repository.konto(c,restaurant(),input.id()).personal();
                    protectPersonal(old,input.rolle(),input.status());
                    if (!input.status().equals(config.activeStatus()) && repository.schichten(c,restaurant()).stream().anyMatch(s -> s.mitarbeiterId()==input.id()))
                        throw new IllegalArgumentException("Mitarbeiter zuerst ausstempeln, danach deaktivieren.");
                    if (config.role(old.rolle()) == Role.ADMIN && (config.role(input.rolle()) != Role.ADMIN || !config.activeStatus().equals(input.status())))
                        protectLastAdmin(c,old.id());
                }
                int id = repository.savePersonal(c,restaurant(),new PersonalEingabe(input.id(),kuerzel,input.rolle(),input.status(),input.stundensatz(),null),hash);
                repository.audit(c,"mitarbeiter",id,actor(),input.id()==null ? "Mitarbeiter angelegt" : "Mitarbeiter bearbeitet" + (hasPassword ? "; Passwort neu gesetzt" : ""));
                return null;
            });
        } finally { if (input.passwort()!=null) Arrays.fill(input.passwort(),'\0'); }
    }
    private void protectLastAdmin(Connection c, int id) throws SQLException {
        boolean another = repository.personal(c,restaurant()).stream().anyMatch(p -> p.id()!=id && config.role(p.rolle())==Role.ADMIN && config.activeStatus().equals(p.status()));
        if (!another) throw new IllegalArgumentException("Der letzte aktive Admin muss erhalten bleiben.");
    }
    public void saveFahrzeug(FahrzeugEingabe input) throws SQLException {
        requireVerwaltung();
        var valid = new FahrzeugEingabe(input.id(),Betriebsregeln.text(input.typ(),"Fahrzeugtyp",20,true),
                Betriebsregeln.text(input.nummer(),"Fahrzeugnummer",10,true),Betriebsregeln.text(input.kennzeichen(),"Kennzeichen",20,false),
                Betriebsregeln.text(input.marke(),"Marke",50,false),resourceStatus(input.status()));
        if (!List.of("Auto","Fahrrad").contains(valid.typ())) throw new IllegalArgumentException("Fahrzeugtyp: Auto oder Fahrrad.");
        transaction(true,c -> {
            if (valid.id()!=null) {
                Fahrzeug old = repository.fahrzeuge(c,restaurant()).stream().filter(f -> f.id()==valid.id()).findFirst().orElseThrow(() -> new IllegalArgumentException("Fahrzeug nicht gefunden."));
                if (old.belegt()) throw new IllegalArgumentException("Zugewiesenes Fahrzeug zuerst zurückgeben.");
            }
            int id=repository.saveFahrzeug(c,restaurant(),valid);
            repository.audit(c,"fahrzeug",id,actor(),valid.id()==null ? "Fahrzeug angelegt" : "Fahrzeug bearbeitet"); return null;
        });
    }
    public void saveHandy(HandyEingabe input) throws SQLException {
        requireVerwaltung();
        var valid = new HandyEingabe(input.id(),Betriebsregeln.text(input.bezeichnung(),"Bezeichnung",50,true),
                Betriebsregeln.text(input.mobilnummer(),"Mobilnummer",30,false),resourceStatus(input.status()),Betriebsregeln.text(input.neuePin(),"SIM-PIN",30,false));
        transaction(true,c -> {
            if (valid.id()!=null) {
                Handy old=repository.handys(c,restaurant()).stream().filter(h -> h.id()==valid.id()).findFirst().orElseThrow(() -> new IllegalArgumentException("Handy nicht gefunden."));
                if (old.belegt()) throw new IllegalArgumentException("Zugewiesenes Handy zuerst zurückgeben.");
            }
            int id=repository.saveHandy(c,restaurant(),valid);
            repository.audit(c,"handy",id,actor(),valid.id()==null ? "Handy angelegt" : "Handy bearbeitet"); return null;
        });
    }
    private String resourceStatus(String value) {
        if (!List.of("VERFUEGBAR","DEFEKT","GESPERRT").contains(value)) throw new IllegalArgumentException("Ungültiger Ressourcenstatus.");
        return value;
    }
    public void delete(StammdatenArt art,int id) throws SQLException {
        requireVerwaltung();
        transaction(true,c -> {
            if (art==StammdatenArt.MITARBEITER) {
                Personal old=repository.konto(c,restaurant(),id).personal();
                if (id==actor()) throw new IllegalArgumentException("Das eigene Konto kann nicht gelöscht werden.");
                protectPersonal(old,old.rolle(),old.status());
                if (config.role(old.rolle())==Role.ADMIN) protectLastAdmin(c,id);
            }
            repository.delete(c,art,restaurant(),id); // FK-RESTRICT erhält historische Referenzen.
            repository.audit(c,BetriebRepository.table(art),id,actor(),"Datensatz gelöscht"); return null;
        });
    }
    public void checkIn(CheckIn input) throws SQLException {
        try {
            if (!Betriebsregeln.TAETIGKEITEN.contains(input.taetigkeit())) throw new IllegalArgumentException("Bitte eine Tätigkeit auswählen.");
            String reason=Betriebsregeln.text(input.begruendung(),"Begründung",500,"Sonstiges".equals(input.taetigkeit()));
            transaction(true,c -> {
                var account=repository.konto(c,restaurant(),input.mitarbeiterId());
                if (!config.activeStatus().equals(account.personal().status()) || input.passwort()==null || !new PasswordHasher().verify(input.passwort(),account.hash()))
                    throw new IllegalArgumentException("Mitarbeiterpasswort oder Aktivstatus ungültig.");
                if ("Head".equals(input.taetigkeit()) && config.role(account.personal().rolle())==null)
                    throw new IllegalArgumentException("Schichtleitung benötigt Head-, Manager- oder Adminrechte.");
                if (repository.schichten(c,restaurant()).stream().anyMatch(s -> s.mitarbeiterId()==input.mitarbeiterId()))
                    throw new IllegalArgumentException("Der Mitarbeiter ist bereits eingestempelt.");
                if (Betriebsregeln.fahrer(input.taetigkeit())) {
                    Fahrzeug vehicle=repository.fahrzeuge(c,restaurant()).stream().filter(f -> Objects.equals(f.id(),input.fahrzeugId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Bitte ein Fahrzeug auswählen."));
                    Handy phone=repository.handys(c,restaurant()).stream().filter(h -> Objects.equals(h.id(),input.handyId())).findFirst().orElseThrow(() -> new IllegalArgumentException("Bitte ein Handy auswählen."));
                    String type="Autofahrer".equals(input.taetigkeit()) ? "Auto" : "Fahrrad";
                    if (vehicle.belegt() || phone.belegt() || !"VERFUEGBAR".equals(vehicle.status()) || !"VERFUEGBAR".equals(phone.status()) || !type.equals(vehicle.typ()))
                        throw new IllegalArgumentException("Fahrzeug/Handy nicht verfügbar oder Fahrzeugtyp unpassend. Bitte aktualisieren.");
                } else if (input.fahrzeugId()!=null || input.handyId()!=null) throw new IllegalArgumentException("Ressourcen sind nur für Fahrer vorgesehen.");
                ZonedDateTime now=ZonedDateTime.now(clock).withZoneSameInstant(Betriebsregeln.ZEITZONE);
                int shift=repository.checkIn(c,input.mitarbeiterId(),actor(),Betriebsregeln.geschaeftstag(now),now.toLocalDateTime(),account.personal().stundensatz(),input.taetigkeit());
                if (input.fahrzeugId()!=null) repository.assign(c,shift,input.fahrzeugId(),null,now.toLocalDateTime());
                if (input.handyId()!=null) repository.assign(c,shift,null,input.handyId(),now.toLocalDateTime());
                repository.audit(c,"zeiterfassung",shift,actor(),"Einstempeln: " + input.taetigkeit() + (reason.isEmpty() ? "" : " – " + reason));
                return null;
            });
        } finally { if (input.passwort()!=null) Arrays.fill(input.passwort(),'\0'); }
    }
    public void checkOut(CheckOut input) throws SQLException {
        transaction(true,c -> {
            Schicht shift=repository.schichten(c,restaurant()).stream().filter(s -> s.id()==input.schichtId()).findFirst().orElseThrow(() -> new IllegalArgumentException("Schicht bereits beendet oder nicht gefunden."));
            repository.validateCheckoutResources(c,restaurant(),shift);
            pruefung(shift.fahrzeugId(),input.fahrzeug()); pruefung(shift.handyId(),input.handy());
            LocalDateTime now=LocalDateTime.ofInstant(clock.instant(),Betriebsregeln.ZEITZONE);
            if (now.isBefore(shift.checkIn())) throw new IllegalArgumentException("Endzeit liegt vor dem Beginn. Systemzeit bzw. Zeitumstellung prüfen.");
            repository.checkOut(c,shift,actor(),now,input.fahrzeug(),input.handy());
            repository.audit(c,"zeiterfassung",shift.id(),actor(),"Mitarbeiter ausgestempelt; Ressourcen geprüft"); return null;
        });
    }
    private void pruefung(Integer resource, Pruefung pruefung) {
        if (resource==null) return;
        if (pruefung==null) throw new IllegalArgumentException("Bitte Ressourcenprüfung bestätigen.");
        Betriebsregeln.text(pruefung.mangel(),"Mangelnotiz",1000,!pruefung.inOrdnung());
    }
}
