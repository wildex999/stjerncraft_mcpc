package net.minecraft.inventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.InventoryView;
// CraftBukkit end

public abstract class Container
{
    /** the list of all items(stacks) for the corresponding slot */
    public List inventoryItemStacks = new ArrayList();

    /** the list of all slots in the inventory */
    public List inventorySlots = new ArrayList();
    public int windowId = 0;
    private short transactionID = 0;

    /**
     * list of all people that need to be notified when this craftinventory changes
     */
    protected List crafters = new ArrayList();
    private Set playerList = new HashSet();

    // CraftBukkit start
    public boolean checkReachable = true;
    public InventoryView getBukkitView() { return null; }
    public void transferTo(Container other, org.bukkit.craftbukkit.entity.CraftHumanEntity player)
    {
        InventoryView source = this.getBukkitView(), destination = other.getBukkitView();
        ((CraftInventory) source.getTopInventory()).getInventory().onClose(player);
        ((CraftInventory) source.getBottomInventory()).getInventory().onClose(player);
        ((CraftInventory) destination.getTopInventory()).getInventory().onOpen(player);
        ((CraftInventory) destination.getBottomInventory()).getInventory().onOpen(player);
    }
    // CraftBukkit end

    public Container() {}

    /**
     * the slot is assumed empty
     */
    protected Slot addSlotToContainer(Slot par1Slot)
    {
        par1Slot.slotNumber = this.inventorySlots.size();
        this.inventorySlots.add(par1Slot);
        this.inventoryItemStacks.add((Object)null);
        return par1Slot;
    }

    public void addCraftingToCrafters(ICrafting par1ICrafting)
    {
        if (this.crafters.contains(par1ICrafting))
        {
            throw new IllegalArgumentException("Listener already listening");
        }
        else
        {
            this.crafters.add(par1ICrafting);
            par1ICrafting.sendContainerAndContentsToPlayer(this, this.getInventory());
            this.detectAndSendChanges();
        }
    }

    /**
     * returns a list if itemStacks, for each slot.
     */
    public List getInventory()
    {
        ArrayList arraylist = new ArrayList();

        for (int i = 0; i < this.inventorySlots.size(); ++i)
        {
            arraylist.add(((Slot)this.inventorySlots.get(i)).getStack());
        }

        return arraylist;
    }

    @SideOnly(Side.CLIENT)

    /**
     * Remove this crafting listener from the listener list.
     */
    public void removeCraftingFromCrafters(ICrafting par1ICrafting)
    {
        this.crafters.remove(par1ICrafting);
    }

    /**
     * Looks for changes made in the container, sends them to every listener.
     */
    public void detectAndSendChanges()
    {
        for (int i = 0; i < this.inventorySlots.size(); ++i)
        {
            ItemStack itemstack = ((Slot)this.inventorySlots.get(i)).getStack();
            ItemStack itemstack1 = (ItemStack)this.inventoryItemStacks.get(i);

            if (!ItemStack.areItemStacksEqual(itemstack1, itemstack))
            {
                itemstack1 = itemstack == null ? null : itemstack.copy();
                this.inventoryItemStacks.set(i, itemstack1);

                for (int j = 0; j < this.crafters.size(); ++j)
                {
                    ((ICrafting)this.crafters.get(j)).sendSlotContents(this, i, itemstack1);
                }
            }
        }
    }

    /**
     * enchants the item on the table using the specified slot; also deducts XP from player
     */
    public boolean enchantItem(EntityPlayer par1EntityPlayer, int par2)
    {
        return false;
    }

    public Slot getSlotFromInventory(IInventory par1IInventory, int par2)
    {
        for (int j = 0; j < this.inventorySlots.size(); ++j)
        {
            Slot slot = (Slot)this.inventorySlots.get(j);

            if (slot.isSlotInInventory(par1IInventory, par2))
            {
                return slot;
            }
        }

        return null;
    }

