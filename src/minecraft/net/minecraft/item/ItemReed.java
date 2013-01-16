package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.block.CraftBlockState; // CraftBukkit

public class ItemReed extends Item
{
    /** The ID of the block the reed will spawn when used from inventory bar. */
    private int spawnID;

    public ItemReed(int par1, Block par2Block)
    {
        super(par1);
        this.spawnID = par2Block.blockID;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int var11 = par4, var12 = par5, var13 = par6; // CraftBukkit
        int i1 = par3World.getBlockId(par4, par5, par6);

        if (i1 == Block.snow.blockID)
        {
            par7 = 1;
        }
        else if (i1 != Block.vine.blockID && i1 != Block.tallGrass.blockID && i1 != Block.deadBush.blockID)
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

        if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else
        {
            if (par3World.canPlaceEntityOnSide(this.spawnID, par4, par5, par6, false, par7, (Entity)null))
            {
                Block block = Block.blocksList[this.spawnID];
                int j1 = block.onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, 0);
                // CraftBukkit start - This executes the placement of the block
                CraftBlockState replacedBlockState = CraftBlockState.getBlockState(par3World, par4, par5, par6); // CraftBukkit

                /*
                 * @see net.minecraft.server.World#setTypeId(int i, int j, int k, int l)
                 *
                 * This replaces world.setTypeId(IIII), we're doing this because we need to
                 * hook between the 'placement' and the informing to 'world' so we can
                 * sanely undo this.
                 *
                 * Whenever the call to 'world.setTypeId' changes we need to figure out again what to
                 * replace this with.
                 */
                if (par3World.setBlockAndMetadata(par4, par5, par6, this.spawnID, j1))   // <-- world.e does this to place the block
                {
                    org.bukkit.event.block.BlockPlaceEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(par3World, par2EntityPlayer, replacedBlockState, var11, var12, var13);

                    if (event.isCancelled() || !event.canBuild())
                    {
                        // CraftBukkit - undo; this only has reed, repeater and pie blocks
                        par3World.setBlockAndMetadataWithNotify(par4, par5, par6, replacedBlockState.getTypeId(), replacedBlockState.getRawData());
                        return true;
                    }

                    par3World.notifyBlockChange(par4, par5, par6, this.spawnID); // <-- world.setTypeId does this on success (tell the world)
                    // CraftBukkit end

                    if (par3World.getBlockId(par4, par5, par6) == this.spawnID)
                    {
                        Block.blocksList[this.spawnID].onBlockPlacedBy(par3World, par4, par5, par6, par2EntityPlayer);
                        Block.blocksList[this.spawnID].onPostBlockPlaced(par3World, par4, par5, par6, j1);
                    }

                    par3World.playSoundEffect((double)((float) par4 + 0.5F), (double)((float) par5 + 0.5F), (double)((float) par6 + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
                    --par1ItemStack.stackSize;
                }
            }

            return true;
        }
    }
}
