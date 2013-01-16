package net.minecraft.server.dedicated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionSet; // CraftBukkit

public class PropertyManager
{
    /** Reference to the logger. */
    public static Logger logger = Logger.getLogger("Minecraft");
    public Properties properties = new Properties(); // CraftBukkit - private -> public
    private File associatedFile;

    public PropertyManager(File par1File)
    {
        this.associatedFile = par1File;

        if (par1File.exists())
        {
            FileInputStream var2 = null;

            try
            {
                var2 = new FileInputStream(par1File);
                this.properties.load(var2);
            }
            catch (Exception var12)
            {
                logger.log(Level.WARNING, "Failed to load " + par1File, var12);
                this.logMessageAndSave();
            }
            finally
            {
                if (var2 != null)
                {
                    try
                    {
                        var2.close();
                    }
                    catch (IOException var11)
                    {
                        ;
                    }
                }
            }
        }
        else
        {
            logger.log(Level.WARNING, par1File + " does not exist");
            this.logMessageAndSave();
        }
    }

    // CraftBukkit start
    private OptionSet options = null;

    public PropertyManager(final OptionSet options)
    {
        this((File) options.valueOf("config"));
        this.options = options;
    }

    private <T> T getOverride(String name, T value)
    {
        if ((this.options != null) && (this.options.has(name)))
        {
            return (T) this.options.valueOf(name);
        }

        return value;
    }
    // CraftBukkit end

    /**
     * logs an info message then calls saveSettingsToFile Yes this appears to be a potential stack overflow - these 2
     * functions call each other repeatdly if an exception occurs.
     */
    public void logMessageAndSave()
    {
        logger.log(Level.INFO, "Generating new properties file");
        this.saveProperties();
    }

    /**
     * Writes the properties to the properties file.
     */
    public void saveProperties()
    {
        FileOutputStream var1 = null;

        try
        {
            // CraftBukkit start - Don't attempt writing to file if it's read only
            if (this.associatedFile.exists() && !this.associatedFile.canWrite())
            {
                return;
            }

            // CraftBukkit end
            var1 = new FileOutputStream(this.associatedFile);
            this.properties.store(var1, "Minecraft server properties");
        }
        catch (Exception var11)
        {
            logger.log(Level.WARNING, "Failed to save " + this.associatedFile, var11);
            this.logMessageAndSave();
        }
        finally
        {
            if (var1 != null)
            {
                try
                {
                    var1.close();
                }
                catch (IOException var10)
                {
                    ;
                }
            }
        }
    }

    /**
     * Returns this PropertyManager's file object used for property saving.
     */
    public File getPropertiesFile()
    {
        return this.associatedFile;
    }

    /**
     * Gets a property. If it does not exist, set it to the specified value.
     */
    public String getProperty(String par1Str, String par2Str)
    {
        if (!this.properties.containsKey(par1Str))
        {
            this.properties.setProperty(par1Str, par2Str);
            this.saveProperties();
        }

        return this.getOverride(par1Str, this.properties.getProperty(par1Str, par2Str)); // CraftBukkit
    }

    /**
     * Gets an integer property. If it does not exist, set it to the specified value.
     */
    public int getIntProperty(String par1Str, int par2)
    {
        try
        {
            return this.getOverride(par1Str, Integer.parseInt(this.getProperty(par1Str, "" + par2))); // CraftBukkit
        }
        catch (Exception var4)
        {
            this.properties.setProperty(par1Str, "" + par2);
            return this.getOverride(par1Str, par2); // CraftBukkit
        }
    }

    /**
     * Gets a boolean property. If it does not exist, set it to the specified value.
     */
    public boolean getBooleanProperty(String par1Str, boolean par2)
    {
        try
        {
            return this.getOverride(par1Str, Boolean.parseBoolean(this.getProperty(par1Str, "" + par2))); // CraftBukkit
        }
        catch (Exception var4)
        {
            this.properties.setProperty(par1Str, "" + par2);
            return this.getOverride(par1Str, par2); // CraftBukkit
        }
    }

    /**
     * Saves an Object with the given property name.
     */
    public void setProperty(String par1Str, Object par2Obj)
    {
        this.properties.setProperty(par1Str, "" + par2Obj);
    }
}