    public Slot getSlot(int par1)
    {
        return (Slot)this.inventorySlots.get(par1);
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2)
    {
        Slot slot = (Slot)this.inventorySlots.get(par2);
        return slot != null ? slot.getStack() : null;
    }

    public ItemStack slotClick(int par1, int par2, int par3, EntityPlayer par4EntityPlayer)
    {
        ItemStack itemstack = null;
        InventoryPlayer inventoryplayer = par4EntityPlayer.inventory;
        Slot slot;
        ItemStack itemstack1;
        int l;
        ItemStack itemstack2;

        if ((par3 == 0 || par3 == 1) && (par2 == 0 || par2 == 1))
        {
            if (par1 == -999)
            {
                if (inventoryplayer.getItemStack() != null && par1 == -999)
                {
                    if (par2 == 0)
                    {
                        par4EntityPlayer.dropPlayerItem(inventoryplayer.getItemStack());
                        inventoryplayer.setItemStack((ItemStack)null);
                    }

                    if (par2 == 1)
                    {
                        // CraftBukkit start - store a reference
                        ItemStack itemstack3 = inventoryplayer.getItemStack();

                        if (itemstack3.stackSize > 0)
                        {
                            par4EntityPlayer.dropPlayerItem(itemstack3.splitStack(1));
                        }

                        if (itemstack3.stackSize == 0)
                        {
                            // CraftBukkit end
                            inventoryplayer.setItemStack((ItemStack)null);
                        }
                    }
                }
            }
            else if (par3 == 1)
            {
                slot = (Slot)this.inventorySlots.get(par1);

                if (slot != null && slot.canTakeStack(par4EntityPlayer))
                {
                    itemstack1 = this.transferStackInSlot(par4EntityPlayer, par1);

                    if (itemstack1 != null)
                    {
                        int i1 = itemstack1.itemID;
                        itemstack = itemstack1.copy();

                        if (slot != null && slot.getStack() != null && slot.getStack().itemID == i1)
                        {
                            this.retrySlotClick(par1, par2, true, par4EntityPlayer);
                        }
                    }
                }
            }
            else
            {
                if (par1 < 0)
                {
                    return null;
                }

                slot = (Slot)this.inventorySlots.get(par1);

                if (slot != null)
                {
                    itemstack1 = slot.getStack();
                    ItemStack itemstack3 = inventoryplayer.getItemStack();

                    if (itemstack1 != null)
                    {
                        itemstack = itemstack1.copy();
                    }

                    if (itemstack1 == null)
                    {
                        if (itemstack3 != null && slot.isItemValid(itemstack3))
                        {
                            l = par2 == 0 ? itemstack3.stackSize : 1;

                            if (l > slot.getSlotStackLimit())
                            {
                                l = slot.getSlotStackLimit();
                            }

                            // CraftBukkit start
                            if (itemstack3.stackSize >= l)
                            {
                                slot.putStack(itemstack3.splitStack(l));
                            }

                            // CraftBukkit end

                            if (itemstack3.stackSize == 0)
                            {
                                inventoryplayer.setItemStack((ItemStack)null);
                            }
                        }
                    }
                    else if (slot.canTakeStack(par4EntityPlayer))
                    {
                        if (itemstack3 == null)
                        {
                            l = par2 == 0 ? itemstack1.stackSize : (itemstack1.stackSize + 1) / 2;
                            itemstack2 = slot.decrStackSize(l);
                            inventoryplayer.setItemStack(itemstack2);

                            if (itemstack1.stackSize == 0)
                            {
                                slot.putStack((ItemStack)null);
                            }

                            slot.onPickupFromSlot(par4EntityPlayer, inventoryplayer.getItemStack());
                        }
                        else if (slot.isItemValid(itemstack3))
                        {
                            if (itemstack1.itemID == itemstack3.itemID && itemstack1.getItemDamage() == itemstack3.getItemDamage() && ItemStack.areItemStackTagsEqual(itemstack1, itemstack3))
                            {
                                l = par2 == 0 ? itemstack3.stackSize : 1;

                                if (l > slot.getSlotStackLimit() - itemstack1.stackSize)
                                {
                                    l = slot.getSlotStackLimit() - itemstack1.stackSize;
                                }

                                if (l > itemstack3.getMaxStackSize() - itemstack1.stackSize)
                                {
                                    l = itemstack3.getMaxStackSize() - itemstack1.stackSize;
                                }

                                itemstack3.splitStack(l);

                                if (itemstack3.stackSize == 0)
                                {
                                    inventoryplayer.setItemStack((ItemStack)null);
                                }

                                itemstack1.stackSize += l;
                            }
                            else if (itemstack3.stackSize <= slot.getSlotStackLimit())
                            {
                                slot.putStack(itemstack3);
                                inventoryplayer.setItemStack(itemstack1);
                            }
                        }
                        else if (itemstack1.itemID == itemstack3.itemID && itemstack3.getMaxStackSize() > 1 && (!itemstack1.getHasSubtypes() || itemstack1.getItemDamage() == itemstack3.getItemDamage()) && ItemStack.areItemStackTagsEqual(itemstack1, itemstack3))
                        {
                            l = itemstack1.stackSize;

                            if (l > 0 && l + itemstack3.stackSize <= itemstack3.getMaxStackSize())
                            {
                                itemstack3.stackSize += l;
                                itemstack1 = slot.decrStackSize(l);

                                if (itemstack1.stackSize == 0)
                                {
                                    slot.putStack((ItemStack)null);
                                }

                                slot.onPickupFromSlot(par4EntityPlayer, inventoryplayer.getItemStack());
                            }
                        }
                    }

                    slot.onSlotChanged();
                }
            }
        }
        else if (par3 == 2 && par2 >= 0 && par2 < 9)
        {
            slot = (Slot)this.inventorySlots.get(par1);

            if (slot.canTakeStack(par4EntityPlayer))
            {
                itemstack1 = inventoryplayer.getStackInSlot(par2);
                boolean flag = itemstack1 == null || slot.inventory == inventoryplayer && slot.isItemValid(itemstack1);
                l = -1;

                if (!flag)
                {
                    l = inventoryplayer.getFirstEmptyStack();
                    flag |= l > -1;
                }

                if (slot.getHasStack() && flag)
                {
                    itemstack2 = slot.getStack();
                    inventoryplayer.setInventorySlotContents(par2, itemstack2);

                    if ((slot.inventory != inventoryplayer || !slot.isItemValid(itemstack1)) && itemstack1 != null)
                    {
                        if (l > -1)
                        {
                            inventoryplayer.addItemStackToInventory(itemstack1);
                            slot.decrStackSize(itemstack2.stackSize);
                            slot.putStack((ItemStack)null);
                            slot.onPickupFromSlot(par4EntityPlayer, itemstack2);
                        }
                    }
                    else
                    {
                        slot.decrStackSize(itemstack2.stackSize);
                        slot.putStack(itemstack1);
                        slot.onPickupFromSlot(par4EntityPlayer, itemstack2);
                    }
                }
                else if (!slot.getHasStack() && itemstack1 != null && slot.isItemValid(itemstack1))
                {
                    inventoryplayer.setInventorySlotContents(par2, (ItemStack)null);
                    slot.putStack(itemstack1);
                }
            }
        }
        else if (par3 == 3 && par4EntityPlayer.capabilities.isCreativeMode && inventoryplayer.getItemStack() == null && par1 >= 0)
        {
            slot = (Slot)this.inventorySlots.get(par1);

            if (slot != null && slot.getHasStack())
            {
                itemstack1 = slot.getStack().copy();
                itemstack1.stackSize = itemstack1.getMaxStackSize();
                inventoryplayer.setItemStack(itemstack1);
            }
        }

        return itemstack;
    }

