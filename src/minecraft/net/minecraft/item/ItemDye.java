package net.minecraft.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCloth;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockStem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.entity.player.BonemealEvent;
// CraftBukkit start
import net.minecraft.entity.player.EntityPlayerMP;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SheepDyeWoolEvent;
// CraftBukkit end

public class ItemDye extends Item
{
    /** List of dye color names */
    public static final String[] dyeColorNames = new String[] {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow", "lightBlue", "magenta", "orange", "white"};
    public static final int[] dyeColors = new int[] {1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320};

    public ItemDye(int par1)
    {
        super(par1);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setCreativeTab(CreativeTabs.tabMaterials);
    }

    @SideOnly(Side.CLIENT)

    /**
     * Gets an icon index based on an item's damage value
     */
    public int getIconFromDamage(int par1)
    {
        int var2 = MathHelper.clamp_int(par1, 0, 15);
        return this.iconIndex + var2 % 8 * 16 + var2 / 8;
    }

    public String getItemNameIS(ItemStack par1ItemStack)
    {
        int var2 = MathHelper.clamp_int(par1ItemStack.getItemDamage(), 0, 15);
        return super.getItemName() + "." + dyeColorNames[var2];
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else
        {
            int var11;
            int var12;
            int var13;

            if (par1ItemStack.getItemDamage() == 15)
            {
                var11 = par3World.getBlockId(par4, par5, par6);

                BonemealEvent event = new BonemealEvent(par2EntityPlayer, par3World, var11, par4, par5, par6);
                if (MinecraftForge.EVENT_BUS.post(event))
                {
                    return false;
                }

                if (event.getResult() == Result.ALLOW)
                {
                    if (!par3World.isRemote)
                    {
                        par1ItemStack.stackSize--;
                    }
                    return true;
                }

                if (var11 == Block.sapling.blockID)
                {
                    if (!par3World.isRemote)
                    {
                        // CraftBukkit start
                        Player player = (par2EntityPlayer instanceof EntityPlayerMP) ? (Player) par2EntityPlayer.getBukkitEntity() : null;
                        ((BlockSapling) Block.sapling).growTree(par3World, par4, par5, par6, par3World.rand, true, player, par1ItemStack);
                        //--itemstack.count; - called later if the bonemeal attempt was succesful
                        // CraftBukkit end
                    }

                    return true;
                }

                if (var11 == Block.mushroomBrown.blockID || var11 == Block.mushroomRed.blockID)
                {
                    // CraftBukkit start
                    if (!par3World.isRemote)
                    {
                        Player player = (par2EntityPlayer instanceof EntityPlayerMP) ? (Player) par2EntityPlayer.getBukkitEntity() : null;
                        ((BlockMushroom) Block.blocksList[var11]).fertilizeMushroom(par3World, par4, par5, par6, par3World.rand, true, player, par1ItemStack);
                        //--itemstack.count; - called later if the bonemeal attempt was succesful
                        // CraftBukkit end
                    }

                    return true;
                }

                if (var11 == Block.melonStem.blockID || var11 == Block.pumpkinStem.blockID)
                {
                    if (par3World.getBlockMetadata(par4, par5, par6) == 7)
                    {
                        return false;
                    }

                    if (!par3World.isRemote)
                    {
                        ((BlockStem)Block.blocksList[var11]).fertilizeStem(par3World, par4, par5, par6);
                        --par1ItemStack.stackSize;
                    }

                    return true;
                }

                if (var11 > 0 && Block.blocksList[var11] instanceof BlockCrops)
                {
                    if (par3World.getBlockMetadata(par4, par5, par6) == 7)
                    {
                        return false;
                    }

                    if (!par3World.isRemote)
                    {
                        ((BlockCrops)Block.blocksList[var11]).fertilize(par3World, par4, par5, par6);
                        --par1ItemStack.stackSize;
                    }

                    return true;
                }

                if (var11 == Block.cocoaPlant.blockID)
                {
                    if (!par3World.isRemote)
                    {
                        par3World.setBlockMetadataWithNotify(par4, par5, par6, 8 | BlockDirectional.getDirection(par3World.getBlockMetadata(par4, par5, par6)));
                        --par1ItemStack.stackSize;
                    }

                    return true;
                }

                if (var11 == Block.grass.blockID)
                {
                    if (!par3World.isRemote)
                    {
                        --par1ItemStack.stackSize;
                        label133:

                        for (var12 = 0; var12 < 128; ++var12)
                        {
                            var13 = par4;
                            int var14 = par5 + 1;
                            int var15 = par6;

                            for (int var16 = 0; var16 < var12 / 16; ++var16)
                            {
                                var13 += itemRand.nextInt(3) - 1;
                                var14 += (itemRand.nextInt(3) - 1) * itemRand.nextInt(3) / 2;
                                var15 += itemRand.nextInt(3) - 1;

                                if (par3World.getBlockId(var13, var14 - 1, var15) != Block.grass.blockID || par3World.isBlockNormalCube(var13, var14, var15))
                                {
                                    continue label133;
                                }
                            }

                            if (par3World.getBlockId(var13, var14, var15) == 0)
                            {
                                if (itemRand.nextInt(10) != 0)
                                {
                                    if (Block.tallGrass.canBlockStay(par3World, var13, var14, var15))
                                    {
                                        par3World.setBlockAndMetadataWithNotify(var13, var14, var15, Block.tallGrass.blockID, 1);
                                    }
                                }
                                else
                                {
                                    ForgeHooks.plantGrass(par3World, var13, var14, var15);
                                }
                            }
                        }
                    }

                    return true;
                }
            }
            else if (par1ItemStack.getItemDamage() == 3)
            {
                var11 = par3World.getBlockId(par4, par5, par6);
                var12 = par3World.getBlockMetadata(par4, par5, par6);

                if (var11 == Block.wood.blockID && BlockLog.limitToValidMetadata(var12) == 3)
                {
                    if (par7 == 0)
                    {
                        return false;
                    }

                    if (par7 == 1)
                    {
                        return false;
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

                    if (par3World.isAirBlock(par4, par5, par6))
                    {
                        var13 = Block.blocksList[Block.cocoaPlant.blockID].onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, 0);
                        par3World.setBlockAndMetadataWithNotify(par4, par5, par6, Block.cocoaPlant.blockID, var13);

                        if (!par2EntityPlayer.capabilities.isCreativeMode)
                        {
                            --par1ItemStack.stackSize;
                        }
                    }

                    return true;
                }
            }

            return false;
        }
    }

    /**
     * dye sheep, place saddles, etc ...
     */
    public boolean itemInteractionForEntity(ItemStack par1ItemStack, EntityLiving par2EntityLiving)
    {
        if (par2EntityLiving instanceof EntitySheep)
        {
            EntitySheep var3 = (EntitySheep)par2EntityLiving;
            int var4 = BlockCloth.getBlockFromDye(par1ItemStack.getItemDamage());

            if (!var3.getSheared() && var3.getFleeceColor() != var4)
            {
                // CraftBukkit start
                byte bColor = (byte) var4;
                SheepDyeWoolEvent event = new SheepDyeWoolEvent((org.bukkit.entity.Sheep) var3.getBukkitEntity(), org.bukkit.DyeColor.getByData(bColor));
                var3.worldObj.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return false;
                }

                var4 = (byte) event.getColor().getData();
                // CraftBukkit end
                var3.setFleeceColor(var4);
                --par1ItemStack.stackSize;
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
    {
        for (int var4 = 0; var4 < 16; ++var4)
        {
            par3List.add(new ItemStack(par1, 1, var4));
        }
    }
}
