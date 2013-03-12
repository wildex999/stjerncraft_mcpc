package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
// CraftBukkit end

public class BlockPistonBase extends Block
{
    /** This pistons is the sticky one? */
    private boolean isSticky;

    public BlockPistonBase(int par1, int par2, boolean par3)
    {
        super(par1, par2, Material.piston);
        this.isSticky = par3;
        this.setStepSound(soundStoneFootstep);
        this.setHardness(0.5F);
        this.setCreativeTab(CreativeTabs.tabRedstone);
    }

    @SideOnly(Side.CLIENT)

    /**
     * Return the either 106 or 107 as the texture index depending on the isSticky flag. This will actually never get
     * called by TileEntityRendererPiston.renderPiston() because TileEntityPiston.shouldRenderHead() will always return
     * false.
     */
    public int getPistonExtensionTexture()
    {
        return this.isSticky ? 106 : 107;
    }

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public int getBlockTextureFromSideAndMetadata(int par1, int par2)
    {
        int k = getOrientation(par2);
        return k > 5 ? this.blockIndexInTexture : (par1 == k ? (!isExtended(par2) && this.minX <= 0.0D && this.minY <= 0.0D && this.minZ <= 0.0D && this.maxX >= 1.0D && this.maxY >= 1.0D && this.maxZ >= 1.0D ? this.blockIndexInTexture : 110) : (par1 == Facing.faceToSide[k] ? 109 : 108));
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 16;
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
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9)
    {
        return false;
    }

