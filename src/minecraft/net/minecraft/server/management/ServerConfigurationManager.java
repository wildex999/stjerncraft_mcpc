package net.minecraft.server.management;

import cpw.mods.fml.common.network.FMLNetworkHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet16BlockItemSwitch;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet201PlayerInfo;
import net.minecraft.network.packet.Packet202PlayerAbilities;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet41EntityEffect;
import net.minecraft.network.packet.Packet43Experience;
import net.minecraft.network.packet.Packet4UpdateTime;
import net.minecraft.network.packet.Packet6SpawnPosition;
import net.minecraft.network.packet.Packet70GameEvent;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.demo.DemoWorldManager;
import net.minecraft.world.storage.IPlayerFileData;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.DimensionManager;
// CraftBukkit start
import net.minecraft.network.NetLoginHandler;
import org.bukkit.Location;
import org.bukkit.TravelAgent;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;
// CraftBukkit end

public abstract class ServerConfigurationManager
{
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");

    /** Reference to the logger. */
    public static final Logger logger = Logger.getLogger("Minecraft");

    /** Reference to the MinecraftServer object. */
    private final MinecraftServer mcServer;

    /** A list of player entities that exist on this server. */
    public final List playerEntityList = new java.util.concurrent.CopyOnWriteArrayList(); // CraftBukkit - ArrayList -> CopyOnWriteArrayList: Iterator safety
    private final BanList bannedPlayers = new BanList(new File("banned-players.txt"));
    private final BanList bannedIPs = new BanList(new File("banned-ips.txt"));

    /** A set containing the OPs. */
    private Set ops = new HashSet();

    /** The Set of all whitelisted players. */
    private Set whiteListedPlayers = new java.util.LinkedHashSet(); // CraftBukkit - HashSet -> LinkedHashSet

    /** Reference to the PlayerNBTManager object. */
    public IPlayerFileData playerNBTManagerObj; // CraftBukkit - private -> public

    /**
     * Server setting to only allow OPs and whitelisted players to join the server.
     */
    public boolean whiteListEnforced; // CraftBukkit - private -> public

    /** The maximum number of players that can be connected at a time. */
    protected int maxPlayers;
    protected int viewDistance;
    private EnumGameType gameType;

    /** True if all players are allowed to use commands (cheats). */
    private boolean commandsAllowedForAll;

    /**
     * index into playerEntities of player to ping, updated every tick; currently hardcoded to max at 200 players
     */
    private int playerPingIndex = 0;
    public boolean bukkitPluginTeleport = false;
    // CraftBukkit start
    private CraftServer cserver;

    public ServerConfigurationManager(MinecraftServer par1MinecraftServer)
    {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
        {
            par1MinecraftServer.server = new CraftServer(par1MinecraftServer, this);
            par1MinecraftServer.console = org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance();
            this.cserver = par1MinecraftServer.server;
            // CraftBukkit end
        }
        this.mcServer = par1MinecraftServer;
        this.bannedPlayers.setListActive(false);
        this.bannedIPs.setListActive(false);
        this.maxPlayers = 8;
    }

    public void initializeConnectionToPlayer(INetworkManager par1INetworkManager, EntityPlayerMP par2EntityPlayerMP)
    {
        this.readPlayerDataFromFile(par2EntityPlayerMP);
        par2EntityPlayerMP.setWorld(this.mcServer.worldServerForDimension(par2EntityPlayerMP.dimension));
        par2EntityPlayerMP.theItemInWorldManager.setWorld((WorldServer)par2EntityPlayerMP.worldObj);
        String var3 = "local";

        if (par1INetworkManager.getSocketAddress() != null)
        {
            var3 = par1INetworkManager.getSocketAddress().toString();
        }

        // CraftBukkit - add world and location to 'logged in' message.
        logger.info(par2EntityPlayerMP.username + "[" + var3 + "] logged in with entity id " + par2EntityPlayerMP.entityId + " at ([" + par2EntityPlayerMP.worldObj.worldInfo.getWorldName() + "] " + par2EntityPlayerMP.posX + ", " + par2EntityPlayerMP.posY + ", " + par2EntityPlayerMP.posZ + ")");
        WorldServer var4 = this.mcServer.worldServerForDimension(par2EntityPlayerMP.dimension);
        ChunkCoordinates var5 = var4.getSpawnPoint();
        this.func_72381_a(par2EntityPlayerMP, (EntityPlayerMP)null, var4);
        NetServerHandler var6 = new NetServerHandler(this.mcServer, par1INetworkManager, par2EntityPlayerMP);
        // CraftBukkit start -- Don't send a higher than 60 MaxPlayer size, otherwise the PlayerInfo window won't render correctly.
        int maxPlayers = this.getMaxPlayers();

        if (maxPlayers > 60)
        {
            maxPlayers = 60;
        }

        var6.sendPacketToPlayer(new Packet1Login(par2EntityPlayerMP.entityId, var4.getWorldInfo().getTerrainType(), par2EntityPlayerMP.theItemInWorldManager.getGameType(), var4.getWorldInfo().isHardcoreModeEnabled(), var4.provider.dimensionId, var4.difficultySetting, var4.getHeight(), maxPlayers));
        par2EntityPlayerMP.getBukkitEntity().sendSupportedChannels();
        // CraftBukkit end
        var6.sendPacketToPlayer(new Packet6SpawnPosition(var5.posX, var5.posY, var5.posZ));
        var6.sendPacketToPlayer(new Packet202PlayerAbilities(par2EntityPlayerMP.capabilities));
        var6.sendPacketToPlayer(new Packet16BlockItemSwitch(par2EntityPlayerMP.inventory.currentItem));
        this.updateTimeAndWeatherForPlayer(par2EntityPlayerMP, var4);
        // this.sendAll(new Packet3Chat("\u00A7e" + entityplayer.name + " joined the game.")); // CraftBukkit - handled in event
        this.playerLoggedIn(par2EntityPlayerMP);
        var6.setPlayerLocation(par2EntityPlayerMP.posX, par2EntityPlayerMP.posY, par2EntityPlayerMP.posZ, par2EntityPlayerMP.rotationYaw, par2EntityPlayerMP.rotationPitch);
        this.mcServer.getNetworkThread().addPlayer(var6);
        var6.sendPacketToPlayer(new Packet4UpdateTime(var4.getTotalWorldTime(), var4.getWorldTime()));

        if (this.mcServer.getTexturePack().length() > 0)
        {
            par2EntityPlayerMP.requestTexturePackLoad(this.mcServer.getTexturePack(), this.mcServer.textureSize());
        }

        Iterator var7 = par2EntityPlayerMP.getActivePotionEffects().iterator();

        while (var7.hasNext())
        {
            PotionEffect var8 = (PotionEffect)var7.next();
            var6.sendPacketToPlayer(new Packet41EntityEffect(par2EntityPlayerMP.entityId, var8));
        }

        par2EntityPlayerMP.addSelfToInternalCraftingInventory();
        FMLNetworkHandler.handlePlayerLogin(par2EntityPlayerMP, var6, par1INetworkManager); // Forge
    }

