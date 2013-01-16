package net.minecraft.inventory;

// CraftBukkit start
import java.util.List;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
// CraftBukkit end
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class InventoryLargeChest implements IInventory
{
    /** Name of the chest. */
    private String name;
    public IInventory upperChest; // CraftBukkit - private -> public
    public IInventory lowerChest; // CraftBukkit - private -> public

    // CraftBukkit start
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();

    public ItemStack[] getContents()
    {
        ItemStack[] result = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < result.length; i++)
        {
            result[i] = this.getStackInSlot(i);
        }

        return result;
    }

    public void onOpen(CraftHumanEntity who)
    {
        this.upperChest.onOpen(who);
        this.lowerChest.onOpen(who);
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        this.upperChest.onClose(who);
        this.lowerChest.onClose(who);
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return transaction;
    }

    public org.bukkit.inventory.InventoryHolder getOwner()
    {
        return null; // This method won't be called since CraftInventoryDoubleChest doesn't defer to here
    }

    public void setMaxStackSize(int size)
    {
        this.upperChest.setMaxStackSize(size);
        this.lowerChest.setMaxStackSize(size);
    }
    // CraftBukkit end

    public InventoryLargeChest(String par1Str, IInventory par2IInventory, IInventory par3IInventory)
    {
        this.name = par1Str;

        if (par2IInventory == null)
        {
            par2IInventory = par3IInventory;
        }

        if (par3IInventory == null)
        {
            par3IInventory = par2IInventory;
        }

        this.upperChest = par2IInventory;
        this.lowerChest = par3IInventory;
    }

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return this.upperChest.getSizeInventory() + this.lowerChest.getSizeInventory();
    }

    /**
     * Return whether the given inventory is part of this large chest.
     */
    public boolean isPartOfLargeChest(IInventory par1IInventory)
    {
        return this.upperChest == par1IInventory || this.lowerChest == par1IInventory;
    }

    /**
     * Returns the name of the inventory.
     */
    public String getInvName()
    {
        return this.name;
    }

    /**
     * Returns the stack in slot i
     */
    public ItemStack getStackInSlot(int par1)
    {
        return par1 >= this.upperChest.getSizeInventory() ? this.lowerChest.getStackInSlot(par1 - this.upperChest.getSizeInventory()) : this.upperChest.getStackInSlot(par1);
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    public ItemStack decrStackSize(int par1, int par2)
    {
        return par1 >= this.upperChest.getSizeInventory() ? this.lowerChest.decrStackSize(par1 - this.upperChest.getSizeInventory(), par2) : this.upperChest.decrStackSize(par1, par2);
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        return par1 >= this.upperChest.getSizeInventory() ? this.lowerChest.getStackInSlotOnClosing(par1 - this.upperChest.getSizeInventory()) : this.upperChest.getStackInSlotOnClosing(par1);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        if (par1 >= this.upperChest.getSizeInventory())
        {
            this.lowerChest.setInventorySlotContents(par1 - this.upperChest.getSizeInventory(), par2ItemStack);
        }
        else
        {
            this.upperChest.setInventorySlotContents(par1, par2ItemStack);
        }
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return Math.min(this.upperChest.getInventoryStackLimit(), this.lowerChest.getInventoryStackLimit()); // CraftBukkit - check both sides
    }

    /**
     * Called when an the contents of an Inventory change, usually
     */
    public void onInventoryChanged()
    {
        this.upperChest.onInventoryChanged();
        this.lowerChest.onInventoryChanged();
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes with Container
     */
    public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
    {
        return this.upperChest.isUseableByPlayer(par1EntityPlayer) && this.lowerChest.isUseableByPlayer(par1EntityPlayer);
    }

    public void openChest()
    {
        this.upperChest.openChest();
        this.lowerChest.openChest();
    }

    public void closeChest()
    {
        this.upperChest.closeChest();
        this.lowerChest.closeChest();
    }
}
