package net.minecraft.block;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
// CraftBukkit end

public class BlockDropper extends BlockDispenser
{
    private final IBehaviorDispenseItem dropperDefaultBehaviour = new BehaviorDefaultDispenseItem();

    protected BlockDropper(int par1)
    {
        super(par1);
    }

    protected IBehaviorDispenseItem getBehaviorForItemStack(ItemStack par1ItemStack)
    {
        return this.dropperDefaultBehaviour;
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    public TileEntity createNewTileEntity(World par1World)
    {
        return new TileEntityDropper();
    }

    public void dispense(World par1World, int par2, int par3, int par4)   // CraftBukkit - protected -> public
    {
        BlockSourceImpl blocksourceimpl = new BlockSourceImpl(par1World, par2, par3, par4);
        TileEntityDispenser tileentitydispenser = (TileEntityDispenser)blocksourceimpl.getBlockTileEntity();

        if (tileentitydispenser != null)
        {
            int l = tileentitydispenser.getRandomStackFromInventory();

            if (l < 0)
            {
                par1World.playAuxSFX(1001, par2, par3, par4, 0);
            }
            else
            {
                ItemStack itemstack = tileentitydispenser.getStackInSlot(l);
                int i1 = par1World.getBlockMetadata(par2, par3, par4) & 7;
                IInventory iinventory = TileEntityHopper.getInventoryAtLocation(par1World, (double)(par2 + Facing.offsetsXForSide[i1]), (double)(par3 + Facing.offsetsYForSide[i1]), (double)(par4 + Facing.offsetsZForSide[i1]));
                ItemStack itemstack1;

                if (iinventory != null)
                {
                    // CraftBukkit start - Fire event when pushing items into other inventories
                    CraftItemStack oitemstack = CraftItemStack.asCraftMirror(itemstack.copy().splitStack(1));
                    org.bukkit.inventory.Inventory destinationInventory;

                    // Have to special case large chests as they work oddly
                    if (iinventory instanceof InventoryLargeChest)
                    {
                        destinationInventory = new org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest((InventoryLargeChest) iinventory);
                    }
                    else
                    {
                        destinationInventory = iinventory.getOwner().getInventory();
                    }

                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(tileentitydispenser.getOwner().getInventory(), oitemstack.clone(), destinationInventory, true);
                    par1World.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled())
                    {
                        return;
                    }

                    itemstack1 = TileEntityHopper.insertStack(iinventory, CraftItemStack.asNMSCopy(event.getItem()), Facing.oppositeSide[i1]);

                    if (event.getItem().equals(oitemstack) && itemstack1 == null)
                    {
                        // CraftBukkit end
                        itemstack1 = itemstack.copy();

                        if (--itemstack1.stackSize == 0)
                        {
                            itemstack1 = null;
                        }
                    }
                    else
                    {
                        itemstack1 = itemstack.copy();
                    }
                }
                else
                {
                    itemstack1 = this.dropperDefaultBehaviour.dispense(blocksourceimpl, itemstack);

                    if (itemstack1 != null && itemstack1.stackSize == 0)
                    {
                        itemstack1 = null;
                    }
                }

                tileentitydispenser.setInventorySlotContents(l, itemstack1);
            }
        }
    }
}
