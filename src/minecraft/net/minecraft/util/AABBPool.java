package net.minecraft.util;

import java.util.ArrayList;
import java.util.List;

public class AABBPool
{
    /**
     * Maximum number of times the pool can be "cleaned" before the list is shrunk
     */
    private final int maxNumCleans;

    /**
     * Number of Pool entries to remove when cleanPool is called maxNumCleans times.
     */
    private final int numEntriesToRemove;

    /** List of AABB stored in this Pool */
    private final List listAABB = new ArrayList();

    /** Next index to use when adding a Pool Entry. */
    private int nextPoolIndex = 0;

    /**
     * Largest index reached by this Pool (can be reset to 0 upon calling cleanPool)
     */
    private int maxPoolIndex = 0;

    /** Number of times this Pool has been cleaned */
    private int numCleans = 0;

    public AABBPool(int par1, int par2)
    {
        this.maxNumCleans = par1;
        this.numEntriesToRemove = par2;
    }

    /**
     * Creates a new AABB, or reuses one that's no longer in use. Parameters: minX, minY, minZ, maxX, maxY, maxZ. AABBs
     * returned from this function should only be used for one frame or tick, as after that they will be reused.
     */
    public AxisAlignedBB getAABB(double par1, double par3, double par5, double par7, double par9, double par11)
    {
        // CraftBukkit - don't pool objects indefinitely if thread doesn't adhere to contract
        if (this.numCleans == 0)
        {
            return new AxisAlignedBB(par1, par3, par5, par7, par9, par11);
        }

        AxisAlignedBB axisalignedbb;

        if (this.nextPoolIndex >= this.listAABB.size())
        {
            axisalignedbb = new AxisAlignedBB(par1, par3, par5, par7, par9, par11);
            this.listAABB.add(axisalignedbb);
        }
        else
        {
            axisalignedbb = (AxisAlignedBB)this.listAABB.get(this.nextPoolIndex);
            axisalignedbb.setBounds(par1, par3, par5, par7, par9, par11);
        }

        ++this.nextPoolIndex;
        return axisalignedbb;
    }

    /**
     * Marks the pool as "empty", starting over when adding new entries. If this is called maxNumCleans times, the list
     * size is reduced
     */
    public void cleanPool()
    {
        if (this.nextPoolIndex > this.maxPoolIndex)
        {
            this.maxPoolIndex = this.nextPoolIndex;
        }

        // CraftBukkit start - Intelligent cache
        if ((this.numCleans++ & 0xff) == 0)
        {
            int newSize = this.listAABB.size() - (this.listAABB.size() >> 3);

            // newSize will be 87.5%, but if we were not in that range, we clear some of the cache
            if (newSize > this.maxPoolIndex)
            {
                // Work down from size() to prevent insane array copies
                for (int i = this.listAABB.size() - 1; i > newSize; i--)
                {
                    this.listAABB.remove(i);
                }
            }

            this.maxPoolIndex = 0;
            // this.resizeTime = 0; // We do not reset to zero; it doubles for a flag
        }

        // CraftBukkit end
        this.nextPoolIndex = 0;
    }

    public int getlistAABBsize()
    {
        return this.listAABB.size();
    }

    public int getnextPoolIndex()
    {
        return this.nextPoolIndex;
    }
}