    /**
     * Sets the NBT manager to the one for the WorldServer given.
     */
    public void setPlayerManager(WorldServer[] par1ArrayOfWorldServer)
    {
        if (this.playerNBTManagerObj != null)
        {
            return;    // CraftBukkit
        }

        this.playerNBTManagerObj = par1ArrayOfWorldServer[0].getSaveHandler().getSaveHandler();
    }

    public void func_72375_a(EntityPlayerMP par1EntityPlayerMP, WorldServer par2WorldServer)
    {
        WorldServer var3 = par1EntityPlayerMP.getServerForPlayer();

        if (par2WorldServer != null)
        {
            par2WorldServer.getPlayerManager().removePlayer(par1EntityPlayerMP);
        }

        var3.getPlayerManager().addPlayer(par1EntityPlayerMP);
        var3.theChunkProviderServer.loadChunk((int)par1EntityPlayerMP.posX >> 4, (int)par1EntityPlayerMP.posZ >> 4);
    }

    public int getEntityViewDistance()
    {
        return PlayerManager.getFurthestViewableBlock(this.getViewDistance());
    }

    /**
     * called during player login. reads the player information from disk.
     */
    public void readPlayerDataFromFile(EntityPlayerMP par1EntityPlayerMP)
    {
        NBTTagCompound var2 = this.mcServer.worlds.get(0).getWorldInfo().getPlayerNBTTagCompound(); // CraftBukkit

        if (par1EntityPlayerMP.getCommandSenderName().equals(this.mcServer.getServerOwner()) && var2 != null)
        {
            par1EntityPlayerMP.readFromNBT(var2);
        }
        else
        {
            this.playerNBTManagerObj.readPlayerData(par1EntityPlayerMP);
        }
    }

    /**
     * also stores the NBTTags if this is an intergratedPlayerList
     */
    protected void writePlayerData(EntityPlayerMP par1EntityPlayerMP)
    {
        this.playerNBTManagerObj.writePlayerData(par1EntityPlayerMP);
    }

    /**
     * Called when a player successfully logs in. Reads player data from disk and inserts the player into the world.
     */
    public void playerLoggedIn(EntityPlayerMP par1EntityPlayerMP)
    {
        cserver.detectListNameConflict(par1EntityPlayerMP); // CraftBukkit
        // this.sendAll(new Packet201PlayerInfo(entityplayer.name, true, 1000)); // CraftBukkit - replaced with loop below
        this.playerEntityList.add(par1EntityPlayerMP);
        WorldServer var2 = this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension);
        // CraftBukkit start
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(this.cserver.getPlayer(par1EntityPlayerMP), "\u00A7e" + par1EntityPlayerMP.username + " joined the game.");
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        String joinMessage = playerJoinEvent.getJoinMessage();

