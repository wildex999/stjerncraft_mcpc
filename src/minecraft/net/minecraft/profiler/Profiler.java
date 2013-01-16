package net.minecraft.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// CraftBukkit start - strip down to empty
public class Profiler
{
    /** Flag profiling enabled */
    public boolean profilingEnabled = false;

    public final void clearProfiling() { }
    public final void startSection(String par1Str) { }
    public final void endSection() { }
    public final List getProfilingData(String par1Str)
    {
        return null;
    }
    public final void endStartSection(String par1Str) { }
    public final String getNameOfLastSection()
    {
        return null;
    }
}
// CraftBukkit end
