package assign251_2;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MemAppender extends AppenderSkeleton implements MemAppenderMBean {
    private static MemAppender instance;
    private final List<String> logMessages; // Store formatted log strings
    private int maxSize = 100;
    private long discardedLogCount = 0;
    private long estimatedCacheSize = 0;
    private final ReentrantLock lock = new ReentrantLock();

    private MemAppender() {
        this.logMessages = new ArrayList<>();
        registerMBean();
    }

    // Constructor with custom list (for testing)
    private MemAppender(List<String> customList) {
        this.logMessages = customList;
        registerMBean();
    }

    public static synchronized MemAppender getInstance() {
        if (instance == null) {
            instance = new MemAppender();
        }
        return instance;
    }

    // Overloaded getInstance for testing (inject String list)
    public static synchronized MemAppender getInstance(List<String> list) {
        if (instance == null) {
            instance = new MemAppender(list);
        }
        return instance;
    }

    // New method to reset singleton for testing
    public static synchronized void resetInstance() {
        instance = null;
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("assign251_2:type=MemAppender");
            if (!mbs.isRegistered(name)) {
                mbs.registerMBean(this, name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        lock.lock();
        try {
            String message = layout.format(event);
            if (logMessages.size() >= maxSize) {
                discardedLogCount++;
                // Remove oldest log
                String oldest = logMessages.remove(0);
                estimatedCacheSize -= oldest.getBytes().length;
            }
            logMessages.add(message);
            estimatedCacheSize += message.getBytes().length;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        // Clean up resources
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    // JMX method implementations
    @Override
    public String[] getLogMessages() {
        lock.lock();
        try {
            return logMessages.toArray(new String[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getDiscardedLogCount() {
        return discardedLogCount;
    }

    @Override
    public long getEstimatedCacheSize() {
        return estimatedCacheSize;
    }

    public void reset() {
        lock.lock();
        try {
            logMessages.clear();
            discardedLogCount = 0;
            estimatedCacheSize = 0;
        } finally {
            lock.unlock();
        }
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    // New method: Get current log list for testing
    public List<String> getCurrentLogs() {
        lock.lock();
        try {
            return new ArrayList<>(logMessages);
        } finally {
            lock.unlock();
        }
    }

    // Method for testing
    public List<String> getEventStrings() {
        return getCurrentLogs();
    }
}