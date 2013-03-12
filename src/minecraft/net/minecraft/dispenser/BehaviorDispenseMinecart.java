package net.minecraft.dispenser;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
// CraftBukkit end
import net.minecraft.block.BlockRail;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BehaviorDispenseMinecart extends BehaviorDefaultDispenseItem
{
    /** Reference to the BehaviorDefaultDispenseItem object. */
    private final BehaviorDefaultDispenseItem defaultItemDispenseBehavior;

    final MinecraftServer mcServer;

    public BehaviorDispenseMinecart(MinecraftServer par1MinecraftServer)
    {
        this.mcServer = par1MinecraftServer;
        this.defaultItemDispenseBehavior = new BehaviorDefaultDispenseItem();
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = EnumFacing.getFront(par1IBlockSource.func_82620_h());
        World world = par1IBlockSource.getWorld();
        double d0 = par1IBlockSource.getX() + (double)((float)enumfacing.getFrontOffsetX() * 1.125F);
        double d1 = par1IBlockSource.getY();
        double d2 = par1IBlockSource.getZ() + (double)((float)enumfacing.getFrontOffsetZ() * 1.125F);
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        int l = world.getBlockId(i, j, k);
        double d3;

        if (BlockRail.isRailBlock(l))
        {
            d3 = 0.0D;
        }
        else
        {
            if (l != 0 || !BlockRail.isRailBlock(world.getBlockId(i, j - 1, k)))
            {
                return this.defaultItemDispenseBehavior.dispense(par1IBlockSource, par2ItemStack);
            }

            d3 = -1.0D;
        }

        // CraftBukkit start
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d0, d1 + d3, d2));

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
        EntityMinecart entityminecart = new EntityMinecart(world, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), ((ItemMinecart) itemstack1.getItem()).minecartType);
        // CraftBukkit end
        world.spawnEntityInWorld(entityminecart);
        // itemstack.a(1); // CraftBukkit - handled during event processing
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}
