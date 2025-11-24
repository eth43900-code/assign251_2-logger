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
 * Stress tests for performance analysis.
 * Implements PDF Requirement 4:
 * - Separate test class.
 * - Compares LinkedList vs ArrayList.
 * - Compares ConsoleAppender vs FileAppender.
 * - Compares VelocityLayout vs PatternLayout.
 * - Measures Time and Memory.
 */
@Tag("stress")
class StressTest {

    private static final int NUM_LOGS = 10000; // 10k logs per PDF requirement
    // Using the full range suggested by PDF
    private static final long[] MAX_SIZES = {1, 10, 100, 1000, 10000, 100000, 1000000};

    @Test
    void runAllStressTests() throws IOException {
        // Ensure clean state
        MemAppender.resetInstance();
        if (MemAppender.getInstance() != null) {
            MemAppender.getInstance().reset();
        }

        System.out.println("=== Warming up JVM (to ensure fair comparison) ===");
        runWarmup();

        System.out.println("\n=== Stress Test Results (Time in ms, Memory in MB) ===");
        // Formatted Header for alignment
        System.out.printf("%-38s %-12s %-12s %-18s %-10s%n", "Configuration", "MaxSize", "Time(ms)", "Est. Memory(MB)", "Discarded");

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

    private void runWarmup() {
        // Run a dummy test to trigger class loading and JIT compilation
        MemAppender.resetInstance();
        MemAppender appender = MemAppender.getInstance();
        appender.setLayout(new SimpleLayout());
        Logger logger = Logger.getLogger("WarmupLogger");
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        for (int i = 0; i < 5000; i++) {
            logger.info("Warmup");
        }
        appender.reset();
        MemAppender.resetInstance();
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

        // Before maxSize: log half
        runLoad(logger, name, String.valueOf(maxSize), NUM_LOGS / 2, true);

        // After maxSize: log rest (with discard)
        runLoad(logger, name, String.valueOf(maxSize), NUM_LOGS / 2, false);

        // Combined output for discard count
        long discarded = appender.getDiscardedLogCount();
        // Print summary row for the configuration
        System.out.printf("%-38s %-12s %-12s %-18s (Total Discarded: %d)%n",
                name + " [Summary]", maxSize, "-", "-", discarded);
    }

    private void testConsoleAppender() {
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        appender.setImmediateFlush(false); // Avoid slow IO to see pure appender overhead
        appender.setWriter(new PrintWriter(new StringWriter())); // Dummy writer

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
        long startV = System.nanoTime();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        appender.getEventStrings(); // Force format
        long endV = System.nanoTime();
        double timeMs = (endV - startV) / 1_000_000.0;
        System.out.printf("VelocityLayout Time (10k logs): %.3f ms%n", timeMs);

        // PatternLayout
        appender.reset();
        appender.setLayout(new PatternLayout("[%p] %c %d: %m%n"));
        logger.removeAllAppenders();
        logger.addAppender(appender);
        long startP = System.nanoTime();
        for (int i = 0; i < NUM_LOGS; i++) {
            logger.info("Layout test message " + i);
        }
        appender.getEventStrings(); // Force format
        long endP = System.nanoTime();
        double timeMsP = (endP - startP) / 1_000_000.0;
        System.out.printf("PatternLayout Time (10k logs): %.3f ms%n", timeMsP);
    }

    // Updated runLoad with formatted output
    private void runLoad(Logger logger, String configName, String maxSizeVal, int numLogs, boolean isBefore) {
        System.gc();
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTime = System.nanoTime();

        for (int i = 0; i < numLogs; i++) {
            logger.info("Stress test log message number " + i + (isBefore ? " (before)" : " (after)"));
        }

        long endTime = System.nanoTime();

        System.gc();
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long usedMem = Math.max(0, endMem - startMem);
        double timeMs = (endTime - startTime) / 1_000_000.0;

        // Append phase to configName
        String phase = isBefore ? " (Before)" : " (After)";

        // Aligned output
        System.out.printf("%-38s %-12s %-12.3f %-18.2f%n",
                configName + phase, maxSizeVal, timeMs, (usedMem / 1024.0 / 1024.0));
    }
}