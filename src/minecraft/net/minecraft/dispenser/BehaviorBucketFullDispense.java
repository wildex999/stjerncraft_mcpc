package net.minecraft.dispenser;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.world.World;
// CraftBukkit end
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;

public class BehaviorBucketFullDispense extends BehaviorDefaultDispenseItem
{
    /** Reference to the BehaviorDefaultDispenseItem object. */
    private final BehaviorDefaultDispenseItem defaultItemDispenseBehavior;

    final MinecraftServer mcServer;

    public BehaviorBucketFullDispense(MinecraftServer par1)
    {
        this.mcServer = par1;
        this.defaultItemDispenseBehavior = new BehaviorDefaultDispenseItem();
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        ItemBucket var3 = (ItemBucket)par2ItemStack.getItem();
        int var4 = par1IBlockSource.getXInt();
        int var5 = par1IBlockSource.getYInt();
        int var6 = par1IBlockSource.getZInt();
        EnumFacing var7 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        // CraftBukkit start
        World world = par1IBlockSource.getWorld();
        int i2 = var4 + var7.func_82601_c();
        int k2 = var6 + var7.func_82599_e();

        if (world.isAirBlock(i2, var5, k2) || world.getBlockMaterial(i2, var5, k2).isSolid())
        {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(var4, var5, var6);
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(par2ItemStack);
            BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));

            if (!BlockDispenser.eventFired)
            {
                world.getServer().getPluginManager().callEvent(event);
            }

            if (event.isCancelled())
            {
                return par2ItemStack;
            }

            if (!event.getItem().equals(craftItem))
            {
                // Chain to handler for new item
                ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                IBehaviorDispenseItem idispensebehavior = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.func_82594_a(eventStack.getItem());

                if (idispensebehavior != IBehaviorDispenseItem.itemDispenseBehaviorProvider && idispensebehavior != this)
                {
                    idispensebehavior.dispense(par1IBlockSource, eventStack);
                    return par2ItemStack;
                }
            }

            var3 = (ItemBucket) CraftItemStack.asNMSCopy(event.getItem()).getItem();
        }

        // CraftBukkit end

        if (var3.tryPlaceContainedLiquid(par1IBlockSource.getWorld(), (double)var4, (double)var5, (double)var6, var4 + var7.func_82601_c(), var5, var6 + var7.func_82599_e()))
        {
            // CraftBukkit start - handle stacked buckets
            Item item = Item.bucketEmpty;

            if (--par2ItemStack.stackSize == 0)
            {
                par2ItemStack.itemID = item.itemID;
                par2ItemStack.stackSize = 1;
            }
            else if (((TileEntityDispenser) par1IBlockSource.func_82619_j()).addItem(new ItemStack(item)) < 0)
            {
                this.defaultItemDispenseBehavior.dispense(par1IBlockSource, new ItemStack(item));
            }

            // CraftBukkit end
            return par2ItemStack;
        }
        else
        {
            return this.defaultItemDispenseBehavior.dispense(par1IBlockSource, par2ItemStack);
        }
    }
}
