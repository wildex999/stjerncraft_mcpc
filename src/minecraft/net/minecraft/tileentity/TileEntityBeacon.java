package net.minecraft.tileentity;

import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;

// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
// CraftBukkit end

public class TileEntityBeacon extends TileEntity implements IInventory
{
    /** List of effects that Beacon can apply */
    public static final Potion[][] effectsList = new Potion[][] {{Potion.moveSpeed, Potion.digSpeed}, {Potion.resistance, Potion.jump}, {Potion.damageBoost}, {Potion.regeneration}};
    private boolean isBeaconActive;

    /** Level of this beacon's pyramid. */
    private int levels = -1;

    /** Primary potion effect given by this beacon. */
    private int primaryEffect;

    /** Secondary potion effect given by this beacon. */
    private int secondaryEffect;

    /** Item given to this beacon as payment. */
    private ItemStack payment;
    private String field_94048_i;
    // CraftBukkit start
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    public ItemStack[] getContents()
    {
        return new ItemStack[] { this.payment };
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

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }
    // CraftBukkit end

    public TileEntityBeacon() {}

    /**
     * Allows the entity to update its state. Overridden in most subclasses, e.g. the mob spawner uses this to count
     * ticks and creates a new spawn inside its implementation.
     */
    public void updateEntity()
    {
        if (this.worldObj.getTotalWorldTime() % 80L == 0L)
        {
            this.updateState();
            this.addEffectsToPlayers();
        }
    }

