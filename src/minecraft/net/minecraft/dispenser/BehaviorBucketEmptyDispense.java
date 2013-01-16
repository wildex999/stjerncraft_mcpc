package net.minecraft.dispenser;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
// CraftBukkit end
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BehaviorBucketEmptyDispense extends BehaviorDefaultDispenseItem
{
    private final BehaviorDefaultDispenseItem field_92018_c;

    /** Reference to the MinecraftServer object. */
    final MinecraftServer mcServer;

    public BehaviorBucketEmptyDispense(MinecraftServer par1)
    {
        this.mcServer = par1;
        this.field_92018_c = new BehaviorDefaultDispenseItem();
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing var3 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        World var4 = par1IBlockSource.getWorld();
        int var5 = par1IBlockSource.getXInt() + var3.func_82601_c();
        int var6 = par1IBlockSource.getYInt();
        int var7 = par1IBlockSource.getZInt() + var3.func_82599_e();
        Material var8 = var4.getBlockMaterial(var5, var6, var7);
        int var9 = var4.getBlockMetadata(var5, var6, var7);
        Item var10;

        if (Material.water.equals(var8) && var9 == 0)
        {
            var10 = Item.bucketWater;
        }
        else
        {
            if (!Material.lava.equals(var8) || var9 != 0)
            {
                return super.dispenseStack(par1IBlockSource, par2ItemStack);
            }

            var10 = Item.bucketLava;
        }

        // CraftBukkit start
        org.bukkit.block.Block block = var4.getWorld().getBlockAt(var5, var6, var7);
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(par2ItemStack);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));

        if (!BlockDispenser.eventFired)
        {
            var4.getServer().getPluginManager().callEvent(event);
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
        // CraftBukkit end
        var4.setBlockWithNotify(var5, var6, var7, 0);

        if (--par2ItemStack.stackSize == 0)
        {
            par2ItemStack.itemID = var10.itemID;
            par2ItemStack.stackSize = 1;
        }
        else if (((TileEntityDispenser)par1IBlockSource.func_82619_j()).addItem(new ItemStack(var10)) < 0)
        {
            this.field_92018_c.dispense(par1IBlockSource, new ItemStack(var10));
        }

        return par2ItemStack;
    }
}
