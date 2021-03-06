package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class ItemBlock extends Item
{
    /** The block ID of the Block associated with this ItemBlock */
    private int blockID;

    public ItemBlock(int par1)
    {
        super(par1);
        this.blockID = par1 + 256;
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
        // MCPC+ start - set the current placed item hit coords for canPlaceEntityOnSide
        par3World.curPlacedItemHitX = par8;
        par3World.curPlacedItemHitY = par9;
        par3World.curPlacedItemHitZ = par10;
        // MCPC+ end        
        int i1 = par3World.getBlockId(par4, par5, par6);

        if (i1 == Block.snow.blockID && (par3World.getBlockMetadata(par4, par5, par6) & 7) < 1)
        {
            par7 = 1;
        }
        else if (i1 != Block.vine.blockID && i1 != Block.tallGrass.blockID && i1 != Block.deadBush.blockID
                && (Block.blocksList[i1] == null || !Block.blocksList[i1].isBlockReplaceable(par3World, par4, par5, par6)))
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
        else if (par3World.canPlaceEntityOnSide(this.blockID, par4, par5, par6, false, par7, par2EntityPlayer, par1ItemStack))
        {
            Block block = Block.blocksList[this.blockID];
            int j1 = this.getMetadata(par1ItemStack.getItemDamage());
            int k1 = Block.blocksList[this.blockID].onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, j1);
            // CraftBukkit start - Redirect to common function handler
            /*
            if (world.setTypeIdAndData(i, j, k, this.id, k1, 3)) {
                if (world.getTypeId(i, j, k) == this.id) {
                    Block.byId[this.id].postPlace(world, i, j, k, entityplayer, itemstack);
                    Block.byId[this.id].postPlace(world, i, j, k, k1);
                }

                world.makeSound((double) ((float) i + 0.5F), (double) ((float) j + 0.5F), (double) ((float) k + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume1() + 1.0F) / 2.0F, block.stepSound.getVolume2() * 0.8F);
                --itemstack.count;
            }
            */            
            // MCPC+ start - seperate forge/vanilla process block calls            
            if (block.isForgeBlock) // process forge block
            {
                if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, k1))
                {
                    par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
                    --par1ItemStack.stackSize;
                }
                return true;
            }
            else // process vanilla block 
            {
            // MCPC+ end
                return processBlockPlace(par3World, par2EntityPlayer, par1ItemStack, par4, par5, par6, this.blockID, k1, clickedX, clickedY, clickedZ);
            }
            // CraftBukkit end            
        }
        else
        {
            return false;
        }
    }

    // CraftBukkit start - Add method to process block placement
    // MCPC+ - public
    public static boolean processBlockPlace(final World world, final EntityPlayer entityplayer, final ItemStack itemstack, final int x, final int y, final int z, final int id, final int data, final int clickedX, final int clickedY, final int clickedZ)
    {
        org.bukkit.block.BlockState blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(world, x, y, z);
        world.callingPlaceEvent = true;
        world.setBlock(x, y, z, id, data, 2);
        org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(world, entityplayer, blockstate, clickedX, clickedY, clickedZ);

        if (event.isCancelled() || !event.canBuild())
        {
            blockstate.update(true, false);
            world.callingPlaceEvent = false;
            return false;
        }

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
            block.onBlockPlacedBy(world, x, y, z, entityplayer, itemstack);
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

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
     * different names based on their damage or NBT.
     */
    public String getUnlocalizedName(ItemStack par1ItemStack)
    {
        return Block.blocksList[this.blockID].getUnlocalizedName();
    }

    /**
     * Returns the unlocalized name of this item.
     */
    public String getUnlocalizedName()
    {
        return Block.blocksList[this.blockID].getUnlocalizedName();
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
       if (!world.setBlock(x, y, z, this.blockID, metadata, 3))
       {
           return false;
       }

       if (world.getBlockId(x, y, z) == this.blockID) {
           if (world.callingPlaceEvent) MinecraftForge.EVENT_BUS.pauseEvents = true; // MCPC+ -- don't let mods post events if simulating place block
           Block.blocksList[this.blockID].onBlockPlacedBy(world, x, y, z, player, stack);
           Block.blocksList[this.blockID].onPostBlockPlaced(world, x, y, z, metadata);
           if (world.callingPlaceEvent) MinecraftForge.EVENT_BUS.pauseEvents = false; // MCPC+ -- re-enable let mods post events if simulating place block
       }

       return true;
    }
}