    private void addEffectsToPlayers()
    {
        if (this.isBeaconActive && this.levels > 0 && !this.worldObj.isRemote && this.primaryEffect > 0)
        {
            double d0 = (double)(this.levels * 10 + 10);
            byte b0 = 0;

            if (this.levels >= 4 && this.primaryEffect == this.secondaryEffect)
            {
                b0 = 1;
            }

            AxisAlignedBB axisalignedbb = AxisAlignedBB.getAABBPool().getAABB((double)this.xCoord, (double)this.yCoord, (double)this.zCoord, (double)(this.xCoord + 1), (double)(this.yCoord + 1), (double)(this.zCoord + 1)).expand(d0, d0, d0);
            axisalignedbb.maxY = (double)this.worldObj.getHeight();
            List list = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, axisalignedbb);
            Iterator iterator = list.iterator();
            EntityPlayer entityplayer;

            while (iterator.hasNext())
            {
                entityplayer = (EntityPlayer)iterator.next();
                entityplayer.addPotionEffect(new PotionEffect(this.primaryEffect, 180, b0, true));
            }

            if (this.levels >= 4 && this.primaryEffect != this.secondaryEffect && this.secondaryEffect > 0)
            {
                iterator = list.iterator();

                while (iterator.hasNext())
                {
                    entityplayer = (EntityPlayer)iterator.next();
                    entityplayer.addPotionEffect(new PotionEffect(this.secondaryEffect, 180, 0, true));
                }
            }
        }
    }

    /**
     * Checks if the Beacon has a valid pyramid underneath and direct sunlight above
     */
    private void updateState()
    {
        if (!this.worldObj.canBlockSeeTheSky(this.xCoord, this.yCoord + 1, this.zCoord))
        {
            this.isBeaconActive = false;
            this.levels = 0;
        }
        else
        {
            this.isBeaconActive = true;
            this.levels = 0;

            for (int i = 1; i <= 4; this.levels = i++)
            {
                int j = this.yCoord - i;

                if (j < 1)
                {
                    break;
                }

                boolean flag = true;

                for (int k = this.xCoord - i; k <= this.xCoord + i && flag; ++k)
                {
                    for (int l = this.zCoord - i; l <= this.zCoord + i; ++l)
                    {
                        int i1 = this.worldObj.getBlockId(k, j, l);
                        Block block = Block.blocksList[i1];

                        if (block == null || !block.isBeaconBase(worldObj, k, j, l, xCoord, yCoord, zCoord))
                        {
                            flag = false;
                            break;
                        }
                    }
                }

                if (!flag)
                {
                    break;
                }
            }

            if (this.levels == 0)
            {
                this.isBeaconActive = false;
            }
        }
    }

    /**
     * Return the primary potion effect given by this beacon.
     */
    public int getPrimaryEffect()
    {
        return this.primaryEffect;
    }

    /**
     * Return the secondary potion effect given by this beacon.
     */
    public int getSecondaryEffect()
    {
        return this.secondaryEffect;
    }

    /**
     * Return the levels of this beacon's pyramid.
     */
    public int getLevels()
    {
        return this.levels;
    }

    public void setPrimaryEffect(int par1)
    {
        this.primaryEffect = 0;

        for (int j = 0; j < this.levels && j < 3; ++j)
        {
            Potion[] apotion = effectsList[j];
            int k = apotion.length;

            for (int l = 0; l < k; ++l)
            {
                Potion potion = apotion[l];

                if (potion.id == par1)
                {
                    this.primaryEffect = par1;
                    return;
                }
            }
        }
    }

    public void setSecondaryEffect(int par1)
    {
        this.secondaryEffect = 0;

        if (this.levels >= 4)
        {
            for (int j = 0; j < 4; ++j)
            {
                Potion[] apotion = effectsList[j];
                int k = apotion.length;

                for (int l = 0; l < k; ++l)
                {
                    Potion potion = apotion[l];

                    if (potion.id == par1)
                    {
                        this.secondaryEffect = par1;
                        return;
                    }
                }
            }
        }
    }

    /**
     * Overriden in a sign to provide the text.
     */
    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 3, nbttagcompound);
    }

    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        this.primaryEffect = par1NBTTagCompound.getInteger("Primary");
        this.secondaryEffect = par1NBTTagCompound.getInteger("Secondary");
        this.levels = par1NBTTagCompound.getInteger("Levels");
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setInteger("Primary", this.primaryEffect);
        par1NBTTagCompound.setInteger("Secondary", this.secondaryEffect);
        par1NBTTagCompound.setInteger("Levels", this.levels);
    }

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return 1;
    }

    /**
     * Returns the stack in slot i
     */
    public ItemStack getStackInSlot(int par1)
    {
        return par1 == 0 ? this.payment : null;
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (par1 == 0 && this.payment != null)
        {
            if (par2 >= this.payment.stackSize)
            {
                ItemStack itemstack = this.payment;
                this.payment = null;
                return itemstack;
            }
            else
            {
                this.payment.stackSize -= par2;
                return new ItemStack(this.payment.itemID, par2, this.payment.getItemDamage());
            }
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
        if (par1 == 0 && this.payment != null)
        {
            ItemStack itemstack = this.payment;
            this.payment = null;
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
        if (par1 == 0)
        {
            this.payment = par2ItemStack;
        }
    }

    /**
     * Returns the name of the inventory.
     */
    public String getInvName()
    {
        return this.isInvNameLocalized() ? this.field_94048_i : "container.beacon";
    }

    /**
     * If this returns false, the inventory name will be used as an unlocalized name, and translated into the player's
     * language. Otherwise it will be used directly.
     */
    public boolean isInvNameLocalized()
    {
        return this.field_94048_i != null && this.field_94048_i.length() > 0;
    }

    public void func_94047_a(String par1Str)
    {
        this.field_94048_i = par1Str;
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return maxStack; // CraftBukkit
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

    /**
     * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
     */
    public boolean isStackValidForSlot(int par1, ItemStack par2ItemStack)
    {
        return par2ItemStack.itemID == Item.emerald.itemID || par2ItemStack.itemID == Item.diamond.itemID || par2ItemStack.itemID == Item.ingotGold.itemID || par2ItemStack.itemID == Item.ingotIron.itemID;
    }
}
