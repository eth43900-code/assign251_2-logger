package assign251_2;

/**
 * MBean interface for JMX monitoring of MemAppender.
 */
public interface MemAppenderMBean {
    /**
     * Gets the current log messages in memory.
     * @return Array of formatted log messages.
     */
    String[] getLogMessages();

    /**
     * Gets the estimated size of cached logs in characters.
     * @return Total characters of messages in memory.
     */
    long getEstimatedCacheSize();

    /**
     * Gets the count of logs discarded due to maxSize limit.
     * @return Number of discarded logs.
     */
    long getDiscardedLogCount();
}
