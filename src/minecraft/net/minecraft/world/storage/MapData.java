package net.minecraft.world.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

// CraftBukkit start
import java.util.UUID;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.map.CraftMapView;
// CraftBukkit end

public class MapData extends WorldSavedData
{
    public int xCenter;
    public int zCenter;
    public int dimension;
    public byte scale;

    /** colours */
    public byte[] colors = new byte[16384];

    /**
     * Holds a reference to the MapInfo of the players who own a copy of the map
     */
    public List playersArrayList = new ArrayList();

    /**
     * Holds a reference to the players who own a copy of the map and a reference to their MapInfo
     */
    private Map playersHashMap = new HashMap();
    public Map playersVisibleOnMap = new LinkedHashMap();

    // CraftBukkit start
    public final CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId = null;
    // CraftBukkit end

    public MapData(String par1Str)
    {
        super(par1Str);
        // CraftBukkit start
        mapView = new CraftMapView(this);
        server = (CraftServer) org.bukkit.Bukkit.getServer();
        // CraftBukkit end
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        // CraftBukkit start
        NBTBase dimTag = par1NBTTagCompound.getTag("dimension");
        int dimension = (dimTag instanceof NBTTagByte) ? ((NBTTagByte) dimTag).data : ((NBTTagInt) dimTag).data;

        this.xCenter = par1NBTTagCompound.getInteger("xCenter");
        this.zCenter = par1NBTTagCompound.getInteger("zCenter");
        this.scale = par1NBTTagCompound.getByte("scale");

        if (this.scale < 0)
        {
            this.scale = 0;
        }

        if (this.scale > 4)
        {
            this.scale = 4;
        }

        short short1 = par1NBTTagCompound.getShort("width");
        short short2 = par1NBTTagCompound.getShort("height");

        if (short1 == 128 && short2 == 128)
        {
            this.colors = par1NBTTagCompound.getByteArray("colors");
        }
        else
        {
            byte[] abyte = par1NBTTagCompound.getByteArray("colors");
            this.colors = new byte[16384];
            int i = (128 - short1) / 2;
            int j = (128 - short2) / 2;

            for (int k = 0; k < short2; ++k)
            {
                int l = k + j;

                if (l >= 0 || l < 128)
                {
                    for (int i1 = 0; i1 < short1; ++i1)
                    {
                        int j1 = i1 + i;

                        if (j1 >= 0 || j1 < 128)
                        {
                            this.colors[j1 + l * 128] = abyte[i1 + k * short1];
                        }
                    }
                }
            }
        }
    }

    /**
     * write data to NBTTagCompound from this MapDataBase, similar to Entities and TileEntities
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        par1NBTTagCompound.setInteger("dimension", this.dimension);
        par1NBTTagCompound.setInteger("xCenter", this.xCenter);
        par1NBTTagCompound.setInteger("zCenter", this.zCenter);
        par1NBTTagCompound.setByte("scale", this.scale);
        par1NBTTagCompound.setShort("width", (short)128);
        par1NBTTagCompound.setShort("height", (short)128);
        par1NBTTagCompound.setByteArray("colors", this.colors);
    }

    /**
     * Adds the player passed to the list of visible players and checks to see which players are visible
     */
    public void updateVisiblePlayers(EntityPlayer par1EntityPlayer, ItemStack par2ItemStack)
    {
        if (!this.playersHashMap.containsKey(par1EntityPlayer))
        {
            MapInfo mapinfo = new MapInfo(this, par1EntityPlayer);
            this.playersHashMap.put(par1EntityPlayer, mapinfo);
            this.playersArrayList.add(mapinfo);
        }

        if (!par1EntityPlayer.inventory.hasItemStack(par2ItemStack))
        {
            this.playersVisibleOnMap.remove(par1EntityPlayer.getCommandSenderName());
        }

        for (int i = 0; i < this.playersArrayList.size(); ++i)
        {
            MapInfo mapinfo1 = (MapInfo)this.playersArrayList.get(i);

            if (!mapinfo1.entityplayerObj.isDead && (mapinfo1.entityplayerObj.inventory.hasItemStack(par2ItemStack) || par2ItemStack.isOnItemFrame()))
            {
                if (!par2ItemStack.isOnItemFrame() && mapinfo1.entityplayerObj.dimension == this.dimension)
                {
                    this.func_82567_a(0, mapinfo1.entityplayerObj.worldObj, mapinfo1.entityplayerObj.getCommandSenderName(), mapinfo1.entityplayerObj.posX, mapinfo1.entityplayerObj.posZ, (double)mapinfo1.entityplayerObj.rotationYaw);
                }
            }
            else
            {
                this.playersHashMap.remove(mapinfo1.entityplayerObj);
                this.playersArrayList.remove(mapinfo1);
            }
        }

        if (par2ItemStack.isOnItemFrame())
        {
            this.func_82567_a(1, par1EntityPlayer.worldObj, "frame-" + par2ItemStack.getItemFrame().entityId, (double)par2ItemStack.getItemFrame().xPosition, (double)par2ItemStack.getItemFrame().zPosition, (double)(par2ItemStack.getItemFrame().hangingDirection * 90));
        }
    }

