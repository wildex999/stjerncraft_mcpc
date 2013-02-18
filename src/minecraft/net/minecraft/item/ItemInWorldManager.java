package net.minecraft.item;

// CraftBukkit start
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.tileentity.TileEntity;
// CraftBukkit end
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

public class ItemInWorldManager
{
    /** Forge reach distance */
    private double blockReachDistance = 5.0d;

    /** The world object that this object is connected to. */
    public World theWorld;

    /** The EntityPlayerMP object that this object is connected to. */
    public EntityPlayerMP thisPlayerMP;
    private EnumGameType gameType;

    /** True if the player is destroying a block */
    private boolean isDestroyingBlock;
    private int initialDamage;
    private int partiallyDestroyedBlockX;
    private int partiallyDestroyedBlockY;
    private int partiallyDestroyedBlockZ;
    private int curblockDamage;

    /**
     * Set to true when the "finished destroying block" packet is received but the block wasn't fully damaged yet. The
     * block will not be destroyed while this is false.
     */
    private boolean receivedFinishDiggingPacket;
    private int posX;
    private int posY;
    private int posZ;
    private int field_73093_n;
    private int durabilityRemainingOnBlock;

    public ItemInWorldManager(World par1World)
    {
        this.gameType = EnumGameType.NOT_SET;
        this.durabilityRemainingOnBlock = -1;
        this.theWorld = par1World;
    }

    // CraftBukkit start - keep this for backwards compatibility
    public ItemInWorldManager(WorldServer world)
    {
        this((World) world);
    }
    // CraftBukkit end

    public void setGameType(EnumGameType par1EnumGameType)
    {
        this.gameType = par1EnumGameType;
        par1EnumGameType.configurePlayerCapabilities(this.thisPlayerMP.capabilities);
        this.thisPlayerMP.sendPlayerAbilities();
    }

    public EnumGameType getGameType()
    {
        return this.gameType;
    }

    /**
     * Get if we are in creative game mode.
     */
    public boolean isCreative()
    {
        return this.gameType.isCreative();
    }

    /**
     * if the gameType is currently NOT_SET then change it to par1
     */
    public void initializeGameType(EnumGameType par1EnumGameType)
    {
        if (this.gameType == EnumGameType.NOT_SET)
        {
            this.gameType = par1EnumGameType;
        }

        this.setGameType(this.gameType);
    }

