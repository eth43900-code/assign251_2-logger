package assign251_2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemAppenderTest {

    private MemAppender appender;

    @BeforeEach
    void setUp() {
        appender = MemAppender.getInstance();
        appender.reset();
        appender.setLayout(new SimpleLayout());
    }

    @Test
    void testSingleton() {
        MemAppender appender1 = MemAppender.getInstance();
        MemAppender appender2 = MemAppender.getInstance();
        assertSame(appender1, appender2, "MemAppender should enforce Singleton pattern");
    }

    @Test
    void testAppendAndGetCurrentLogs() {
        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("TestLogger", logger, Level.INFO, "Test message", null);
        appender.append(event);

        List<String> logs = appender.getCurrentLogs(); // Use the added getCurrentLogs() method
        assertEquals(1, logs.size());
        assertEquals("INFO - Test message" + System.lineSeparator(), logs.get(0)); // Match SimpleLayout format
    }

    @Test
    void testMaxSizeAndDiscard() {
        appender.setMaxSize(2);
        Logger logger = Logger.getLogger("TestLogger");

        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 1", null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 2", null));

        assertEquals(2, appender.getCurrentLogs().size(), "Should have 2 logs before discard");
        assertEquals(0, appender.getDiscardedLogCount());

        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 3", null));

        List<String> logs = appender.getCurrentLogs();
        assertEquals(2, logs.size(), "Should still have 2 logs after discard");
        assertEquals("INFO - Msg 2" + System.lineSeparator(), logs.get(0));
        assertEquals("INFO - Msg 3" + System.lineSeparator(), logs.get(1));
        assertEquals(1, appender.getDiscardedLogCount());
    }

    @Test
    void testDependencyInjection() {
        appender.close();
        // Reset instance to allow injection
        MemAppender.resetInstance();
        // Inject String-typed list (resolve type mismatch issue)
        LinkedList<String> injectedList = new LinkedList<>();
        appender = MemAppender.getInstance(injectedList);
        appender.setLayout(new SimpleLayout());

        Logger logger = Logger.getLogger("TestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Injected list test", null));

        assertEquals(1, injectedList.size());
        assertEquals("INFO - Injected list test" + System.lineSeparator(), injectedList.get(0));
    }
}