        if ((joinMessage != null) && (joinMessage.length() > 0))
        {
            this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat(joinMessage));
        }

        this.cserver.onPlayerJoin(playerJoinEvent.getPlayer());
        ChunkIOExecutor.adjustPoolSize(this.getCurrentPlayerCount());
        // CraftBukkit end

        // CraftBukkit start - only add if the player wasn't moved in the event
        if (par1EntityPlayerMP.worldObj == var2 && !var2.playerEntities.contains(par1EntityPlayerMP))
        {
            var2.spawnEntityInWorld(par1EntityPlayerMP);
            this.func_72375_a(par1EntityPlayerMP, (WorldServer) null);
        }

        // CraftBukkit end
        // CraftBukkit start - sendAll above replaced with this loop
        Packet201PlayerInfo packet = new Packet201PlayerInfo(par1EntityPlayerMP.listName, true, 1000);

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            EntityPlayerMP entityplayer1 = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayer1.getBukkitEntity().canSee(par1EntityPlayerMP.getBukkitEntity()))
            {
                entityplayer1.playerNetServerHandler.sendPacketToPlayer(packet);
            }
        }

        // CraftBukkit end

        for (int var3 = 0; var3 < this.playerEntityList.size(); ++var3)
        {
            EntityPlayerMP var4 = (EntityPlayerMP)this.playerEntityList.get(var3);

            // CraftBukkit start - .name -> .listName
            if (par1EntityPlayerMP.getBukkitEntity().canSee(var4.getBukkitEntity()))
            {
                par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet201PlayerInfo(var4.listName, true, var4.ping));
            }

            // CraftBukkit end
        }
    }

    /**
     * using player's dimension, update their movement when in a vehicle (e.g. cart, boat)
     */
    public void serverUpdateMountedMovingPlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        par1EntityPlayerMP.getServerForPlayer().getPlayerManager().updateMountedMovingPlayer(par1EntityPlayerMP);
    }

    /**
     * Called when a player disconnects from the game. Writes player data to disk and removes them from the world.
     */
    public String playerLoggedOut(EntityPlayerMP entityplayer)   // CraftBukkit - return string
    {
        if (entityplayer.playerNetServerHandler.connectionClosed)
        {
            return null;    // CraftBukkit - exploitsies fix
        }

        // CraftBukkit start - quitting must be before we do final save of data, in case plugins need to modify it
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(this.cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.username + " left the game.");
        this.cserver.getPluginManager().callEvent(playerQuitEvent);
        entityplayer.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
        // CraftBukkit end
        GameRegistry.onPlayerLogout(entityplayer); // Forge
        this.writePlayerData(entityplayer);
        WorldServer worldserver = entityplayer.getServerForPlayer();
        worldserver.removeEntity(entityplayer);
        worldserver.getPlayerManager().removePlayer(entityplayer);
        this.playerEntityList.remove(entityplayer);
        ChunkIOExecutor.adjustPoolSize(this.getCurrentPlayerCount()); // CraftBukkit
        // CraftBukkit start - .name -> .listName, replace sendAll with loop
        Packet201PlayerInfo packet = new Packet201PlayerInfo(entityplayer.listName, false, 9999);

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            EntityPlayerMP entityplayer1 = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayer1.getBukkitEntity().canSee(entityplayer.getBukkitEntity()))
            {
                entityplayer1.playerNetServerHandler.sendPacketToPlayer(packet);
            }
        }

        return playerQuitEvent.getQuitMessage();
        // CraftBukkit end
    }

    /**
     * checks ban-lists, then white-lists, then space for the server. Returns null on success, or an error message
     */
    public String allowUserToConnect(SocketAddress par1SocketAddress, String par2Str)
    {
        if (this.bannedPlayers.isBanned(par2Str))
        {
            BanEntry var6 = (BanEntry)this.bannedPlayers.getBannedList().get(par2Str);
            String var7 = "You are banned from this server!\nReason: " + var6.getBanReason();

            if (var6.getBanEndDate() != null)
            {
                var7 = var7 + "\nYour ban will be removed on " + dateFormat.format(var6.getBanEndDate());
            }

            return var7;
        }
        else if (!this.isAllowedToLogin(par2Str))
        {
            return "You are not white-listed on this server!";
        }
        else
        {
            String var3 = par1SocketAddress.toString();
            var3 = var3.substring(var3.indexOf("/") + 1);
            var3 = var3.substring(0, var3.indexOf(":"));

            if (this.bannedIPs.isBanned(var3))
            {
                BanEntry var4 = (BanEntry)this.bannedIPs.getBannedList().get(var3);
                String var5 = "Your IP address is banned from this server!\nReason: " + var4.getBanReason();

                if (var4.getBanEndDate() != null)
                {
                    var5 = var5 + "\nYour ban will be removed on " + dateFormat.format(var4.getBanEndDate());
                }

                return var5;
            }
            else
            {
                return this.playerEntityList.size() >= this.maxPlayers ? "The server is full!" : null;
            }
        }
    }

    // CraftBukkit start - Whole method and signature
    public EntityPlayerMP attemptLogin(NetLoginHandler pendingconnection, String s, String hostname)
    {
        // Instead of kicking then returning, we need to store the kick reason
        // in the event, check with plugins to see if it's ok, and THEN kick
        // depending on the outcome.
        EntityPlayerMP entity = new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), s, this.mcServer.isDemo() ? new DemoWorldManager(this.mcServer.worldServerForDimension(0)) : new ItemInWorldManager(this.mcServer.worldServerForDimension(0)));
        Player player = entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, pendingconnection.getSocket().getInetAddress());
        SocketAddress socketaddress = pendingconnection.myTCPConnection.getSocketAddress();
        if (this.bannedPlayers.isBanned(s))
        {
            BanEntry banentry = (BanEntry) this.bannedPlayers.getBannedList().get(s);
            String s1 = "You are banned from this server!\nReason: " + banentry.getBanReason();

            if (banentry.getBanEndDate() != null)
            {
                s1 = s1 + "\nYour ban will be removed on " + dateFormat.format(banentry.getBanEndDate());
            }

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s1);
        }
        else if (!this.isAllowedToLogin(s))
        {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, cserver.whitelistMessage); // Spigot
        }
        else
        {
            String s2 = socketaddress.toString();
            s2 = s2.substring(s2.indexOf("/") + 1);
            s2 = s2.substring(0, s2.indexOf(":"));

            if (this.bannedIPs.isBanned(s2))
            {
                BanEntry banentry1 = (BanEntry) this.bannedIPs.getBannedList().get(s2);
                String s3 = "Your IP address is banned from this server!\nReason: " + banentry1.getBanReason();

                if (banentry1.getBanEndDate() != null)
                {
                    s3 = s3 + "\nYour ban will be removed on " + dateFormat.format(banentry1.getBanEndDate());
                }

                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s3);
            }
            else if (this.playerEntityList.size() >= this.maxPlayers)
            {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, "The server is full!");
            }
            else
            {
                event.disallow(PlayerLoginEvent.Result.ALLOWED, s2);
            }
        }
        this.cserver.getPluginManager().callEvent(event);

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED)
        {
            pendingconnection.raiseErrorAndDisconnect(event.getKickMessage());
            return null;
        }
        return entity;
        // CraftBukkit end
    }

    /**
     * also checks for multiple logins
     */
    public EntityPlayerMP createPlayerForUser(String par1Str)
    {
        ArrayList var2 = new ArrayList();
        EntityPlayerMP var4;

        for (int var3 = 0; var3 < this.playerEntityList.size(); ++var3)
        {
            var4 = (EntityPlayerMP)this.playerEntityList.get(var3);

            if (var4.username.equalsIgnoreCase(par1Str))
            {
                var2.add(var4);
            }
        }

        Iterator var5 = var2.iterator();

        while (var5.hasNext())
        {
            var4 = (EntityPlayerMP)var5.next();
            var4.playerNetServerHandler.kickPlayerFromServer("You logged in from another location");
        }

        Object var6;

        if (this.mcServer.isDemo())
        {
            var6 = new DemoWorldManager(this.mcServer.worldServerForDimension(0));
        }
        else
        {
            var6 = new ItemInWorldManager(this.mcServer.worldServerForDimension(0));
        }

        return new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), par1Str, (ItemInWorldManager)var6);
    }

    public EntityPlayerMP processLogin(EntityPlayerMP player)   // CraftBukkit - String -> EntityPlayer
    {
        String s = player.username; // CraftBukkit
        ArrayList arraylist = new ArrayList();
        EntityPlayerMP entityplayer;

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            entityplayer = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayer.username.equalsIgnoreCase(s))
            {
                arraylist.add(entityplayer);
            }
        }

        Iterator iterator = arraylist.iterator();

        while (iterator.hasNext())
        {
            entityplayer = (EntityPlayerMP) iterator.next();
            entityplayer.playerNetServerHandler.kickPlayerFromServer("You logged in from another location");
        }

        /* CraftBukkit start
        Object object;

        if (this.server.M()) {
            object = new DemoPlayerInteractManager(this.server.getWorldServer(0));
        } else {
            object = new PlayerInteractManager(this.server.getWorldServer(0));
        }

        return new EntityPlayer(this.server, this.server.getWorldServer(0), s, (PlayerInteractManager) object);
        */
        return player;
        // CraftBukkit end
    }

    // CraftBukkit start

    /**
     * creates and returns a respawned player based on the provided PlayerEntity. Args are the PlayerEntityMP to
     * respawn, an INT for the dimension to respawn into (usually 0), and a boolean value that is true if the player
     * beat the game rather than dying
     */
    public EntityPlayerMP respawnPlayer(EntityPlayerMP par1EntityPlayerMP, int par2, boolean par3)
    {
        return this.respawnPlayer(par1EntityPlayerMP, par2, par3, null);
    }

    public EntityPlayerMP respawnPlayer(EntityPlayerMP entityplayer, int i, boolean flag, Location location)
    {
        // CraftBukkit end
        entityplayer.getServerForPlayer().getEntityTracker().removeAllTrackingPlayers(entityplayer);
        // entityplayer.p().getTracker().untrackEntity(entityplayer); // CraftBukkit
        entityplayer.getServerForPlayer().getPlayerManager().removePlayer(entityplayer);
        this.playerEntityList.remove(entityplayer);
        this.mcServer.worldServerForDimension(entityplayer.dimension).removePlayerEntityDangerously(entityplayer);
        ChunkCoordinates chunkcoordinates = entityplayer.getBedLocation();
        boolean flag1 = entityplayer.isSpawnForced();
        // CraftBukkit start
        EntityPlayerMP entityplayer1 = entityplayer;
        org.bukkit.World fromWorld = entityplayer1.getBukkitEntity().getWorld();
        entityplayer1.playerConqueredTheEnd = false;
        entityplayer1.clonePlayer(entityplayer, flag);
        ChunkCoordinates chunkcoordinates1;

        if (location == null)
        {
            boolean isBedSpawn = false;
            CraftWorld cworld = (CraftWorld) this.mcServer.server.getWorld(entityplayer.spawnWorld);

            if (cworld != null && chunkcoordinates != null)
            {
                chunkcoordinates1 = EntityPlayer.verifyRespawnCoordinates(cworld.getHandle(), chunkcoordinates, flag1);

                if (chunkcoordinates1 != null)
                {
                    isBedSpawn = true;
                    location = new Location(cworld, chunkcoordinates1.posX + 0.5, chunkcoordinates1.posY, chunkcoordinates1.posZ + 0.5);
                }
                else
                {
                    entityplayer1.setSpawnChunk(null, true);
                    entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(0, 0));
                }
            }

            if (location == null)
            {
                cworld = (CraftWorld) this.mcServer.server.getWorlds().get(0);
                chunkcoordinates = cworld.getHandle().getSpawnPoint();
                location = new Location(cworld, chunkcoordinates.posX + 0.5, chunkcoordinates.posY, chunkcoordinates.posZ + 0.5);
            }

            Player respawnPlayer = this.cserver.getPlayer(entityplayer1);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            location = respawnEvent.getRespawnLocation();
            entityplayer.reset();
        }
        else
        {
            location.setWorld(this.mcServer.worldServerForDimension(i).getWorld());
        }

        WorldServer worldserver = ((CraftWorld) location.getWorld()).getHandle();
        entityplayer1.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        // CraftBukkit end
        worldserver.theChunkProviderServer.loadChunk((int) entityplayer1.posX >> 4, (int) entityplayer1.posZ >> 4);

        while (!worldserver.getCollidingBoundingBoxes(entityplayer1, entityplayer1.boundingBox).isEmpty())
        {
            entityplayer1.setPosition(entityplayer1.posX, entityplayer1.posY + 1.0D, entityplayer1.posZ);
        }

        // CraftBukkit start
        byte actualDimension = (byte)worldserver.getWorld().getEnvironment().getId(); // MCPC+ - represents the actual dimension for target world
        // MCPC+ start - add support for Mystcraft dimensions
        if (worldserver.getWorld().getEnvironment().name().equals("MYST"))
            actualDimension = (byte)i;
        // MCPC+ end
        // Force the client to refresh their chunk cache.
        entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet9Respawn((byte)(actualDimension >= 0 ? -1 : 0), (byte) worldserver.difficultySetting, worldserver.getWorldInfo().getTerrainType(), worldserver.getHeight(), entityplayer.theItemInWorldManager.getGameType()));
        entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet9Respawn(actualDimension, (byte) worldserver.difficultySetting, worldserver.getWorldInfo().getTerrainType(), worldserver.getHeight(), entityplayer.theItemInWorldManager.getGameType()));
        entityplayer1.setWorld(worldserver);
        entityplayer1.isDead = false;
        entityplayer1.playerNetServerHandler.teleport(new Location(worldserver.getWorld(), entityplayer1.posX, entityplayer1.posY, entityplayer1.posZ, entityplayer1.rotationYaw, entityplayer1.rotationPitch));
        // MCPC+ start - This flag is set when a bukkit plugin initiates a teleport. This forces a dimension update to guarantee that the client is in sync with server. Fixes the IC2 texture orientation bug.
        if (bukkitPluginTeleport && !DimensionManager.checkMVDim(i))
        {
            entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet9Respawn(i, (byte) worldserver.difficultySetting, worldserver.getWorldInfo().getTerrainType(), worldserver.getHeight(), entityplayer.theItemInWorldManager.getGameType()));
            bukkitPluginTeleport = false;
        }
        // MCPC+ end
        entityplayer1.setSneaking(false);
        chunkcoordinates1 = worldserver.getSpawnPoint();
        // CraftBukkit end
        entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet6SpawnPosition(chunkcoordinates1.posX, chunkcoordinates1.posY, chunkcoordinates1.posZ));
        entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet43Experience(entityplayer1.experience, entityplayer1.experienceTotal, entityplayer1.experienceLevel));
        this.updateTimeAndWeatherForPlayer(entityplayer1, worldserver);
        worldserver.getPlayerManager().addPlayer(entityplayer1);
        worldserver.spawnEntityInWorld(entityplayer1);
        this.playerEntityList.add(entityplayer1);
        // CraftBukkit start - added from changeDimension
        this.syncPlayerInventory(entityplayer1); // CraftBukkit
        entityplayer1.sendPlayerAbilities();
        Iterator iterator = entityplayer1.getActivePotionEffects().iterator();

        while (iterator.hasNext())
        {
            PotionEffect mobeffect = (PotionEffect) iterator.next();
            entityplayer1.playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(entityplayer1.entityId, mobeffect));
        }

        // CraftBukkit end

        // CraftBukkit start - don't fire on respawn
        if (fromWorld != location.getWorld())
        {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent((Player) entityplayer1.getBukkitEntity(), fromWorld);
            Bukkit.getServer().getPluginManager().callEvent(event);
        }

        // CraftBukkit end
        GameRegistry.onPlayerRespawn(entityplayer1); // Forge
        return entityplayer1;
    }

    public void transferPlayerToDimension(EntityPlayerMP par1EntityPlayerMP, int par2)
    {
        this.transferPlayerToDimension(par1EntityPlayerMP, par2, (TeleportCause)null);
    }

    public void transferPlayerToDimension(EntityPlayerMP par1EntityPlayerMP, int par2, Teleporter teleporter)
    {
        int var3 = par1EntityPlayerMP.dimension;
        WorldServer var4 = this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension);
        par1EntityPlayerMP.dimension = par2;
        WorldServer var5 = this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension);

        par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet9Respawn(par1EntityPlayerMP.dimension, (byte)par1EntityPlayerMP.worldObj.difficultySetting, var5.getWorldInfo().getTerrainType(), var5.getHeight(), par1EntityPlayerMP.theItemInWorldManager.getGameType()));
        var4.removePlayerEntityDangerously(par1EntityPlayerMP);
        par1EntityPlayerMP.isDead = false;
        this.transferEntityToWorld(par1EntityPlayerMP, var3, var4, var5, teleporter);
        this.func_72375_a(par1EntityPlayerMP, var4);
        par1EntityPlayerMP.playerNetServerHandler.setPlayerLocation(par1EntityPlayerMP.posX, par1EntityPlayerMP.posY, par1EntityPlayerMP.posZ, par1EntityPlayerMP.rotationYaw, par1EntityPlayerMP.rotationPitch);
        par1EntityPlayerMP.theItemInWorldManager.setWorld(var5);
        this.updateTimeAndWeatherForPlayer(par1EntityPlayerMP, var5);
        this.syncPlayerInventory(par1EntityPlayerMP);
        Iterator var6 = par1EntityPlayerMP.getActivePotionEffects().iterator();

        while (var6.hasNext())
        {
            PotionEffect var7 = (PotionEffect)var6.next();
            par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(par1EntityPlayerMP.entityId, var7));
        }

        GameRegistry.onPlayerChangedDimension(par1EntityPlayerMP);
    }
    
    public void transferPlayerToDimension(EntityPlayerMP entityplayer, int i, TeleportCause cause)
    {
        // CraftBukkit start -- Replaced the standard handling of portals with a more customised method.
        WorldServer exitWorld = this.mcServer.worldServerForDimension(i);

        Location enter = entityplayer.getBukkitEntity().getLocation();
        Location exit = null;
        if ((cause == TeleportCause.END_PORTAL) && (i == 0)) {
            // THE_END -> NORMAL; use bed if available
            exit = ((CraftPlayer) entityplayer.getBukkitEntity()).getBedSpawnLocation();
        }
        if (exit == null) {
            exit = this.calculateTarget(enter, exitWorld);
        }

        TravelAgent agent = (TravelAgent) ((CraftWorld) exit.getWorld()).getHandle().func_85176_s();
        PlayerPortalEvent event = new PlayerPortalEvent(entityplayer.getBukkitEntity(), enter, exit, agent, cause);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null) {
            return;
        }
        exit = event.useTravelAgent() ? event.getPortalTravelAgent().findOrCreate(exit) : event.getTo();
        exitWorld = ((CraftWorld) exit.getWorld()).getHandle();

        Vector velocity = entityplayer.getBukkitEntity().getVelocity();
        boolean before = exitWorld.theChunkProviderServer.loadChunkOnProvideRequest;
        exitWorld.theChunkProviderServer.loadChunkOnProvideRequest = true;
        exitWorld.func_85176_s().adjustExit(entityplayer, exit, velocity);
        exitWorld.theChunkProviderServer.loadChunkOnProvideRequest = before;

        this.respawnPlayer(entityplayer, exitWorld.dimension, true, exit);
        if (entityplayer.motionX != velocity.getX() || entityplayer.motionY != velocity.getY() || entityplayer.motionZ != velocity.getZ()) {
            entityplayer.getBukkitEntity().setVelocity(velocity);
        }
        // CraftBukkit end
        GameRegistry.onPlayerChangedDimension(entityplayer);
    }

    // copy of original a(Entity, int, WorldServer, WorldServer) method with only location calculation logic
    public Location calculateTarget(Location enter, World target) {
        WorldServer worldserver = ((CraftWorld) enter.getWorld()).getHandle();
        WorldServer worldserver1 = ((CraftWorld) target.getWorld()).getHandle();
        int i = worldserver.dimension;

        double y = enter.getY();
        float yaw = enter.getYaw();
        float pitch = enter.getPitch();
        double d0 = enter.getX();
        double d1 = enter.getZ();
        double d2 = 8.0D;

        if (worldserver1.dimension == -1) {
            d0 /= d2;
            d1 /= d2;
        } else if (worldserver1.dimension == 0) {
            d0 *= d2;
            d1 *= d2;
        } else {
            ChunkCoordinates chunkcoordinates;

            if (i == 1) {
                chunkcoordinates = worldserver1.getSpawnPoint();
            } else {
                chunkcoordinates = worldserver1.getEntrancePortalLocation();
            }
            if (chunkcoordinates == null)
                chunkcoordinates = worldserver1.getSpawnPoint();
            d0 = (double) chunkcoordinates.posX;
            y = (double) chunkcoordinates.posY;
            d1 = (double) chunkcoordinates.posZ;
            yaw = 90.0F;
            pitch = 0.0F;
        }

        if (i != 1) {
            d0 = (double) MathHelper.clamp_int((int) d0, -29999872, 29999872);
            d1 = (double) MathHelper.clamp_int((int) d1, -29999872, 29999872);
        }

        return new Location(target.getWorld(), d0, y, d1, yaw, pitch);
    }

    // copy of original a(Entity, int, WorldServer, WorldServer) method with only entity repositioning logic
    public void repositionEntity(Entity entity, Location exit, boolean portal) {
        int i = entity.dimension;
        WorldServer worldserver = (WorldServer) entity.worldObj;
        WorldServer worldserver1 = ((CraftWorld) exit.getWorld()).getHandle();

        worldserver.theProfiler.startSection("moving");
        entity.setLocationAndAngles(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
        if (entity.isEntityAlive()) {
            worldserver.updateEntityWithOptionalForce(entity, false);
        }

        worldserver.theProfiler.endSection();
        if (i != 1) {
            worldserver.theProfiler.startSection("placing");
            if (entity.isEntityAlive()) {
                worldserver1.spawnEntityInWorld(entity);
                worldserver1.updateEntityWithOptionalForce(entity, false);
                if (portal) {
                    Vector velocity = entity.getBukkitEntity().getVelocity();
                    worldserver1.func_85176_s().adjustExit(entity, exit, velocity);
                    entity.setLocationAndAngles(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
                    if (entity.motionX != velocity.getX() || entity.motionY != velocity.getY() || entity.motionZ != velocity.getZ()) {
                        entity.getBukkitEntity().setVelocity(velocity);
                    }
                }
            }

            worldserver.theProfiler.endSection();
        }

        entity.setWorld(worldserver1);
        // CraftBukkit end
    }
    public void transferEntityToWorld(Entity par1Entity, int par2, WorldServer par3WorldServer, WorldServer par4WorldServer, Teleporter teleporter)
    {
        WorldProvider pOld = par3WorldServer.provider;
        WorldProvider pNew = par4WorldServer.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double var5 = par1Entity.posX * moveFactor;
        double var7 = par1Entity.posZ * moveFactor;
        double var11 = par1Entity.posX;
        double var13 = par1Entity.posY;
        double var15 = par1Entity.posZ;
        float var17 = par1Entity.rotationYaw;
        par3WorldServer.theProfiler.startSection("moving");

        if (par1Entity.dimension == 1)
        {
            ChunkCoordinates var18;

            if (par2 == 1)
            {
                var18 = par4WorldServer.getSpawnPoint();
            }
            else
            {
                var18 = par4WorldServer.getEntrancePortalLocation();
            }

            var5 = (double)var18.posX;
            par1Entity.posY = (double)var18.posY;
            var7 = (double)var18.posZ;
            par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, 90.0F, 0.0F);

            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }

        par3WorldServer.theProfiler.endSection();

        if (par2 != 1)
        {
            par3WorldServer.theProfiler.startSection("placing");
            var5 = (double)MathHelper.clamp_int((int)var5, -29999872, 29999872);
            var7 = (double)MathHelper.clamp_int((int)var7, -29999872, 29999872);

            if (par1Entity.isEntityAlive())
            {
                par4WorldServer.spawnEntityInWorld(par1Entity);
                par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, par1Entity.rotationYaw, par1Entity.rotationPitch);
                par4WorldServer.updateEntityWithOptionalForce(par1Entity, false);
                teleporter.placeInPortal(par1Entity, var11, var13, var15, var17);
            }

            par3WorldServer.theProfiler.endSection();
        }

        par1Entity.setWorld(par4WorldServer);
    }

    /**
     * Transfers an entity from a world to another world.
     */
    public void transferEntityToWorld(Entity par1Entity, int par2, WorldServer par3WorldServer, WorldServer par4WorldServer)
    {
        double var5 = par1Entity.posX;
        double var7 = par1Entity.posZ;
        double var9 = 8.0D;
        double var11 = par1Entity.posX;
        double var13 = par1Entity.posY;
        double var15 = par1Entity.posZ;
        float var17 = par1Entity.rotationYaw;
        par3WorldServer.theProfiler.startSection("moving");
        if (par1Entity.dimension == -1)
        {
            var5 /= var9;
            var7 /= var9;
            par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, par1Entity.rotationYaw, par1Entity.rotationPitch);
            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }
        else if (par1Entity.dimension == 0)
        {
            var5 *= var9;
            var7 *= var9;
            par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, par1Entity.rotationYaw, par1Entity.rotationPitch);
            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }
        else
        {
            ChunkCoordinates var18;
            if (par2 == 1)
            {
                var18 = par4WorldServer.getSpawnPoint();
            }
            else
            {
                var18 = par4WorldServer.getEntrancePortalLocation();
            }

            var5 = (double)var18.posX;
            par1Entity.posY = (double)var18.posY;
            var7 = (double)var18.posZ;
            par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, 90.0F, 0.0F);

            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }

        par3WorldServer.theProfiler.endSection();

        if (par2 != 1)
        {
            par3WorldServer.theProfiler.startSection("placing");
            var5 = (double)MathHelper.clamp_int((int)var5, -29999872, 29999872);
            var7 = (double)MathHelper.clamp_int((int)var7, -29999872, 29999872);
            if (par1Entity.isEntityAlive())
            {
                par4WorldServer.spawnEntityInWorld(par1Entity);
                par1Entity.setLocationAndAngles(var5, par1Entity.posY, var7, par1Entity.rotationYaw, par1Entity.rotationPitch);
                par4WorldServer.updateEntityWithOptionalForce(par1Entity, false);
                par4WorldServer.func_85176_s().placeInPortal(par1Entity, var11, var13, var15, var17);
            }

            par3WorldServer.theProfiler.endSection();
        }
        par1Entity.setWorld(par4WorldServer);
    }

    /**
     * sends 1 player per tick, but only sends a player once every 600 ticks
     */
    public void sendPlayerInfoToAllPlayers()
    {
        if (++this.playerPingIndex > 600)
        {
            this.playerPingIndex = 0;
        }

        /* CraftBukkit start - remove updating of lag to players -- it spams way to much on big servers.
        if (this.o < this.players.size()) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(this.o);

            this.sendAll(new Packet201PlayerInfo(entityplayer.name, true, entityplayer.ping));
        }
        // CraftBukkit end */
    }

    /**
     * sends a packet to all players
     */
    public void sendPacketToAllPlayers(Packet par1Packet)
    {
        for (int var2 = 0; var2 < this.playerEntityList.size(); ++var2)
        {
            ((EntityPlayerMP)this.playerEntityList.get(var2)).playerNetServerHandler.sendPacketToPlayer(par1Packet);
        }
    }

    /**
     * Sends a packet to all players in the specified Dimension
     */
    public void sendPacketToAllPlayersInDimension(Packet par1Packet, int par2)
    {
        for (int var3 = 0; var3 < this.playerEntityList.size(); ++var3)
        {
            EntityPlayerMP var4 = (EntityPlayerMP)this.playerEntityList.get(var3);

            if (var4.dimension == par2)
            {
                var4.playerNetServerHandler.sendPacketToPlayer(par1Packet);
            }
        }
    }

    /**
     * returns a string containing a comma-seperated list of player names
     */
    public String getPlayerListAsString()
    {
        String var1 = "";

        for (int var2 = 0; var2 < this.playerEntityList.size(); ++var2)
        {
            if (var2 > 0)
            {
                var1 = var1 + ", ";
            }

            var1 = var1 + ((EntityPlayerMP)this.playerEntityList.get(var2)).username;
        }

        return var1;
    }

    /**
     * Returns an array of the usernames of all the connected players.
     */
    public String[] getAllUsernames()
    {
        String[] var1 = new String[this.playerEntityList.size()];

        for (int var2 = 0; var2 < this.playerEntityList.size(); ++var2)
        {
            var1[var2] = ((EntityPlayerMP)this.playerEntityList.get(var2)).username;
        }

        return var1;
    }

    public BanList getBannedPlayers()
    {
        return this.bannedPlayers;
    }

    public BanList getBannedIPs()
    {
        return this.bannedIPs;
    }

    /**
     * This adds a username to the ops list, then saves the op list
     */
    public void addOp(String par1Str)
    {
        this.ops.add(par1Str.toLowerCase());
        // CraftBukkit start
        Player player = mcServer.server.getPlayer(par1Str);

        if (player != null)
        {
            player.recalculatePermissions();
        }

        // CraftBukkit end
    }

    /**
     * This removes a username from the ops list, then saves the op list
     */
    public void removeOp(String par1Str)
    {
        this.ops.remove(par1Str.toLowerCase());
        // CraftBukkit start
        Player player = mcServer.server.getPlayer(par1Str);

        if (player != null)
        {
            player.recalculatePermissions();
        }

        // CraftBukkit end
    }

    /**
     * Determine if the player is allowed to connect based on current server settings.
     */
    public boolean isAllowedToLogin(String par1Str)
    {
        par1Str = par1Str.trim().toLowerCase();
        return !this.whiteListEnforced || this.ops.contains(par1Str) || this.whiteListedPlayers.contains(par1Str);
    }

    /**
     * Returns true if the specific player is allowed to use commands.
     */
    public boolean areCommandsAllowed(String par1Str)
    {
        // CraftBukkit
        return this.ops.contains(par1Str.trim().toLowerCase()) || this.mcServer.isSinglePlayer() && this.mcServer.worlds.get(0).getWorldInfo().areCommandsAllowed() && this.mcServer.getServerOwner().equalsIgnoreCase(par1Str) || this.commandsAllowedForAll;
    }

    public EntityPlayerMP getPlayerForUsername(String par1Str)
    {
        Iterator var2 = this.playerEntityList.iterator();
        EntityPlayerMP var3;

        do
        {
            if (!var2.hasNext())
            {
                return null;
            }

            var3 = (EntityPlayerMP)var2.next();
        }
        while (!var3.username.equalsIgnoreCase(par1Str));

        return var3;
    }

    /**
     * Find all players in a specified range and narrowing down by other parameters
     */
    public List findPlayers(ChunkCoordinates par1ChunkCoordinates, int par2, int par3, int par4, int par5, int par6, int par7)
    {
        if (this.playerEntityList.isEmpty())
        {
            return null;
        }
        else
        {
            Object var8 = new ArrayList();
            boolean var9 = par4 < 0;
            int var10 = par2 * par2;
            int var11 = par3 * par3;
            par4 = MathHelper.abs_int(par4);

            for (int var12 = 0; var12 < this.playerEntityList.size(); ++var12)
            {
                EntityPlayerMP var13 = (EntityPlayerMP)this.playerEntityList.get(var12);

                if (par1ChunkCoordinates != null && (par2 > 0 || par3 > 0))
                {
                    float var14 = par1ChunkCoordinates.getDistanceSquaredToChunkCoordinates(var13.getPlayerCoordinates());

                    if (par2 > 0 && var14 < (float)var10 || par3 > 0 && var14 > (float)var11)
                    {
                        continue;
                    }
                }

                if ((par5 == EnumGameType.NOT_SET.getID() || par5 == var13.theItemInWorldManager.getGameType().getID()) && (par6 <= 0 || var13.experienceLevel >= par6) && var13.experienceLevel <= par7)
                {
                    ((List)var8).add(var13);
                }
            }

            if (par1ChunkCoordinates != null)
            {
                Collections.sort((List)var8, new PlayerPositionComparator(par1ChunkCoordinates));
            }

            if (var9)
            {
                Collections.reverse((List)var8);
            }

            if (par4 > 0)
            {
                var8 = ((List)var8).subList(0, Math.min(par4, ((List)var8).size()));
            }

            return (List)var8;
        }
    }

    /**
     * params: x,y,z,d,dimension. The packet is sent to all players within d distance of x,y,z (d^2<x^2+y^2+z^2)
     */
    public void sendToAllNear(double par1, double par3, double par5, double par7, int par9, Packet par10Packet)
    {
        this.sendToAllNearExcept((EntityPlayer)null, par1, par3, par5, par7, par9, par10Packet);
    }

    /**
     * params: srcPlayer,x,y,z,d,dimension. The packet is not sent to the srcPlayer, but all other players where
     * dx*dx+dy*dy+dz*dz<d*d
     */
    public void sendToAllNearExcept(EntityPlayer par1EntityPlayer, double par2, double par4, double par6, double par8, int par10, Packet par11Packet)
    {
        for (int var12 = 0; var12 < this.playerEntityList.size(); ++var12)
        {
            EntityPlayerMP var13 = (EntityPlayerMP)this.playerEntityList.get(var12);

            // CraftBukkit start - Test if player receiving packet can see the source of the packet
            if (par1EntityPlayer != null && par1EntityPlayer instanceof EntityPlayerMP && !var13.getBukkitEntity().canSee(((EntityPlayerMP) par1EntityPlayer).getBukkitEntity()))
            {
                continue;
            }

            // CraftBukkit end
            if (var13 != par1EntityPlayer && var13.dimension == par10)
            {
                double var14 = par2 - var13.posX;
                double var16 = par4 - var13.posY;
                double var18 = par6 - var13.posZ;

                if (var14 * var14 + var16 * var16 + var18 * var18 < par8 * par8)
                {
                    var13.playerNetServerHandler.sendPacketToPlayer(par11Packet);
                }
            }
        }
    }

    /**
     * Saves all of the players' current states.
     */
    public void saveAllPlayerData()
    {
        for (int var1 = 0; var1 < this.playerEntityList.size(); ++var1)
        {
            this.writePlayerData((EntityPlayerMP)this.playerEntityList.get(var1));
        }
    }

    /**
     * Add the specified player to the white list.
     */
    public void addToWhiteList(String par1Str)
    {
        this.whiteListedPlayers.add(par1Str);
    }

    /**
     * Remove the specified player from the whitelist.
     */
    public void removeFromWhitelist(String par1Str)
    {
        this.whiteListedPlayers.remove(par1Str);
    }

    /**
     * Returns the whitelisted players.
     */
    public Set getWhiteListedPlayers()
    {
        return this.whiteListedPlayers;
    }

    public Set getOps()
    {
        return this.ops;
    }

    /**
     * Either does nothing, or calls readWhiteList.
     */
    public void loadWhiteList() {}

    /**
     * Updates the time and weather for the given player to those of the given world
     */
    public void updateTimeAndWeatherForPlayer(EntityPlayerMP par1EntityPlayerMP, WorldServer par2WorldServer)
    {
        par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet4UpdateTime(par2WorldServer.getTotalWorldTime(), par2WorldServer.getWorldTime()));

        if (par2WorldServer.isRaining())
        {
            par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(1, 0));
        }
    }

    /**
     * sends the players inventory to himself
     */
    public void syncPlayerInventory(EntityPlayerMP par1EntityPlayerMP)
    {
        par1EntityPlayerMP.sendContainerToPlayer(par1EntityPlayerMP.inventoryContainer);
        par1EntityPlayerMP.setPlayerHealthUpdated();
        par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet16BlockItemSwitch(par1EntityPlayerMP.inventory.currentItem));
    }

    /**
     * Returns the number of players currently on the server.
     */
    public int getCurrentPlayerCount()
    {
        return this.playerEntityList.size();
    }

    /**
     * Returns the maximum number of players allowed on the server.
     */
    public int getMaxPlayers()
    {
        return this.maxPlayers;
    }

    /**
     * Returns an array of usernames for which player.dat exists for.
     */
    public String[] getAvailablePlayerDat()
    {
        return this.mcServer.worlds.get(0).getSaveHandler().getSaveHandler().getAvailablePlayerDat(); // CraftBukkit
    }

    public boolean isWhiteListEnabled()
    {
        return this.whiteListEnforced;
    }

    public void setWhiteListEnabled(boolean par1)
    {
        this.whiteListEnforced = par1;
    }

    public List getPlayerList(String par1Str)
    {
        ArrayList var2 = new ArrayList();
        Iterator var3 = this.playerEntityList.iterator();

        while (var3.hasNext())
        {
            EntityPlayerMP var4 = (EntityPlayerMP)var3.next();

            if (var4.func_71114_r().equals(par1Str))
            {
                var2.add(var4);
            }
        }

        return var2;
    }

    /**
     * Gets the View Distance.
     */
    public int getViewDistance()
    {
        return this.viewDistance;
    }

    public MinecraftServer getServerInstance()
    {
        return this.mcServer;
    }

    /**
     * gets the tags created in the last writePlayerData call
     */
    public NBTTagCompound getTagsFromLastWrite()
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void setGameType(EnumGameType par1EnumGameType)
    {
        this.gameType = par1EnumGameType;
    }

    private void func_72381_a(EntityPlayerMP par1EntityPlayerMP, EntityPlayerMP par2EntityPlayerMP, World par3World)
    {
        if (par2EntityPlayerMP != null)
        {
            par1EntityPlayerMP.theItemInWorldManager.setGameType(par2EntityPlayerMP.theItemInWorldManager.getGameType());
        }
        else if (this.gameType != null)
        {
            par1EntityPlayerMP.theItemInWorldManager.setGameType(this.gameType);
        }

        par1EntityPlayerMP.theItemInWorldManager.initializeGameType(par3World.getWorldInfo().getGameType());
    }

    @SideOnly(Side.CLIENT)

    /**
     * Sets whether all players are allowed to use commands (cheats) on the server.
     */
    public void setCommandsAllowedForAll(boolean par1)
    {
        this.commandsAllowedForAll = par1;
    }

    /**
     * Kicks everyone with "Server closed" as reason.
     */
    public void removeAllPlayers()
    {
        while (!this.playerEntityList.isEmpty())
        {
            // Spigot start
            EntityPlayerMP p = (EntityPlayerMP) this.playerEntityList.get(0);
            p.playerNetServerHandler.kickPlayerFromServer(this.mcServer.server.getShutdownMessage());

            if ((!this.playerEntityList.isEmpty()) && (this.playerEntityList.get(0) == p))
            {
                this.playerEntityList.remove(0); // Prevent shutdown hang if already disconnected
            }

            // Spigot end
        }
    }

    /**
     * Sends the given string to every player as chat message.
     */
    public void sendChatMsg(String par1Str)
    {
        this.mcServer.logInfo(par1Str);
        this.sendPacketToAllPlayers(new Packet3Chat(par1Str));
    }
}
