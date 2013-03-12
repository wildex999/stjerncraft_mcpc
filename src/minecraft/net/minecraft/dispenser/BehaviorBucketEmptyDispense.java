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
    private final BehaviorDefaultDispenseItem field_92073_c;

    /** Reference to the MinecraftServer object. */
    final MinecraftServer mcServer;

    public BehaviorBucketEmptyDispense(MinecraftServer par1)
    {
        this.mcServer = par1;
        this.field_92073_c = new BehaviorDefaultDispenseItem();
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = EnumFacing.getFront(par1IBlockSource.func_82620_h());
        World world = par1IBlockSource.getWorld();
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        Material material = world.getBlockMaterial(i, j, k);
        int l = world.getBlockMetadata(i, j, k);
        Item item;

        if (Material.water.equals(material) && l == 0)
        {
            item = Item.bucketWater;
        }
        else
        {
            if (!Material.lava.equals(material) || l != 0)
            {
                return super.dispenseStack(par1IBlockSource, par2ItemStack);
            }

            item = Item.bucketLava;
        }

        // CraftBukkit start
        org.bukkit.block.Block block = world.getWorld().getBlockAt(i, j, k);
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
        // CraftBukkit end
        world.setBlockWithNotify(i, j, k, 0);

        if (--par2ItemStack.stackSize == 0)
        {
            par2ItemStack.itemID = item.itemID;
            par2ItemStack.stackSize = 1;
        }
        else if (((TileEntityDispenser)par1IBlockSource.func_82619_j()).addItem(new ItemStack(item)) < 0)
        {
            this.field_92073_c.dispense(par1IBlockSource, new ItemStack(item));
        }

        return par2ItemStack;
    }
}
