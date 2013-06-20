package net.minecraft.crash;

import java.util.concurrent.Callable;

public class CallableMinecraftVersion implements Callable
{
    /** Reference to the CrashReport object. */
    final CrashReport theCrashReport;

    public CallableMinecraftVersion(CrashReport par1CrashReport)
    {
        this.theCrashReport = par1CrashReport;
    }

    /**
     * The current version of Minecraft
     */
    public String minecraftVersion()
    {
        // MCPC+ start - compatibility mode detection + overwrite detection
        try {
            if (cpw.mods.fml.relauncher.FMLInjectionData.obf151()) return "1.5.1";
        } catch (NoSuchMethodError ex) {
            System.out.println("SERIOUS ERROR OCCURRED: Critical classes were overwritten! This is an invalid installation. MCPC+ cannot continue. Please retry with a clean, unmodified, MCPC+ server jar.");
            System.exit(1);
        }
        // MCPC+ end
        return "1.5.2";
    }

    public Object call()
    {
        return this.minecraftVersion();
    }
}
