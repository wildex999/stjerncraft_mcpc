package net.minecraft.inventory;

// CraftBukkit start
import java.util.List;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import net.minecraft.item.ItemStack;
// CraftBukkit end

public class InventoryRepair extends InventoryBasic   // CraftBukkit - public
{
    /** Container of this anvil's block. */
    final ContainerRepair theContainer;

    // CraftBukkit start
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    public org.bukkit.entity.Player player;
    private int maxStack = MAX_STACK;

    public ItemStack[] getContents()
    {
        return this.inventoryContents;
    }

    public void onOpen(CraftHumanEntity who)
    {
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return transaction;
    }

    public org.bukkit.inventory.InventoryHolder getOwner()
    {
        return this.player;
    }

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }
    // CraftBukkit end

    InventoryRepair(ContainerRepair par1ContainerRepair, String par2Str, int par3)
    {
        super(par2Str, par3);
        this.theContainer = par1ContainerRepair;
        this.setMaxStackSize(1); // CraftBukkit
    }

    /**
     * Called when an the contents of an Inventory change, usually
     */
    public void onInventoryChanged()
    {
        super.onInventoryChanged();
        this.theContainer.onCraftMatrixChanged(this);
    }
}
