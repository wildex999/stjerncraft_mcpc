package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockRedstoneWire extends Block
{
    /**
     * When false, power transmission methods do not look at other redstone wires. Used internally during
     * updateCurrentStrength.
     */
    private boolean wiresProvidePower = true;
    private Set blocksNeedingUpdate = new java.util.LinkedHashSet(); // CraftBukkit - HashSet -> LinkedHashSet

    public BlockRedstoneWire(int par1, int par2)
    {
        super(par1, par2, Material.circuits);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public int getBlockTextureFromSideAndMetadata(int par1, int par2)
    {
        return this.blockIndexInTexture;
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        return null;
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 5;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(IBlockAccess par1IBlockAccess, int par2, int par3, int par4)
    {
        return 8388608;
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        return par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4) || par1World.getBlockId(par2, par3 - 1, par4) == Block.glowStone.blockID;
    }

    /**
     * Sets the strength of the wire current (0-15) for this block based on neighboring blocks and propagates to
     * neighboring redstone wires
     */
    private void updateAndPropagateCurrentStrength(World par1World, int par2, int par3, int par4)
    {
        this.calculateCurrentChanges(par1World, par2, par3, par4, par2, par3, par4);
        ArrayList arraylist = new ArrayList(this.blocksNeedingUpdate);
        this.blocksNeedingUpdate.clear();

        for (int l = 0; l < arraylist.size(); ++l)
        {
            ChunkPosition chunkposition = (ChunkPosition)arraylist.get(l);
            par1World.notifyBlocksOfNeighborChange(chunkposition.x, chunkposition.y, chunkposition.z, this.blockID);
        }
    }

    private void calculateCurrentChanges(World par1World, int par2, int par3, int par4, int par5, int par6, int par7)
    {
        int k1 = par1World.getBlockMetadata(par2, par3, par4);
        int l1 = 0;
        this.wiresProvidePower = false;
        boolean flag = par1World.isBlockIndirectlyGettingPowered(par2, par3, par4);
        this.wiresProvidePower = true;
        int i2;
        int j2;
        int k2;

        if (flag)
        {
            l1 = 15;
        }
        else
        {
            for (i2 = 0; i2 < 4; ++i2)
            {
                j2 = par2;
                k2 = par4;

                if (i2 == 0)
                {
                    j2 = par2 - 1;
                }

                if (i2 == 1)
                {
                    ++j2;
                }

                if (i2 == 2)
                {
                    k2 = par4 - 1;
                }

                if (i2 == 3)
                {
                    ++k2;
                }

                if (j2 != par5 || par3 != par6 || k2 != par7)
                {
                    l1 = this.getMaxCurrentStrength(par1World, j2, par3, k2, l1);
                }

                if (par1World.isBlockNormalCube(j2, par3, k2) && !par1World.isBlockNormalCube(par2, par3 + 1, par4))
                {
                    if (j2 != par5 || par3 + 1 != par6 || k2 != par7)
                    {
                        l1 = this.getMaxCurrentStrength(par1World, j2, par3 + 1, k2, l1);
                    }
                }
                else if (!par1World.isBlockNormalCube(j2, par3, k2) && (j2 != par5 || par3 - 1 != par6 || k2 != par7))
                {
                    l1 = this.getMaxCurrentStrength(par1World, j2, par3 - 1, k2, l1);
                }
            }

            if (l1 > 0)
            {
                --l1;
            }
            else
            {
                l1 = 0;
            }
        }

        // CraftBukkit start
        if (k1 != l1)
        {
            BlockRedstoneEvent event = new BlockRedstoneEvent(par1World.getWorld().getBlockAt(par2, par3, par4), k1, l1);
            par1World.getServer().getPluginManager().callEvent(event);
            l1 = event.getNewCurrent();
        }

        // CraftBukkit end

        if (k1 != l1)
        {
            par1World.editingBlocks = true;
            par1World.setBlockMetadataWithNotify(par2, par3, par4, l1);
            par1World.markBlockRangeForRenderUpdate(par2, par3, par4, par2, par3, par4);
            par1World.editingBlocks = false;

            for (i2 = 0; i2 < 4; ++i2)
            {
                j2 = par2;
                k2 = par4;
                int l2 = par3 - 1;

                if (i2 == 0)
                {
                    j2 = par2 - 1;
                }

                if (i2 == 1)
                {
                    ++j2;
                }

                if (i2 == 2)
                {
                    k2 = par4 - 1;
                }

                if (i2 == 3)
                {
                    ++k2;
                }

                if (par1World.isBlockNormalCube(j2, par3, k2))
                {
                    l2 += 2;
                }

                boolean flag1 = false;
                int i3 = this.getMaxCurrentStrength(par1World, j2, par3, k2, -1);
                l1 = par1World.getBlockMetadata(par2, par3, par4);

                if (l1 > 0)
                {
                    --l1;
                }

                if (i3 >= 0 && i3 != l1)
                {
                    this.calculateCurrentChanges(par1World, j2, par3, k2, par2, par3, par4);
                }

                i3 = this.getMaxCurrentStrength(par1World, j2, l2, k2, -1);
                l1 = par1World.getBlockMetadata(par2, par3, par4);

                if (l1 > 0)
                {
                    --l1;
                }

                if (i3 >= 0 && i3 != l1)
                {
                    this.calculateCurrentChanges(par1World, j2, l2, k2, par2, par3, par4);
                }
            }

            if (k1 < l1 || l1 == 0)
            {
                this.blocksNeedingUpdate.add(new ChunkPosition(par2, par3, par4));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2 - 1, par3, par4));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2 + 1, par3, par4));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2, par3 - 1, par4));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2, par3 + 1, par4));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2, par3, par4 - 1));
                this.blocksNeedingUpdate.add(new ChunkPosition(par2, par3, par4 + 1));
            }
        }
    }

    /**
     * Calls World.notifyBlocksOfNeighborChange() for all neighboring blocks, but only if the given block is a redstone
     * wire.
     */
    private void notifyWireNeighborsOfNeighborChange(World par1World, int par2, int par3, int par4)
    {
        if (par1World.getBlockId(par2, par3, par4) == this.blockID)
        {
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 - 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 + 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 - 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 + 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
        }
    }

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int par2, int par3, int par4)
    {
        super.onBlockAdded(par1World, par2, par3, par4);

        if (!par1World.isRemote)
        {
            this.updateAndPropagateCurrentStrength(par1World, par2, par3, par4);
            par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3, par4);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3, par4);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3, par4 - 1);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3, par4 + 1);

            if (par1World.isBlockNormalCube(par2 - 1, par3, par4))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3 + 1, par4);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3 - 1, par4);
            }

            if (par1World.isBlockNormalCube(par2 + 1, par3, par4))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3 + 1, par4);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3 - 1, par4);
            }

            if (par1World.isBlockNormalCube(par2, par3, par4 - 1))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 + 1, par4 - 1);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 - 1, par4 - 1);
            }

            if (par1World.isBlockNormalCube(par2, par3, par4 + 1))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 + 1, par4 + 1);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 - 1, par4 + 1);
            }
        }
    }

    /**
     * ejects contained items into the world, and notifies neighbours of an update, as appropriate
     */
    public void breakBlock(World par1World, int par2, int par3, int par4, int par5, int par6)
    {
        super.breakBlock(par1World, par2, par3, par4, par5, par6);

        if (!par1World.isRemote)
        {
            par1World.notifyBlocksOfNeighborChange(par2, par3 + 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3 - 1, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 + 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2 - 1, par3, par4, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 + 1, this.blockID);
            par1World.notifyBlocksOfNeighborChange(par2, par3, par4 - 1, this.blockID);
            this.updateAndPropagateCurrentStrength(par1World, par2, par3, par4);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3, par4);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3, par4);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3, par4 - 1);
            this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3, par4 + 1);

            if (par1World.isBlockNormalCube(par2 - 1, par3, par4))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3 + 1, par4);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 - 1, par3 - 1, par4);
            }

            if (par1World.isBlockNormalCube(par2 + 1, par3, par4))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3 + 1, par4);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2 + 1, par3 - 1, par4);
            }

            if (par1World.isBlockNormalCube(par2, par3, par4 - 1))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 + 1, par4 - 1);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 - 1, par4 - 1);
            }

            if (par1World.isBlockNormalCube(par2, par3, par4 + 1))
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 + 1, par4 + 1);
            }
            else
            {
                this.notifyWireNeighborsOfNeighborChange(par1World, par2, par3 - 1, par4 + 1);
            }
        }
    }

    /**
     * Returns the current strength at the specified block if it is greater than the passed value, or the passed value
     * otherwise. Signature: (world, x, y, z, strength)
     */
    public int getMaxCurrentStrength(World par1World, int par2, int par3, int par4, int par5) // CraftBukkit - private -> public
    {
        if (par1World.getBlockId(par2, par3, par4) != this.blockID)
        {
            return par5;
        }
        else
        {
            int i1 = par1World.getBlockMetadata(par2, par3, par4);
            return i1 > par5 ? i1 : par5;
        }
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World par1World, int par2, int par3, int par4, int par5)
    {
        if (!par1World.isRemote)
        {
            int i1 = par1World.getBlockMetadata(par2, par3, par4);
            boolean flag = this.canPlaceBlockAt(par1World, par2, par3, par4);

            if (flag)
            {
                this.updateAndPropagateCurrentStrength(par1World, par2, par3, par4);
            }
            else
            {
                this.dropBlockAsItem(par1World, par2, par3, par4, i1, 0);
                par1World.setBlockWithNotify(par2, par3, par4, 0);
            }

            super.onNeighborBlockChange(par1World, par2, par3, par4, par5);
        }
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return Item.redstone.itemID;
    }

    /**
     * Returns true if the block is emitting direct/strong redstone power on the specified side. Args: World, X, Y, Z,
     * side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.
     */
    public boolean isProvidingStrongPower(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        return !this.wiresProvidePower ? false : this.isProvidingWeakPower(par1IBlockAccess, par2, par3, par4, par5);
    }

    /**
     * Returns true if the block is emitting indirect/weak redstone power on the specified side. If isBlockNormalCube
     * returns true, standard redstone propagation rules will apply instead and this will not be called. Args: World, X,
     * Y, Z, side. Note that the side is reversed - eg it is 1 (up) when checking the bottom of the block.
     */
    public boolean isProvidingWeakPower(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        if (!this.wiresProvidePower)
        {
            return false;
        }
        else if (par1IBlockAccess.getBlockMetadata(par2, par3, par4) == 0)
        {
            return false;
        }
        else if (par5 == 1)
        {
            return true;
        }
        else
        {
            boolean flag = isPoweredOrRepeater(par1IBlockAccess, par2 - 1, par3, par4, 1) || !par1IBlockAccess.isBlockNormalCube(par2 - 1, par3, par4) && isPoweredOrRepeater(par1IBlockAccess, par2 - 1, par3 - 1, par4, -1);
            boolean flag1 = isPoweredOrRepeater(par1IBlockAccess, par2 + 1, par3, par4, 3) || !par1IBlockAccess.isBlockNormalCube(par2 + 1, par3, par4) && isPoweredOrRepeater(par1IBlockAccess, par2 + 1, par3 - 1, par4, -1);
            boolean flag2 = isPoweredOrRepeater(par1IBlockAccess, par2, par3, par4 - 1, 2) || !par1IBlockAccess.isBlockNormalCube(par2, par3, par4 - 1) && isPoweredOrRepeater(par1IBlockAccess, par2, par3 - 1, par4 - 1, -1);
            boolean flag3 = isPoweredOrRepeater(par1IBlockAccess, par2, par3, par4 + 1, 0) || !par1IBlockAccess.isBlockNormalCube(par2, par3, par4 + 1) && isPoweredOrRepeater(par1IBlockAccess, par2, par3 - 1, par4 + 1, -1);

            if (!par1IBlockAccess.isBlockNormalCube(par2, par3 + 1, par4))
            {
                if (par1IBlockAccess.isBlockNormalCube(par2 - 1, par3, par4) && isPoweredOrRepeater(par1IBlockAccess, par2 - 1, par3 + 1, par4, -1))
                {
                    flag = true;
                }

                if (par1IBlockAccess.isBlockNormalCube(par2 + 1, par3, par4) && isPoweredOrRepeater(par1IBlockAccess, par2 + 1, par3 + 1, par4, -1))
                {
                    flag1 = true;
                }

                if (par1IBlockAccess.isBlockNormalCube(par2, par3, par4 - 1) && isPoweredOrRepeater(par1IBlockAccess, par2, par3 + 1, par4 - 1, -1))
                {
                    flag2 = true;
                }

                if (par1IBlockAccess.isBlockNormalCube(par2, par3, par4 + 1) && isPoweredOrRepeater(par1IBlockAccess, par2, par3 + 1, par4 + 1, -1))
                {
                    flag3 = true;
                }
            }

            return !flag2 && !flag1 && !flag && !flag3 && par5 >= 2 && par5 <= 5 ? true : (par5 == 2 && flag2 && !flag && !flag1 ? true : (par5 == 3 && flag3 && !flag && !flag1 ? true : (par5 == 4 && flag && !flag2 && !flag3 ? true : par5 == 5 && flag1 && !flag2 && !flag3)));
        }
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return this.wiresProvidePower;
    }

    @SideOnly(Side.CLIENT)

    /**
     * A randomly called display update to be able to add particles or other items for display
     */
    public void randomDisplayTick(World par1World, int par2, int par3, int par4, Random par5Random)
    {
        int l = par1World.getBlockMetadata(par2, par3, par4);

        if (l > 0)
        {
            double d0 = (double)par2 + 0.5D + ((double)par5Random.nextFloat() - 0.5D) * 0.2D;
            double d1 = (double)((float)par3 + 0.0625F);
            double d2 = (double)par4 + 0.5D + ((double)par5Random.nextFloat() - 0.5D) * 0.2D;
            float f = (float)l / 15.0F;
            float f1 = f * 0.6F + 0.4F;

            if (l == 0)
            {
                f1 = 0.0F;
            }

            float f2 = f * f * 0.7F - 0.5F;
            float f3 = f * f * 0.6F - 0.7F;

            if (f2 < 0.0F)
            {
                f2 = 0.0F;
            }

            if (f3 < 0.0F)
            {
                f3 = 0.0F;
            }

            par1World.spawnParticle("reddust", d0, d1, d2, (double)f1, (double)f2, (double)f3);
        }
    }

    /**
     * Returns true if redstone wire can connect to the specified block. Params: World, X, Y, Z, side (not a normal
     * notch-side, this can be 0, 1, 2, 3 or -1)
     */
    public static boolean isPowerProviderOrWire(IBlockAccess par0IBlockAccess, int par1, int par2, int par3, int par4)
    {
        int i1 = par0IBlockAccess.getBlockId(par1, par2, par3);

        if (i1 == Block.redstoneWire.blockID)
        {
            return true;
        }
        else if (i1 == 0)
        {
            return false;
        }
        else if (i1 != Block.redstoneRepeaterIdle.blockID && i1 != Block.redstoneRepeaterActive.blockID)
        {
            return (Block.blocksList[i1] != null && Block.blocksList[i1].canConnectRedstone(par0IBlockAccess, par1, par2, par3, par4));
        }
        else
        {
            int j1 = par0IBlockAccess.getBlockMetadata(par1, par2, par3);
            return par4 == (j1 & 3) || par4 == Direction.footInvisibleFaceRemap[j1 & 3];
        }
    }

    /**
     * Returns true if the block coordinate passed can provide power, or is a redstone wire, or if its a repeater that
     * is powered.
     */
    public static boolean isPoweredOrRepeater(IBlockAccess par0IBlockAccess, int par1, int par2, int par3, int par4)
    {
        if (isPowerProviderOrWire(par0IBlockAccess, par1, par2, par3, par4))
        {
            return true;
        }
        else
        {
            int i1 = par0IBlockAccess.getBlockId(par1, par2, par3);

            if (i1 == Block.redstoneRepeaterActive.blockID)
            {
                int j1 = par0IBlockAccess.getBlockMetadata(par1, par2, par3);
                return par4 == (j1 & 3);
            }
            else
            {
                return false;
            }
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * only called by clickMiddleMouseButton , and passed to inventory.setCurrentItem (along with isCreative)
     */
    public int idPicked(World par1World, int par2, int par3, int par4)
    {
        return Item.redstone.itemID;
    }
}
