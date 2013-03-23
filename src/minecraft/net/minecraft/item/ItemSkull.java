package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
public class ItemSkull extends Item
{
    private static final String[] skullTypes = new String[] {"skeleton", "wither", "zombie", "char", "creeper"};
    public static final String[] field_94587_a = new String[] {"skull_skeleton", "skull_wither", "skull_zombie", "skull_char", "skull_creeper"};

    public ItemSkull(int par1)
    {
        super(par1);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        final int clickedX = par4, clickedY = par5, clickedZ = par6; // CraftBukkit

        if (par7 == 0)
        {
            return false;
        }
        else if (!par3World.getBlockMaterial(par4, par5, par6).isSolid())
        {
            return false;
        }
        else
        {
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

            if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
            {
                return false;
            }
            else if (!Block.skull.canPlaceBlockAt(par3World, par4, par5, par6))
            {
                return false;
            }
            else
            {
                // CraftBukkit start - handle in ItemBlock
                // world.setTypeIdAndData(i, j, k, Block.SKULL.id, l, 2);
                if (!ItemBlock.processBlockPlace(par3World, par2EntityPlayer, null, par4, par5, par6, Block.skull.blockID, par7, clickedX, clickedY, clickedZ))
                {
                    return false;
                }

                par7 = par3World.getBlockMetadata(par4, par5, par6);
                // CraftBukkit end
                int i1 = 0;

                if (par7 == 1)
                {
                    i1 = MathHelper.floor_double((double)(par2EntityPlayer.rotationYaw * 16.0F / 360.0F) + 0.5D) & 15;
                }

                TileEntity tileentity = par3World.getBlockTileEntity(par4, par5, par6);

                if (tileentity != null && tileentity instanceof TileEntitySkull)
                {
                    String s = "";

                    if (par1ItemStack.hasTagCompound() && par1ItemStack.getTagCompound().hasKey("SkullOwner"))
                    {
                        s = par1ItemStack.getTagCompound().getString("SkullOwner");
                    }

                    ((TileEntitySkull)tileentity).setSkullType(par1ItemStack.getItemDamage(), s);
                    ((TileEntitySkull)tileentity).setSkullRotation(i1);
                    ((BlockSkull)Block.skull).makeWither(par3World, par4, par5, par6, (TileEntitySkull)tileentity);
                }

                --par1ItemStack.stackSize;
                return true;
            }
        }
    }

    /**
     * Returns the metadata of the block which this Item (ItemBlock) can place
     */
    public int getMetadata(int par1)
    {
        return par1;
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
     * different names based on their damage or NBT.
     */
    public String getUnlocalizedName(ItemStack par1ItemStack)
    {
        int i = par1ItemStack.getItemDamage();

        if (i < 0 || i >= skullTypes.length)
        {
            i = 0;
        }

        return super.getUnlocalizedName() + "." + skullTypes[i];
    }

    public String getItemDisplayName(ItemStack par1ItemStack)
    {
        return par1ItemStack.getItemDamage() == 3 && par1ItemStack.hasTagCompound() && par1ItemStack.getTagCompound().hasKey("SkullOwner") ? StatCollector.translateToLocalFormatted("item.skull.player.name", new Object[] {par1ItemStack.getTagCompound().getString("SkullOwner")}): super.getItemDisplayName(par1ItemStack);
    }
}
