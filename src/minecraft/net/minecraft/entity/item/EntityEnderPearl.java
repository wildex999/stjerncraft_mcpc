package net.minecraft.entity.item;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
// CraftBukkit end
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

public class EntityEnderPearl extends EntityThrowable
{
    public EntityEnderPearl(World par1World)
    {
        super(par1World);
    }

    public EntityEnderPearl(World par1World, EntityLiving par2EntityLiving)
    {
        super(par1World, par2EntityLiving);
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    protected void onImpact(MovingObjectPosition par1MovingObjectPosition)
    {
        if (par1MovingObjectPosition.entityHit != null)
        {
            par1MovingObjectPosition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 0);
        }

        for (int i = 0; i < 32; ++i)
        {
            this.worldObj.spawnParticle("portal", this.posX, this.posY + this.rand.nextDouble() * 2.0D, this.posZ, this.rand.nextGaussian(), 0.0D, this.rand.nextGaussian());
        }

        if (!this.worldObj.isRemote)
        {
            if (this.getThrower() != null && this.getThrower() instanceof EntityPlayerMP)
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP)this.getThrower();

                if (!entityplayermp.playerNetServerHandler.connectionClosed && entityplayermp.worldObj == this.worldObj)
                {
                    EnderTeleportEvent event = new EnderTeleportEvent(entityplayermp, this.posX, this.posY, this.posZ, 5);
                    // MCPC+ start - invert condition; return if cancelled otherwise fall through to CB event
                    if (MinecraftForge.EVENT_BUS.post(event)){
                        this.setDead();
                        return;
                    }
                    // MCPC+ end
                                    
                    // CraftBukkit start
                    org.bukkit.craftbukkit.entity.CraftPlayer player = entityplayermp.getBukkitEntity();
                    org.bukkit.Location location = getBukkitEntity().getLocation();
                    location.setPitch(player.getLocation().getPitch());
                    location.setYaw(player.getLocation().getYaw());
                    PlayerTeleportEvent teleEvent = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                    Bukkit.getPluginManager().callEvent(teleEvent);

                    if (!teleEvent.isCancelled() && !entityplayermp.playerNetServerHandler.connectionClosed)
                    {
                        entityplayermp.playerNetServerHandler.teleport(teleEvent.getTo());
                        this.getThrower().fallDistance = 0.0F;
                        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(this.getBukkitEntity(), player, EntityDamageByEntityEvent.DamageCause.FALL, 5);
                        Bukkit.getPluginManager().callEvent(damageEvent);

                        if (!damageEvent.isCancelled() && !entityplayermp.playerNetServerHandler.connectionClosed)
                        {
                            entityplayermp.initialInvulnerability = -1; // Remove spawning invulnerability
                            player.setLastDamageCause(damageEvent);
                            entityplayermp.attackEntityFrom(DamageSource.fall, damageEvent.getDamage());
                        }
                    }

                    // CraftBukkit end
                }
            }

            this.setDead();
        }
    }
}
