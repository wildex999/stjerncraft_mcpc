package net.minecraft.dispenser;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
// CraftBukkit end
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BehaviorDispenseBoat extends BehaviorDefaultDispenseItem
{
    /** Reference to the BehaviorDefaultDispenseItem object. */
    private final BehaviorDefaultDispenseItem defaultItemDispenseBehavior;

    /** Reference to the MinecraftServer object. */
    final MinecraftServer mcServer;

    public BehaviorDispenseBoat(MinecraftServer par1MinecraftServer)
    {
        this.mcServer = par1MinecraftServer;
        this.defaultItemDispenseBehavior = new BehaviorDefaultDispenseItem();
    }

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing var3 = EnumFacing.func_82600_a(par1IBlockSource.func_82620_h());
        World var4 = par1IBlockSource.getWorld();
        double var5 = par1IBlockSource.getX() + (double)((float)var3.func_82601_c() * 1.125F);
        double var7 = par1IBlockSource.getY();
        double var9 = par1IBlockSource.getZ() + (double)((float)var3.func_82599_e() * 1.125F);
        int var11 = par1IBlockSource.getXInt() + var3.func_82601_c();
        int var12 = par1IBlockSource.getYInt();
        int var13 = par1IBlockSource.getZInt() + var3.func_82599_e();
        Material var14 = var4.getBlockMaterial(var11, var12, var13);
        double var15;

        if (Material.water.equals(var14))
        {
            var15 = 1.0D;
        }
        else
        {
            if (!Material.air.equals(var14) || !Material.water.equals(var4.getBlockMaterial(var11, var12 - 1, var13)))
            {
                return this.defaultItemDispenseBehavior.dispense(par1IBlockSource, par2ItemStack);
            }

            var15 = 0.0D;
        }

        // CraftBukkit start
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = var4.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(var5, var7 + var15, var9));

        if (!BlockDispenser.eventFired)
        {
            var4.getServer().getPluginManager().callEvent(event);
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

        EntityBoat entityboat = new EntityBoat(var4, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
        // CraftBukkit end
        var4.spawnEntityInWorld(entityboat);
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