    protected void retrySlotClick(int par1, int par2, boolean par3, EntityPlayer par4EntityPlayer)
    {
        this.slotClick(par1, par2, 1, par4EntityPlayer);
    }

    /**
     * Callback for when the crafting gui is closed.
     */
    public void onCraftGuiClosed(EntityPlayer par1EntityPlayer)
    {
        InventoryPlayer inventoryplayer = par1EntityPlayer.inventory;

        if (inventoryplayer.getItemStack() != null)
        {
            par1EntityPlayer.dropPlayerItem(inventoryplayer.getItemStack());
            inventoryplayer.setItemStack((ItemStack)null);
        }
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void onCraftMatrixChanged(IInventory par1IInventory)
    {
        this.detectAndSendChanges();
    }

    /**
     * args: slotID, itemStack to put in slot
     */
    public void putStackInSlot(int par1, ItemStack par2ItemStack)
    {
        this.getSlot(par1).putStack(par2ItemStack);
    }

    @SideOnly(Side.CLIENT)

    /**
     * places itemstacks in first x slots, x being aitemstack.lenght
     */
    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack)
    {
        for (int i = 0; i < par1ArrayOfItemStack.length; ++i)
        {
            this.getSlot(i).putStack(par1ArrayOfItemStack[i]);
        }
    }

    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int par1, int par2) {}

    @SideOnly(Side.CLIENT)

    /**
     * Gets a unique transaction ID. Parameter is unused.
     */
    public short getNextTransactionID(InventoryPlayer par1InventoryPlayer)
    {
        ++this.transactionID;
        return this.transactionID;
    }

    /**
     * NotUsing because adding a player twice is an error
     */
    public boolean isPlayerNotUsingContainer(EntityPlayer par1EntityPlayer)
    {
        return !this.playerList.contains(par1EntityPlayer);
    }

    /**
     * adds or removes the player from the container based on par2
     */
    public void setPlayerIsPresent(EntityPlayer par1EntityPlayer, boolean par2)
    {
        if (par2)
        {
            this.playerList.remove(par1EntityPlayer);
        }
        else
        {
            this.playerList.add(par1EntityPlayer);
        }
    }

    public abstract boolean canInteractWith(EntityPlayer entityplayer);

    /**
     * merges provided ItemStack with the first avaliable one in the container/player inventory
     */
    protected boolean mergeItemStack(ItemStack par1ItemStack, int par2, int par3, boolean par4)
    {
        boolean flag1 = false;
        int k = par2;

        if (par4)
        {
            k = par3 - 1;
        }

        Slot slot;
        ItemStack itemstack1;

        if (par1ItemStack.isStackable())
        {
            while (par1ItemStack.stackSize > 0 && (!par4 && k < par3 || par4 && k >= par2))
            {
                slot = (Slot)this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 != null && itemstack1.itemID == par1ItemStack.itemID && (!par1ItemStack.getHasSubtypes() || par1ItemStack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(par1ItemStack, itemstack1))
                {
                    int l = itemstack1.stackSize + par1ItemStack.stackSize;

                    if (l <= par1ItemStack.getMaxStackSize())
                    {
                        par1ItemStack.stackSize = 0;
                        itemstack1.stackSize = l;
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                    else if (itemstack1.stackSize < par1ItemStack.getMaxStackSize())
                    {
                        par1ItemStack.stackSize -= par1ItemStack.getMaxStackSize() - itemstack1.stackSize;
                        itemstack1.stackSize = par1ItemStack.getMaxStackSize();
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                }

                if (par4)
                {
                    --k;
                }
                else
                {
                    ++k;
                }
            }
        }

        if (par1ItemStack.stackSize > 0)
        {
            if (par4)
            {
                k = par3 - 1;
            }
            else
            {
                k = par2;
            }

            while (!par4 && k < par3 || par4 && k >= par2)
            {
                slot = (Slot)this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 == null)
                {
                    slot.putStack(par1ItemStack.copy());
                    slot.onSlotChanged();
                    par1ItemStack.stackSize = 0;
                    flag1 = true;
                    break;
                }

                if (par4)
                {
                    --k;
                }
                else
                {
                    ++k;
                }
            }
        }

        return flag1;
    }
}
