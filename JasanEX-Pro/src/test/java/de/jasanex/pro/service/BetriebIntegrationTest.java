package de.jasanex.pro.service;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import de.jasanex.pro.config.DatabaseConfig;
import de.jasanex.pro.model.*;
import de.jasanex.pro.model.Betriebsdaten.*;
import de.jasanex.pro.security.*;

/** Echte SQL-Operationen in einer äußeren Transaktion. Niemals Commit der Testdaten. */
@EnabledIfEnvironmentVariable(named="JASANEX_WRITE_TEST", matches="true")
class BetriebIntegrationTest {
    private Connection connection;
    private DatabaseConfig config;
    private int restaurant,admin,head,manager,worker,second;
    private String password,hash;
    private BetriebService service;
    private final Clock start=Clock.fixed(Instant.parse("2026-10-30T01:30:00Z"),Betriebsregeln.ZEITZONE);
    private final Clock end=Clock.fixed(Instant.parse("2026-10-30T05:30:00Z"),Betriebsregeln.ZEITZONE);

    @BeforeEach void setup() throws Exception {
        config=DatabaseConfig.load();restaurant=config.restaurantId();
        connection=config.connect();connection.setAutoCommit(false);
        password=UUID.randomUUID().toString();hash=new PasswordHasher().hash(password.toCharArray());
        try {
            admin=fixture(config.roleValue(Role.ADMIN));head=fixture(config.roleValue(Role.HEAD));
            manager=fixture(config.roleValue(Role.MANAGER));worker=fixture("Mitarbeiter");second=fixture("Mitarbeiter");
            service=as(admin,Role.ADMIN,start);
        } catch(Exception e) { connection.rollback();connection.close();throw e; }
    }
    @AfterEach void cleanup() throws Exception { if(connection!=null && !connection.isClosed()){try{connection.rollback();}finally{connection.close();}} }
    private String freeCode() throws SQLException {
        for(int i=0;i<1296;i++) {
            String code=String.format("%2s",Integer.toString(i,36)).replace(' ','0').toUpperCase(Locale.ROOT);
            try(var s=connection.prepareStatement("SELECT 1 FROM mitarbeiter WHERE restaurant_id=? AND kuerzel=?")){
                s.setInt(1,restaurant);s.setString(2,code);try(var r=s.executeQuery()){if(!r.next())return code;}
            }
        }
        throw new SQLException("Keine freie Testkennung.");
    }
    private int fixture(String role) throws SQLException {
        try(var s=connection.prepareStatement("INSERT INTO mitarbeiter (restaurant_id,kuerzel,rolle,m_status,passwort_hash,stundensatz) VALUES (?,?,?,?,?,15.50)",Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1,restaurant);s.setString(2,freeCode());s.setString(3,role);s.setString(4,config.activeStatus());s.setString(5,hash);s.executeUpdate();
            try(var r=s.getGeneratedKeys()){r.next();return r.getInt(1);}
        }
    }
    private BetriebService as(int id,Role role,Clock clock) {
        var session=new SessionManager.Session(new Mitarbeiter(id,restaurant,"TT",config.roleValue(role),config.activeStatus()),role,"Test");
        return new BetriebService(config,session,() -> {
            Savepoint[] boundary={null};
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
                switch(m.getName()) {
                    case "close","setTransactionIsolation" -> {return null;}
                    case "setAutoCommit" -> {boundary[0]=connection.setSavepoint();return null;}
                    case "commit" -> {connection.releaseSavepoint(boundary[0]);return null;}
                    case "rollback" -> {connection.rollback(boundary[0]);return null;}
                    default -> {try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}}
                }
            });
        },clock);
    }
    private CheckIn checkIn(int id,String activity,Integer vehicle,Integer phone) {
        return new CheckIn(id,activity,"",password.toCharArray(),vehicle,phone);
    }
    private int vehicle() throws Exception {
        String number="T"+UUID.randomUUID().toString().substring(0,8);
        service.saveFahrzeug(new FahrzeugEingabe(null,"Auto",number,"TEST","Test","VERFUEGBAR"));
        return service.load().fahrzeuge().stream().filter(f->number.equals(f.nummer())).findFirst().orElseThrow().id();
    }
    private int phone() throws Exception {
        String name="Test "+UUID.randomUUID();service.saveHandy(new HandyEingabe(null,name,"","VERFUEGBAR","1234"));
        return service.load().handys().stream().filter(h->name.equals(h.bezeichnung())).findFirst().orElseThrow().id();
    }
    @Test void staffCrudAndPermissions() throws Exception {
        String code=freeCode();char[] secret=password.toCharArray();
        service.savePersonal(new PersonalEingabe(null,code,"Mitarbeiter",config.activeStatus(),new BigDecimal("18.25"),secret));
        assertEquals('\0',secret[0]);
        Personal created=service.load().personal().stream().filter(p->code.equals(p.kuerzel())).findFirst().orElseThrow();
        service.savePersonal(new PersonalEingabe(created.id(),code,"Küche",config.activeStatus(),new BigDecimal("20.00"),new char[0]));
        assertTrue(new PasswordHasher().verify(password.toCharArray(),new de.jasanex.pro.repository.BetriebRepository().konto(connection,restaurant,created.id()).hash()));
        assertEquals(new BigDecimal("20.00"),service.load().personal().stream().filter(p->p.id()==created.id()).findFirst().orElseThrow().stundensatz());
        assertThrows(IllegalArgumentException.class,()->as(head,Role.HEAD,start).savePersonal(new PersonalEingabe(null,"ZZ","Mitarbeiter",config.activeStatus(),BigDecimal.ZERO,password.toCharArray())));
        assertTrue(as(head,Role.HEAD,start).load().personal().isEmpty());
        assertThrows(IllegalArgumentException.class,()->as(manager,Role.MANAGER,start).savePersonal(new PersonalEingabe(null,"ZZ",config.roleValue(Role.ADMIN),config.activeStatus(),BigDecimal.ZERO,password.toCharArray())));
        assertThrows(IllegalArgumentException.class,()->service.delete(StammdatenArt.MITARBEITER,admin));
        service.delete(StammdatenArt.MITARBEITER,created.id());
        assertTrue(service.load().personal().stream().noneMatch(p->p.id()==created.id()));
    }
    @Test void shiftsPreserveBusinessDayAndRejectDuplicates() throws Exception {
        assertThrows(IllegalArgumentException.class,()->service.checkIn(new CheckIn(worker,"Küche","","wrong".toCharArray(),null,null)));
        service.checkIn(checkIn(worker,"Küche",null,null));
        var shift=service.load().schichten().stream().filter(s->s.mitarbeiterId()==worker).findFirst().orElseThrow();
        assertEquals("Küche",shift.taetigkeit());
        assertThrows(IllegalArgumentException.class,()->service.checkIn(checkIn(worker,"Küche",null,null)));
        assertThrows(IllegalArgumentException.class,()->service.delete(StammdatenArt.MITARBEITER,worker));
        as(admin,Role.ADMIN,end).checkOut(new CheckOut(shift.id(),null,null));
        assertTrue(service.load().schichten().stream().noneMatch(s->s.id()==shift.id()));
        try(var s=connection.prepareStatement("SELECT geschaeftstag,check_out,stundensatz_snapshot,angemeldet_von_mitarbeiter_id,abgemeldet_von_mitarbeiter_id FROM zeiterfassung WHERE zeiterfassung_id=?")){
            s.setInt(1,shift.id());try(var r=s.executeQuery()){
                assertTrue(r.next());assertEquals(LocalDate.of(2026,10,29),r.getObject(1,LocalDate.class));assertNotNull(r.getTimestamp(2));
                assertEquals(new BigDecimal("15.50"),r.getBigDecimal(3));assertEquals(admin,r.getInt(4));assertEquals(admin,r.getInt(5));
            }
        }
        assertThrows(IllegalArgumentException.class,()->service.checkOut(new CheckOut(shift.id(),null,null)));
    }
    @Test void resourceAssignmentsReturnAndDefectsAreAtomic() throws Exception {
        int vehicle=vehicle(),phone=phone();
        service.checkIn(checkIn(worker,"Autofahrer",vehicle,phone));
        assertThrows(IllegalArgumentException.class,()->service.checkIn(checkIn(second,"Autofahrer",vehicle,phone)));
        assertThrows(IllegalArgumentException.class,()->service.saveFahrzeug(new FahrzeugEingabe(vehicle,"Auto","X","","","DEFEKT")));
        var shift=service.load().schichten().stream().filter(s->s.mitarbeiterId()==worker).findFirst().orElseThrow();
        assertEquals(vehicle,shift.fahrzeugId());assertEquals(phone,shift.handyId());
        assertThrows(IllegalArgumentException.class,()->service.checkOut(new CheckOut(shift.id(),null,null)));
        assertTrue(service.load().schichten().stream().anyMatch(s->s.id()==shift.id()));
        as(admin,Role.ADMIN,end).checkOut(new CheckOut(shift.id(),new Pruefung(false,"Testmangel"),new Pruefung(true,"")));
        Fahrzeug returned=service.load().fahrzeuge().stream().filter(f->f.id()==vehicle).findFirst().orElseThrow();
        assertFalse(returned.belegt());assertEquals("DEFEKT",returned.status());
        assertFalse(service.load().handys().stream().filter(h->h.id()==phone).findFirst().orElseThrow().belegt());
        assertThrows(IllegalArgumentException.class,()->service.delete(StammdatenArt.FAHRZEUG,vehicle));
    }
    @Test void unusedResourcesCanBeEditedAndDeleted() throws Exception {
        int v=vehicle(),h=phone();
        service.saveFahrzeug(new FahrzeugEingabe(v,"Fahrrad","T"+UUID.randomUUID().toString().substring(0,8),"","Testmarke","GESPERRT"));
        service.saveHandy(new HandyEingabe(h,"Umbenannt","123","DEFEKT",""));
        assertEquals("DEFEKT",service.load().handys().stream().filter(p->p.id()==h).findFirst().orElseThrow().status());
        service.delete(StammdatenArt.FAHRZEUG,v);service.delete(StammdatenArt.HANDY,h);
        assertTrue(service.load().fahrzeuge().stream().noneMatch(f->f.id()==v));
        assertTrue(service.load().handys().stream().noneMatch(p->p.id()==h));
    }
}
