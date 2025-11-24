package assign251_2;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Custom Log4j Appender that stores LoggingEvents in memory.
 * Implements PDF requirements and Bonus MBean requirements.
 */
public class MemAppender extends AppenderSkeleton implements MemAppenderMBean {
    private static MemAppender instance;
    // Stores LoggingEvents as required by PDF
    private final List<LoggingEvent> logEvents;
    private int maxSize = 100;
    private long discardedLogCount = 0;
    private final ReentrantLock lock = new ReentrantLock();

    // Store MBean name for un-registration
    private ObjectName mbeanName = null;

    // Default constructor uses ArrayList
    private MemAppender() {
        this(new ArrayList<>()); // Default to ArrayList
    }

    // Constructor with custom list for Dependency Injection
    private MemAppender(List<LoggingEvent> customList) {
        this.logEvents = customList;
        registerMBean();
    }

    public static synchronized MemAppender getInstance() {
        if (instance == null) {
            instance = new MemAppender();
        }
        return instance;
    }

    // Overloaded getInstance for testing (inject LoggingEvent list)
    public static synchronized MemAppender getInstance(List<LoggingEvent> list) {
        // Close previous instance if it exists to ensure new list is used
        if (instance != null) {
            instance.close();
        }
        instance = new MemAppender(list);
        return instance;
    }

    // Method to reset singleton for testing
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.close();
        }
        instance = null;
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            this.mbeanName = new ObjectName("assign251_2:type=MemAppender");
            if (!mbs.isRegistered(mbeanName)) {
                mbs.registerMBean(this, mbeanName);
            }
        } catch (Exception e) {
            // In a real app, consider logging this to a fallback logger
            e.printStackTrace();
        }
    }

    // Method to unregister the MBean
    private void unregisterMBean() {
        if (this.mbeanName != null) {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                if (mbs.isRegistered(mbeanName)) {
                    mbs.unregisterMBean(mbeanName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.mbeanName = null; // Ensure we don't try to unregister twice
        }
    }


    @Override
    protected void append(LoggingEvent event) {
        // This method only adds the event. Formatting is done on demand.
        lock.lock();
        try {
            if (logEvents.size() >= maxSize) {
                discardedLogCount++;
                // Remove oldest log
                logEvents.remove(0);
            }
            logEvents.add(event);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        // Clean up resources
        lock.lock();
        try {
            logEvents.clear();
            discardedLogCount = 0;
        } finally {
            lock.unlock();
        }
        // CRITICAL: Unregister MBean when closing to avoid "InstanceAlreadyExists" errors in tests
        unregisterMBean();
    }

    @Override
    public boolean requiresLayout() {
        return true; // Yes, getEventStrings, printLogs, and MBean methods need a layout
    }

    // === PDF Requirement Methods ===

    /**
     * PDF Req 1a: Returns an unmodifiable list of the cached LoggingEvents.
     */
    public List<LoggingEvent> getCurrentLogs() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(logEvents));
        } finally {
            lock.unlock();
        }
    }

    /**
     * PDF Req 1b: Returns an unmodifiable list of formatted strings.
     * Formatting happens here, on demand.
     */
    public List<String> getEventStrings() {
        lock.lock();
        try {
            // Precondition check
            if (layout == null) {
                throw new IllegalStateException("Layout is required for getEventStrings()");
            }
            List<String> formattedMessages = logEvents.stream()
                    .map(layout::format)
                    .collect(Collectors.toList());
            return Collections.unmodifiableList(formattedMessages);
        } finally {
            lock.unlock();
        }
    }

    /**
     * PDF Req 1c: Prints formatted logs to console and clears the cache.
     */
    public void printLogs() {
        lock.lock();
        try {
            // Precondition check
            if (layout == null) {
                throw new IllegalStateException("Layout is required for printLogs()");
            }
            for (LoggingEvent event : logEvents) {
                // Use print, as layout (e.g., PatternLayout %n, VelocityLayout $n) handles newlines
                System.out.print(layout.format(event));
            }
            logEvents.clear();
            discardedLogCount = 0; // Cleared logs are not counted as discarded
        } finally {
            lock.unlock();
        }
    }

    // === JMX MBean (Bonus) Implementations ===

    /**
     * MBean Req 1: Get log messages as a String array.
     * Formats on demand.
     */
    @Override
    public String[] getLogMessages() {
        lock.lock();
        try {
            if (layout == null) {
                // Fallback to raw messages if no layout is set
                return logEvents.stream()
                        .map(LoggingEvent::getRenderedMessage)
                        .toArray(String[]::new);
            }
            return logEvents.stream()
                    .map(layout::format)
                    .toArray(String[]::new);
        } finally {
            lock.unlock();
        }
    }

    /**
     * MBean Req 3: Get discarded log count.
     */
    @Override
    public long getDiscardedLogCount() {
        return discardedLogCount;
    }

    /**
     * MBean Req 2: Get estimated cache size in bytes (total characters).
     */
    @Override
    public long getEstimatedCacheSize() {
        lock.lock();
        try {
            if (layout == null) {
                // Estimate based on raw message length
                return logEvents.stream()
                        .mapToLong(e -> e.getRenderedMessage().length())
                        .sum();
            }
            // Estimate based on formatted message byte length
            return logEvents.stream()
                    .mapToLong(e -> layout.format(e).getBytes().length)
                    .sum();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the appender to a clean state.
     */
    public void reset() {
        lock.lock();
        try {
            logEvents.clear();
            discardedLogCount = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the max cache size.
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}