    /**
     * Called when the block is placed in the world.
     */
    public void onBlockPlacedBy(World par1World, int par2, int par3, int par4, EntityLiving par5EntityLiving)
    {
        int l = determineOrientation(par1World, par2, par3, par4, (EntityPlayer)par5EntityLiving);
        par1World.setBlockMetadataWithNotify(par2, par3, par4, l);

        if (!par1World.isRemote)
        {
            this.updatePistonState(par1World, par2, par3, par4);
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
            this.updatePistonState(par1World, par2, par3, par4);
        }
    }

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int par2, int par3, int par4)
    {
        if (!par1World.isRemote && par1World.getBlockTileEntity(par2, par3, par4) == null)
        {
            this.updatePistonState(par1World, par2, par3, par4);
        }
    }

    /**
     * handles attempts to extend or retract the piston.
     */
    private void updatePistonState(World par1World, int par2, int par3, int par4)
    {
        int l = par1World.getBlockMetadata(par2, par3, par4);
        int i1 = getOrientation(l);

        if (i1 != 7)
        {
            boolean flag = this.isIndirectlyPowered(par1World, par2, par3, par4, i1);

            if (flag && !isExtended(l))
            {
                // CraftBukkit start
                int length = canExtend_IntCB(par1World, par2, par3, par4, i1); // MCPC+ - update from rename

                if (length >= 0)
                {
                    org.bukkit.block.Block block = par1World.getWorld().getBlockAt(par2, par3, par4);
                    BlockPistonExtendEvent event = new BlockPistonExtendEvent(block, length, CraftBlock.notchToBlockFace(i1));
                    par1World.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled())
                    {
                        return;
                    }

                    // CraftBukkit end
                    par1World.addBlockEvent(par2, par3, par4, this.blockID, 0, i1);
                }
            }
            else if (!flag && isExtended(l))
            {
                // CraftBukkit start
                org.bukkit.block.Block block = par1World.getWorld().getBlockAt(par2, par3, par4);
                BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, CraftBlock.notchToBlockFace(i1));
                par1World.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return;
                }

                // CraftBukkit end
                par1World.addBlockEvent(par2, par3, par4, this.blockID, 1, i1);
            }
        }
    }

    /**
     * checks the block to that side to see if it is indirectly powered.
     */
    private boolean isIndirectlyPowered(World par1World, int par2, int par3, int par4, int par5)
    {
        return par5 != 0 && par1World.isBlockIndirectlyProvidingPowerTo(par2, par3 - 1, par4, 0) ? true : (par5 != 1 && par1World.isBlockIndirectlyProvidingPowerTo(par2, par3 + 1, par4, 1) ? true : (par5 != 2 && par1World.isBlockIndirectlyProvidingPowerTo(par2, par3, par4 - 1, 2) ? true : (par5 != 3 && par1World.isBlockIndirectlyProvidingPowerTo(par2, par3, par4 + 1, 3) ? true : (par5 != 5 && par1World.isBlockIndirectlyProvidingPowerTo(par2 + 1, par3, par4, 5) ? true : (par5 != 4 && par1World.isBlockIndirectlyProvidingPowerTo(par2 - 1, par3, par4, 4) ? true : (par1World.isBlockIndirectlyProvidingPowerTo(par2, par3, par4, 0) ? true : (par1World.isBlockIndirectlyProvidingPowerTo(par2, par3 + 2, par4, 1) ? true : (par1World.isBlockIndirectlyProvidingPowerTo(par2, par3 + 1, par4 - 1, 2) ? true : (par1World.isBlockIndirectlyProvidingPowerTo(par2, par3 + 1, par4 + 1, 3) ? true : (par1World.isBlockIndirectlyProvidingPowerTo(par2 - 1, par3 + 1, par4, 4) ? true : par1World.isBlockIndirectlyProvidingPowerTo(par2 + 1, par3 + 1, par4, 5)))))))))));
    }

    /**
     * Called when the block receives a BlockEvent - see World.addBlockEvent. By default, passes it on to the tile
     * entity at this location. Args: world, x, y, z, blockID, EventID, event parameter
     */
    public void onBlockEventReceived(World par1World, int par2, int par3, int par4, int par5, int par6)
    {
        if (par5 == 0)
        {
            par1World.setBlockMetadata(par2, par3, par4, par6 | 8);
        }
        else
        {
            par1World.setBlockMetadata(par2, par3, par4, par6);
        }

        if (par5 == 0)
        {
            if (this.tryExtend(par1World, par2, par3, par4, par6))
            {
                par1World.setBlockMetadataWithNotify(par2, par3, par4, par6 | 8);
                par1World.playSoundEffect((double)par2 + 0.5D, (double)par3 + 0.5D, (double)par4 + 0.5D, "tile.piston.out", 0.5F, par1World.rand.nextFloat() * 0.25F + 0.6F);
            }
            else
            {
                par1World.setBlockMetadata(par2, par3, par4, par6);
            }
        }
        else if (par5 == 1)
        {
            TileEntity tileentity = par1World.getBlockTileEntity(par2 + Facing.offsetsXForSide[par6], par3 + Facing.offsetsYForSide[par6], par4 + Facing.offsetsZForSide[par6]);

            if (tileentity instanceof TileEntityPiston)
            {
                ((TileEntityPiston)tileentity).clearPistonTileEntity();
            }

            par1World.setBlockAndMetadata(par2, par3, par4, Block.pistonMoving.blockID, par6);
            par1World.setBlockTileEntity(par2, par3, par4, BlockPistonMoving.getTileEntity(this.blockID, par6, par6, false, true));

            if (this.isSticky)
            {
                int j1 = par2 + Facing.offsetsXForSide[par6] * 2;
                int k1 = par3 + Facing.offsetsYForSide[par6] * 2;
                int l1 = par4 + Facing.offsetsZForSide[par6] * 2;
                int i2 = par1World.getBlockId(j1, k1, l1);
                int j2 = par1World.getBlockMetadata(j1, k1, l1);
                boolean flag = false;

                if (i2 == Block.pistonMoving.blockID)
                {
                    TileEntity tileentity1 = par1World.getBlockTileEntity(j1, k1, l1);

                    if (tileentity1 instanceof TileEntityPiston)
                    {
                        TileEntityPiston tileentitypiston = (TileEntityPiston)tileentity1;

                        if (tileentitypiston.getPistonOrientation() == par6 && tileentitypiston.isExtending())
                        {
                            tileentitypiston.clearPistonTileEntity();
                            i2 = tileentitypiston.getStoredBlockID();
                            j2 = tileentitypiston.getBlockMetadata();
                            flag = true;
                        }
                    }
                }

                if (!flag && i2 > 0 && canPushBlock(i2, par1World, j1, k1, l1, false) && (Block.blocksList[i2].getMobilityFlag() == 0 || i2 == Block.pistonBase.blockID || i2 == Block.pistonStickyBase.blockID))
                {
                    par2 += Facing.offsetsXForSide[par6];
                    par3 += Facing.offsetsYForSide[par6];
                    par4 += Facing.offsetsZForSide[par6];
                    par1World.setBlockAndMetadata(par2, par3, par4, Block.pistonMoving.blockID, j2);
                    par1World.setBlockTileEntity(par2, par3, par4, BlockPistonMoving.getTileEntity(i2, j2, par6, false, false));
                    par1World.setBlockWithNotify(j1, k1, l1, 0);
                }
                else if (!flag)
                {
                    par1World.setBlockWithNotify(par2 + Facing.offsetsXForSide[par6], par3 + Facing.offsetsYForSide[par6], par4 + Facing.offsetsZForSide[par6], 0);
                }
            }
            else
            {
                par1World.setBlockWithNotify(par2 + Facing.offsetsXForSide[par6], par3 + Facing.offsetsYForSide[par6], par4 + Facing.offsetsZForSide[par6], 0);
            }

            par1World.playSoundEffect((double)par2 + 0.5D, (double)par3 + 0.5D, (double)par4 + 0.5D, "tile.piston.in", 0.5F, par1World.rand.nextFloat() * 0.15F + 0.6F);
        }
    }

    /**
     * Updates the blocks bounds based on its current state. Args: world, x, y, z
     */
    public void setBlockBoundsBasedOnState(IBlockAccess par1IBlockAccess, int par2, int par3, int par4)
    {
        int l = par1IBlockAccess.getBlockMetadata(par2, par3, par4);

        if (isExtended(l))
        {
            switch (getOrientation(l))
            {
                case 0:
                    this.setBlockBounds(0.0F, 0.25F, 0.0F, 1.0F, 1.0F, 1.0F);
                    break;
                case 1:
                    this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
                    break;
                case 2:
                    this.setBlockBounds(0.0F, 0.0F, 0.25F, 1.0F, 1.0F, 1.0F);
                    break;
                case 3:
                    this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.75F);
                    break;
                case 4:
                    this.setBlockBounds(0.25F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    break;
                case 5:
                    this.setBlockBounds(0.0F, 0.0F, 0.0F, 0.75F, 1.0F, 1.0F);
            }
        }
        else
        {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * Sets the block's bounds for rendering it as an item
     */
    public void setBlockBoundsForItemRender()
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * if the specified block is in the given AABB, add its collision bounding box to the given list
     */
    public void addCollidingBlockToList(World par1World, int par2, int par3, int par4, AxisAlignedBB par5AxisAlignedBB, List par6List, Entity par7Entity)
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        super.addCollidingBlockToList(par1World, par2, par3, par4, par5AxisAlignedBB, par6List, par7Entity);
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4)
    {
        this.setBlockBoundsBasedOnState(par1World, par2, par3, par4);
        return super.getCollisionBoundingBoxFromPool(par1World, par2, par3, par4);
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    /**
     * returns an int which describes the direction the piston faces
     */
    public static int getOrientation(int par0)
    {
        if ((par0 & 7) >= Facing.faceToSide.length)
        {
            return 7;    // CraftBukkit - check for AIOOB on piston data
        }

        return par0 & 7;
    }

    /**
     * Determine if the metadata is related to something powered.
     */
    public static boolean isExtended(int par0)
    {
        return (par0 & 8) != 0;
    }

    /**
     * gets the way this piston should face for that entity that placed it.
     */
    public static int determineOrientation(World par0World, int par1, int par2, int par3, EntityPlayer par4EntityPlayer)
    {
        if (MathHelper.abs((float)par4EntityPlayer.posX - (float)par1) < 2.0F && MathHelper.abs((float)par4EntityPlayer.posZ - (float)par3) < 2.0F)
        {
            double d0 = par4EntityPlayer.posY + 1.82D - (double)par4EntityPlayer.yOffset;

            if (d0 - (double)par2 > 2.0D)
            {
                return 1;
            }

            if ((double)par2 - d0 > 0.0D)
            {
                return 0;
            }
        }

        int l = MathHelper.floor_double((double)(par4EntityPlayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        return l == 0 ? 2 : (l == 1 ? 5 : (l == 2 ? 3 : (l == 3 ? 4 : 0)));
    }

    /**
     * returns true if the piston can push the specified block
     */
    private static boolean canPushBlock(int par0, World par1World, int par2, int par3, int par4, boolean par5)
    {
        if (par0 == Block.obsidian.blockID)
        {
            return false;
        }
        else
        {
            if (par0 != Block.pistonBase.blockID && par0 != Block.pistonStickyBase.blockID)
            {
                if (Block.blocksList[par0].getBlockHardness(par1World, par2, par3, par4) == -1.0F)
                {
                    return false;
                }

                if (Block.blocksList[par0].getMobilityFlag() == 2)
                {
                    return false;
                }

                if (!par5 && Block.blocksList[par0].getMobilityFlag() == 1)
                {
                    return false;
                }
            }
            else if (isExtended(par1World.getBlockMetadata(par2, par3, par4)))
            {
                return false;
            }

            return !par1World.blockHasTileEntity(par2, par3, par4);
        }
    }

    /**
     * checks to see if this piston could push the blocks in front of it.
     */
    // MCPC+ start - vanilla compatibility
    private static boolean canExtend(World world, int i, int j, int k, int l) {
        return canExtend_IntCB(world, i, j, k, l) >= 0;
    }
    // MCPC+ end

    // CraftBukkit - boolean -> int return
    private static int canExtend_IntCB(World world, int i, int j, int k, int l) // MCPC+ - rename from obf
    {
        int i1 = i + Facing.offsetsXForSide[l];
        int j1 = j + Facing.offsetsYForSide[l];
        int k1 = k + Facing.offsetsZForSide[l];
        int l1 = 0;

        while (true)
        {
            if (l1 < 13)
            {
                if (j1 <= 0 || j1 >= world.getHeight() - 1)
                {
                    return -1; // CraftBukkit
                }

                int i2 = world.getBlockId(i1, j1, k1);

                if (i2 != 0)
                {
                    if (!canPushBlock(i2, world, i1, j1, k1, true))
                    {
                        return -1; // CraftBukkit
                    }

                    if (Block.blocksList[i2].getMobilityFlag() != 1)
                    {
                        if (l1 == 12)
                        {
                            return -1; // CraftBukkit
                        }

                        i1 += Facing.offsetsXForSide[l];
                        j1 += Facing.offsetsYForSide[l];
                        k1 += Facing.offsetsZForSide[l];
                        ++l1;
                        continue;
                    }
                }
            }

            return l1; // CraftBukkit
        }
    }

    /**
     * attempts to extend the piston. returns false if impossible.
     */
    private boolean tryExtend(World par1World, int par2, int par3, int par4, int par5)
    {
        int i1 = par2 + Facing.offsetsXForSide[par5];
        int j1 = par3 + Facing.offsetsYForSide[par5];
        int k1 = par4 + Facing.offsetsZForSide[par5];
        int l1 = 0;

        while (true)
        {
            int i2;

            if (l1 < 13)
            {
                if (j1 <= 0 || j1 >= par1World.getHeight() - 1)
                {
                    return false;
                }

                i2 = par1World.getBlockId(i1, j1, k1);

                if (i2 != 0)
                {
                    if (!canPushBlock(i2, par1World, i1, j1, k1, true))
                    {
                        return false;
                    }

                    if (Block.blocksList[i2].getMobilityFlag() != 1)
                    {
                        if (l1 == 12)
                        {
                            return false;
                        }

                        i1 += Facing.offsetsXForSide[par5];
                        j1 += Facing.offsetsYForSide[par5];
                        k1 += Facing.offsetsZForSide[par5];
                        ++l1;
                        continue;
                    }

                    Block.blocksList[i2].dropBlockAsItem(par1World, i1, j1, k1, par1World.getBlockMetadata(i1, j1, k1), 0);
                    par1World.setBlockWithNotify(i1, j1, k1, 0);
                }
            }

            while (i1 != par2 || j1 != par3 || k1 != par4)
            {
                l1 = i1 - Facing.offsetsXForSide[par5];
                i2 = j1 - Facing.offsetsYForSide[par5];
                int j2 = k1 - Facing.offsetsZForSide[par5];
                int k2 = par1World.getBlockId(l1, i2, j2);
                int l2 = par1World.getBlockMetadata(l1, i2, j2);

                if (k2 == this.blockID && l1 == par2 && i2 == par3 && j2 == par4)
                {
                    par1World.setBlockAndMetadataWithUpdate(i1, j1, k1, Block.pistonMoving.blockID, par5 | (this.isSticky ? 8 : 0), false);
                    par1World.setBlockTileEntity(i1, j1, k1, BlockPistonMoving.getTileEntity(Block.pistonExtension.blockID, par5 | (this.isSticky ? 8 : 0), par5, true, false));
                }
                else
                {
                    par1World.setBlockAndMetadataWithUpdate(i1, j1, k1, Block.pistonMoving.blockID, l2, false);
                    par1World.setBlockTileEntity(i1, j1, k1, BlockPistonMoving.getTileEntity(k2, l2, par5, true, false));
                }

                i1 = l1;
                j1 = i2;
                k1 = j2;
            }

            return true;
        }
    }
}
