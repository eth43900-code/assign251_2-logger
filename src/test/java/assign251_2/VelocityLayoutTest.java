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
        assertEquals("[WARN] TestLogger: Hello Velocity" + System.lineSeparator(), formatted);
    }

    @Test
    void testFormatDateAndThread() {
        VelocityLayout layout = new VelocityLayout("$t|$m");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Thread test", null);

        String formatted = layout.format(event);
        String threadName = Thread.currentThread().getName();
        assertEquals(threadName + "|Thread test" + System.lineSeparator(), formatted);
    }

    @Test
    void testPatternSetter() {
        VelocityLayout layout = new VelocityLayout();
        layout.setPattern("$m");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Just message", null);

        assertEquals("Just message" + System.lineSeparator(), layout.format(event));
    }

    @Test
    void testFormatWithInvalidTemplate() {
        VelocityLayout layout = new VelocityLayout("#if ($p == 'INFO')");  // Incomplete #if directive (missing #end) to force ParseErrorException
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Test with invalid template", null);

        String result = layout.format(event);
        // Fallback to raw message + line separator due to exception
        assertTrue(result.contains("Test with invalid template"), "Should catch template syntax errors");
        assertTrue(result.endsWith(System.lineSeparator()), "Should end with platform line separator");
    }

    @Test
    void testFormatWithNullPattern() {
        VelocityLayout layout = new VelocityLayout(null);
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Null Pattern Test", null);

        String result = layout.format(event);
        assertEquals("Null Pattern Test" + System.lineSeparator(), result, "Null template should output raw message with platform line separator");
    }
}