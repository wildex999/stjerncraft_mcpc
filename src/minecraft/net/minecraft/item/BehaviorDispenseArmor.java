package net.minecraft.item;

import java.util.List;
import net.minecraft.block.BlockDispenser;
import net.minecraft.command.IEntitySelector;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySelectorArmoredMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

final class BehaviorDispenseArmor extends BehaviorDefaultDispenseItem
{
    BehaviorDispenseArmor() {}

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    protected ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt() + enumfacing.getFrontOffsetY();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        AxisAlignedBB axisalignedbb = AxisAlignedBB.getAABBPool().getAABB((double)i, (double)j, (double)k, (double)(i + 1), (double)(j + 1), (double)(k + 1));
        List list = par1IBlockSource.getWorld().selectEntitiesWithinAABB(EntityLiving.class, axisalignedbb, (IEntitySelector)(new EntitySelectorArmoredMob(par2ItemStack)));

        if (list.size() > 0)
        {
            EntityLiving entityliving = (EntityLiving)list.get(0);
            int l = entityliving instanceof EntityPlayer ? 1 : 0;
            int i1 = EntityLiving.getArmorPosition(par2ItemStack);
            // CraftBukkit start
            ItemStack itemstack1 = par2ItemStack.splitStack(1);
            World world = par1IBlockSource.getWorld();
            org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
            BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));

            if (!BlockDispenser.eventFired)
            {
                world.getServer().getPluginManager().callEvent(event);
            }

            if (event.isCancelled())
            {
                par2ItemStack.stackSize++;
                return par2ItemStack;
            }

            if (!event.getItem().equals(craftItem))
            {
                par2ItemStack.stackSize++;
                // Chain to handler for new item
                ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                IBehaviorDispenseItem ibehaviordispenseitem = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());

                if (ibehaviordispenseitem != IBehaviorDispenseItem.itemDispenseBehaviorProvider && ibehaviordispenseitem != this)
                {
                    ibehaviordispenseitem.dispense(par1IBlockSource, eventStack);
                    return par2ItemStack;
                }
            }

            // CraftBukkit end
            itemstack1.stackSize = 1;
            entityliving.setCurrentItemOrArmor(i1, itemstack1); //Forge: Vanilla bug fix associated with fixed setCurrentItemOrArmor indexs for players.
            entityliving.func_96120_a(i1, 2.0F);
            // --itemstack.count; // CraftBukkit - handled above
            return par2ItemStack;
        }
        else
        {
            return super.dispenseStack(par1IBlockSource, par2ItemStack);
        }
    }
}
