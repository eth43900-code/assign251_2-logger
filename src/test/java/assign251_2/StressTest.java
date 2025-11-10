package assign251_2;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Stress tests for performance analysis. [cite: 68, 69]
 * Prints results to standard output for report generation.
 */
@Tag("stress")
class StressTest {

    private static final int NUM_LOGS = 10000; // [cite: 75]
    private static final long[] MAX_SIZES = {10, 1000, 100000}; // [cite: 73]

    @Test
    void runAllStressTests() throws IOException {
        // Ensure clean state before starting stress tests
        MemAppender.resetInstance();
        MemAppender.getInstance().reset();

        System.out.println("=== Stress Test Results (Time in ms) ===");
        System.out.printf("%-30s %-10s %-15s %-15s%n", "Configuration", "MaxSize", "Time(ms)", "Est. Memory(MB)");

        // Compare ArrayList vs LinkedList for MemAppender [cite: 70]
        for (long maxSize : MAX_SIZES) {
            // Test with ArrayList<LoggingEvent>
            testMemAppender(new ArrayList<>(), "MemAppender(ArrayList)", maxSize);
            // Test with LinkedList<LoggingEvent>
            testMemAppender(new LinkedList<>(), "MemAppender(LinkedList)", maxSize);
        }

        // Compare with other appenders
        testConsoleAppender(); // [cite: 70]
        testFileAppender(); // [cite: 70]

        System.out.println("\n=== Layout Comparison ===");
        testLayoutPerformance(); // [cite: 72]
    }

    // Modified testMemAppender parameter type to List<LoggingEvent>
    private void testMemAppender(List<LoggingEvent> list, String name, long maxSize) {
        // Reset instance to allow injection of new list
        MemAppender.resetInstance();
        // Inject list via parameterized getInstance (now LoggingEvent type)
        MemAppender appender = MemAppender.getInstance(list);
        appender.reset();
        appender.setMaxSize((int) maxSize);
        appender.setLayout(new SimpleLayout());

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        runLoad(logger, name, String.valueOf(maxSize));
    }

    private void testConsoleAppender() {
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        // Set ImmediateFlush to false to avoid super slow console I/O dominating test
        appender.setImmediateFlush(false);
        // Use a dummy writer to avoid flooding actual console during test
        appender.setWriter(new PrintWriter(new StringWriter()));

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        runLoad(logger, "ConsoleAppender(Dummy)", "N/A");
    }

    private void testFileAppender() throws IOException {
        FileAppender appender = new FileAppender(new SimpleLayout(), "target/stress-test.log", false);
        appender.setBufferedIO(true);
        appender.setBufferSize(8192);

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        runLoad(logger, "FileAppender(Buffered)", "N/A");
        appender.close();
    }

    private void testLayoutPerformance() {
        // Use a clean instance for layout tests
        MemAppender.resetInstance();
        MemAppender appender = MemAppender.getInstance();
        Logger logger = Logger.getLogger("LayoutTest");
        logger.setLevel(Level.INFO);

        // Test VelocityLayout [cite: 72]
        appender.reset(); // Ensure clean state
        // Add $n for newline
        appender.setLayout(new VelocityLayout("[$p] $c $d: $m$n"));
        // Ensure no discard for fair layout test, we want to format ALL logs
        appender.setMaxSize(NUM_LOGS + 1);

        logger.removeAllAppenders();
        logger.addAppender(appender);

        long startV = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        // Force formatting of all logs at once (now correct behavior)
        appender.getEventStrings();
        long endV = System.currentTimeMillis();
        System.out.println("VelocityLayout Time (10k logs): " + (endV - startV) + "ms");

        // Test PatternLayout [cite: 72]
        appender.reset(); // Reset again for next layout
        appender.setLayout(new PatternLayout("[%p] %c %d: %m%n"));
        appender.setMaxSize(NUM_LOGS + 1);
        logger.removeAllAppenders();
        logger.addAppender(appender);

        long startP = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        // Force formatting of all logs at once
        appender.getEventStrings();
        long endP = System.currentTimeMillis();
        System.out.println("PatternLayout Time (10k logs): " + (endP - startP) + "ms");
    }

    private void runLoad(Logger logger, String configName, String maxSizeVal) {
        System.gc(); // Request GC to get a cleaner memory baseline
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Stress test log message number " + i);
        }

        long endTime = System.currentTimeMillis();
        System.gc(); // Request GC again to measure memory after run
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        // Use max(0, ...) in case GC freed more than was allocated
        long usedMem = Math.max(0, endMem - startMem);

        System.out.printf("%-30s %-10s %-15d %-15.2f%n",
                configName, maxSizeVal, (endTime - startTime), (usedMem / 1024.0 / 1024.0));
    }
}