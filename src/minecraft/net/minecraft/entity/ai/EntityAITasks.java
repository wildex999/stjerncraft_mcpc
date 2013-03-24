package net.minecraft.entity.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.profiler.Profiler;

import org.bukkit.craftbukkit.util.UnsafeList; // CraftBukkit

public class EntityAITasks
{
    // CraftBukkit start - ArrayList -> UnsafeList

    /** A list of EntityAITaskEntrys in EntityAITasks. */
    public List taskEntries = new ArrayList(); // MCPC+ - private->public, UnsafeList -> ArrayList
    
    /** A list of EntityAITaskEntrys that are currently being executed. */
    private List executingTaskEntries = new UnsafeList();
    // CraftBukkit end

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

    /**
     * removes the indicated task from the entity's AI tasks.
     */
    public void removeTask(EntityAIBase par1EntityAIBase)
    {
        Iterator iterator = this.taskEntries.iterator();

        while (iterator.hasNext())
        {
            EntityAITaskEntry entityaitaskentry = (EntityAITaskEntry)iterator.next();
            EntityAIBase entityaibase1 = entityaitaskentry.action;

            if (entityaibase1 == par1EntityAIBase)
            {
                if (this.executingTaskEntries.contains(entityaitaskentry))
                {
                    entityaibase1.resetTask();
                    this.executingTaskEntries.remove(entityaitaskentry);
                }

                iterator.remove();
            }
        }
    }

    public void onUpdateTasks()
    {
        // ArrayList arraylist = new ArrayList(); // CraftBukkit - remove usage
        Iterator iterator;
        EntityAITaskEntry entityaitaskentry;

        if (this.field_75778_d++ % this.field_75779_e == 0)
        {
            iterator = this.taskEntries.iterator();

            while (iterator.hasNext())
            {
                entityaitaskentry = (EntityAITaskEntry)iterator.next();
                boolean flag = this.executingTaskEntries.contains(entityaitaskentry);

                if (flag)
                {
                    if (this.canUse(entityaitaskentry) && this.canContinue(entityaitaskentry))
                    {
                        continue;
                    }

                    entityaitaskentry.action.resetTask();
                    this.executingTaskEntries.remove(entityaitaskentry);
                }

                if (this.canUse(entityaitaskentry) && entityaitaskentry.action.shouldExecute())
                {
                    // CraftBukkit start - call method now instead of queueing
                    // arraylist.add(entityaitaskentry);
                    entityaitaskentry.action.startExecuting();
                    // CraftBukkit end
                    this.executingTaskEntries.add(entityaitaskentry);
                }
            }
        }
        else
        {
            iterator = this.executingTaskEntries.iterator();

            while (iterator.hasNext())
            {
                entityaitaskentry = (EntityAITaskEntry)iterator.next();

                if (!entityaitaskentry.action.continueExecuting())
                {
                    entityaitaskentry.action.resetTask();
                    iterator.remove();
                }
            }
        }

        this.theProfiler.startSection("goalStart");
        // CraftBukkit start - removed usage of arraylist
        /*iterator = arraylist.iterator();

        while (iterator.hasNext()) {
            entityaitaskentry = (PathfinderGoalSelectorItem) iterator.next();
            this.c.a(entityaitaskentry.a.getClass().getSimpleName());
            entityaitaskentry.a.c();
            this.c.b();
        }*/
        // CraftBukkit end
        this.theProfiler.endSection();
        this.theProfiler.startSection("goalTick");
        iterator = this.executingTaskEntries.iterator();

        while (iterator.hasNext())
        {
            entityaitaskentry = (EntityAITaskEntry)iterator.next();
            entityaitaskentry.action.updateTask();
        }

        this.theProfiler.endSection();
    }

    /**
     * Determine if a specific AI Task should continue being executed.
     */
    private boolean canContinue(EntityAITaskEntry par1EntityAITaskEntry)
    {
        this.theProfiler.startSection("canContinue");
        boolean flag = par1EntityAITaskEntry.action.continueExecuting();
        this.theProfiler.endSection();
        return flag;
    }

    /**
     * Determine if a specific AI Task can be executed, which means that all running higher (= lower int value) priority
     * tasks are compatible with it or all lower priority tasks can be interrupted.
     */
    private boolean canUse(EntityAITaskEntry par1EntityAITaskEntry)
    {
        this.theProfiler.startSection("canUse");
        Iterator iterator = this.taskEntries.iterator();

        while (iterator.hasNext())
        {
            EntityAITaskEntry entityaitaskentry1 = (EntityAITaskEntry)iterator.next();

            if (entityaitaskentry1 != par1EntityAITaskEntry)
            {
                if (par1EntityAITaskEntry.priority >= entityaitaskentry1.priority)
                {
                    // CraftBukkit - switch order
                    if (!this.areTasksCompatible(par1EntityAITaskEntry, entityaitaskentry1) && this.executingTaskEntries.contains(entityaitaskentry1))
                    {
                        this.theProfiler.endSection();
                        //((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse // MCPC+
                        return false;
                    }

                    // CraftBukkit - switch order
                }
                else if (!entityaitaskentry1.action.isInterruptible() && this.executingTaskEntries.contains(entityaitaskentry1))
                {
                    this.theProfiler.endSection();
                    //((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse // MCPC+
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