    public void updateBlockRemoving()
    {
        this.curblockDamage = (int)(System.currentTimeMillis() / 50);  // CraftBukkit
        int var1;
        float var4;
        int var5;

        if (this.receivedFinishDiggingPacket)
        {
            var1 = this.curblockDamage - this.field_73093_n;
            int var2 = this.theWorld.getBlockId(this.posX, this.posY, this.posZ);

            if (var2 == 0)
            {
                this.receivedFinishDiggingPacket = false;
            }
            else
            {
                Block var3 = Block.blocksList[var2];
                var4 = var3.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, this.posX, this.posY, this.posZ) * (float)(var1 + 1);
                var5 = (int)(var4 * 10.0F);

                if (var5 != this.durabilityRemainingOnBlock)
                {
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.posX, this.posY, this.posZ, var5);
                    this.durabilityRemainingOnBlock = var5;
                }

                if (var4 >= 1.0F)
                {
                    this.receivedFinishDiggingPacket = false;
                    this.tryHarvestBlock(this.posX, this.posY, this.posZ);
                }
            }
        }
        else if (this.isDestroyingBlock)
        {
            var1 = this.theWorld.getBlockId(this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ);
            Block var6 = Block.blocksList[var1];

            if (var6 == null)
            {
                this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, -1);
                this.durabilityRemainingOnBlock = -1;
                this.isDestroyingBlock = false;
            }
            else
            {
                int var7 = this.curblockDamage - this.initialDamage;
                var4 = var6.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ) * (float)(var7 + 1);
                var5 = (int)(var4 * 10.0F);

                if (var5 != this.durabilityRemainingOnBlock)
                {
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, var5);
                    this.durabilityRemainingOnBlock = var5;
                }
            }
        }
    }

    /**
     * if not creative, it calls destroyBlockInWorldPartially untill the block is broken first. par4 is the specific
     * side. tryHarvestBlock can also be the result of this call
     */
    public void onBlockClicked(int par1, int par2, int par3, int par4)
    {
        // this.world.douseFire((EntityHuman) null, i, j, k, l); // CraftBukkit - moved down
        // CraftBukkit
        PlayerInteractEvent var5 = CraftEventFactory.callPlayerInteractEvent(this.thisPlayerMP, Action.LEFT_CLICK_BLOCK, par1, par2, par3, par4, this.thisPlayerMP.inventory.getCurrentItem());

        if (!this.gameType.isAdventure() || this.thisPlayerMP.canCurrentToolHarvestBlock(par1, par2, par3))
        {
            net.minecraftforge.event.entity.player.PlayerInteractEvent var6 = ForgeEventFactory.onPlayerInteract(this.thisPlayerMP, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, par1, par2, par3, par4); // Forge

            // CraftBukkit start
            if (var5.isCancelled() || var6.isCanceled()) // Forge
            {
                // Let the client know the block still exists
                ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                // Update any tile entity data for this block
                TileEntity tileentity = this.theWorld.getBlockTileEntity(par1, par2, par3);

                if (tileentity != null)
                {
                    this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(tileentity.getDescriptionPacket());
                }

                return;
            }

            // CraftBukkit end
            if (this.isCreative())
            {
                if (!this.theWorld.extinguishFire((EntityPlayer)null, par1, par2, par3, par4))
                {
                    this.tryHarvestBlock(par1, par2, par3);
                }
            }
            else
            {
                //this.world.douseFire(this.player, i, j, k, l);  // Forge
                this.initialDamage = this.curblockDamage;
                float var7 = 1.0F;
                int i1 = this.theWorld.getBlockId(par1, par2, par3);
                // CraftBukkit start - Swings at air do *NOT* exist.
                Block block = Block.blocksList[i1]; // Forge

                if (block != null)
                {
                    if (var5.useInteractedBlock() == Event.Result.DENY || var6.useBlock == net.minecraftforge.event.Event.Result.DENY)   // MCPC
                    {
                        // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
                        if (i1 == Block.doorWood.blockID)
                        {
                            // For some reason *BOTH* the bottom/top part have to be marked updated.
                            boolean bottom = (this.theWorld.getBlockMetadata(par1, par2, par3) & 8) == 0;
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2 + (bottom ? 1 : -1), par3, this.theWorld));
                        }
                        else if (i1 == Block.trapdoor.blockID)
                        {
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                        }
                    }
                    else
                    {
                        // Forge start
                        block.onBlockClicked(theWorld, par1, par2, par3, this.thisPlayerMP);
                        theWorld.extinguishFire(this.thisPlayerMP, par1, par2, par3, par4);
                        var7 = block.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.theWorld, par1, par2, par3);
                        // Forge end
                    }
                }

                if (var5.useItemInHand() == Event.Result.DENY || var6.useItem == net.minecraftforge.event.Event.Result.DENY)   // Forge
                {
                    // If we 'insta destroyed' then the client needs to be informed.
                    if (var7 > 1.0f)
                    {
                        ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                    }

                    return;
                }

                org.bukkit.event.block.BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.thisPlayerMP, par1, par2, par3, this.thisPlayerMP.inventory.getCurrentItem(), var7 >= 1.0f);

                if (blockEvent.isCancelled())
                {
                    // Let the client know the block still exists
                    ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                    return;
                }

                if (blockEvent.getInstaBreak())
                {
                    var7 = 2.0f;
                }

                // CraftBukkit end

                if (i1 > 0 && var7 >= 1.0F)
                {
                    this.tryHarvestBlock(par1, par2, par3);
                }
                else
                {
                    this.isDestroyingBlock = true;
                    this.partiallyDestroyedBlockX = par1;
                    this.partiallyDestroyedBlockY = par2;
                    this.partiallyDestroyedBlockZ = par3;
                    int j1 = (int)(var7 * 10.0F);
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, par1, par2, par3, j1);
                    this.durabilityRemainingOnBlock = j1;
                }
            }
        }
    }

    public void uncheckedTryHarvestBlock(int par1, int par2, int par3)
    {
        if (par1 == this.partiallyDestroyedBlockX && par2 == this.partiallyDestroyedBlockY && par3 == this.partiallyDestroyedBlockZ)
        {
            this.curblockDamage = (int)(System.currentTimeMillis() / 50);  // CraftBukkit
            int var4 = this.curblockDamage - this.initialDamage;
            int var5 = this.theWorld.getBlockId(par1, par2, par3);

            if (var5 != 0)
            {
                Block var6 = Block.blocksList[var5];
                float var7 = var6.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, par1, par2, par3) * (float)(var4 + 1);

                if (var7 >= 0.7F)
                {
                    this.isDestroyingBlock = false;
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, par1, par2, par3, -1);
                    this.tryHarvestBlock(par1, par2, par3);
                }
                else if (!this.receivedFinishDiggingPacket)
                {
                    this.isDestroyingBlock = false;
                    this.receivedFinishDiggingPacket = true;
                    this.posX = par1;
                    this.posY = par2;
                    this.posZ = par3;
                    this.field_73093_n = this.initialDamage;
                }
            }

            // CraftBukkit start - force blockreset to client
        }
        else
        {
            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
            // CraftBukkit end
        }
    }

    /**
     * note: this ignores the pars passed in and continues to destroy the onClickedBlock
     */
    public void cancelDestroyingBlock(int par1, int par2, int par3)
    {
        this.isDestroyingBlock = false;
        this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, -1);
    }

    /**
     * Removes a block and triggers the appropriate events
     */
    private boolean removeBlock(int par1, int par2, int par3)
    {
        Block var4 = Block.blocksList[this.theWorld.getBlockId(par1, par2, par3)];
        int var5 = this.theWorld.getBlockMetadata(par1, par2, par3);

        if (var4 != null)
        {
            var4.onBlockHarvested(this.theWorld, par1, par2, par3, var5, this.thisPlayerMP);
        }

        boolean var6 = (var4 != null && var4.removeBlockByPlayer(theWorld, thisPlayerMP, par1, par2, par3));

        if (var4 != null && var6)
        {
            var4.onBlockDestroyedByPlayer(this.theWorld, par1, par2, par3, var5);
        }

        return var6;
    }

    /**
     * Attempts to harvest a block at the given coordinate
     */
    public boolean tryHarvestBlock(int par1, int par2, int par3)
    {
        // CraftBukkit start
        BlockBreakEvent event = null;

        if (this.thisPlayerMP instanceof EntityPlayerMP)
        {
            org.bukkit.block.Block block = this.theWorld.getWorld().getBlockAt(par1, par2, par3);

            // Tell client the block is gone immediately then process events
            if (theWorld.getBlockTileEntity(par1, par2, par3) == null)
            {
                Packet53BlockChange packet = new Packet53BlockChange(par1, par2, par3, this.theWorld);
                packet.type = 0;
                packet.metadata = 0;
                ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(packet);
            }

            event = new BlockBreakEvent(block, this.thisPlayerMP.getBukkitEntity());
            // Adventure mode pre-cancel
            event.setCancelled(this.gameType.isAdventure() && !this.thisPlayerMP.canCurrentToolHarvestBlock(par1, par2, par3));
            // Calculate default block experience
            Block nmsBlock = Block.blocksList[block.getTypeId()];

            if (nmsBlock != null && !event.isCancelled() && !this.isCreative() && this.thisPlayerMP.canHarvestBlock(nmsBlock))
            {
                // Copied from Block.a(world, entityhuman, int, int, int, int)
                if (!(nmsBlock.canSilkHarvest() && EnchantmentHelper.getSilkTouchModifier(this.thisPlayerMP)))
                {
                    int data = block.getData();
                    int bonusLevel = EnchantmentHelper.getFortuneModifier(this.thisPlayerMP);
                    event.setExpToDrop(nmsBlock.getExpDrop(this.theWorld, data, bonusLevel));
                }
            }

            this.theWorld.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                // Let the client know the block still exists
                ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                // Update any tile entity data for this block
                TileEntity tileentity = this.theWorld.getBlockTileEntity(par1, par2, par3);

                if (tileentity != null)
                {
                    this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(tileentity.getDescriptionPacket());
                }

                return false;
            }
            // Spigot (Orebfuscator) start
            else
            {
                org.bukkit.craftbukkit.OrebfuscatorManager.updateNearbyBlocks(theWorld, par1, par2, par3);
            }

            // Spigot (Orebfuscator) end
        }

        // Forge start
        ItemStack itemstack = this.thisPlayerMP.getCurrentEquippedItem();

        if (itemstack != null && itemstack.getItem().onBlockStartBreak(itemstack, par1, par2, par3, this.thisPlayerMP))
        {
            return false;
        }

        // Forge end
        int var4 = this.theWorld.getBlockId(par1, par2, par3);

        if (Block.blocksList[var4] == null)
        {
            return false;    // CraftBukkit - a plugin set block to air without cancelling
        }

        int var5 = this.theWorld.getBlockMetadata(par1, par2, par3);
        this.theWorld.playAuxSFXAtEntity(this.thisPlayerMP, 2001, par1, par2, par3, var4 + (this.theWorld.getBlockMetadata(par1, par2, par3) << 12));
        boolean var6 = false; // Forge

        if (this.isCreative())
        {
            var6 = this.removeBlock(par1, par2, par3); // Forge
            this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
        }
        else
        {
            // Forge start
            boolean flag1 = false;
            Block block = Block.blocksList[var4];

            if (block != null)
            {
                flag1 = block.canHarvestBlock(this.thisPlayerMP, var5);
            }

            // Forge end

            if (itemstack != null)
            {
                itemstack.onBlockDestroyed(this.theWorld, var4, par1, par2, par3, this.thisPlayerMP);

                if (itemstack.stackSize == 0)
                {
                    this.thisPlayerMP.destroyCurrentEquippedItem();
                }
            }

            var6 = this.removeBlock(par1, par2, par3); // Forge

            if (var6 && flag1)
            {
                Block.blocksList[var4].harvestBlock(this.theWorld, this.thisPlayerMP, par1, par2, par3, var5);
            }
        }

        // CraftBukkit start - drop event experience
        if (var6 && event != null)
        {
            Block.blocksList[var4].dropXpOnBlockBreak(this.theWorld, par1, par2, par3, event.getExpToDrop());
        }

        // CraftBukkit end
        return var6;
    }

    /**
     * Attempts to right-click use an item by the given EntityPlayer in the given World
     */
    public boolean tryUseItem(EntityPlayer par1EntityPlayer, World par2World, ItemStack par3ItemStack)
    {
        int var4 = par3ItemStack.stackSize;
        int var5 = par3ItemStack.getItemDamage();
        ItemStack var6 = par3ItemStack.useItemRightClick(par2World, par1EntityPlayer);

        if (var6 == par3ItemStack && (var6 == null || var6.stackSize == var4 && var6.getMaxItemUseDuration() <= 0 && var6.getItemDamage() == var5))
        {
            return false;
        }
        else
        {
            par1EntityPlayer.inventory.mainInventory[par1EntityPlayer.inventory.currentItem] = var6;

            if (this.isCreative())
            {
                var6.stackSize = var4;

                if (var6.isItemStackDamageable())
                {
                    var6.setItemDamage(var5);
                }
            }

            if (var6.stackSize == 0)
            {
                par1EntityPlayer.inventory.mainInventory[par1EntityPlayer.inventory.currentItem] = null;
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(thisPlayerMP, var6));
            }

            if (!par1EntityPlayer.isUsingItem())
            {
                ((EntityPlayerMP)par1EntityPlayer).sendContainerToPlayer(par1EntityPlayer.inventoryContainer);
            }

            return true;
        }
    }

    /**
     * Activate the clicked on block, otherwise use the held item. Args: player, world, itemStack, x, y, z, side,
     * xOffset, yOffset, zOffset
     */
    public boolean activateBlockOrUseItem(EntityPlayer par1EntityPlayer, World par2World, ItemStack par3ItemStack, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int var11 = par2World.getBlockId(par4, par5, par6);

        // CraftBukkit start - Interact
        boolean result = false;

        if (var11 > 0)
        {
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(par1EntityPlayer, Action.RIGHT_CLICK_BLOCK, par4, par5, par6, par7, par3ItemStack);
            net.minecraftforge.event.entity.player.PlayerInteractEvent forgeEvent = ForgeEventFactory.onPlayerInteract(par1EntityPlayer, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, par4, par5, par6, par7);
            // MCPC+ start
            // if forge event is explicitly cancelled, return
            if (forgeEvent.isCanceled())
            {
                thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par4, par5, par6, theWorld));
                return false;
            }
            // if we have no explicit deny, check if item can be used
            if (event.useItemInHand() != Event.Result.DENY && forgeEvent.useItem != net.minecraftforge.event.Event.Result.DENY)
            {
                Item item = (par3ItemStack != null ? par3ItemStack.getItem() : null);
                // MCPC+ - try to use an item in hand before activating a block. Used for items such as IC2's wrench.
                if (item != null && item.onItemUseFirst(par3ItemStack, par1EntityPlayer, par2World, par4, par5, par6, par7, par8, par9, par10))
                {
                    if (par3ItemStack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(thisPlayerMP, par3ItemStack);
                        return true;
                }
            }
            // MCPC+ end
            if (event.useInteractedBlock() == Event.Result.DENY || forgeEvent.useBlock == net.minecraftforge.event.Event.Result.DENY)
            {
                // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
                if (var11 == Block.doorWood.blockID)
                {
                    boolean bottom = (par2World.getBlockMetadata(par4, par5, par6) & 8) == 0;
                    ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par4, par5 + (bottom ? 1 : -1), par6, par2World));
                }

                result = (event.useItemInHand() != Event.Result.ALLOW);
            }
            else if (!par1EntityPlayer.isSneaking() || par3ItemStack == null || par1EntityPlayer.getHeldItem().getItem().shouldPassSneakingClickToBlock(par2World, par4, par5, par6))
            {
                result = Block.blocksList[var11].onBlockActivated(par2World, par4, par5, par6, par1EntityPlayer, par7, par8, par9, par10);
            }

            if (par3ItemStack != null && !result)
            {
                int meta = par3ItemStack.getItemDamage();
                int size = par3ItemStack.stackSize;
                result = par3ItemStack.tryPlaceItemIntoWorld(par1EntityPlayer, par2World, par4, par5, par6, par7, par8, par9, par10);

                // The item count should not decrement in Creative mode.
                if (this.isCreative())
                {
                    par3ItemStack.setItemDamage(meta);
                    par3ItemStack.stackSize = size;
                }

                if (par3ItemStack.stackSize <= 0)
                {
                    ForgeEventFactory.onPlayerDestroyItem(this.thisPlayerMP, par3ItemStack);    // Forge
                }
            }

            /* Re-enable if this causes bukkit incompatibility, or re-write client side to only send a single packet per right click.
            // If we have 'true' and no explicit deny *or* an explicit allow -- run the item part of the hook
            /*if (par3ItemStack != null && ((!result && event.useItemInHand() != Event.Result.DENY) || event.useItemInHand() == Event.Result.ALLOW))
            {
                this.tryUseItem(par1EntityPlayer, par2World, par3ItemStack);
            }*/
        }

        return result;
        // CraftBukkit end
    }

    /**
     * Sets the world instance.
     */
    public void setWorld(WorldServer par1WorldServer)
    {
        this.theWorld = par1WorldServer;
    }

    public double getBlockReachDistance()
    {
        return blockReachDistance;
    }
    public void setBlockReachDistance(double distance)
    {
        blockReachDistance = distance;
    }
}
