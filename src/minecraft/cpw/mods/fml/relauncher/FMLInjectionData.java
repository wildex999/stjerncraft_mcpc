/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.relauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class FMLInjectionData
{
    static File minecraftHome;
    static String major;
    static String minor;
    static String rev;
    static String build;
    static String mccversion;
    static String mcpversion;
    static String deobfuscationDataHash;

    public static List<String> containers = new ArrayList<String>();

    static void build(File mcHome, RelaunchClassLoader classLoader)
    {
        minecraftHome = mcHome;
        InputStream stream = classLoader.getResourceAsStream("fmlversion.properties");
        Properties properties = new Properties();

        if (stream != null)
        {
            try
            {
                properties.load(stream);
            }
            catch (IOException ex)
            {
                FMLRelaunchLog.log(Level.SEVERE, ex, "Could not get FML version information - corrupted installation detected!");
            }
        }

        major = properties.getProperty("fmlbuild.major.number", "missing");
        minor = properties.getProperty("fmlbuild.minor.number", "missing");
        rev = properties.getProperty("fmlbuild.revision.number", "missing");
        build = properties.getProperty("fmlbuild.build.number", "missing");
        mccversion = properties.getProperty("fmlbuild.mcversion", "missing");
        mcpversion = properties.getProperty("fmlbuild.mcpversion", "missing");
        deobfuscationDataHash = properties.getProperty("fmlbuild.deobfuscation.hash","deadbeef");
        // MCPC+ start - obfuscation for 1.5.1
        if (obf151()) {
            mccversion = "1.5.1";
            deobfuscationDataHash = "22e221a0d89516c1f721d6cab056a7e37471d0a6"; // final 1.5.1 deobf, Forge 7.7.2.682
            FMLRelaunchLog.log(Level.INFO, "MCPC+ enabling 1.5.1 obfuscation compatibility mode");
        }
    }

    private static boolean isObfuscated151;
    private static boolean checkedObfuscated151 = false;

    public static boolean obf151() {
        if (!checkedObfuscated151) {
            // This class is reobfuscated (original name: MCPCCompatibilityMarker) in the 1.5.1-compatibility build
            // so we can detect it here. Load the proper 1.5.1 deobfuscation data instead of 1.5.2.
            isObfuscated151 = classExists("cpw.mods.fml.relauncher.MCPCCompatibilityMarker151");
            checkedObfuscated151 = true;
        }

        return isObfuscated151;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return false;
        }
        return true;
    }
    // MCPC+ end

    static String debfuscationDataName()
    {
        return "deobfuscation_data_"+mccversion+".zip";
    }
    public static Object[] data()
    {
        return new Object[] { major, minor, rev, build, mccversion, mcpversion, minecraftHome, containers };
    }
}
