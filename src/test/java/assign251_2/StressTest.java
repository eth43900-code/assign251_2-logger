package assign251_2;

import org.apache.log4j.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Stress tests for performance analysis.
 * Prints results to standard output for report generation.
 */
@Tag("stress")
class StressTest {

    private static final int NUM_LOGS = 10000;
    private static final long[] MAX_SIZES = {10, 1000, 100000};

    @Test
    void runAllStressTests() throws IOException {
        // Ensure clean state before starting stress tests
        MemAppender.getInstance().reset();

        System.out.println("=== Stress Test Results (Time in ms) ===");
        System.out.printf("%-30s %-10s %-15s %-15s%n", "Configuration", "MaxSize", "Time(ms)", "Est. Memory(MB)");

        for (long maxSize : MAX_SIZES) {
            testMemAppender(new ArrayList<>(), "MemAppender(ArrayList)", maxSize);
            testMemAppender(new LinkedList<>(), "MemAppender(LinkedList)", maxSize);
        }

        testConsoleAppender();
        testFileAppender();

        System.out.println("\n=== Layout Comparison ===");
        testLayoutPerformance();
    }

    private void testMemAppender(java.util.List<org.apache.log4j.spi.LoggingEvent> list, String name, long maxSize) {
        MemAppender appender = MemAppender.getInstance();
        appender.reset(); // Reset before each configuration test
        appender.setLogList(list);
        appender.setMaxSize(maxSize);
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
        appender.setWriter(new java.io.PrintWriter(new java.io.StringWriter()));

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
        MemAppender appender = MemAppender.getInstance();
        Logger logger = Logger.getLogger("LayoutTest");
        logger.setLevel(Level.INFO);

        // Test VelocityLayout
        appender.reset(); // Ensure clean state
        appender.setLayout(new VelocityLayout("[$p] $c $d: $m$n"));
        // Ensure no discard for fair layout test, we want to format ALL logs
        appender.setMaxSize(NUM_LOGS + 1);

        logger.removeAllAppenders();
        logger.addAppender(appender);

        long startV = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        // Force formatting of all logs at once to measure layout performance
        appender.getEventStrings();
        long endV = System.currentTimeMillis();
        System.out.println("VelocityLayout Time (10k logs): " + (endV - startV) + "ms");

        // Test PatternLayout
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
        System.gc();
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Stress test log message number " + i);
        }

        long endTime = System.currentTimeMillis();
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMem = Math.max(0, endMem - startMem);

        System.out.printf("%-30s %-10s %-15d %-15.2f%n",
                configName, maxSizeVal, (endTime - startTime), (usedMem / 1024.0 / 1024.0));
    }
}