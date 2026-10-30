package de.jasanex.pro.security;
import de.jasanex.pro.model.*;
import de.jasanex.pro.service.AuthService.LoginResult;
public final class SessionManager {
    public record Session(Mitarbeiter user, Role role, String restaurant) { }
    private Session current;
    public void start(LoginResult result) { current = new Session(result.user(), result.role(), result.restaurant()); }
    public Session current() { if (current == null) throw new IllegalStateException("Keine aktive Sitzung."); return current; }
    public void clear() { current = null; }
    public boolean isActive() { return current != null; }
}
