package de.jasanex.pro.service;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class BetriebsregelnTest {
    @Test void businessDayChangesAtFourIncludingClockChanges() {
        for (LocalDate date : new LocalDate[]{LocalDate.of(2026,10,30),LocalDate.of(2026,10,25),LocalDate.of(2026,3,29)}) {
            assertEquals(date.minusDays(1),Betriebsregeln.geschaeftstag(date.atTime(3,59).atZone(Betriebsregeln.ZEITZONE)));
            assertEquals(date,Betriebsregeln.geschaeftstag(date.atTime(4,0).atZone(Betriebsregeln.ZEITZONE)));
        }
    }
    @Test void validatesMoneyAndCategories() {
        assertThrows(IllegalArgumentException.class,()->Betriebsregeln.stundensatz(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class,()->Betriebsregeln.stundensatz(new BigDecimal("12.345")));
        assertDoesNotThrow(()->Betriebsregeln.stundensatz(new BigDecimal("15.50")));
        assertEquals("Küche",Betriebsregeln.kategorie("Küche"));
        assertEquals("Sonstiges",Betriebsregeln.kategorie("Altwert"));
    }
}
