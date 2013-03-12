package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionHelper;

import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;
// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.BrewEvent;
// CraftBukkit end

public class TileEntityBrewingStand extends TileEntity implements IInventory, ISidedInventory
{
    /** The itemstacks currently placed in the slots of the brewing stand */
    public ItemStack[] brewingItemStacks = new ItemStack[4]; // CraftBukkit private -> public
    public int brewTime; // CraftBukkit private -> public

    /**
     * an integer with each bit specifying whether that slot of the stand contains a potion
     */
    private int filledSlots;
    private int ingredientID;

    public TileEntityBrewingStand() {}

    // CraftBukkit start
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = 1;

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

    public ItemStack[] getContents()
    {
        return this.brewingItemStacks;
    }

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }
    // CraftBukkit end

    /**
     * Returns the name of the inventory.
     */
    public String getInvName()
    {
        return "container.brewing";
    }

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return this.brewingItemStacks.length;
    }

    /**
     * Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count
     * ticks and creates a new spawn inside its implementation.
     */
    public void updateEntity()
    {
        if (this.brewTime > 0)
        {
            --this.brewTime;

            if (this.brewTime == 0)
            {
                this.brewPotions();
                this.onInventoryChanged();
            }
            else if (!this.canBrew())
            {
                this.brewTime = 0;
                this.onInventoryChanged();
            }
            else if (this.ingredientID != this.brewingItemStacks[3].itemID)
            {
                this.brewTime = 0;
                this.onInventoryChanged();
            }
        }
        else if (this.canBrew())
        {
            this.brewTime = 400;
            this.ingredientID = this.brewingItemStacks[3].itemID;
        }

        int i = this.getFilledSlots();

        if (i != this.filledSlots)
        {
            this.filledSlots = i;
            this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, i);
        }

        super.updateEntity();
    }

    public int getBrewTime()
    {
        return this.brewTime;
    }

    private boolean canBrew()
    {
        if (this.brewingItemStacks[3] != null && this.brewingItemStacks[3].stackSize > 0)
        {
            ItemStack itemstack = this.brewingItemStacks[3];

            if (!Item.itemsList[itemstack.itemID].isPotionIngredient(itemstack))
            {
                return false;
            }
            else
            {
                boolean flag = false;

                for (int i = 0; i < 3; ++i)
                {
                    if (this.brewingItemStacks[i] != null && this.brewingItemStacks[i].itemID == Item.potion.itemID)
                    {
                        int j = this.brewingItemStacks[i].getItemDamage();
                        int k = this.getPotionResult(j, itemstack);

                        if (!ItemPotion.isSplash(j) && ItemPotion.isSplash(k))
                        {
                            flag = true;
                            break;
                        }

                        List list = Item.potion.getEffects(j);
                        List list1 = Item.potion.getEffects(k);

                        if ((j <= 0 || list != list1) && (list == null || !list.equals(list1) && list1 != null) && j != k)
                        {
                            flag = true;
                            break;
                        }
                    }
                }

                return flag;
            }
        }
        else
        {
            return false;
        }
    }

    private void brewPotions()
    {
        if (this.canBrew())
        {
            ItemStack itemstack = this.brewingItemStacks[3];

            // CraftBukkit start - fire BREW event
            if (getOwner() != null)
            {
                BrewEvent event = new BrewEvent(worldObj.getWorld().getBlockAt(xCoord, yCoord, zCoord), (org.bukkit.inventory.BrewerInventory) this.getOwner().getInventory());
                org.bukkit.Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled())
                {
                    return;
                }
            }

            // CraftBukkit end

            for (int i = 0; i < 3; ++i)
            {
                if (this.brewingItemStacks[i] != null && this.brewingItemStacks[i].itemID == Item.potion.itemID)
                {
                    int j = this.brewingItemStacks[i].getItemDamage();
                    int k = this.getPotionResult(j, itemstack);
                    List list = Item.potion.getEffects(j);
                    List list1 = Item.potion.getEffects(k);

                    if ((j <= 0 || list != list1) && (list == null || !list.equals(list1) && list1 != null))
                    {
                        if (j != k)
                        {
                            this.brewingItemStacks[i].setItemDamage(k);
                        }
                    }
                    else if (!ItemPotion.isSplash(j) && ItemPotion.isSplash(k))
                    {
                        this.brewingItemStacks[i].setItemDamage(k);
                    }
                }
            }

            if (Item.itemsList[itemstack.itemID].hasContainerItem())
            {
                this.brewingItemStacks[3] = Item.itemsList[itemstack.itemID].getContainerItemStack(brewingItemStacks[3]);
            }
            else
            {
                --this.brewingItemStacks[3].stackSize;

                if (this.brewingItemStacks[3].stackSize <= 0)
                {
                    this.brewingItemStacks[3] = null;
                }
            }
        }
    }

    /**
     * The result of brewing a potion of the specified damage value with an ingredient itemstack.
     */
    private int getPotionResult(int par1, ItemStack par2ItemStack)
    {
        return par2ItemStack == null ? par1 : (Item.itemsList[par2ItemStack.itemID].isPotionIngredient(par2ItemStack) ? PotionHelper.applyIngredient(par1, Item.itemsList[par2ItemStack.itemID].getPotionEffect(par2ItemStack)) : par1);
    }

    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        NBTTagList nbttaglist = par1NBTTagCompound.getTagList("Items");
        this.brewingItemStacks = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(i);
            byte b0 = nbttagcompound1.getByte("Slot");

            if (b0 >= 0 && b0 < this.brewingItemStacks.length)
            {
                this.brewingItemStacks[b0] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }

        this.brewTime = par1NBTTagCompound.getShort("BrewTime");
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setShort("BrewTime", (short)this.brewTime);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.brewingItemStacks.length; ++i)
        {
            if (this.brewingItemStacks[i] != null)
            {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte)i);
                this.brewingItemStacks[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        par1NBTTagCompound.setTag("Items", nbttaglist);
    }

    /**
     * Returns the stack in slot i
     */
    public ItemStack getStackInSlot(int par1)
    {
        return par1 >= 0 && par1 < this.brewingItemStacks.length ? this.brewingItemStacks[par1] : null;
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (par1 >= 0 && par1 < this.brewingItemStacks.length)
        {
            ItemStack itemstack = this.brewingItemStacks[par1];
            this.brewingItemStacks[par1] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (par1 >= 0 && par1 < this.brewingItemStacks.length)
        {
            ItemStack itemstack = this.brewingItemStacks[par1];
            this.brewingItemStacks[par1] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
    {
        if (par1 >= 0 && par1 < this.brewingItemStacks.length)
        {
            this.brewingItemStacks[par1] = par2ItemStack;
        }
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return this.maxStack; // CraftBukkit
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes with Container
     */
    public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
    {
        return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq((double)this.xCoord + 0.5D, (double)this.yCoord + 0.5D, (double)this.zCoord + 0.5D) <= 64.0D;
    }

    public void openChest() {}

    public void closeChest() {}

    @SideOnly(Side.CLIENT)
    public void setBrewTime(int par1)
    {
        this.brewTime = par1;
    }

    /**
     * returns an integer with each bit specifying wether that slot of the stand contains a potion
     */
    public int getFilledSlots()
    {
        int i = 0;

        for (int j = 0; j < 3; ++j)
        {
            if (this.brewingItemStacks[j] != null)
            {
                i |= 1 << j;
            }
        }

        return i;
    }

    @Override
    public int getStartInventorySide(ForgeDirection side)
    {
        return (side == ForgeDirection.UP ? 3 : 0);
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side)
    {
        return (side == ForgeDirection.UP ? 1 : 3);
    }
}
