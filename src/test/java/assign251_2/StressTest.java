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
 * Stress tests for performance analysis. [6 marks: separate class, comparisons, layouts, before/after maxSize, 10k logs]
 * Uses loop for maxSizes (parameterised alternative).
 * Prints results to standard output for report generation.
 */
@Tag("stress")
class StressTest {

    private static final int NUM_LOGS = 10000; // 10k logs sufficient
    private static final long[] MAX_SIZES = {1, 10, 100, 1000, 10000, 100000, 1000000}; // Full range as suggested

    @Test
    void runAllStressTests() throws IOException {
        // Ensure clean state
        MemAppender.resetInstance();
        MemAppender.getInstance().reset();

        System.out.println("=== Stress Test Results (Time in ms, Memory in MB) ===");
        System.out.printf("%-30s %-10s %-15s %-15s %-10s%n", "Configuration", "MaxSize", "Time(ms)", "Est. Memory(MB)", "Discarded");

        // Loop for MemAppender comparisons (simulates parameterised for all maxSizes)
        for (long maxSize : MAX_SIZES) {
            testMemAppender(new ArrayList<>(), "MemAppender(ArrayList)", maxSize);
            testMemAppender(new LinkedList<>(), "MemAppender(LinkedList)", maxSize);
        }

        // Compare with other appenders (no maxSize)
        testConsoleAppender();
        testFileAppender();

        System.out.println("\n=== Layout Comparison (10k logs) ===");
        testLayoutPerformance();

        System.out.println("\n=== Test finished. Sleeping for 30 seconds to allow profiler connection (e.g., VisualVM)... ===");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testMemAppender(List<LoggingEvent> list, String name, long maxSize) {
        MemAppender.resetInstance();
        MemAppender appender = MemAppender.getInstance(list);
        appender.reset();
        appender.setMaxSize((int) maxSize);
        appender.setLayout(new SimpleLayout());

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        // Before maxSize: log half (no/less discard)
        runLoad(logger, name + " (Before)", String.valueOf(maxSize), NUM_LOGS / 2, true);

        // After maxSize: log rest (with discard)
        runLoad(logger, name + " (After)", String.valueOf(maxSize), NUM_LOGS / 2, false);

        // Combined output for config
        long discarded = appender.getDiscardedLogCount();
        System.out.printf("%-30s %-10s (Total Discarded: %d)%n", name, maxSize, discarded);
    }

    private void testConsoleAppender() {
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        appender.setImmediateFlush(false); // Avoid slow IO
        appender.setWriter(new PrintWriter(new StringWriter())); // Dummy for sensible comparison

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        runLoad(logger, "ConsoleAppender(Dummy)", "N/A", NUM_LOGS, true);
    }

    private void testFileAppender() throws IOException {
        FileAppender appender = new FileAppender(new SimpleLayout(), "target/stress-test.log", false);
        appender.setBufferedIO(true);
        appender.setBufferSize(8192);

        Logger logger = Logger.getLogger("StressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        runLoad(logger, "FileAppender(Buffered)", "N/A", NUM_LOGS, true);
        appender.close();
    }

    private void testLayoutPerformance() {
        MemAppender appender = MemAppender.getInstance(); // Clean instance
        Logger logger = Logger.getLogger("LayoutTest");
        logger.setLevel(Level.INFO);
        appender.setMaxSize(NUM_LOGS + 1); // No discard for fair layout test

        // VelocityLayout
        appender.reset();
        appender.setLayout(new VelocityLayout("[$p] $c $d: $m$n"));
        logger.removeAllAppenders();
        logger.addAppender(appender);
        long startV = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        appender.getEventStrings(); // Force format
        long endV = System.currentTimeMillis();
        System.out.println("VelocityLayout Time (10k logs): " + (endV - startV) + "ms");

        // PatternLayout
        appender.reset();
        appender.setLayout(new PatternLayout("[%p] %c %d: %m%n"));
        logger.removeAllAppenders();
        logger.addAppender(appender);
        long startP = System.currentTimeMillis();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        appender.getEventStrings(); // Force format
        long endP = System.currentTimeMillis();
        System.out.println("PatternLayout Time (10k logs): " + (endP - startP) + "ms");
    }

    // Updated runLoad: phase (before/after), isBefore flag
    private void runLoad(Logger logger, String configName, String maxSizeVal, int numLogs, boolean isBefore) {
        System.gc();
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numLogs; i++) {
            logger.info("Stress test log message number " + i + (isBefore ? " (before)" : " (after)"));
        }

        long endTime = System.currentTimeMillis();
        System.gc();
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMem = Math.max(0, endMem - startMem);

        // Append phase to configName
        String phase = isBefore ? "Before" : "After";
        System.out.printf("%-30s %-10s %-15d %-15.2f%n",
                configName + " " + phase, maxSizeVal, (endTime - startTime), (usedMem / 1024.0 / 1024.0));
    }
}