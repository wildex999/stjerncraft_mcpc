package net.minecraft.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCloth;
import net.minecraft.block.BlockCocoa;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import net.minecraftforge.common.FakePlayerFactory;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.entity.player.BonemealEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SheepDyeWoolEvent;

public class ItemDye extends Item
{
    /** List of dye color names */
    public static final String[] dyeColorNames = new String[] {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow", "lightBlue", "magenta", "orange", "white"};
    public static final String[] field_94595_b = new String[] {"dyePowder_black", "dyePowder_red", "dyePowder_green", "dyePowder_brown", "dyePowder_blue", "dyePowder_purple", "dyePowder_cyan", "dyePowder_silver", "dyePowder_gray", "dyePowder_pink", "dyePowder_lime", "dyePowder_yellow", "dyePowder_lightBlue", "dyePowder_magenta", "dyePowder_orange", "dyePowder_white"};
    public static final int[] dyeColors = new int[] {1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320};

    public ItemDye(int par1)
    {
        super(par1);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        this.setCreativeTab(CreativeTabs.tabMaterials);
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
     * different names based on their damage or NBT.
     */
    public String getUnlocalizedName(ItemStack par1ItemStack)
    {
        int i = MathHelper.clamp_int(par1ItemStack.getItemDamage(), 0, 15);
        return super.getUnlocalizedName() + "." + dyeColorNames[i];
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
            if (par1ItemStack.getItemDamage() == 15)
            {
                if (a(par1ItemStack, par3World, par4, par5, par6, par2EntityPlayer))
                {
                    if (!par3World.isRemote)
                    {
                        par3World.playAuxSFX(2005, par4, par5, par6, 0);
                    }

                    return true;
                }
            }
            else if (par1ItemStack.getItemDamage() == 3)
            {
                int i1 = par3World.getBlockId(par4, par5, par6);
                int j1 = par3World.getBlockMetadata(par4, par5, par6);

                if (i1 == Block.wood.blockID && BlockLog.limitToValidMetadata(j1) == 3)
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
                        int k1 = Block.blocksList[Block.cocoaPlant.blockID].onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, 0);
                        par3World.setBlock(par4, par5, par6, Block.cocoaPlant.blockID, k1, 2);

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


    // MCPC+ end - CB compatibility (TODO: add manual plugin mapping?)
    public static boolean a(ItemStack itemstack, World world, int i, int j, int k, EntityPlayer entityplayer)
    {
        return applyBonemeal(itemstack, world, i, j, k, entityplayer);
    }
    // MCPC+ end
    
    public static boolean func_96604_a(ItemStack par0ItemStack, World par1World, int par2, int par3, int par4)
    {
        return applyBonemeal(par0ItemStack, par1World, par2, par3, par4, FakePlayerFactory.getMinecraft(par1World));
    }

    public static boolean applyBonemeal(ItemStack par0ItemStack, World par1World, int par2, int par3, int par4, EntityPlayer player)
    {
        int l = par1World.getBlockId(par2, par3, par4);

        BonemealEvent event = new BonemealEvent(player, par1World, l, par2, par3, par4);
        if (MinecraftForge.EVENT_BUS.post(event))
        {
            return false;
        }

        if (event.getResult() == Result.ALLOW)
        {
            if (!par1World.isRemote)
            {
                par0ItemStack.stackSize--;
            }
            return true;
        }
                
        if (l == Block.sapling.blockID)
        {
            if (!par1World.isRemote)
            {
                if ((double)par1World.rand.nextFloat() < 0.45D)
                {
                    // CraftBukkit start // MCPC+ - cbPlayer
                    Player cbPlayer = (player instanceof EntityPlayerMP) ? (Player) player.getBukkitEntity() : null;
                    ((BlockSapling) Block.sapling).growTree(par1World, par2, par3, par4, par1World.rand, true, cbPlayer, null);
                    // CraftBukkit end
                }

                --par0ItemStack.stackSize;
            }

            return true;
        }
        else if (l != Block.mushroomBrown.blockID && l != Block.mushroomRed.blockID)
        {
            if (l != Block.melonStem.blockID && l != Block.pumpkinStem.blockID)
            {
                if (l > 0 && Block.blocksList[l] instanceof BlockCrops)
                {
                    if (par1World.getBlockMetadata(par2, par3, par4) == 7)
                    {
                        return false;
                    }
                    else
                    {
                        if (!par1World.isRemote)
                        {
                            ((BlockCrops)Block.blocksList[l]).fertilize(par1World, par2, par3, par4);
                            --par0ItemStack.stackSize;
                        }

                        return true;
                    }
                }
                else
                {
                    int i1;
                    int j1;
                    int k1;

                    if (l == Block.cocoaPlant.blockID)
                    {
                        i1 = par1World.getBlockMetadata(par2, par3, par4);
                        j1 = BlockDirectional.getDirection(i1);
                        k1 = BlockCocoa.func_72219_c(i1);

                        if (k1 >= 2)
                        {
                            return false;
                        }
                        else
                        {
                            if (!par1World.isRemote)
                            {
                                ++k1;
                                par1World.setBlockMetadataWithNotify(par2, par3, par4, k1 << 2 | j1, 2);
                                --par0ItemStack.stackSize;
                            }

                            return true;
                        }
                    }
                    else if (l != Block.grass.blockID)
                    {
                        return false;
                    }
                    else
                    {
                        if (!par1World.isRemote)
                        {
                            --par0ItemStack.stackSize;
                            label102:

                            for (i1 = 0; i1 < 128; ++i1)
                            {
                                j1 = par2;
                                k1 = par3 + 1;
                                int l1 = par4;

                                for (int i2 = 0; i2 < i1 / 16; ++i2)
                                {
                                    j1 += itemRand.nextInt(3) - 1;
                                    k1 += (itemRand.nextInt(3) - 1) * itemRand.nextInt(3) / 2;
                                    l1 += itemRand.nextInt(3) - 1;

                                    if (par1World.getBlockId(j1, k1 - 1, l1) != Block.grass.blockID || par1World.isBlockNormalCube(j1, k1, l1))
                                    {
                                        continue label102;
                                    }
                                }

                                if (par1World.getBlockId(j1, k1, l1) == 0)
                                {
                                    if (itemRand.nextInt(10) != 0)
                                    {
                                        if (Block.tallGrass.canBlockStay(par1World, j1, k1, l1))
                                        {
                                            par1World.setBlock(j1, k1, l1, Block.tallGrass.blockID, 1, 3);
                                        }
                                    }
                                    else
                                    {
                                        ForgeHooks.plantGrass(par1World, j1, k1, l1);
                                    }
                                }
                            }
                        }

                        return true;
                    }
                }
            }
            else if (par1World.getBlockMetadata(par2, par3, par4) == 7)
            {
                return false;
            }
            else
            {
                if (!par1World.isRemote)
                {
                    ((BlockStem)Block.blocksList[l]).fertilizeStem(par1World, par2, par3, par4);
                    --par0ItemStack.stackSize;
                }

                return true;
            }
        }
        else
        {
            if (!par1World.isRemote)
            {
                if ((double)par1World.rand.nextFloat() < 0.4D)
                {
                    // CraftBukkit start - validate // MCPC+
                    Player cbPlayer = (player instanceof EntityPlayerMP) ? (Player) player.getBukkitEntity() : null;
                    ((BlockMushroom) Block.blocksList[l]).fertilizeMushroom(par1World, par2, par3, par4, par1World.rand, true, cbPlayer, par0ItemStack);
                }

                //--par0ItemStack.stackSize; - called later if the bonemeal attempt was not cancelled by a plugin
                // CraftBukkit end
            }

            return true;
        }
    }

    /**
     * Called when a player right clicks an entity with an item.
     */
    public boolean itemInteractionForEntity(ItemStack par1ItemStack, EntityLiving par2EntityLiving)
    {
        if (par2EntityLiving instanceof EntitySheep)
        {
            EntitySheep entitysheep = (EntitySheep)par2EntityLiving;
            int i = BlockCloth.getBlockFromDye(par1ItemStack.getItemDamage());

            if (!entitysheep.getSheared() && entitysheep.getFleeceColor() != i)
            {
                // CraftBukkit start
                byte bColor = (byte) i;
                SheepDyeWoolEvent event = new SheepDyeWoolEvent((org.bukkit.entity.Sheep) entitysheep.getBukkitEntity(), org.bukkit.DyeColor.getByData(bColor));
                entitysheep.worldObj.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return false;
                }

                i = (byte) event.getColor().getWoolData();
                // CraftBukkit end
                entitysheep.setFleeceColor(i);
                --par1ItemStack.stackSize;
            }

            return true;
        }
        else
        {
            return false;
        }
    }
}
