package assign251_2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import static org.junit.jupiter.api.Assertions.*;

class MemAppenderJmxTest {

    private MemAppender appender;
    private MBeanServer mbs;
    private ObjectName mbeanName;

    @BeforeEach
    void setUp() throws Exception {
        appender = MemAppender.getInstance();
        appender.reset();
        appender.setMaxSize(100);
        appender.setLayout(new SimpleLayout());  // Add layout for formatting
        mbs = ManagementFactory.getPlatformMBeanServer();
        mbeanName = new ObjectName("assign251_2:type=MemAppender");
    }

    @AfterEach
    void tearDown() {
        appender.reset();
    }

    @Test
    void testMBeanRegistration() {
        assertTrue(mbs.isRegistered(mbeanName), "MemAppenderMBean not registered to JMX server");
    }

    @Test
    void testJmxGetLogMessages() throws Exception {
        Logger logger = Logger.getLogger("JmxTestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, "JMX Test 1", null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, "JMX Test 2", null));

        // Use getAttribute for LogMessages attribute (standard for getters in MBeans)
        String[] messages = (String[]) mbs.getAttribute(mbeanName, "LogMessages");

        assertEquals(2, messages.length, "JMX should retrieve 2 logs");
        assertEquals("INFO - JMX Test 1" + System.lineSeparator(), messages[0], "First log content is incorrect"); // Include prefix and platform line separator
        assertEquals("INFO - JMX Test 2" + System.lineSeparator(), messages[1], "Second log content is incorrect");
    }

    @Test
    void testJmxGetDiscardedLogCount() throws Exception {
        Logger logger = Logger.getLogger("DiscardTestLogger");
        for (int i = 0; i < 150; i++) {
            appender.append(new LoggingEvent("c", logger, Level.INFO, "Log " + i, null));
        }

        long discarded = (long) mbs.getAttribute(mbeanName, "DiscardedLogCount");
        assertEquals(50, discarded, "JMX reported incorrect discarded log count");
    }

    @Test
    void testJmxGetEstimatedCacheSize() throws Exception {
        // Dynamic calculation for platform line separator bytes: "INFO - " = 7 chars, msg chars, + lsBytes
        // Short: 7 + 9 + lsBytes = 16 + lsBytes
        // Long: 7 + 20 + lsBytes = 27 + lsBytes
        // Total: 43 + 2 * lsBytes (e.g., 45 on Unix, 47 on Windows)
        int lsBytes = System.lineSeparator().getBytes().length;
        long expected = 43L + 2L * lsBytes;
        String log1 = "Short log";
        String log2 = "A longer log message";
        Logger logger = Logger.getLogger("CacheSizeTestLogger");
        appender.append(new LoggingEvent("c", logger, Level.INFO, log1, null));
        appender.append(new LoggingEvent("c", logger, Level.INFO, log2, null));

        long cacheSize = (long) mbs.getAttribute(mbeanName, "EstimatedCacheSize");
        assertEquals(expected, cacheSize, "JMX estimated cache size is incorrect");
    }
}