package net.minecraft.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;

import java.io.File; // CraftBukkit

public class LogAgent implements ILogAgent
{
    private final Logger field_98242_a;
    private final String field_98240_b;
    private final String field_98241_c;
    private final String field_98239_d;
    public static Logger global = Logger.getLogger(""); // CraftBukkit

    public LogAgent(String par1Str, String par2Str, String par3Str)
    {
        this.field_98242_a = Logger.getLogger(par1Str);
        this.field_98241_c = par1Str;
        this.field_98239_d = par2Str;
        this.field_98240_b = par3Str;
        this.func_98238_b();
    }

    private void func_98238_b()
    {
        this.field_98242_a.setUseParentHandlers(false);
        Handler[] ahandler = this.field_98242_a.getHandlers();
        int i = ahandler.length;

        for (int j = 0; j < i; ++j)
        {
            Handler handler = ahandler[j];
            this.field_98242_a.removeHandler(handler);
        }

        LogFormatter logformatter = new LogFormatter(this, (LogAgentINNER1)null);
        // CraftBukkit start
        MinecraftServer server = MinecraftServer.getServer();
        ConsoleHandler consolehandler = new org.bukkit.craftbukkit.util.TerminalConsoleHandler(server.reader);
        // CraftBukkit end
        consolehandler.setFormatter(logformatter);
        this.field_98242_a.addHandler(consolehandler);

        // CraftBukkit start
        for (java.util.logging.Handler handler : global.getHandlers())
        {
            global.removeHandler(handler);
        }

        consolehandler.setFormatter(new org.bukkit.craftbukkit.util.ShortConsoleLogFormatter(server));
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
            this.field_98242_a.addHandler(filehandler);
            global.addHandler(filehandler); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.field_98242_a.log(Level.WARNING, "Failed to log " + this.field_98241_c + " to " + this.field_98240_b, exception);
        }
    }

    public Logger func_98076_a()
    {
        return this.field_98242_a;
    }

    public void logInfo(String par1Str)
    {
        this.field_98242_a.log(Level.INFO, par1Str);
    }

    public void logWarning(String par1Str)
    {
        this.field_98242_a.log(Level.WARNING, par1Str);
    }

    public void func_98231_b(String par1Str, Object ... par2ArrayOfObj)
    {
        this.field_98242_a.log(Level.WARNING, par1Str, par2ArrayOfObj);
    }

    public void func_98235_b(String par1Str, Throwable par2Throwable)
    {
        this.field_98242_a.log(Level.WARNING, par1Str, par2Throwable);
    }

    public void func_98232_c(String par1Str)
    {
        this.field_98242_a.log(Level.SEVERE, par1Str);
    }

    public void func_98234_c(String par1Str, Throwable par2Throwable)
    {
        this.field_98242_a.log(Level.SEVERE, par1Str, par2Throwable);
    }

    static String func_98237_a(LogAgent par0LogAgent)
    {
        return par0LogAgent.field_98239_d;
    }
}
