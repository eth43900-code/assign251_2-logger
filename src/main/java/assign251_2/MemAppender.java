package assign251_2;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom Log4j appender that stores logs in memory.
 * Enforces Singleton pattern and supports dependency injection for its storage list.
 */
public class MemAppender extends AppenderSkeleton implements MemAppenderMBean {

    private static MemAppender instance;
    private List<LoggingEvent> events;
    private long maxSize = 1000; // Default max size
    private long discardedLogCount = 0;

    /**
     * Private constructor for Singleton pattern.
     * Initializes with a default ArrayList and registers MBean.
     */
    private MemAppender() {
        this.events = new ArrayList<>();
        // Optional: Register MBean here or lazily.
        registerMBean();
    }

    /**
     * Public accessor for the singleton instance.
     * @return The single instance of MemAppender.
     */
    public static synchronized MemAppender getInstance() {
        if (instance == null) {
            instance = new MemAppender();
        }
        return instance;
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("assign251_2:type=MemAppender");
            if (!mbs.isRegistered(name)) {
                StandardMBean mbean = new StandardMBean(this, MemAppenderMBean.class);
                mbs.registerMBean(mbean, name);
            }
        } catch (Exception e) {
            // Suppress errors during testing if MBean fails (e.g. duplicate registration in some test runners)
        }
    }

    /**
     * Dependency Injection for the log storage list.
     * @param list The list implementation to use for storing LoggingEvents.
     */
    public void setLogList(List<LoggingEvent> list) {
        if (list == null) {
            throw new IllegalArgumentException("Log list cannot be null");
        }
        if (this.events != null && !this.events.isEmpty()) {
            list.addAll(this.events);
        }
        this.events = list;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected void append(LoggingEvent event) {
        // Crucial: if it's closed, don't append.
        if (this.closed) {
            return;
        }

        while (events.size() >= maxSize) {
            events.remove(0);
            discardedLogCount++;
        }
        events.add(event);
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<LoggingEvent> getCurrentLogs() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public List<String> getEventStrings() {
        if (this.layout == null) {
            return Collections.emptyList();
        }
        List<String> strings = new ArrayList<>();
        for (LoggingEvent event : this.getCurrentLogs()) {
            strings.add(this.layout.format(event));
        }
        return Collections.unmodifiableList(strings);
    }

    public void printLogs() {
        if (this.layout == null) {
            // As per requirements, sensible check.
            return;
        }
        for (LoggingEvent event : this.getCurrentLogs()) {
            System.out.print(this.layout.format(event));
        }
        this.events.clear();
    }

    @Override
    public long getDiscardedLogCount() {
        return discardedLogCount;
    }

    // --- MBean Implementation ---

    @Override
    public String[] getLogMessages() {
        return getEventStrings().toArray(new String[0]);
    }

    @Override
    public long getEstimatedCacheSize() {
        long size = 0;
        for (LoggingEvent event : this.getCurrentLogs()) {
            if (event.getRenderedMessage() != null) {
                size += event.getRenderedMessage().length();
            }
        }
        return size;
    }

    /**
     * CRITICAL FOR TESTING: Resets the singleton state.
     * This must explicitly set 'closed = false' because AppenderSkeleton might have been closed by previous tests.
     */
    public void reset() {
        this.events = new ArrayList<>(); // Reset to default ArrayList
        this.discardedLogCount = 0;
        this.maxSize = 1000;
        this.layout = null;
        this.closed = false; // resurrect the appender if it was closed
    }
}