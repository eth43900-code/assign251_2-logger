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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stress tests with enhanced memory fluctuation for clear VisualVM sawtooth pattern.
 * Implements PDF Requirement 4 with high-volume, concurrent log generation.
 */
@Tag("stress")
class StressTest {
    private static final int TOTAL_LOGS = 200000;
    private static final int CONCURRENT_THREADS = 30;
    private static final int LOG_MESSAGE_LENGTH = 1000;
    private static final int BATCH_COUNT = 4;
    private static final int LOGS_PER_BATCH = TOTAL_LOGS / BATCH_COUNT;
    private static final long[] MAX_SIZES = {100, 500, 1000, 10000, 100000, 1000000};
    private static final List<LoggingEvent> retainedLogs = new ArrayList<>(1000);

    @Test
    void runAllStressTests() throws IOException, InterruptedException {

        MemAppender.resetInstance();
        if (MemAppender.getInstance() != null) {
            MemAppender.getInstance().reset();
        }

        System.out.println("=== Warming up JVM (10k logs + concurrent threads) ===");
        runWarmup();

        System.out.println("\n=== Stress Test Results (Time in ms, Memory in MB) ===");
        System.out.printf("%-50s %-12s %-12s %-18s %-10s%n",
                "Configuration", "MaxSize", "Time(ms)", "Est. Memory(MB)", "Discarded");

        for (long maxSize : MAX_SIZES) {
            testMemAppender(new ArrayList<>(), "MemAppender(ArrayList)", maxSize);
            testMemAppender(new LinkedList<>(), "MemAppender(LinkedList)", maxSize);
        }

        testConsoleAppender();
        testFileAppender();

        System.out.println("\n=== Layout Comparison (200k logs, 30 threads) ===");
        testLayoutPerformance();

        retainedLogs.clear();
        System.out.println("\n=== Test finished. Sleeping for 90 seconds to allow profiler connection... ===");
        Thread.sleep(90000);
    }


    private void runWarmup() throws InterruptedException {
        MemAppender.resetInstance();
        MemAppender appender = MemAppender.getInstance();
        appender.setLayout(new SimpleLayout());
        Logger logger = Logger.getLogger("WarmupLogger");
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    logger.info(generateLongLogMessage("Warmup", j));
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        appender.reset();
        MemAppender.resetInstance();
    }


    private void testMemAppender(List<LoggingEvent> list, String name, long maxSize) throws InterruptedException {
        MemAppender.resetInstance();
        MemAppender appender = MemAppender.getInstance(list);
        appender.reset();
        appender.setMaxSize((int) maxSize);
        appender.setLayout(new SimpleLayout());
        Logger logger = Logger.getLogger("StressLogger-" + name);
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        long totalDiscarded = 0;
        long totalTime = 0;
        long peakMemory = 0;

        for (int batch = 0; batch < BATCH_COUNT; batch++) {
            System.out.printf("%n=== Batch %d/%d for %s (maxSize=%d) ===%n",
                    batch + 1, BATCH_COUNT, name, maxSize);

            long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.nanoTime();

            runConcurrentBatchLoad(logger, batch);

            long endTime = System.nanoTime();
            long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long batchTime = (endTime - startTime) / 1_000_000;
            long batchMem = Math.max(0, endMem - startMem);

            totalTime += batchTime;
            peakMemory = Math.max(peakMemory, batchMem);
            totalDiscarded = appender.getDiscardedLogCount();

            System.out.printf("Batch %d: Time=%.3f ms, Mem=%.2f MB, Discarded=%d%n",
                    batch + 1, (double) batchTime, batchMem / 1024.0 / 1024.0, totalDiscarded);


            Thread.sleep(1000);


            retainedLogs.addAll(appender.getCurrentLogs().subList(0, Math.min(250, appender.getCurrentLogs().size())));
        }


        System.out.printf("%-50s %-12s %-12.3f %-18.2f (Total Discarded: %d)%n",
                name + " [Summary]", maxSize, (double) totalTime, peakMemory / 1024.0 / 1024.0, totalDiscarded);
    }

