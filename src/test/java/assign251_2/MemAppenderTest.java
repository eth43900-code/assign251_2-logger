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
        // Use the new reset() method to ensure a clean, non-closed state for every test
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

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(1, logs.size());
        assertEquals("Test message", logs.get(0).getRenderedMessage());
    }

    @Test
    void testMaxSizeAndDiscard() {
        appender.setMaxSize(2);
        Logger logger = Logger.getLogger("TestLogger");

        // These appends should now work because reset() ensured closed=false
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 1", null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 2", null));

        assertEquals(2, appender.getCurrentLogs().size(), "Should have 2 logs before discard");
        assertEquals(0, appender.getDiscardedLogCount());

        // This should cause "Msg 1" to be discarded
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Msg 3", null));

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(2, logs.size(), "Should still have 2 logs after discard");
        assertEquals("Msg 2", logs.get(0).getRenderedMessage());
        assertEquals("Msg 3", logs.get(1).getRenderedMessage());
        assertEquals(1, appender.getDiscardedLogCount());
    }

    @Test
    void testDependencyInjection() {
        LinkedList<LoggingEvent> injectedList = new LinkedList<>();
        appender.setLogList(injectedList);

        Logger logger = Logger.getLogger("TestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "Injected list test", null));

        assertEquals(1, injectedList.size());
        assertEquals("Injected list test", injectedList.get(0).getRenderedMessage());
    }
}