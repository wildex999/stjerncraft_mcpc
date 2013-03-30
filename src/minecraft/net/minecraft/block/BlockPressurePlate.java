package net.minecraft.block;

import java.util.Iterator;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.bukkit.event.entity.EntityInteractEvent; // CraftBukkit

public class BlockPressurePlate extends BlockBasePressurePlate
{
    /** The mob type that can trigger this pressure plate. */
    private EnumMobType triggerMobType;

    protected BlockPressurePlate(int par1, String par2Str, Material par3Material, EnumMobType par4EnumMobType)
    {
        super(par1, par2Str, par3Material);
        this.triggerMobType = par4EnumMobType;
    }

    protected int func_94355_d(int par1)
    {
        return par1 > 0 ? 1 : 0;
    }

    protected int func_94350_c(int par1)
    {
        return par1 == 1 ? 15 : 0;
    }

    protected int func_94351_d(World par1World, int par2, int par3, int par4)
    {
        List list = null;

        if (this.triggerMobType == EnumMobType.everything)
        {
            list = par1World.getEntitiesWithinAABBExcludingEntity((Entity)null, this.func_94352_a(par2, par3, par4));
        }

        if (this.triggerMobType == EnumMobType.mobs)
        {
            list = par1World.getEntitiesWithinAABB(EntityLiving.class, this.func_94352_a(par2, par3, par4));
        }

        if (this.triggerMobType == EnumMobType.players)
        {
            list = par1World.getEntitiesWithinAABB(EntityPlayer.class, this.func_94352_a(par2, par3, par4));
        }

        if (!list.isEmpty())
        {
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                Entity entity = (Entity)iterator.next();
                // CraftBukkit start - Call interact event when turning on a pressure plate
                if (this.func_94350_c(par1World.getBlockMetadata(par2, par3, par4)) == 0)
                {
                    org.bukkit.World bworld = par1World.getWorld();
                    org.bukkit.plugin.PluginManager manager = par1World.getServer().getPluginManager();
                    org.bukkit.event.Cancellable cancellable;

                    if (entity instanceof EntityPlayer)
                    {
                        cancellable = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent((EntityPlayer) entity, org.bukkit.event.block.Action.PHYSICAL, par2, par3, par4, -1, null);
                    }
                    else
                    {
                        cancellable = new EntityInteractEvent(entity.getBukkitEntity(), bworld.getBlockAt(par2, par3, par4));
                        manager.callEvent((EntityInteractEvent) cancellable);
                    }

                    // We only want to block turning the plate on if all events are cancelled
                    if (cancellable.isCancelled())
                    {
                        continue;
                    }
                }

                // CraftBukkit end

                if (!entity.doesEntityNotTriggerPressurePlate())
                {
                    return 15;
                }
            }
        }

        return 0;
    }
}
