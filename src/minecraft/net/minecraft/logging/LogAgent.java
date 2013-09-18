package net.minecraft.logging;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;

import java.io.File; // CraftBukkit
// Forge start
import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
// Forge end

public class LogAgent implements ILogAgent
{
    private final Logger serverLogger;
    private final String logFile;
    private final String loggerName;
    private final String loggerPrefix;
    public static Logger global = Logger.getLogger(""); // CraftBukkit

    public LogAgent(String par1Str, String par2Str, String par3Str)
    {
        this.serverLogger = Logger.getLogger(par1Str);
        this.loggerName = par1Str;
        this.loggerPrefix = par2Str;
        this.logFile = par3Str;
        this.setupLogger();
    }

    /**
     * Sets up the logger for usage.
     */
    private void setupLogger()
    {
        this.serverLogger.setUseParentHandlers(false);
        //this.serverLogger.setParent(FMLLog.getLogger()); // MCPC+ - do not send to FML log, only CB
        Handler[] ahandler = this.serverLogger.getHandlers();
        int i = ahandler.length;

        for (int j = 0; j < i; ++j)
        {
            Handler handler = ahandler[j];
            this.serverLogger.removeHandler(handler);
        }

        LogFormatter logformatter = new LogFormatter(this, (LogAgentINNER1)null);
        // CraftBukkit start
        MinecraftServer server = MinecraftServer.getServer();
        ConsoleHandler consolehandler = new org.bukkit.craftbukkit.util.TerminalConsoleHandler(server.reader);
        // CraftBukkit end
        consolehandler.setFormatter(logformatter);
        this.serverLogger.addHandler(consolehandler);

        // CraftBukkit start
        for (java.util.logging.Handler handler : global.getHandlers())
        {
            global.removeHandler(handler);
        }

        // MCPC+ start
        if (!FMLRelaunchLog.useOnlyThisLogger)
        {
            consolehandler.setFormatter(new org.bukkit.craftbukkit.util.ShortConsoleLogFormatter(server));
        }
        else
        {
            consolehandler.setFormatter(new Formatter()
            {
                @Override
                public String format(final LogRecord record)
                {
                    return MessageFormat.format(record.getMessage(), record.getParameters()).concat("\n");
                }
            });
        }
        // MCPC+ end
        global.addHandler(consolehandler);
        // CraftBukkit end

        try
        {
            // CraftBukkit start
            String pattern = (String) server.options.valueOf("log-pattern");
            // We have to parse the pattern ourself so we can create directories as needed (java #6244047)
            String tmpDir = System.getProperty("java.io.tmpdir");
            String homeDir = System.getProperty("user.home");

            if (tmpDir == null)
            {
                tmpDir = homeDir;
            }

            // We only care about parsing for directories, FileHandler can do file names by itself
            File parent = new File(pattern).getParentFile();
            StringBuilder fixedPattern = new StringBuilder();
            String parentPath = "";

            if (parent != null)
            {
                parentPath = parent.getPath();
            }

            int j = 0;

            while (j < parentPath.length())
            {
                char ch = parentPath.charAt(j);
                char ch2 = 0;

                if (j + 1 < parentPath.length())
                {
                    ch2 = Character.toLowerCase(pattern.charAt(j + 1));
                }

                if (ch == '%')
                {
                    if (ch2 == 'h')
                    {
                        j += 2;
                        fixedPattern.append(homeDir);
                        continue;
                    }
                    else if (ch2 == 't')
                    {
                        j += 2;
                        fixedPattern.append(tmpDir);
                        continue;
                    }
                    else if (ch2 == '%')
                    {
                        // Even though we don't care about this we have to skip it to avoid matching %%t
                        j += 2;
                        fixedPattern.append("%%");
                        continue;
                    }
                    else if (ch2 != 0)
                    {
                        throw new java.io.IOException("log-pattern can only use %t and %h for directories, got %" + ch2);
                    }
                }

                fixedPattern.append(ch);
                j++;
            }

            // Try to create needed parent directories
            parent = new File(fixedPattern.toString());

            if (parent != null)
            {
                parent.mkdirs();
            }

            int limit = (Integer) server.options.valueOf("log-limit");
            int count = (Integer) server.options.valueOf("log-count");
            boolean append = (Boolean) server.options.valueOf("log-append");
            FileHandler filehandler = new FileHandler(pattern, limit, count, append);
            // CraftBukkit end
            filehandler.setFormatter(logformatter);
            this.serverLogger.addHandler(filehandler);
            global.addHandler(filehandler); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.serverLogger.log(Level.WARNING, "Failed to log " + this.loggerName + " to " + this.logFile, exception);
        }
    }

    public void logInfo(String par1Str)
    {
        this.serverLogger.log(Level.INFO, par1Str);
    }

    @SideOnly(Side.SERVER)
    public Logger getServerLogger()
    {
        return this.serverLogger;
    }

    public void logWarning(String par1Str)
    {
        this.serverLogger.log(Level.WARNING, par1Str);
    }

    public void logWarningFormatted(String par1Str, Object ... par2ArrayOfObj)
    {
        this.serverLogger.log(Level.WARNING, par1Str, par2ArrayOfObj);
    }

    public void logWarningException(String par1Str, Throwable par2Throwable)
    {
        this.serverLogger.log(Level.WARNING, par1Str, par2Throwable);
    }

    public void logSevere(String par1Str)
    {
        this.serverLogger.log(Level.SEVERE, par1Str);
    }

    public void logSevereException(String par1Str, Throwable par2Throwable)
    {
        this.serverLogger.log(Level.SEVERE, par1Str, par2Throwable);
    }

    static String func_98237_a(LogAgent par0LogAgent)
    {
        return par0LogAgent.loggerPrefix;
    }
}