    private void func_82567_a(int par1, World par2World, String par3Str, double par4, double par6, double par8)
    {
        int j = 1 << this.scale;
        float f = (float)(par4 - (double)this.xCenter) / (float)j;
        float f1 = (float)(par6 - (double)this.zCenter) / (float)j;
        byte b0 = (byte)((int)((double)(f * 2.0F) + 0.5D));
        byte b1 = (byte)((int)((double)(f1 * 2.0F) + 0.5D));
        byte b2 = 63;
        byte b3;

        if (f >= (float)(-b2) && f1 >= (float)(-b2) && f <= (float)b2 && f1 <= (float)b2)
        {
            par8 += par8 < 0.0D ? -8.0D : 8.0D;
            b3 = (byte)((int)(par8 * 16.0D / 360.0D));

            if (par2World.provider.shouldMapSpin(par3Str, par4, par6, par8))
            {
                int k = (int)(par2World.getWorldInfo().getWorldTime() / 10L);
                b3 = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
            }
        }
        else
        {
            if (Math.abs(f) >= 320.0F || Math.abs(f1) >= 320.0F)
            {
                this.playersVisibleOnMap.remove(par3Str);
                return;
            }

            par1 = 6;
            b3 = 0;

            if (f <= (float)(-b2))
            {
                b0 = (byte)((int)((double)(b2 * 2) + 2.5D));
            }

            if (f1 <= (float)(-b2))
            {
                b1 = (byte)((int)((double)(b2 * 2) + 2.5D));
            }

            if (f >= (float)b2)
            {
                b0 = (byte)(b2 * 2 + 1);
            }

            if (f1 >= (float)b2)
            {
                b1 = (byte)(b2 * 2 + 1);
            }
        }

        this.playersVisibleOnMap.put(par3Str, new MapCoord(this, (byte)par1, b0, b1, b3));
    }

    /**
     * Get byte array of packet data to send to players on map for updating map data
     */
    public byte[] getUpdatePacketData(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        MapInfo mapinfo = (MapInfo)this.playersHashMap.get(par3EntityPlayer);
        return mapinfo == null ? null : mapinfo.getPlayersOnMap(par1ItemStack);
    }

    /**
     * Marks a vertical range of pixels as being modified so they will be resent to clients. Parameters: X, lowest Y,
     * highest Y
     */
    public void setColumnDirty(int par1, int par2, int par3)
    {
        super.markDirty();

        for (int l = 0; l < this.playersArrayList.size(); ++l)
        {
            MapInfo mapinfo = (MapInfo)this.playersArrayList.get(l);

            if (mapinfo.field_76209_b[par1] < 0 || mapinfo.field_76209_b[par1] > par2)
            {
                mapinfo.field_76209_b[par1] = par2;
            }

            if (mapinfo.field_76210_c[par1] < 0 || mapinfo.field_76210_c[par1] < par3)
            {
                mapinfo.field_76210_c[par1] = par3;
            }
        }
    }

    public MapInfo func_82568_a(EntityPlayer par1EntityPlayer)
    {
        MapInfo mapinfo = (MapInfo)this.playersHashMap.get(par1EntityPlayer);

        if (mapinfo == null)
        {
            mapinfo = new MapInfo(this, par1EntityPlayer);
            this.playersHashMap.put(par1EntityPlayer, mapinfo);
            this.playersArrayList.add(mapinfo);
        }

        return mapinfo;
    }
}
