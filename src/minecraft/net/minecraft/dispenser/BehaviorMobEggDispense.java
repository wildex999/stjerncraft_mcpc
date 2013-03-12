package net.minecraft.dispenser;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
import net.minecraft.world.World;
// CraftBukkit end
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;

public class BehaviorMobEggDispense extends BehaviorDefaultDispenseItem
{
    /** Reference to the MinecraftServer object. */
    final MinecraftServer mcServer;

    public BehaviorMobEggDispense(MinecraftServer par1)
    {
        this.mcServer = par1;
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = EnumFacing.getFront(par1IBlockSource.func_82620_h());
        double d0 = par1IBlockSource.getX() + (double)enumfacing.getFrontOffsetX();
        double d1 = (double)((float)par1IBlockSource.getYInt() + 0.2F);
        double d2 = par1IBlockSource.getZ() + (double)enumfacing.getFrontOffsetZ();
        // CraftBukkit start
        World world = par1IBlockSource.getWorld();
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d0, d1, d2));

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
            IBehaviorDispenseItem idispensebehavior = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());

            if (idispensebehavior != IBehaviorDispenseItem.itemDispenseBehaviorProvider && idispensebehavior != this)
            {
                idispensebehavior.dispense(par1IBlockSource, eventStack);
                return par2ItemStack;
            }
        }

        itemstack1 = CraftItemStack.asNMSCopy(event.getItem());
        ItemMonsterPlacer.spawnCreature(par1IBlockSource.getWorld(), itemstack1.getItemDamage(), event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
        // itemstack.a(1); // Handled during event processing
        // CraftBukkit end
        return par2ItemStack;
    }
}
