package assign251_2;

public interface MemAppenderMBean {
    String[] getLogMessages();
    long getDiscardedLogCount();
    long getEstimatedCacheSize();
}
