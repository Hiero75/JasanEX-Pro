package de.jasanex.pro.cache;
import java.util.*;
import de.jasanex.pro.model.*;
import de.jasanex.pro.service.AuthService.LoginResult;
public final class StammdatenCache {
    private Map<Integer, Mitarbeiter> mitarbeiter = Map.of();
    private List<OffeneSchicht> schichten = List.of();
    public void replace(LoginResult result) { mitarbeiter = Map.copyOf(result.mitarbeiter()); schichten = List.copyOf(result.schichten()); }
    public void replace(de.jasanex.pro.model.Betriebsdaten.Snapshot result) {
        mitarbeiter = Map.copyOf(result.mitarbeiter());
        schichten = List.copyOf(result.offeneSchichten());
    }
    public Map<Integer, Mitarbeiter> mitarbeiter() { return mitarbeiter; }
    public List<OffeneSchicht> schichten() { return schichten; }
    public void clear() { mitarbeiter = Map.of(); schichten = List.of(); }
}
