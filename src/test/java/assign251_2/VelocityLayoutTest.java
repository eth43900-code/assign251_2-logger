package assign251_2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VelocityLayoutTest {

    @Test
    void testFormatVariables() {
        VelocityLayout layout = new VelocityLayout("[$p] $c: $m");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger", logger, Level.WARN, "Hello Velocity", null);

        String formatted = layout.format(event);
        assertEquals("[WARN] TestLogger: Hello Velocity", formatted);
    }

    @Test
    void testFormatDateAndThread() {
        VelocityLayout layout = new VelocityLayout("$t|$m");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Thread test", null);

        String formatted = layout.format(event);
        String threadName = Thread.currentThread().getName();
        assertTrue(formatted.startsWith(threadName + "|Thread test"));
    }

    @Test
    void testPatternSetter() {
        VelocityLayout layout = new VelocityLayout();
        layout.setPattern("$m");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Just message", null);

        assertEquals("Just message", layout.format(event));
    }
}
