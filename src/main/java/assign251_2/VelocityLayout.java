package assign251_2;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import java.io.StringWriter;
import java.util.Date;

/**
 * Custom Log4j Layout using Apache Velocity template engine.
 * Supported variables: $c, $d, $m, $p, $t, $n
 */
public class VelocityLayout extends Layout {

    private String pattern;

    static {
        // Initialize Velocity once
        try {
            java.util.Properties p = new java.util.Properties();
            // Use StringResourceLoader if needed for more complex templates,
            // but standard initialization works for inline evaluation.
            // Turning off logging for velocity itself to keep output clean
            p.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
            Velocity.init(p);
        } catch (Exception e) {
            System.err.println("Failed to initialize Velocity: " + e.getMessage());
        }
    }

    /**
     * Default constructor with a default pattern.
     */
    public VelocityLayout() {
        this("[%p] $c $d: $m$n");
    }

    /**
     * Constructor with a custom pattern.
     * @param pattern The velocity template pattern.
     */
    public VelocityLayout(String pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String format(LoggingEvent event) {
        VelocityContext context = new VelocityContext();
        // Map required variables
        context.put("c", event.getLoggerName());
        context.put("d", new Date(event.getTimeStamp()).toString());
        context.put("m", event.getRenderedMessage());
        context.put("p", event.getLevel().toString());
        context.put("t", event.getThreadName());
        context.put("n", System.lineSeparator());

        StringWriter sw = new StringWriter();
        try {
            Velocity.evaluate(context, sw, "VelocityLayout", pattern);
        } catch (Exception e) {
            return "Velocity Error: " + e.getMessage() + System.lineSeparator();
        }
        return sw.toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    @Override
    public void activateOptions() {
        // No specific options to activate
    }
}
