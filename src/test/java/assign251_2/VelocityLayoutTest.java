package assign251_2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class VelocityLayoutTest {

    @Test
    void testFormatVariables() {
        // Add $n to template
        VelocityLayout layout = new VelocityLayout("[$p] $c: $m$n");
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger", logger, Level.WARN, "Hello Velocity", null);

        String formatted = layout.format(event);
        assertEquals("[WARN] TestLogger: Hello Velocity" + System.lineSeparator(), formatted);
    }

    @Test
    void testFormatDateAndThread() {
        // Add $n to template
        VelocityLayout layout = new VelocityLayout("$t|$m|$d$n");
        Logger logger = Logger.getLogger("TestLogger");
        long timestamp = System.currentTimeMillis();
        LoggingEvent event = new LoggingEvent("c", logger, timestamp, Level.INFO, "Thread test", null);

        String formatted = layout.format(event);
        String threadName = Thread.currentThread().getName();
        String dateString = new Date(timestamp).toString();
        assertEquals(threadName + "|Thread test|" + dateString + System.lineSeparator(), formatted);
    }

    @Test
    void testPatternSetter() {
        VelocityLayout layout = new VelocityLayout();
        // Add $n to template
        layout.setPattern("$m$n");
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
        // Fallback to raw message, *without* line separator
        assertEquals("Test with invalid template", result, "Should fallback to raw message on error");
    }

    @Test
    void testFormatWithNullPattern() {
        VelocityLayout layout = new VelocityLayout(null);
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Null Pattern Test", null);

        String result = layout.format(event);
        // Null template should output raw message, *without* line separator
        assertEquals("Null Pattern Test", result, "Null template should output raw message");
    }

    @Test
    void testIgnoresThrowable() {
        VelocityLayout layout = new VelocityLayout();
        assertTrue(layout.ignoresThrowable(), "VelocityLayout should ignore throwables");
    }

    @Test
    void testActivateOptions() {
        VelocityLayout layout = new VelocityLayout();
        layout.activateOptions(); // Call to cover empty method
        // No assert needed, just coverage
    }

    @Test
    void testVelocityInitException() {
        assertThrows(RuntimeException.class, () -> {
            Properties props = new Properties();
            props.setProperty("runtime.log.logsystem.class", "invalid.class.name"); // Force ClassNotFound
            VelocityEngine ve = new VelocityEngine();
            ve.init(props);
        }, "Should throw on invalid logsystem class");
    }

}

