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
        // Reset singleton before each test for isolation
        MemAppender.resetInstance();
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

        // Test PDF Req 1a: getCurrentLogs() returns List<LoggingEvent>
        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(1, logs.size());
        assertSame(event, logs.get(0), "Should store the original LoggingEvent");
        assertEquals("Test message", logs.get(0).getRenderedMessage());
    }

    @Test
    void testMaxSizeAndDiscard() {
        appender.setMaxSize(2); // [cite: 37]
        Logger logger = Logger.getLogger("TestLogger");

        LoggingEvent event1 = new LoggingEvent("c", logger, Level.INFO, "Msg 1", null);
        LoggingEvent event2 = new LoggingEvent("c", logger, Level.INFO, "Msg 2", null);
        appender.append(event1);
        appender.append(event2);

        assertEquals(2, appender.getCurrentLogs().size(), "Should have 2 logs before discard");
        assertEquals(0, appender.getDiscardedLogCount());

        LoggingEvent event3 = new LoggingEvent("c", logger, Level.INFO, "Msg 3", null);
        appender.append(event3); // This should discard event1 [cite: 38]

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(2, logs.size(), "Should still have 2 logs after discard");
        assertSame(event2, logs.get(0), "Event 2 should be the first log");
        assertSame(event3, logs.get(1), "Event 3 should be the second log");
        assertEquals(1, appender.getDiscardedLogCount(), "Discard count should be 1"); // [cite: 39]
    }

    @Test
    void testDependencyInjection() {
        // Reset instance to allow injection
        MemAppender.resetInstance();

        // Inject LoggingEvent-typed list
        LinkedList<LoggingEvent> injectedList = new LinkedList<>();
        appender = MemAppender.getInstance(injectedList);
        appender.setLayout(new SimpleLayout());

        Logger logger = Logger.getLogger("TestLogger");
        LoggingEvent event = new LoggingEvent("c", logger, Level.INFO, "Injected list test", null);
        appender.append(event);

        assertEquals(1, injectedList.size(), "Injected list should contain the event");
        assertSame(event, injectedList.get(0), "Injected list should have the exact event instance");
    }

    @Test
    void testGetEventStrings() {
        appender.setLayout(new SimpleLayout());
        Logger logger = Logger.getLogger("TestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 1", null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 2", null));

        // Test PDF Req 1b: getEventStrings()
        List<String> strings = appender.getEventStrings();
        assertEquals(2, strings.size());
        assertEquals("INFO - Msg 1" + System.lineSeparator(), strings.get(0));
        assertEquals("INFO - Msg 2" + System.lineSeparator(), strings.get(1));
    }

    @Test
    void testPrintLogs() {
        // This test is basic, just checks that logs are cleared
        appender.setMaxSize(5);
        Logger logger = Logger.getLogger("TestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 1", null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 2", null));

        assertEquals(2, appender.getCurrentLogs().size());

        // We can't easily test System.out, but we can test the side effects
        appender.printLogs();

        assertEquals(0, appender.getCurrentLogs().size(), "printLogs() should clear the log list");
    }

    @Test
    void testGetEventStringsPrecondition() {
        appender.setLayout(null); // Set layout to null
        Logger logger = Logger.getLogger("TestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 1", null));

        // Test precondition check [cite: 32]
        assertThrows(IllegalStateException.class, () -> {
            appender.getEventStrings();
        }, "Should throw exception if layout is null");
    }
}