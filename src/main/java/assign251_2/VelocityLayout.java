package assign251_2;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class VelocityLayout extends Layout {
    private final VelocityEngine velocityEngine;
    private final VelocityContext context;
    private String template;

    public VelocityLayout() {
        this(null);
    }

    public VelocityLayout(String pattern) {
        this.template = pattern;
        this.context = new VelocityContext();
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
            context.put("m", message);  // Support $m variable [cite: 54]
            context.put("p", event.getLevel().toString());  // Support $p variable [cite: 56]
            context.put("c", event.getLoggerName());       // Support $c variable [cite: 50]
            context.put("t", Thread.currentThread().getName()); // Support $t variable [cite: 58]

            // Format date for $d variable [cite: 52]
            // Using a simple format for $d.toString() representation
            String formattedDate = new Date(event.getTimeStamp()).toString();
            context.put("d", formattedDate);

            // Support $n variable (platform line separator)
            context.put("n", System.lineSeparator());

            Writer writer = new StringWriter();
            velocityEngine.evaluate(context, writer, "VelocityLayout", template);
            // DO NOT append line separator automatically.
            // The template (e.g., "$m$n") must control this.
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
     * Set the layout pattern. [cite: 61]
     * @param pattern The Velocity template string.
     */
    public void setPattern(String pattern) {
        this.template = pattern;
    }
}