package de.jasanex.pro.config;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import de.jasanex.pro.model.Role;
class DatabaseConfigTest {
    @Test void requiresConfirmedStatusAndValidRestaurant() {
        var config = new DatabaseConfig(new Properties(), Map.of());
        assertThrows(IllegalStateException.class, config::activeStatus);
        assertNull(config.restaurantId()); assertEquals(Role.HEAD, config.role("Head")); assertNull(config.role("Fahrer Auto"));
        assertThrows(IllegalStateException.class, () -> new DatabaseConfig(new Properties(), Map.of("JASANEX_RESTAURANT_ID", "-1")).restaurantId());
    }
    @Test void environmentOverridesLocalConfiguration() {
        Properties p = new Properties(); p.setProperty("auth.activeStatus", "local");
        var config = new DatabaseConfig(p, Map.of("JASANEX_ACTIVE_STATUS", "confirmed", "JASANEX_RESTAURANT_ID", "7"));
        assertEquals("confirmed", config.activeStatus()); assertEquals(7, config.restaurantId());
    }
}
