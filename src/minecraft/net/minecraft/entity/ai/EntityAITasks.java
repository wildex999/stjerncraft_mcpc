package net.minecraft.entity.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.profiler.Profiler;
import org.bukkit.craftbukkit.util.UnsafeList; // CraftBukkit

public class EntityAITasks
{
    public List taskEntries = new ArrayList(); // MCPC+  private->public
    private List executingTaskEntries = new ArrayList();

    /** Instance of Profiler. */
    private final Profiler theProfiler;
    private int field_75778_d = 0;
    private int field_75779_e = 3;

    public EntityAITasks(Profiler par1Profiler)
    {
        this.theProfiler = par1Profiler;
    }

    public void addTask(int par1, EntityAIBase par2EntityAIBase)
    {
        this.taskEntries.add(new EntityAITaskEntry(this, par1, par2EntityAIBase));
    }

    public void func_85156_a(EntityAIBase par1EntityAIBase)
    {
        Iterator var2 = this.taskEntries.iterator();

        while (var2.hasNext())
        {
            EntityAITaskEntry var3 = (EntityAITaskEntry)var2.next();
            EntityAIBase var4 = var3.action;

            if (var4 == par1EntityAIBase)
            {
                if (this.executingTaskEntries.contains(var3))
                {
                    var4.resetTask();
                    this.executingTaskEntries.remove(var3);
                }

                var2.remove();
            }
        }
    }

    public void onUpdateTasks()
    {
        // ArrayList arraylist = new ArrayList(); // CraftBukkit - remove usage
        Iterator var1;
        EntityAITaskEntry var2;

        if (this.field_75778_d++ % this.field_75779_e == 0)
        {
            var1 = this.taskEntries.iterator();

            while (var1.hasNext())
            {
                var2 = (EntityAITaskEntry) var1.next();
                boolean var3 = this.executingTaskEntries.contains(var2);

                if (var3)
                {
                    if (this.func_75775_b(var2) && this.func_75773_a(var2))
                    {
                        continue;
                    }

                    var2.action.resetTask();
                    this.executingTaskEntries.remove(var2);
                }

                if (this.func_75775_b(var2) && var2.action.shouldExecute())
                {
                    // CraftBukkit start - call method now instead of queueing
                    // arraylist.add(pathfindergoalselectoritem);
                    var2.action.startExecuting();
                    // CraftBukkit end
                    this.executingTaskEntries.add(var2);
                }
            }
        }
        else
        {
            var1 = this.executingTaskEntries.iterator();

            while (var1.hasNext())
            {
                var2 = (EntityAITaskEntry) var1.next();

                if (!var2.action.continueExecuting())
                {
                    var2.action.resetTask();
                    var1.remove();
                }
            }
        }

        this.theProfiler.startSection("goalStart");
        // CraftBukkit start - removed usage of arraylist
        /*iterator = arraylist.iterator();

        while (iterator.hasNext()) {
            pathfindergoalselectoritem = (PathfinderGoalSelectorItem) iterator.next();
            this.c.a(pathfindergoalselectoritem.a.getClass().getSimpleName());
            pathfindergoalselectoritem.a.c();
            this.c.b();
        }*/
        // CraftBukkit end
        this.theProfiler.endSection();
        this.theProfiler.startSection("goalTick");
        var1 = this.executingTaskEntries.iterator();

        while (var1.hasNext())
        {
            var2 = (EntityAITaskEntry) var1.next();
            var2.action.updateTask();
        }

        this.theProfiler.endSection();
    }

    private boolean func_75773_a(EntityAITaskEntry par1EntityAITaskEntry)
    {
        this.theProfiler.startSection("canContinue");
        boolean var2 = par1EntityAITaskEntry.action.continueExecuting();
        this.theProfiler.endSection();
        return var2;
    }

    private boolean func_75775_b(EntityAITaskEntry par1EntityAITaskEntry)
    {
        this.theProfiler.startSection("canUse");
        Iterator var2 = this.taskEntries.iterator();

        while (var2.hasNext())
        {
            EntityAITaskEntry var3 = (EntityAITaskEntry)var2.next();

            if (var3 != par1EntityAITaskEntry)
            {
                if (par1EntityAITaskEntry.priority >= var3.priority)
                {
                    if (this.executingTaskEntries.contains(var3) && !this.areTasksCompatible(par1EntityAITaskEntry, var3))
                    {
                        this.theProfiler.endSection();
                        return false;
                    }
                }
                else if (this.executingTaskEntries.contains(var3) && !var3.action.isContinuous())
                {
                    this.theProfiler.endSection();
                    return false;
                }
            }
        }

        this.theProfiler.endSection();
        return true;
    }

    /**
     * Returns whether two EntityAITaskEntries can be executed concurrently
     */
    private boolean areTasksCompatible(EntityAITaskEntry par1EntityAITaskEntry, EntityAITaskEntry par2EntityAITaskEntry)
    {
        return (par1EntityAITaskEntry.action.getMutexBits() & par2EntityAITaskEntry.action.getMutexBits()) == 0;
    }
}
