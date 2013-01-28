package net.minecraft.server.dedicated;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import java.util.regex.Pattern; // CraftBukkit

final class ConsoleLogFormatter extends Formatter
{
    /** The date format to use in the console log. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // CraftBukkit start - add color stripping, change constructor to take it
    private Pattern pattern = Pattern.compile("\\x1B\\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]");
    private boolean strip = false;

    ConsoleLogFormatter(boolean strip)
    {
        this.strip = strip;
    }

    // MCPC+ start - vanilla compatibility
    ConsoleLogFormatter()
    {
        this(false);
    }
    // MCPC+ end
    // CraftBukkit end

    public String format(LogRecord par1LogRecord)
    {
        StringBuilder var2 = new StringBuilder();
        var2.append(this.dateFormat.format(Long.valueOf(par1LogRecord.getMillis())));
        Level var3 = par1LogRecord.getLevel();

        if (var3 == Level.FINEST)
        {
            var2.append(" [FINEST] ");
        }
        else if (var3 == Level.FINER)
        {
            var2.append(" [FINER] ");
        }
        else if (var3 == Level.FINE)
        {
            var2.append(" [FINE] ");
        }
        else if (var3 == Level.INFO)
        {
            var2.append(" [INFO] ");
        }
        else if (var3 == Level.WARNING)
        {
            var2.append(" [WARNING] ");
        }
        else if (var3 == Level.SEVERE)
        {
            var2.append(" [SEVERE] ");
        }
        else     // CraftBukkit
        {
            var2.append(" [").append(var3.getLocalizedName()).append("] ");
        }

        var2.append(formatMessage(par1LogRecord)); // CraftBukkit
        var2.append('\n');
        Throwable var4 = par1LogRecord.getThrown();

        if (var4 != null)
        {
            StringWriter var5 = new StringWriter();
            var4.printStackTrace(new PrintWriter(var5));
            var2.append(var5.toString());
        }

        // CraftBukkit start - handle stripping color
        if (this.strip)
        {
            return this.pattern.matcher(var2.toString()).replaceAll("");
        }
        else
        {
            return var2.toString();
        }

        // CraftBukkit end
    }
}
