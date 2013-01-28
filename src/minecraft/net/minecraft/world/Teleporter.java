package net.minecraft.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityPortalExitEvent;
import org.bukkit.util.Vector;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Direction;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;

public class Teleporter
{
    private final WorldServer worldServerInstance;

    /** A private Random() function in Teleporter */
    private final Random random;
    public final LongHashMap field_85191_c = new LongHashMap();
    private final List field_85190_d = new ArrayList();

    public Teleporter(WorldServer par1WorldServer)
    {
        this.worldServerInstance = par1WorldServer;
        this.random = new Random(par1WorldServer.getSeed());
    }

    /**
     * Place an entity in a nearby portal, creating one if necessary.
     */
    public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8)
    {
        if (this.worldServerInstance.provider.dimensionId != 1)
        {
            if (!this.placeInExistingPortal(par1Entity, par2, par4, par6, par8))
            {
                this.func_85188_a(par1Entity);
                this.placeInExistingPortal(par1Entity, par2, par4, par6, par8);
            }
        } else {
            // CraftBukkit start - modularize end portal creation
            ChunkCoordinates created = this.createEndPortal(par2, par4, par6);
            par1Entity.setLocationAndAngles((double) created.posX, (double) created.posY, (double) created.posZ, par1Entity.rotationYaw, 0.0F);
            par1Entity.motionX = par1Entity.motionY = par1Entity.motionZ = 0.0D;
        }
    }
     
    // split out from original placeInPortal(Entity, double, double, double, float) method in order to enable being called from createPortal
    private ChunkCoordinates createEndPortal(double x, double y, double z) {
            int var9 = MathHelper.floor_double(x);
            int var10 = MathHelper.floor_double(y) - 1;
            int var11 = MathHelper.floor_double(z);
            byte var12 = 1;
            byte var13 = 0;

            for (int var14 = -2; var14 <= 2; ++var14)
            {
                for (int var15 = -2; var15 <= 2; ++var15)
                {
                    for (int var16 = -1; var16 < 3; ++var16)
                    {
                        int var17 = var9 + var15 * var12 + var14 * var13;
                        int var18 = var10 + var16;
                        int var19 = var11 + var15 * var13 - var14 * var12;
                        boolean var20 = var16 < 0;
                        this.worldServerInstance.setBlockWithNotify(var17, var18, var19, var20 ? Block.obsidian.blockID : 0);
                    }
                }
            }

            //par1Entity.setLocationAndAngles((double)var9, (double)var10, (double)var11, par1Entity.rotationYaw, 0.0F);
            //par1Entity.motionX = par1Entity.motionY = par1Entity.motionZ = 0.0D;
       // }
            // CraftBukkit start
            return new ChunkCoordinates(var9, var10, var11);
    }

    // use logic based on creation to verify end portal
    private ChunkCoordinates findEndPortal(ChunkCoordinates portal) {
        int i = portal.posX;
        int j = portal.posY - 1;
        int k = portal.posZ;
        byte b0 = 1;
        byte b1 = 0;

        for (int l = -2; l <= 2; ++l) {
            for (int i1 = -2; i1 <= 2; ++i1) {
                for (int j1 = -1; j1 < 3; ++j1) {
                    int k1 = i + i1 * b0 + l * b1;
                    int l1 = j + j1;
                    int i2 = k + i1 * b1 - l * b0;
                    boolean flag = j1 < 0;

                    if (this.worldServerInstance.getTypeId(k1, l1, i2) != (flag ? Block.obsidian.blockID : 0)) {
                        return null;
                    }
                }
            }
        }
        return new ChunkCoordinates(i, j, k);
    }
    // CraftBukkit end

    public boolean placeInExistingPortal(Entity entity, double d0, double d1, double d2, float f) {
        // CraftBukkit start - modularize portal search process and entity teleportation
        ChunkCoordinates found = this.findPortal(entity.motionX, entity.motionY, entity.motionZ, 128);
        if (found == null) {
            return false;
        }

        Location exit = new Location(this.worldServerInstance.getWorld(), found.posX, found.posY, found.posZ, f, entity.rotationPitch);
        Vector velocity = entity.getBukkitEntity().getVelocity();
        this.adjustExit(entity, exit, velocity);
        entity.setLocationAndAngles(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
        if (entity.motionX != velocity.getX() || entity.motionY != velocity.getY() || entity.motionZ != velocity.getZ()) {
            entity.getBukkitEntity().setVelocity(velocity);
        }
        return true;
    }

    public ChunkCoordinates findPortal(double x, double y, double z, int short1) {
        if (this.worldServerInstance.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            return this.findEndPortal(this.worldServerInstance.provider.getEntrancePortalLocation());
        }
        // CraftBukkit end
        double d3 = -1.0D;
        int i = 0;
        int j = 0;
        int k = 0;
        // CraftBukkit start
        int l = MathHelper.floor_double(x);
        int i1 = MathHelper.floor_double(z);
        // CraftBukkit end
        long j1 = ChunkCoordIntPair.chunkXZ2Int(l, i1);
        boolean flag = true;
        double d4;
        int k1;

        if (this.field_85191_c.containsItem(j1)) {
            PortalPosition chunkcoordinatesportal = (PortalPosition) this.field_85191_c.getValueByKey(j1);

            d3 = 0.0D;
            i = chunkcoordinatesportal.posX;
            j = chunkcoordinatesportal.posY;
            k = chunkcoordinatesportal.posZ;
            chunkcoordinatesportal.field_85087_d = this.worldServerInstance.getTotalWorldTime();
            flag = false;
        } else {
            for (k1 = l - short1; k1 <= l + short1; ++k1) {
                double d5 = (double) k1 + 0.5D - x; // CraftBukkit

                for (int l1 = i1 - short1; l1 <= i1 + short1; ++l1) {
                    double d6 = (double) l1 + 0.5D - z; // CraftBukkit

                    for (int i2 = this.worldServerInstance.getActualHeight() - 1; i2 >= 0; --i2) {
                        if (this.worldServerInstance.getTypeId(k1, i2, l1) == Block.portal.blockID) {
                            while (this.worldServerInstance.getTypeId(k1, i2 - 1, l1) == Block.portal.blockID) {
                                --i2;
                            }

                            d4 = (double) i2 + 0.5D - y; // CraftBukkit
                            double d7 = d5 * d5 + d4 * d4 + d6 * d6;

                            if (d3 < 0.0D || d7 < d3) {
                                d3 = d7;
                                i = k1;
                                j = i2;
                                k = l1;
                            }
                        }
                    }
                }
            }
        }

        if (d3 >= 0.0D) {
            if (flag) {
                this.field_85191_c.add(j1, new PortalPosition(this, i, j, k, this.worldServerInstance.getTotalWorldTime()));
                this.field_85190_d.add(Long.valueOf(j1));
            }
            // CraftBukkit start - moved entity teleportation logic into exit
            return new ChunkCoordinates(i, j, k);
        } else {
            return null;
        }
    }
    // entity repositioning logic split out from original b method and combined with repositioning logic for The End from original a method
    public void adjustExit(Entity entity, Location position, Vector velocity) {
        Location from = position.clone();
        Vector before = velocity.clone();
        int i = position.getBlockX();
        int j = position.getBlockY();
        int k = position.getBlockZ();
        float f = position.getYaw();

        if (this.worldServerInstance.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            // entity.setPositionRotation((double) i, (double) j, (double) k, entity.yaw, 0.0F);
            // entity.motX = entity.motY = entity.motZ = 0.0D;
            position.setPitch(0.0F);
            position.setX(0);
            position.setY(0);
            position.setZ(0);
        } else {
            double d4;
            int k1;
            // CraftBukkit end

            double d8 = (double) i + 0.5D;
            double d9 = (double) j + 0.5D;

            d4 = (double) k + 0.5D;
            int j2 = -1;

            if (this.worldServerInstance.getTypeId(i - 1, j, k) == Block.portal.blockID) {
                j2 = 2;
            }

            if (this.worldServerInstance.getTypeId(i + 1, j, k) == Block.portal.blockID) {
                j2 = 0;
            }

            if (this.worldServerInstance.getTypeId(i, j, k - 1) == Block.portal.blockID) {
                j2 = 3;
            }

            if (this.worldServerInstance.getTypeId(i, j, k + 1) == Block.portal.blockID) {
                j2 = 1;
            }

            int k2 = entity.func_82148_at();

            if (j2 > -1) {
                int l2 = Direction.field_71578_g[j2];
                int i3 = Direction.offsetX[j2];
                int j3 = Direction.offsetZ[j2];
                int k3 = Direction.offsetX[l2];
                int l3 = Direction.offsetZ[l2];
                boolean flag1 = !this.worldServerInstance.isEmpty(i + i3 + k3, j, k + j3 + l3) || !this.worldServerInstance.isEmpty(i + i3 + k3, j + 1, k + j3 + l3);
                boolean flag2 = !this.worldServerInstance.isEmpty(i + i3, j, k + j3) || !this.worldServerInstance.isEmpty(i + i3, j + 1, k + j3);

                if (flag1 && flag2) {
                    j2 = Direction.footInvisibleFaceRemap[j2];
                    l2 = Direction.footInvisibleFaceRemap[l2];
                    i3 = Direction.offsetX[j2];
                    j3 = Direction.offsetZ[j2];
                    k3 = Direction.offsetX[l2];
                    l3 = Direction.offsetZ[l2];
                    k1 = i - k3;
                    d8 -= (double) k3;
                    int i4 = k - l3;

                    d4 -= (double) l3;
                    flag1 = !this.worldServerInstance.isEmpty(k1 + i3 + k3, j, i4 + j3 + l3) || !this.worldServerInstance.isEmpty(k1 + i3 + k3, j + 1, i4 + j3 + l3);
                    flag2 = !this.worldServerInstance.isEmpty(k1 + i3, j, i4 + j3) || !this.worldServerInstance.isEmpty(k1 + i3, j + 1, i4 + j3);
                }

                float f1 = 0.5F;
                float f2 = 0.5F;

                if (!flag1 && flag2) {
                    f1 = 1.0F;
                } else if (flag1 && !flag2) {
                    f1 = 0.0F;
                } else if (flag1 && flag2) {
                    f2 = 0.0F;
                }

                d8 += (double) ((float) k3 * f1 + f2 * (float) i3);
                d4 += (double) ((float) l3 * f1 + f2 * (float) j3);
                float f3 = 0.0F;
                float f4 = 0.0F;
                float f5 = 0.0F;
                float f6 = 0.0F;

                if (j2 == k2) {
                    f3 = 1.0F;
                    f4 = 1.0F;
                } else if (j2 == Direction.footInvisibleFaceRemap[k2]) {
                    f3 = -1.0F;
                    f4 = -1.0F;
                } else if (j2 == Direction.enderEyeMetaToDirection[k2]) {
                    f5 = 1.0F;
                    f6 = -1.0F;
                } else {
                    f5 = -1.0F;
                    f6 = 1.0F;
                }

                // CraftBukkit start
                double d10 = velocity.getX();
                double d11 = velocity.getZ();
                // CraftBukkit end

                // CraftBukkit start - adjust position and velocity instances instead of entity
                velocity.setX(d10 * (double) f3 + d11 * (double) f6);
                velocity.setZ(d10 * (double) f5 + d11 * (double) f4);
                f = f - (float) (k2 * 90) + (float) (j2 * 90);
            } else {
                // entity.motX = entity.motY = entity.motZ = 0.0D;
                velocity.setX(0);
                velocity.setY(0);
                velocity.setZ(0);
            }

            // entity.setPositionRotation(d8, d9, d4, entity.yaw, entity.pitch);
            position.setX(d8);
            position.setY(d9);
            position.setZ(d4);
            position.setYaw(f);
        }

        EntityPortalExitEvent event = new EntityPortalExitEvent(entity.getBukkitEntity(), from, position, before, velocity);
        this.worldServerInstance.getServer().getPluginManager().callEvent(event);
        Location to = event.getTo();
        if (event.isCancelled() || to == null || !entity.isEntityAlive()) {
            position = from;
            velocity = before;
        } else {
            position = to;
            velocity = event.getAfter();
        }
        // CraftBukkit end
    }

    public boolean func_85188_a(Entity par1Entity)
    {
        // CraftBukkit start - allow for portal creation to be based on coordinates instead of entity
        return this.createPortal(par1Entity.motionX, par1Entity.motionY, par1Entity.motionZ, 16);
    }

    public boolean createPortal(double x, double y, double z, int b0) {
        if (this.worldServerInstance.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            this.createEndPortal(x, y, z);
            return true;
        }
        // CraftBukkit end
        byte var2 = 16;
        double var3 = -1.0D;
        int var5 = MathHelper.floor_double(x);
        int var6 = MathHelper.floor_double(y);
        int var7 = MathHelper.floor_double(z);
        int var8 = var5;
        int var9 = var6;
        int var10 = var7;
        int var11 = 0;
        int var12 = this.random.nextInt(4);
        int var13;
        double var14;
        double var17;
        int var16;
        int var19;
        int var21;
        int var20;
        int var23;
        int var22;
        int var25;
        int var24;
        int var27;
        int var26;
        double var31;
        double var32;

        for (var13 = var5 - var2; var13 <= var5 + var2; ++var13)
        {
            var14 = (double)var13 + 0.5D - x;

            for (var16 = var7 - var2; var16 <= var7 + var2; ++var16)
            {
                var17 = (double)var16 + 0.5D - z;
                label274:

                for (var19 = this.worldServerInstance.getActualHeight() - 1; var19 >= 0; --var19)
                {
                    if (this.worldServerInstance.isAirBlock(var13, var19, var16))
                    {
                        while (var19 > 0 && this.worldServerInstance.isAirBlock(var13, var19 - 1, var16))
                        {
                            --var19;
                        }

                        for (var20 = var12; var20 < var12 + 4; ++var20)
                        {
                            var21 = var20 % 2;
                            var22 = 1 - var21;

                            if (var20 % 4 >= 2)
                            {
                                var21 = -var21;
                                var22 = -var22;
                            }

                            for (var23 = 0; var23 < 3; ++var23)
                            {
                                for (var24 = 0; var24 < 4; ++var24)
                                {
                                    for (var25 = -1; var25 < 4; ++var25)
                                    {
                                        var26 = var13 + (var24 - 1) * var21 + var23 * var22;
                                        var27 = var19 + var25;
                                        int var28 = var16 + (var24 - 1) * var22 - var23 * var21;

                                        if (var25 < 0 && !this.worldServerInstance.getBlockMaterial(var26, var27, var28).isSolid() || var25 >= 0 && !this.worldServerInstance.isAirBlock(var26, var27, var28))
                                        {
                                            continue label274;
                                        }
                                    }
                                }
                            }

                            var32 = (double)var19 + 0.5D - y;
                            var31 = var14 * var14 + var32 * var32 + var17 * var17;

                            if (var3 < 0.0D || var31 < var3)
                            {
                                var3 = var31;
                                var8 = var13;
                                var9 = var19;
                                var10 = var16;
                                var11 = var20 % 4;
                            }
                        }
                    }
                }
            }
        }

        if (var3 < 0.0D)
        {
            for (var13 = var5 - var2; var13 <= var5 + var2; ++var13)
            {
                var14 = (double)var13 + 0.5D - x;

                for (var16 = var7 - var2; var16 <= var7 + var2; ++var16)
                {
                    var17 = (double)var16 + 0.5D - z;
                    label222:

                    for (var19 = this.worldServerInstance.getActualHeight() - 1; var19 >= 0; --var19)
                    {
                        if (this.worldServerInstance.isAirBlock(var13, var19, var16))
                        {
                            while (var19 > 0 && this.worldServerInstance.isAirBlock(var13, var19 - 1, var16))
                            {
                                --var19;
                            }

                            for (var20 = var12; var20 < var12 + 2; ++var20)
                            {
                                var21 = var20 % 2;
                                var22 = 1 - var21;

                                for (var23 = 0; var23 < 4; ++var23)
                                {
                                    for (var24 = -1; var24 < 4; ++var24)
                                    {
                                        var25 = var13 + (var23 - 1) * var21;
                                        var26 = var19 + var24;
                                        var27 = var16 + (var23 - 1) * var22;

                                        if (var24 < 0 && !this.worldServerInstance.getBlockMaterial(var25, var26, var27).isSolid() || var24 >= 0 && !this.worldServerInstance.isAirBlock(var25, var26, var27))
                                        {
                                            continue label222;
                                        }
                                    }
                                }

                                var32 = (double)var19 + 0.5D - y;
                                var31 = var14 * var14 + var32 * var32 + var17 * var17;

                                if (var3 < 0.0D || var31 < var3)
                                {
                                    var3 = var31;
                                    var8 = var13;
                                    var9 = var19;
                                    var10 = var16;
                                    var11 = var20 % 2;
                                }
                            }
                        }
                    }
                }
            }
        }

        int var29 = var8;
        int var15 = var9;
        var16 = var10;
        int var30 = var11 % 2;
        int var18 = 1 - var30;

        if (var11 % 4 >= 2)
        {
            var30 = -var30;
            var18 = -var18;
        }

        boolean var33;

        if (var3 < 0.0D)
        {
            if (var9 < 70)
            {
                var9 = 70;
            }

            if (var9 > this.worldServerInstance.getActualHeight() - 10)
            {
                var9 = this.worldServerInstance.getActualHeight() - 10;
            }

            var15 = var9;

            for (var19 = -1; var19 <= 1; ++var19)
            {
                for (var20 = 1; var20 < 3; ++var20)
                {
                    for (var21 = -1; var21 < 3; ++var21)
                    {
                        var22 = var29 + (var20 - 1) * var30 + var19 * var18;
                        var23 = var15 + var21;
                        var24 = var16 + (var20 - 1) * var18 - var19 * var30;
                        var33 = var21 < 0;
                        this.worldServerInstance.setBlockWithNotify(var22, var23, var24, var33 ? Block.obsidian.blockID : 0);
                    }
                }
            }
        }

        for (var19 = 0; var19 < 4; ++var19)
        {
            this.worldServerInstance.editingBlocks = true;

            for (var20 = 0; var20 < 4; ++var20)
            {
                for (var21 = -1; var21 < 4; ++var21)
                {
                    var22 = var29 + (var20 - 1) * var30;
                    var23 = var15 + var21;
                    var24 = var16 + (var20 - 1) * var18;
                    var33 = var20 == 0 || var20 == 3 || var21 == -1 || var21 == 3;
                    this.worldServerInstance.setBlockWithNotify(var22, var23, var24, var33 ? Block.obsidian.blockID : Block.portal.blockID);
                }
            }

            this.worldServerInstance.editingBlocks = false;

            for (var20 = 0; var20 < 4; ++var20)
            {
                for (var21 = -1; var21 < 4; ++var21)
                {
                    var22 = var29 + (var20 - 1) * var30;
                    var23 = var15 + var21;
                    var24 = var16 + (var20 - 1) * var18;
                    this.worldServerInstance.notifyBlocksOfNeighborChange(var22, var23, var24, this.worldServerInstance.getBlockId(var22, var23, var24));
                }
            }
        }

        return true;
    }

    public void func_85189_a(long par1)
    {
        if (par1 % 100L == 0L)
        {
            Iterator var3 = this.field_85190_d.iterator();
            long var4 = par1 - 600L;

            while (var3.hasNext())
            {
                Long var6 = (Long)var3.next();
                PortalPosition var7 = (PortalPosition)this.field_85191_c.getValueByKey(var6.longValue());

                if (var7 == null || var7.field_85087_d < var4)
                {
                    var3.remove();
                    this.field_85191_c.remove(var6.longValue());
                }
            }
        }
    }
}
