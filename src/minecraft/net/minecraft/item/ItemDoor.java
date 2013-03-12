package net.minecraft.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet53BlockChange;
import org.bukkit.craftbukkit.block.CraftBlockState; // CraftBukkit
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class ItemDoor extends Item
{
    private Material doorMaterial;

    public ItemDoor(int par1, Material par2Material)
    {
        super(par1);
        this.doorMaterial = par2Material;
        this.maxStackSize = 1;
        this.setCreativeTab(CreativeTabs.tabRedstone);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        final int clickedX = par4, clickedY = par5, clickedZ = par6; // CraftBukkit

        if (par7 != 1)
        {
            return false;
        }
        else
        {
            ++par5;
            Block block;

            if (this.doorMaterial == Material.wood)
            {
                block = Block.doorWood;
            }
            else
            {
                block = Block.doorSteel;
            }

            if (par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack) && par2EntityPlayer.canPlayerEdit(par4, par5 + 1, par6, par7, par1ItemStack))
            {
                if (!block.canPlaceBlockAt(par3World, par4, par5, par6))
                {
                    return false;
                }
                else
                {
                    int i1 = MathHelper.floor_double((double)((par2EntityPlayer.rotationYaw + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3;

                    // CraftBukkit start
                    if (!place(par3World, par4, par5, par6, i1, block, par2EntityPlayer, clickedX, clickedY, clickedZ))
                    {
                        return false;
                    }

                    // CraftBukkit end
                    --par1ItemStack.stackSize;
                    return true;
                }
            }
            else
            {
                return false;
            }
        }
    }

    public static void placeDoorBlock(World par0World, int par1, int par2, int par3, int par4, Block par5Block)
    {
        // CraftBukkit start
        place(par0World, par1, par2, par3, par4, par5Block, null, par1, par2, par3);
    }

    public static boolean place(World world, int i, int j, int k, int l, Block block, EntityPlayer entityhuman, int clickedX, int clickedY, int clickedZ)
    {
        // CraftBukkit end
        byte b0 = 0;
        byte b1 = 0;

        if (l == 0)
        {
            b1 = 1;
        }

        if (l == 1)
        {
            b0 = -1;
        }

        if (l == 2)
        {
            b1 = -1;
        }

        if (l == 3)
        {
            b0 = 1;
        }

        int i1 = (world.isBlockNormalCube(i - b0, j, k - b1) ? 1 : 0) + (world.isBlockNormalCube(i - b0, j + 1, k - b1) ? 1 : 0);
        int j1 = (world.isBlockNormalCube(i + b0, j, k + b1) ? 1 : 0) + (world.isBlockNormalCube(i + b0, j + 1, k + b1) ? 1 : 0);
        boolean flag = world.getBlockId(i - b0, j, k - b1) == block.blockID || world.getBlockId(i - b0, j + 1, k - b1) == block.blockID;
        boolean flag1 = world.getBlockId(i + b0, j, k + b1) == block.blockID || world.getBlockId(i + b0, j + 1, k + b1) == block.blockID;
        boolean flag2 = false;

        if (flag && !flag1)
        {
            flag2 = true;
        }
        else if (j1 > i1)
        {
            flag2 = true;
        }

        world.editingBlocks = true;

        // CraftBukkit start
        if (entityhuman != null)
        {
            if (!ItemBlock.processBlockPlace(world, entityhuman, null, i, j, k, block.blockID, l, clickedX, clickedY, clickedZ))
            {
                ((EntityPlayerMP) entityhuman).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j + 1, k, world));
                return false;
            }

            if (world.getBlockId(i, j, k) != block.blockID)
            {
                ((EntityPlayerMP) entityhuman).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j + 1, k, world));
                return true;
            }

            world.editingBlocks = true;
        }
        else
        {
            world.setBlockAndMetadataWithNotify(i, j, k, block.blockID, l);
        }

        // CraftBukkit end
        world.setBlockAndMetadataWithNotify(i, j + 1, k, block.blockID, 8 | (flag2 ? 1 : 0));
        world.editingBlocks = false;
        world.notifyBlocksOfNeighborChange(i, j, k, block.blockID);
        world.notifyBlocksOfNeighborChange(i, j + 1, k, block.blockID);
        return true; // CraftBukkit
    }
}
