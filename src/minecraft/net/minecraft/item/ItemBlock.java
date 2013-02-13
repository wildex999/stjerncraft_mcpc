package net.minecraft.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import org.bukkit.craftbukkit.block.CraftBlockState; // CraftBukkit
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ItemBlock extends Item
{
    /** The block ID of the Block associated with this ItemBlock */
    private int blockID;

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
        final int clickedX = par4, clickedY = par5, clickedZ = par6;

        int var11 = par3World.getBlockId(par4, par5, par6);
        if (var11 == Block.snow.blockID)
        {
            par7 = 1;
        }
        else if (var11 != Block.vine.blockID && var11 != Block.tallGrass.blockID && var11 != Block.deadBush.blockID
                && (Block.blocksList[var11] == null || !Block.blocksList[var11].isBlockReplaceable(par3World, par4, par5, par6)))
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
            if (par3World.getBlockId(par4, par5, par6) == this.blockID)
            {
                Block.blocksList[this.blockID].onBlockPlacedBy(par3World, par4, par5, par6, par2EntityPlayer);
                Block.blocksList[this.blockID].onPostBlockPlaced(par3World, par4, par5, par6, var14);
            }

            par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), var12.stepSound.getPlaceSound(), (var12.stepSound.getVolume() + 1.0F) / 2.0F, var12.stepSound.getPitch() * 0.8F);
            --par1ItemStack.stackSize;
            */
            // MCPC+ start - merge CraftBukkit processBlockPlace() and Forge placeBlockAt()
            //return processBlockPlace(par3World, par2EntityPlayer, par1ItemStack, par4, par5, par6, this.blockID, var14, clickedX, clickedY, clickedZ);
            //// CraftBukkit end

            // we MUST call placeBlockAt() since Forge mods can override it
            // TODO: fix mods overriding placeBlockAt() not sending Bukkit place events
            if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, var14))
            {
                par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), var12.stepSound.getPlaceSound(), (var12.stepSound.getVolume() + 1.0F) / 2.0F, var12.stepSound.getPitch() * 0.8F);
                --par1ItemStack.stackSize;
            }

            return true;
            // MCPC+ end
        }
        else
        {
            return false;
        }
    }

    // CraftBukkit start - add method to process block placement // MCPC+ note: see above - this method is now ONLY used for NMS-patched items!
    static boolean processBlockPlace(final World world, final EntityPlayer entityhuman, final ItemStack itemstack, final int x, final int y, final int z, final int id, final int data, final int clickedX, final int clickedY, final int clickedZ)
    {
        org.bukkit.block.BlockState blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(world, x, y, z);
        world.editingBlocks = true;
        world.callingPlaceEvent = true;
        world.setBlockAndMetadata(x, y, z, id, data);
        org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(world, entityhuman, blockstate, clickedX, clickedY, clickedZ);

        if (event.isCancelled() || !event.canBuild())
        {
            blockstate.update(true);
            world.editingBlocks = false;
            world.callingPlaceEvent = false;
            return false;
        }

        world.editingBlocks = false;
        world.callingPlaceEvent = false;
        int newId = world.getBlockId(x, y, z);
        int newData = world.getBlockMetadata(x, y, z);
        Block block = Block.blocksList[newId];

        if (block != null && !(block instanceof BlockContainer))   // Containers get placed automatically
        {
            block.onBlockAdded(world, x, y, z);
        }

        world.notifyBlockChange(x, y, z, newId);

        // Skulls don't get block data applied to them
        if (block != null && block != Block.skull)
        {
            block.onBlockPlacedBy(world, x, y, z, entityhuman);
            block.onPostBlockPlaced(world, x, y, z, newData);
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

    // Forge start
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
        // MCPC+ start - merge Forge method with CB processBlockPlace()
        // send event
        org.bukkit.block.BlockState blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(world, x, y, z);
        world.editingBlocks = true;
        world.callingPlaceEvent = true;
        // set block, then call the event (see also World#setBlockIDWithMetadata)
        world.setBlockAndMetadata(x, y, z, this.blockID, metadata);
        int clickedX = (int)(hitX + 0.5), clickedY = (int)(hitY + 0.5), clickedZ = (int)(hitZ + 0.5);
        org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(world, player, blockstate, clickedX, clickedY, clickedZ);

        if (event.isCancelled() || !event.canBuild())
        {
            blockstate.update(true);
            world.editingBlocks = false;
            world.callingPlaceEvent = false;
            return false;
        }

        world.editingBlocks = false;
        world.callingPlaceEvent = false;
        int newId = world.getBlockId(x, y, z);
        int newData = world.getBlockMetadata(x, y, z);

        Block block = Block.blocksList[newId];

        if (block != null && !(block instanceof BlockContainer))   // Containers get placed automatically
        {
            block.onBlockAdded(world, x, y, z);
        }

        world.notifyBlockChange(x, y, z, newId);

        // Skulls don't get block data applied to them
        if (block != null && block != Block.skull)
        {
            block.onBlockPlacedBy(world, x, y, z, player);
            block.onPostBlockPlaced(world, x, y, z, newData);
            // MCPC+ - moved to end of onItemUse()
        }
        /*
            world.playSoundEffect((double)((float) x + 0.5F), (double)((float) y + 0.5F), (double)((float) z + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
        }

        if (stack != null)
        {
            --stack.stackSize;
        }*/

        return true;
        // MCPC+ end
    }
    // Forge end
}
