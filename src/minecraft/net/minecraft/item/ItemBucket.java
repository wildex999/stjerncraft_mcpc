package net.minecraft.item;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
// CraftBukkit end
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.entity.player.FillBucketEvent;

public class ItemBucket extends Item
{
    /** field for checking if the bucket has been filled. */
    private int isFull;

    public ItemBucket(int par1, int par2)
    {
        super(par1);
        this.maxStackSize = 1;
        this.isFull = par2;
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        float f = 1.0F;
        double d0 = par3EntityPlayer.prevPosX + (par3EntityPlayer.posX - par3EntityPlayer.prevPosX) * (double)f;
        double d1 = par3EntityPlayer.prevPosY + (par3EntityPlayer.posY - par3EntityPlayer.prevPosY) * (double)f + 1.62D - (double)par3EntityPlayer.yOffset;
        double d2 = par3EntityPlayer.prevPosZ + (par3EntityPlayer.posZ - par3EntityPlayer.prevPosZ) * (double)f;
        boolean flag = this.isFull == 0;
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(par2World, par3EntityPlayer, flag);

        if (movingobjectposition == null)
        {
            return par1ItemStack;
        }
        else
        {
            FillBucketEvent event = new FillBucketEvent(par3EntityPlayer, par1ItemStack, par2World, movingobjectposition);
            if (MinecraftForge.EVENT_BUS.post(event))
            {
                return par1ItemStack;
            }

            if (event.getResult() == Event.Result.ALLOW)
            {
                if (par3EntityPlayer.capabilities.isCreativeMode)
                {
                    return par1ItemStack;
                }

                if (--par1ItemStack.stackSize <= 0)
                {
                    return event.result;
                }

                if (!par3EntityPlayer.inventory.addItemStackToInventory(event.result))
                    {
                    par3EntityPlayer.dropPlayerItem(event.result);
                    }

                    return par1ItemStack;
                }

            if (movingobjectposition.typeOfHit == EnumMovingObjectType.TILE)
            {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                if (!par2World.canMineBlock(par3EntityPlayer, i, j, k))
                {
                    return par1ItemStack;
                }

                if (this.isFull == 0)
                {
                    if (!par3EntityPlayer.canPlayerEdit(i, j, k, movingobjectposition.sideHit, par1ItemStack))
                    {
                        return par1ItemStack;
                    }

                    if (par2World.getBlockMaterial(i, j, k) == Material.water && par2World.getBlockMetadata(i, j, k) == 0)
                    {
                        // CraftBukkit start
                        PlayerBucketFillEvent cbEvent = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, i, j, k, -1, par1ItemStack, Item.bucketWater);

                        if (cbEvent.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        // CraftBukkit end
                        par2World.setBlockWithNotify(i, j, k, 0);

                        if (par3EntityPlayer.capabilities.isCreativeMode)
                        {
                            return par1ItemStack;
                        }

                        ItemStack result = CraftItemStack.asNMSCopy(cbEvent.getItemStack()); // CraftBukkit - TODO: Check this stuff later... Not sure how this behavior should work

                        if (--par1ItemStack.stackSize <= 0)
                        {
                            return result; // CraftBukkit
                        }

                        if (!par3EntityPlayer.inventory.addItemStackToInventory(result))   // CraftBukkit
                        {
                            par3EntityPlayer.dropPlayerItem(CraftItemStack.asNMSCopy(cbEvent.getItemStack())); // CraftBukkit
                        }

                        return par1ItemStack;
                    }

                    if (par2World.getBlockMaterial(i, j, k) == Material.lava && par2World.getBlockMetadata(i, j, k) == 0)
                    {
                        // CraftBukkit start
                        PlayerBucketFillEvent cbEvent = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, i, j, k, -1, par1ItemStack, Item.bucketLava);

                        if (cbEvent.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        // CraftBukkit end
                        par2World.setBlockWithNotify(i, j, k, 0);

                        if (par3EntityPlayer.capabilities.isCreativeMode)
                        {
                            return par1ItemStack;
                        }

                        ItemStack result = CraftItemStack.asNMSCopy(cbEvent.getItemStack()); // CraftBukkit - TODO: Check this stuff later... Not sure how this behavior should work

                        if (--par1ItemStack.stackSize <= 0)
                        {
                            return result; // CraftBukkit
                        }

                        if (!par3EntityPlayer.inventory.addItemStackToInventory(result))   // CraftBukkit
                        {
                            par3EntityPlayer.dropPlayerItem(CraftItemStack.asNMSCopy(cbEvent.getItemStack())); // CraftBukkit
                        }

                        return par1ItemStack;
                    }
                }
                else
                {
                    if (this.isFull < 0)
                    {
                        // CraftBukkit start
                        PlayerBucketEmptyEvent cbEvent = CraftEventFactory.callPlayerBucketEmptyEvent(par3EntityPlayer, i, j, k, movingobjectposition.sideHit, par1ItemStack);

                        if (cbEvent.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        return CraftItemStack.asNMSCopy(cbEvent.getItemStack());
                    }

                    int clickedX = i, clickedY = j, clickedZ = k;
                    // CraftBukkit end

                    if (movingobjectposition.sideHit == 0)
                    {
                        --j;
                    }

                    if (movingobjectposition.sideHit == 1)
                    {
                        ++j;
                    }

                    if (movingobjectposition.sideHit == 2)
                    {
                        --k;
                    }

                    if (movingobjectposition.sideHit == 3)
                    {
                        ++k;
                    }

                    if (movingobjectposition.sideHit == 4)
                    {
                        --i;
                    }

                    if (movingobjectposition.sideHit == 5)
                    {
                        ++i;
                    }

                    if (!par3EntityPlayer.canPlayerEdit(i, j, k, movingobjectposition.sideHit, par1ItemStack))
                    {
                        return par1ItemStack;
                    }

                    // CraftBukkit start
                    PlayerBucketEmptyEvent cbEvent = CraftEventFactory.callPlayerBucketEmptyEvent(par3EntityPlayer, clickedX, clickedY, clickedZ, movingobjectposition.sideHit, par1ItemStack);

                    if (cbEvent.isCancelled())
                    {
                        return par1ItemStack;
                    }

                    // CraftBukkit end

                    if (this.tryPlaceContainedLiquid(par2World, d0, d1, d2, i, j, k) && !par3EntityPlayer.capabilities.isCreativeMode)
                    {
                        return CraftItemStack.asNMSCopy(cbEvent.getItemStack()); // CraftBukkit
                    }
                }
            }
            else if (this.isFull == 0 && movingobjectposition.entityHit instanceof EntityCow)
            {
                // CraftBukkit start - This codepath seems to be *NEVER* called
                org.bukkit.Location loc = movingobjectposition.entityHit.getBukkitEntity().getLocation();
                PlayerBucketFillEvent cbEvent = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), -1, par1ItemStack, Item.bucketMilk);

                if (cbEvent.isCancelled())
                {
                    return par1ItemStack;
                }

                return CraftItemStack.asNMSCopy(cbEvent.getItemStack());
                // CraftBukkit end
            }

            return par1ItemStack;
        }
    }

    /**
     * Attempts to place the liquid contained inside the bucket.
     */
    public boolean tryPlaceContainedLiquid(World par1World, double par2, double par4, double par6, int par8, int par9, int par10)
    {
        if (this.isFull <= 0)
        {
            return false;
        }
        else if (!par1World.isAirBlock(par8, par9, par10) && par1World.getBlockMaterial(par8, par9, par10).isSolid())
        {
            return false;
        }
        else
        {
            if (par1World.provider.isHellWorld && this.isFull == Block.waterMoving.blockID)
            {
                par1World.playSoundEffect(par2 + 0.5D, par4 + 0.5D, par6 + 0.5D, "random.fizz", 0.5F, 2.6F + (par1World.rand.nextFloat() - par1World.rand.nextFloat()) * 0.8F);

                for (int l = 0; l < 8; ++l)
                {
                    par1World.spawnParticle("largesmoke", (double)par8 + Math.random(), (double)par9 + Math.random(), (double)par10 + Math.random(), 0.0D, 0.0D, 0.0D);
                }
            }
            else
            {
                par1World.setBlockAndMetadataWithNotify(par8, par9, par10, this.isFull, 0);
            }

            return true;
        }
    }
}
