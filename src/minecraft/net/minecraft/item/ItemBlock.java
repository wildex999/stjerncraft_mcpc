package net.minecraft.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemBlock extends Item
{
    /** The block ID of the Block associated with this ItemBlock */
    private int blockID;
    private int localId; // Forge

    public ItemBlock(int par1)
    {
        super(par1);
        this.blockID = par1 + 256;
        this.setIconIndex(Block.blocksList[par1 + 256].getBlockTextureFromSide(2));
        isDefaultTexture = Block.blocksList[par1 + 256].isDefaultTexture;
    }

    /**
     * Returns the blockID for this Item
     */
    public int getBlockID()
    {
        return this.blockID;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int var11 = par3World.getBlockId(par4, par5, par6);

        if (var11 == Block.snow.blockID)
        {
            par7 = 1;
        }
        else if (var11 != Block.vine.blockID && var11 != Block.tallGrass.blockID && var11 != Block.deadBush.blockID && (Block.blocksList[var11] == null || !Block.blocksList[var11].isBlockReplaceable(par3World, par4, par5, par6)))     // Forge
        {
            if (par7 == 0)
            {
                --par5;
            }

            if (par7 == 1)
            {
                ++par5;
            }

            if (par7 == 2)
            {
                --par6;
            }

            if (par7 == 3)
            {
                ++par6;
            }

            if (par7 == 4)
            {
                --par4;
            }

            if (par7 == 5)
            {
                ++par4;
            }
        }

        if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else if (par5 == 255 && Block.blocksList[this.blockID].blockMaterial.isSolid())
        {
            return false;
        }
        else if (par3World.canPlaceEntityOnSide(this.blockID, par4, par5, par6, false, par7, par2EntityPlayer))
        {
            Block var12 = Block.blocksList[this.blockID];
            int var13 = this.getMetadata(par1ItemStack.getItemDamage());
            int var14 = Block.blocksList[this.blockID].onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, var13);
            // CraftBukkit start - redirect to common function handler
            /*
            if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, data))  // Forge
            {
                par3World.playSoundEffect((double)((float) par4 + 0.5F), (double)((float) par5 + 0.5F), (double)((float) par6 + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
                --par1ItemStack.stackSize;
            }

            return true;
            */
            return processBlockPlace(par3World, par2EntityPlayer, par1ItemStack, par4, par5, par6, this.blockID, var14);
            // CraftBukkit end
        }
        else
        {
            return false;
        }
    }

    // CraftBukkit start - add method to process block placement
    static boolean processBlockPlace(final World world, final EntityPlayer entityhuman, final ItemStack itemstack, final int x, final int y, final int z, final int id, final int data)
    {
        world.editingBlocks = true;
        world.setBlockAndMetadataWithNotify(x, y, z, id, data);
        org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(world, entityhuman, org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(world, x, y, z), x, y, z);

        if (event.isCancelled() || !event.canBuild())
        {
            event.getBlockReplacedState().update(true);
            world.editingBlocks = false;
            return false;
        }

        world.editingBlocks = false;
        world.notifyBlocksOfNeighborChange(x, y, z, world.getBlockId(x, y, z));
        Block block = Block.blocksList[world.getBlockId(x, y, z)];

        if (block != null)
        {
            block.onBlockPlacedBy(world, x, y, z, entityhuman);
            block.onPostBlockPlaced(world, x, y, z, world.getBlockMetadata(x, y, z));
            world.playSoundEffect((double)((float) x + 0.5F), (double)((float) y + 0.5F), (double)((float) z + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
        }

        if (itemstack != null)
        {
            --itemstack.stackSize;
        }

        return true;
    }
    // CraftBukkit end

    @SideOnly(Side.CLIENT)

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */
    public boolean canPlaceItemBlockOnSide(World par1World, int par2, int par3, int par4, int par5, EntityPlayer par6EntityPlayer, ItemStack par7ItemStack)
    {
        int var8 = par1World.getBlockId(par2, par3, par4);

        if (var8 == Block.snow.blockID)
        {
            par5 = 1;
        }
        else if (var8 != Block.vine.blockID && var8 != Block.tallGrass.blockID && var8 != Block.deadBush.blockID
                && (Block.blocksList[var8] == null || !Block.blocksList[var8].isBlockReplaceable(par1World, par2, par3, par4)))
        {
            if (par5 == 0)
            {
                --par3;
            }

            if (par5 == 1)
            {
                ++par3;
            }

            if (par5 == 2)
            {
                --par4;
            }

            if (par5 == 3)
            {
                ++par4;
            }

            if (par5 == 4)
            {
                --par2;
            }

            if (par5 == 5)
            {
                ++par2;
            }
        }

        return par1World.canPlaceEntityOnSide(this.getBlockID(), par2, par3, par4, false, par5, (Entity)null);
    }

    public String getItemNameIS(ItemStack par1ItemStack)
    {
        return Block.blocksList[this.blockID].getBlockName();
    }

    public String getItemName()
    {
        return Block.blocksList[this.blockID].getBlockName();
    }

    @SideOnly(Side.CLIENT)

    /**
     * gets the CreativeTab this item is displayed on
     */
    public CreativeTabs getCreativeTab()
    {
        return Block.blocksList[this.blockID].getCreativeTabToDisplayOn();
    }

    @SideOnly(Side.CLIENT)

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        Block.blocksList[this.blockID].getSubBlocks(par1, par2CreativeTabs, par3List);
    }

    /**
     * Called to actually place the block, after the location is determined
     * and all permission checks have been made.
     *
     * @param stack The item stack that was used to place the block. This can be changed inside the method.
     * @param player The player who is placing the block. Can be null if the block is not being placed by a player.
     * @param side The side the player (or machine) right-clicked on.
     */
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
        // MCPC+ start - delegate Forge's placeBlockAt to CB's processBlockPlace for sending Bukkit events
        return processBlockPlace(world, player, stack, x, y, z, localId, metadata);

        /*
        if (world.setBlockAndMetadataWithNotify(x, y, z, localId, metadata))
        {
            if (world.getBlockId(x, y, z) == localId)
            {
                Block.blocksList[localId].onBlockPlacedBy(world, x, y, z, player);
                Block.blocksList[this.blockID].onPostBlockPlaced(world, x, y, z, metadata);
            }

            return true;
        }

        return false;
        // MCPC+ end */
    }
}