    /**
     * ConsoleAppender
     */
    private void testConsoleAppender() throws InterruptedException {
        ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
        appender.setImmediateFlush(false);
        appender.setWriter(new PrintWriter(new StringWriter()));
        Logger logger = Logger.getLogger("ConsoleStressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        long totalTime = 0;
        long peakMemory = 0;

        for (int batch = 0; batch < BATCH_COUNT; batch++) {
            long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.nanoTime();

            runConcurrentBatchLoad(logger, batch);

            long endTime = System.nanoTime();
            long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalTime += (endTime - startTime) / 1_000_000;
            peakMemory = Math.max(peakMemory, Math.max(0, endMem - startMem));

            Thread.sleep(800); // 暂停，堆积内存
        }

        System.out.printf("%-50s %-12s %-12.3f %-18.2f%n",
                "ConsoleAppender(Dummy) [Summary]", "N/A", (double) totalTime, peakMemory / 1024.0 / 1024.0);
    }

    /*
     FileAppender
     */
    private void testFileAppender() throws IOException, InterruptedException {
        FileAppender appender = new FileAppender(new SimpleLayout(), "target/stress-test.log", false);
        appender.setBufferedIO(true);
        appender.setBufferSize(16384);
        Logger logger = Logger.getLogger("FileStressLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        long totalTime = 0;
        long peakMemory = 0;

        for (int batch = 0; batch < BATCH_COUNT; batch++) {
            long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.nanoTime();

            runConcurrentBatchLoad(logger, batch);

            long endTime = System.nanoTime();
            long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            totalTime += (endTime - startTime) / 1_000_000;
            peakMemory = Math.max(peakMemory, Math.max(0, endMem - startMem));

            Thread.sleep(800);
        }

        appender.close();
        System.out.printf("%-50s %-12s %-12.3f %-18.2f%n",
                "FileAppender(Buffered) [Summary]", "N/A", (double) totalTime, peakMemory / 1024.0 / 1024.0);
    }


    private void testLayoutPerformance() throws InterruptedException {
        MemAppender appender = MemAppender.getInstance();
        Logger logger = Logger.getLogger("LayoutTest");
        logger.setLevel(Level.INFO);
        appender.setMaxSize(TOTAL_LOGS + 1);

        // VelocityLayout
        appender.reset();
        appender.setLayout(new VelocityLayout("[$p] $c $d: $m$n"));
        logger.removeAllAppenders();
        logger.addAppender(appender);

        long startV = System.nanoTime();
        for (int batch = 0; batch < BATCH_COUNT; batch++) {
            runConcurrentBatchLoad(logger, batch);
            Thread.sleep(500);
        }
        appender.getEventStrings();
        long endV = System.nanoTime();
        double timeMsV = (endV - startV) / 1_000_000.0;

        // PatternLayout
        appender.reset();
        appender.setLayout(new PatternLayout("[%p] %c %d: %m%n"));
        logger.removeAllAppenders();
        logger.addAppender(appender);

        long startP = System.nanoTime();
        for (int batch = 0; batch < BATCH_COUNT; batch++) {
            runConcurrentBatchLoad(logger, batch);
            Thread.sleep(500);
        }
        appender.getEventStrings();
        long endP = System.nanoTime();
        double timeMsP = (endP - startP) / 1_000_000.0;


        System.out.printf("VelocityLayout Time (200k logs): %.3f ms%n", timeMsV);
        System.out.printf("PatternLayout Time (200k logs): %.3f ms%n", timeMsP);
        System.out.printf("Performance Ratio: VelocityLayout is %.1f x slower%n", timeMsV / timeMsP);
    }


    private void runConcurrentBatchLoad(Logger logger, int batch) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        int logsPerThread = LOGS_PER_BATCH / CONCURRENT_THREADS;

        for (int threadId = 0; threadId < CONCURRENT_THREADS; threadId++) {
            int finalThreadId = threadId;
            int finalBatch = batch;
            executor.submit(() -> {
                for (int i = 0; i < logsPerThread; i++) {
                    String message = generateLongLogMessage(
                            "Batch" + (finalBatch + 1) + "-Thread" + finalThreadId,
                            i
                    );
                    logger.info(message);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
    }

    private String generateLongLogMessage(String prefix, int index) {
        StringBuilder sb = new StringBuilder(prefix)
                .append("-log-")
                .append(index)
                .append("-")
                .append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?");

        while (sb.length() < LOG_MESSAGE_LENGTH) {
            sb.append("xyz123");
        }
        return sb.substring(0, LOG_MESSAGE_LENGTH);
    }
}