package assign251_2;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    @BeforeEach
    void resetLog4j() {
        Logger.getRootLogger().removeAllAppenders();
        // Crucial: fully reset the singleton appender
        MemAppender.resetInstance();
        MemAppender.getInstance().reset();
    }

    @Test
    void testMemAppenderWithVelocityLayout() {
        MemAppender memAppender = MemAppender.getInstance();
        // Add $n to the template to get a newline
        memAppender.setLayout(new VelocityLayout("[$p] $m$n"));

        Logger logger = Logger.getRootLogger();
        logger.addAppender(memAppender);
        logger.setLevel(Level.INFO);

        logger.info("Integration test");

        // getEventStrings() now formats the event
        List<String> strings = memAppender.getEventStrings();
        assertEquals(1, strings.size());
        assertEquals("[INFO] Integration test" + System.lineSeparator(), strings.get(0));
    }

    @Test
    void testVelocityLayoutWithConsoleAppender() {
        // Add $n to the template for a newline
        VelocityLayout layout = new VelocityLayout("Console: $m$n");
        ConsoleAppender consoleAppender = new ConsoleAppender(layout);

        Logger logger = Logger.getRootLogger();
        logger.addAppender(consoleAppender);

        logger.info("Visible on console if run manually");
        // No assertions here, just ensuring no exceptions when used with standard appender
    }

    @Test
    void testMemAppenderWithPatternLayout() {
        MemAppender memAppender = MemAppender.getInstance();
        memAppender.setLayout(new PatternLayout("%p - %m%n"));  // %n ensures platform line separator

        Logger logger = Logger.getLogger("PatternTest");
        logger.addAppender(memAppender);

        logger.error("Pattern layout works");

        List<String> strings = memAppender.getEventStrings();
        assertEquals(1, strings.size());
        assertEquals("ERROR - Pattern layout works" + System.lineSeparator(), strings.get(0));
    }
}