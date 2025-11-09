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
            Properties props = new Properties();
            props.setProperty("resource.loader", "class");
            props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            velocityEngine.init(props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize VelocityEngine", e);
        }
    }

    @Override
    public String format(LoggingEvent event) {
        String message = event.getRenderedMessage() == null ? "" : event.getRenderedMessage();
        String lineSeparator = System.lineSeparator();

        // Handle null template: return raw message with platform line separator
        if (template == null) {
            return message + lineSeparator;
        }

        try {
            context.put("m", message);  // Support $m variable (added)
            context.put("message", message);
            context.put("p", event.getLevel().toString());  // Support $p variable
            context.put("level", event.getLevel().toString());
            context.put("c", event.getLoggerName());       // Support $c variable
            context.put("logger", event.getLoggerName());
            context.put("t", Thread.currentThread().getName()); // Support $t variable
            context.put("thread", Thread.currentThread().getName());
            // Format date for $d variable (ISO-like for consistency with PatternLayout)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = dateFormat.format(new Date(event.getTimeStamp()));
            context.put("d", formattedDate);         // Support $d variable
            context.put("date", formattedDate);

            Writer writer = new StringWriter();
            velocityEngine.evaluate(context, writer, "VelocityLayout", template);
            return writer.toString() + lineSeparator;  // Use platform line separator
        } catch (Exception e) {
            // Handle invalid template: return raw message with platform line separator
            return message + lineSeparator;
        }
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    @Override
    public void activateOptions() {}

    public void setPattern(String pattern) {
        this.template = pattern;
    }
}