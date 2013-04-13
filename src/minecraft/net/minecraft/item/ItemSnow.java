package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
public class ItemSnow extends ItemBlockWithMetadata
{
    public ItemSnow(int par1, Block par2Block)
    {
        super(par1, par2Block);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        final int clickedX = par4, clickedY = par5, clickedZ = par6;

        if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else
        {
            int i1 = par3World.getBlockId(par4, par5, par6);

            if (i1 == Block.snow.blockID)
            {
                Block block = Block.blocksList[this.getBlockID()];
                int j1 = par3World.getBlockMetadata(par4, par5, par6);
                int k1 = j1 & 7;

                // CraftBukkit start - Redirect to common handler
                if (k1 <= 6 && par3World.checkNoEntityCollision(block.getCollisionBoundingBoxFromPool(par3World, par4, par5, par6)) && ItemBlock.processBlockPlace(par3World, par2EntityPlayer, par1ItemStack, par4, par5, par6, Block.snow.blockID, k1 + 1 | j1 & -8, clickedX, clickedY, clickedZ))
                {
                    return true;
                }

                /*
                if (k1 <= 6 && world.b(block.b(world, i, j, k)) && world.setData(i, j, k, k1 + 1 | j1 & -8, 2)) {
                    world.makeSound((double) ((float) i + 0.5F), (double) ((float) j + 0.5F), (double) ((float) k + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume1() + 1.0F) / 2.0F, block.stepSound.getVolume2() * 0.8F);
                    --itemstack.count;
                    return true;
                }
                */
                // CraftBukkit end
            }

            return super.onItemUse(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10);
        }
    }
}
