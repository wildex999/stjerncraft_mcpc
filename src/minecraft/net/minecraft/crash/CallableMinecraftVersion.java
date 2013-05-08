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
        if (cpw.mods.fml.relauncher.FMLInjectionData.obf151()) return "1.5.1"; // MCPC+
        return "1.5.2";
    }

    public Object call()
    {
        return this.minecraftVersion();
    }
}
