package assign251_2;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;

public class VelocityLayout extends Layout {
    private final VelocityEngine velocityEngine;
    // Removed class-level VelocityContext to ensure thread safety
    private String template;

    public VelocityLayout() {
        this(null);
    }

    public VelocityLayout(String pattern) {
        this.template = pattern;
        this.velocityEngine = new VelocityEngine();
        try {
            // Configure Velocity to use a simple string-based logger
            // and avoid classloader issues
            Properties props = new Properties();
            props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
            velocityEngine.init(props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize VelocityEngine", e);
        }
    }

    @Override
    public String format(LoggingEvent event) {
        String message = event.getRenderedMessage() == null ? "" : event.getRenderedMessage();

        // Handle null template: return raw message
        if (template == null) {
            return message;
        }

        try {
            // CRITICAL FIX: Create context inside format() to be thread-safe.
            // Logging events can happen concurrently.
            VelocityContext context = new VelocityContext();

            context.put("m", message);  // Support $m variable
            context.put("p", event.getLevel().toString());  // Support $p variable
            context.put("c", event.getLoggerName());       // Support $c variable
            context.put("t", Thread.currentThread().getName()); // Support $t variable

            // Format date for $d variable
            String formattedDate = new Date(event.getTimeStamp()).toString();
            context.put("d", formattedDate);

            // Support $n variable (platform line separator)
            context.put("n", System.lineSeparator());

            Writer writer = new StringWriter();
            velocityEngine.evaluate(context, writer, "VelocityLayout", template);

            return writer.toString();
        } catch (Exception e) {
            // Handle invalid template: return raw message
            return message;
        }
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    @Override
    public void activateOptions() {}

    /**
     * Set the layout pattern.
     * @param pattern The Velocity template string.
     */
    public void setPattern(String pattern) {
        this.template = pattern;
    }
}