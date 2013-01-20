package org.bukkit.craftbukkit.entity;

import guava10.com.google.common.collect.ImmutableSet;
import guava10.com.google.common.collect.MapMaker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.commons.lang.Validate;
import org.apache.commons.lang.NotImplementedException;

import org.bukkit.*;
import org.bukkit.Achievement;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ManuallyAbandonedConversationCanceller;
import org.bukkit.craftbukkit.conversations.ConversationTracker;
import org.bukkit.craftbukkit.CraftEffect;
import org.bukkit.craftbukkit.CraftOfflinePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.craftbukkit.map.RenderData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.StandardMessenger;

@DelegateDeserialization(CraftOfflinePlayer.class)
public class CraftPlayer extends CraftHumanEntity implements Player {
    private long firstPlayed = 0;
    private long lastPlayed = 0;
    private boolean hasPlayedBefore = false;
    private final ConversationTracker conversationTracker = new ConversationTracker();
    private final Set<String> channels = new HashSet<String>();
    private final Map<String, Player> hiddenPlayers = new MapMaker().softValues().makeMap();
    private int hash = 0;

    public CraftPlayer(CraftServer server, net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entity) {
        super(server, entity);

        firstPlayed = System.currentTimeMillis();
    }

    @Override
    public boolean isOp() {
        return server.getHandle().areCommandsAllowed/*was:isOp*/(getName());
    }

    @Override
    public void setOp(boolean value) {
        if (value == isOp()) return;

        if (value) {
            server.getHandle().addOp/*was:addOp*/(getName());
        } else {
            server.getHandle().removeOp/*was:removeOp*/(getName());
        }

        perm.recalculatePermissions();
    }

    public boolean isOnline() {
        for (Object obj : server.getHandle().playerEntityList/*was:players*/) {
            net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ player = (net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) obj;
            if (player.username/*was:name*/.equalsIgnoreCase(getName())) {
                return true;
            }
        }
        return false;
    }

    public InetSocketAddress getAddress() {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return null;

        SocketAddress addr = getHandle().playerNetServerHandler/*was:playerConnection*/.netManager/*was:networkManager*/.getSocketAddress/*was:getSocketAddress*/();
        if (addr instanceof InetSocketAddress) {
            return (InetSocketAddress) addr;
        } else {
            return null;
        }
    }

    @Override
    public double getEyeHeight() {
        return getEyeHeight(false);
    }

    @Override
    public double getEyeHeight(boolean ignoreSneaking) {
        if (ignoreSneaking) {
            return 1.62D;
        } else {
            if (isSneaking()) {
                return 1.54D;
            } else {
                return 1.62D;
            }
        }
    }

    public void sendRawMessage(String message) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet3Chat/*was:Packet3Chat*/(message));
    }

    public void sendMessage(String message) {
        if (!conversationTracker.isConversingModaly()) {
            this.sendRawMessage(message);
        }
    }

    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    public String getDisplayName() {
        return getHandle().displayName;
    }

    public void setDisplayName(final String name) {
        getHandle().displayName = name;
    }

    public String getPlayerListName() {
        return getHandle().listName;
    }

    public void setPlayerListName(String name) {
        String oldName = getHandle().listName;

        if (name == null) {
            name = getName();
        }

        if (oldName.equals(name)) {
            return;
        }

        if (name.length() > 16) {
            throw new IllegalArgumentException("Player list names can only be a maximum of 16 characters long");
        }

        // Collisions will make for invisible people
        for (int i = 0; i < server.getHandle().playerEntityList/*was:players*/.size(); ++i) {
            if (((net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) server.getHandle().playerEntityList/*was:players*/.get(i)).listName.equals(name)) {
                throw new IllegalArgumentException(name + " is already assigned as a player list name for someone");
            }
        }

        getHandle().listName = name;

        // Change the name on the client side
        net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/ oldpacket = new net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/(oldName, false, 9999);
        net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/ packet = new net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/(name, true, getHandle().ping/*was:ping*/);
        for (int i = 0; i < server.getHandle().playerEntityList/*was:players*/.size(); ++i) {
            net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entityplayer = (net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) server.getHandle().playerEntityList/*was:players*/.get(i);
            if (entityplayer.playerNetServerHandler/*was:playerConnection*/ == null) continue;

            if (entityplayer.getBukkitEntity().canSee(this)) {
                entityplayer.playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(oldpacket);
                entityplayer.playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OfflinePlayer)) {
            return false;
        }
        OfflinePlayer other = (OfflinePlayer) obj;
        if ((this.getName() == null) || (other.getName() == null)) {
            return false;
        }

        boolean nameEquals = this.getName().equalsIgnoreCase(other.getName());
        boolean idEquals = true;

        if (other instanceof CraftPlayer) {
            idEquals = this.getEntityId() == ((CraftPlayer) other).getEntityId();
        }

        return nameEquals && idEquals;
    }

    public void kickPlayer(String message) {
        // Spigot start
        kickPlayer(message, false);
    }

    public void kickPlayer(String message, boolean async){
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;
        if (!async && !Bukkit.isPrimaryThread()) throw new IllegalStateException("Cannot kick player from asynchronous thread!"); // Spigot

        getHandle().playerNetServerHandler/*was:playerConnection*/.kickPlayerFromServer/*was:disconnect*/(message == null ? "" : message);
    }
    // Spigot end

    public void setCompassTarget(Location loc) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        // Do not directly assign here, from the packethandler we'll assign it.
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet6SpawnPosition/*was:Packet6SpawnPosition*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    public Location getCompassTarget() {
        return getHandle().compassTarget;
    }

    public void chat(String msg) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        getHandle().playerNetServerHandler/*was:playerConnection*/.chat(msg, false);
    }

    public boolean performCommand(String command) {
        return server.dispatchCommand(this, command);
    }

    public void playNote(Location loc, byte instrument, byte note) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        int id = getHandle().worldObj/*was:world*/.getBlockId/*was:getTypeId*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet54PlayNoteBlock/*was:Packet54PlayNoteBlock*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), id, instrument, note));
    }

    public void playNote(Location loc, Instrument instrument, Note note) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        int id = getHandle().worldObj/*was:world*/.getBlockId/*was:getTypeId*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet54PlayNoteBlock/*was:Packet54PlayNoteBlock*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), id, instrument.getType(), note.getId()));
    }

    public void playSound(Location loc, Sound sound, float volume, float pitch) {
        if (loc == null || sound == null || getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        double x = loc.getBlockX() + 0.5;
        double y = loc.getBlockY() + 0.5;
        double z = loc.getBlockZ() + 0.5;

        net.minecraft.network.packet.Packet62LevelSound/*was:Packet62NamedSoundEffect*/ packet = new net.minecraft.network.packet.Packet62LevelSound/*was:Packet62NamedSoundEffect*/(CraftSound.getSound(sound), x, y, z, volume, pitch);
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
    }

    public void playEffect(Location loc, Effect effect, int data) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        int packetData = effect.getId();
        net.minecraft.network.packet.Packet61DoorChange/*was:Packet61WorldEvent*/ packet = new net.minecraft.network.packet.Packet61DoorChange/*was:Packet61WorldEvent*/(packetData, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), data, false);
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
    }

    public <T> void playEffect(Location loc, Effect effect, T data) {
        if (data != null) {
            Validate.isTrue(data.getClass().equals(effect.getData()), "Wrong kind of data for this effect!");
        } else {
            Validate.isTrue(effect.getData() == null, "Wrong kind of data for this effect!");
        }

        int datavalue = data == null ? 0 : CraftEffect.getDataValue(effect, data);
        playEffect(loc, effect, datavalue);
    }

    public void sendBlockChange(Location loc, Material material, byte data) {
        sendBlockChange(loc, material.getId(), data);
    }

    public void sendBlockChange(Location loc, int material, byte data) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        net.minecraft.network.packet.Packet53BlockChange/*was:Packet53BlockChange*/ packet = new net.minecraft.network.packet.Packet53BlockChange/*was:Packet53BlockChange*/(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), ((CraftWorld) loc.getWorld()).getHandle());

        packet.type/*was:material*/ = material;
        packet.metadata/*was:data*/ = data;
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
    }

    public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return false;

        /*
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        int cx = x >> 4;
        int cz = z >> 4;

        if (sx <= 0 || sy <= 0 || sz <= 0) {
            return false;
        }

        if ((x + sx - 1) >> 4 != cx || (z + sz - 1) >> 4 != cz || y < 0 || y + sy > 128) {
            return false;
        }

        if (data.length != (sx * sy * sz * 5) / 2) {
            return false;
        }

        Packet51MapChunk packet = new Packet51MapChunk(x, y, z, sx, sy, sz, data);

        getHandle().playerConnection.sendPacket(packet);

        return true;
        */

        throw new NotImplementedException("Chunk changes do not yet work"); // TODO: Chunk changes.
    }

    public void sendMap(MapView map) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        RenderData data = ((CraftMapView) map).render(this);
        for (int x = 0; x < 128; ++x) {
            byte[] bytes = new byte[131];
            bytes[1] = (byte) x;
            for (int y = 0; y < 128; ++y) {
                bytes[y + 3] = data.buffer[y * 128 + x];
            }
            net.minecraft.network.packet.Packet131MapData/*was:Packet131ItemData*/ packet = new net.minecraft.network.packet.Packet131MapData/*was:Packet131ItemData*/((short) Material.MAP.getId(), map.getId(), bytes);
            getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
        }
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entity = getHandle();

        if (getHealth() == 0 || entity.isDead/*was:dead*/) {
            return false;
        }

        if (entity.playerNetServerHandler/*was:playerConnection*/ == null || entity.playerNetServerHandler/*was:playerConnection*/.connectionClosed/*was:disconnected*/) {
            return false;
        }

        if (entity.ridingEntity/*was:vehicle*/ != null || entity.riddenByEntity/*was:passenger*/ != null) {
            return false;
        }

        // From = Players current Location
        Location from = this.getLocation();
        // To = Players new Location if Teleport is Successful
        Location to = location;
        // Create & Call the Teleport Event.
        PlayerTeleportEvent event = new PlayerTeleportEvent((Player) this, from, to, cause);
        server.getPluginManager().callEvent(event);

        // Return False to inform the Plugin that the Teleport was unsuccessful/cancelled.
        if (event.isCancelled()) {
            return false;
        }

        // Update the From Location
        from = event.getFrom();
        // Grab the new To Location dependent on whether the event was cancelled.
        to = event.getTo();
        // Grab the To and From World Handles.
        net.minecraft.world.WorldServer/*was:WorldServer*/ fromWorld = ((CraftWorld) from.getWorld()).getHandle();
        net.minecraft.world.WorldServer/*was:WorldServer*/ toWorld = ((CraftWorld) to.getWorld()).getHandle();

        // Check if the fromWorld and toWorld are the same.
        if (fromWorld == toWorld) {
            entity.playerNetServerHandler/*was:playerConnection*/.teleport(to);
        } else {
            // Close any foreign inventory
            if (getHandle().openContainer/*was:activeContainer*/ != getHandle().inventoryContainer/*was:defaultContainer*/){
                getHandle().closeScreen/*was:closeInventory*/();
            }
            server.getHandle().respawnPlayer(entity, toWorld.dimension, true, to);
        }
        return true;
    }

    public void setSneaking(boolean sneak) {
        getHandle().setSneaking/*was:setSneaking*/(sneak);
    }

    public boolean isSneaking() {
        return getHandle().isSneaking/*was:isSneaking*/();
    }

    public boolean isSprinting() {
        return getHandle().isSprinting/*was:isSprinting*/();
    }

    public void setSprinting(boolean sprinting) {
        getHandle().setSprinting/*was:setSprinting*/(sprinting);
    }

    public void loadData() {
        server.getHandle().playerNBTManagerObj/*was:playerFileData*/.readPlayerData/*was:load*/(getHandle());
    }

    public void saveData() {
        server.getHandle().playerNBTManagerObj/*was:playerFileData*/.writePlayerData/*was:save*/(getHandle());
    }

    public void updateInventory() {
        getHandle().sendContainerToPlayer/*was:updateInventory*/(getHandle().openContainer/*was:activeContainer*/);
    }

    public void setSleepingIgnored(boolean isSleeping) {
        getHandle().fauxSleeping = isSleeping;
        ((CraftWorld) getWorld()).getHandle().checkSleepStatus();
    }

    public boolean isSleepingIgnored() {
        return getHandle().fauxSleeping;
    }

    public void awardAchievement(Achievement achievement) {
        sendStatistic(achievement.getId(), 1);
    }

    public void incrementStatistic(Statistic statistic) {
        incrementStatistic(statistic, 1);
    }

    public void incrementStatistic(Statistic statistic, int amount) {
        sendStatistic(statistic.getId(), amount);
    }

    public void incrementStatistic(Statistic statistic, Material material) {
        incrementStatistic(statistic, material, 1);
    }

    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        if (!statistic.isSubstatistic()) {
            throw new IllegalArgumentException("Given statistic is not a substatistic");
        }
        if (statistic.isBlock() != material.isBlock()) {
            throw new IllegalArgumentException("Given material is not valid for this substatistic");
        }

        int mat = material.getId();

        if (!material.isBlock()) {
            mat -= 255;
        }

        sendStatistic(statistic.getId() + mat, amount);
    }

    private void sendStatistic(int id, int amount) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        while (amount > Byte.MAX_VALUE) {
            sendStatistic(id, Byte.MAX_VALUE);
            amount -= Byte.MAX_VALUE;
        }

        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet200Statistic/*was:Packet200Statistic*/(id, amount));
    }

    public void setPlayerTime(long time, boolean relative) {
        getHandle().timeOffset = time;
        getHandle().relativeTime = relative;
    }

    public long getPlayerTimeOffset() {
        return getHandle().timeOffset;
    }

    public long getPlayerTime() {
        return getHandle().getPlayerTime();
    }

    public boolean isPlayerTimeRelative() {
        return getHandle().relativeTime;
    }

    public void resetPlayerTime() {
        setPlayerTime(0, true);
    }

    public boolean isBanned() {
        return server.getHandle().getBannedPlayers/*was:getNameBans*/().isBanned/*was:isBanned*/(getName().toLowerCase());
    }

    public void setBanned(boolean value) {
        if (value) {
            net.minecraft.server.management.BanEntry/*was:BanEntry*/ entry = new net.minecraft.server.management.BanEntry/*was:BanEntry*/(getName().toLowerCase());
            server.getHandle().getBannedPlayers/*was:getNameBans*/().put/*was:add*/(entry);
        } else {
            server.getHandle().getBannedPlayers/*was:getNameBans*/().remove/*was:remove*/(getName().toLowerCase());
        }

        server.getHandle().getBannedPlayers/*was:getNameBans*/().saveToFileWithHeader/*was:save*/();
    }

    public boolean isWhitelisted() {
        return server.getHandle().getWhiteListedPlayers/*was:getWhitelisted*/().contains(getName().toLowerCase());
    }

    public void setWhitelisted(boolean value) {
        if (value) {
            server.getHandle().addToWhiteList/*was:addWhitelist*/(getName().toLowerCase());
        } else {
            server.getHandle().removeFromWhitelist/*was:removeWhitelist*/(getName().toLowerCase());
        }
    }

    @Override
    public void setGameMode(GameMode mode) {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        if (mode != getGameMode()) {
            PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this, mode);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            getHandle().theItemInWorldManager/*was:playerInteractManager*/.setGameType/*was:setGameMode*/(net.minecraft.world.EnumGameType/*was:EnumGamemode*/.getByID/*was:a*/(mode.getValue()));
            getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet70GameEvent/*was:Packet70Bed*/(3, mode.getValue()));
        }
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.getByValue(getHandle().theItemInWorldManager/*was:playerInteractManager*/.getGameType/*was:getGameMode*/().getID/*was:a*/());
    }

    public void giveExp(int exp) {
        getHandle().addExperience/*was:giveExp*/(exp);
    }

    public void giveExpLevels(int levels) {
        getHandle().addExperienceLevel/*was:levelDown*/(levels);
    }

    public float getExp() {
        return getHandle().experience/*was:exp*/;
    }

    public void setExp(float exp) {
        getHandle().experience/*was:exp*/ = exp;
        getHandle().lastExperience/*was:lastSentExp*/ = -1;
    }

    public int getLevel() {
        return getHandle().experienceLevel/*was:expLevel*/;
    }

    public void setLevel(int level) {
        getHandle().experienceLevel/*was:expLevel*/ = level;
        getHandle().lastExperience/*was:lastSentExp*/ = -1;
    }

    public int getTotalExperience() {
        return getHandle().experienceTotal/*was:expTotal*/;
    }

    public void setTotalExperience(int exp) {
        getHandle().experienceTotal/*was:expTotal*/ = exp;
    }

    public float getExhaustion() {
        return getHandle().getFoodStats/*was:getFoodData*/().foodExhaustionLevel/*was:exhaustionLevel*/;
    }

    public void setExhaustion(float value) {
        getHandle().getFoodStats/*was:getFoodData*/().foodExhaustionLevel/*was:exhaustionLevel*/ = value;
    }

    public float getSaturation() {
        return getHandle().getFoodStats/*was:getFoodData*/().foodSaturationLevel/*was:saturationLevel*/;
    }

    public void setSaturation(float value) {
        getHandle().getFoodStats/*was:getFoodData*/().foodSaturationLevel/*was:saturationLevel*/ = value;
    }

    public int getFoodLevel() {
        return getHandle().getFoodStats/*was:getFoodData*/().foodLevel/*was:foodLevel*/;
    }

    public void setFoodLevel(int value) {
        getHandle().getFoodStats/*was:getFoodData*/().foodLevel/*was:foodLevel*/ = value;
    }

    public Location getBedSpawnLocation() {
        World world = getServer().getWorld(getHandle().spawnWorld);
        if ((world != null) && (getHandle().getBedLocation/*was:getBed*/() != null)) {
            return new Location(world, getHandle().getBedLocation/*was:getBed*/().posX/*was:x*/, getHandle().getBedLocation/*was:getBed*/().posY/*was:y*/, getHandle().getBedLocation/*was:getBed*/().posZ/*was:z*/);
        }
        return null;
    }

    public void setBedSpawnLocation(Location location) {
        setBedSpawnLocation(location, false);
    }

    public void setBedSpawnLocation(Location location, boolean override) {
        if (location == null) {
            getHandle().setSpawnChunk/*was:setRespawnPosition*/(null, override);
        } else {
            getHandle().setSpawnChunk/*was:setRespawnPosition*/(new net.minecraft.util.ChunkCoordinates/*was:ChunkCoordinates*/(location.getBlockX(), location.getBlockY(), location.getBlockZ()), override);
            getHandle().spawnWorld = location.getWorld().getName();
        }
    }

    public void hidePlayer(Player player) {
        Validate.notNull(player, "hidden player cannot be null");
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;
        if (equals(player)) return;
        if (hiddenPlayers.containsKey(player.getName())) return;
        hiddenPlayers.put(player.getName(), player);

        //remove this player from the hidden player's EntityTrackerEntry
        net.minecraft.entity.EntityTracker/*was:EntityTracker*/ tracker = ((net.minecraft.world.WorldServer/*was:WorldServer*/) entity.worldObj/*was:world*/).theEntityTracker/*was:tracker*/;
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ other = ((CraftPlayer) player).getHandle();
        net.minecraft.entity.EntityTrackerEntry/*was:EntityTrackerEntry*/ entry = (net.minecraft.entity.EntityTrackerEntry/*was:EntityTrackerEntry*/) tracker.trackedEntityIDs/*was:trackedEntities*/.lookup/*was:get*/(other.entityId/*was:id*/);
        if (entry != null) {
            entry.removePlayerFromTracker/*was:clear*/(getHandle());
        }

        //remove the hidden player from this player user list
        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/(player.getPlayerListName(), false, 9999));
    }

    public void showPlayer(Player player) {
        Validate.notNull(player, "shown player cannot be null");
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;
        if (equals(player)) return;
        if (!hiddenPlayers.containsKey(player.getName())) return;
        hiddenPlayers.remove(player.getName());

        net.minecraft.entity.EntityTracker/*was:EntityTracker*/ tracker = ((net.minecraft.world.WorldServer/*was:WorldServer*/) entity.worldObj/*was:world*/).theEntityTracker/*was:tracker*/;
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ other = ((CraftPlayer) player).getHandle();
        net.minecraft.entity.EntityTrackerEntry/*was:EntityTrackerEntry*/ entry = (net.minecraft.entity.EntityTrackerEntry/*was:EntityTrackerEntry*/) tracker.trackedEntityIDs/*was:trackedEntities*/.lookup/*was:get*/(other.entityId/*was:id*/);
        if (entry != null && !entry.trackedPlayers/*was:trackedPlayers*/.contains(getHandle())) {
            entry.tryStartWachingThis/*was:updatePlayer*/(getHandle());
        }

        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet201PlayerInfo/*was:Packet201PlayerInfo*/(player.getPlayerListName(), true, getHandle().ping/*was:ping*/));
    }

    public boolean canSee(Player player) {
        return !hiddenPlayers.containsKey(player.getName());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("name", getName());

        return result;
    }

    public Player getPlayer() {
        return this;
    }

    @Override
    public net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ getHandle() {
        return (net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/) entity;
    }

    public void setHandle(final net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ entity) {
        super.setHandle(entity);
    }

    @Override
    public String toString() {
        return "CraftPlayer{" + "name=" + getName() + '}';
    }

    @Override
    public int hashCode() {
        if (hash == 0 || hash == 485) {
            hash = 97 * 5 + (this.getName() != null ? this.getName().toLowerCase().hashCode() : 0);
        }
        return hash;
    }

    public long getFirstPlayed() {
        return firstPlayed;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public boolean hasPlayedBefore() {
        return hasPlayedBefore;
    }

    public void setFirstPlayed(long firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public void readExtraData(net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ nbttagcompound) {
        hasPlayedBefore = true;
        if (nbttagcompound.hasKey/*was:hasKey*/("bukkit")) {
            net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ data = nbttagcompound.getCompoundTag/*was:getCompound*/("bukkit");

            if (data.hasKey/*was:hasKey*/("firstPlayed")) {
                firstPlayed = data.getLong/*was:getLong*/("firstPlayed");
                lastPlayed = data.getLong/*was:getLong*/("lastPlayed");
            }

            if (data.hasKey/*was:hasKey*/("newExp")) {
                net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ handle = getHandle();
                handle.newExp = data.getInteger/*was:getInt*/("newExp");
                handle.newTotalExp = data.getInteger/*was:getInt*/("newTotalExp");
                handle.newLevel = data.getInteger/*was:getInt*/("newLevel");
                handle.expToDrop = data.getInteger/*was:getInt*/("expToDrop");
                handle.keepLevel = data.getBoolean/*was:getBoolean*/("keepLevel");
            }
        }
    }

    public void setExtraData(net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ nbttagcompound) {
        if (!nbttagcompound.hasKey/*was:hasKey*/("bukkit")) {
            nbttagcompound.setCompoundTag/*was:setCompound*/("bukkit", new net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/());
        }

        net.minecraft.nbt.NBTTagCompound/*was:NBTTagCompound*/ data = nbttagcompound.getCompoundTag/*was:getCompound*/("bukkit");
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ handle = getHandle();
        data.setInteger/*was:setInt*/("newExp", handle.newExp);
        data.setInteger/*was:setInt*/("newTotalExp", handle.newTotalExp);
        data.setInteger/*was:setInt*/("newLevel", handle.newLevel);
        data.setInteger/*was:setInt*/("expToDrop", handle.expToDrop);
        data.setBoolean/*was:setBoolean*/("keepLevel", handle.keepLevel);
        data.setLong/*was:setLong*/("firstPlayed", getFirstPlayed());
        data.setLong/*was:setLong*/("lastPlayed", System.currentTimeMillis());
    }

    public boolean beginConversation(Conversation conversation) {
        return conversationTracker.beginConversation(conversation);
    }

    public void abandonConversation(Conversation conversation) {
        conversationTracker.abandonConversation(conversation, new ConversationAbandonedEvent(conversation, new ManuallyAbandonedConversationCanceller()));
    }

    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        conversationTracker.abandonConversation(conversation, details);
    }

    public void acceptConversationInput(String input) {
        conversationTracker.acceptConversationInput(input);
    }

    public boolean isConversing() {
        return conversationTracker.isConversing();
    }

    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(server.getMessenger(), source, channel, message);
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;

        if (channels.contains(channel)) {
            net.minecraft.network.packet.Packet250CustomPayload/*was:Packet250CustomPayload*/ packet = new net.minecraft.network.packet.Packet250CustomPayload/*was:Packet250CustomPayload*/();
            packet.channel/*was:tag*/ = channel;
            packet.length/*was:length*/ = message.length;
            packet.data/*was:data*/ = message;
            getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
        }
    }

    public void setTexturePack(String url) {
        Validate.notNull(url, "Texture pack URL cannot be null");

        byte[] message = (url + "\0" + "16").getBytes();
        Validate.isTrue(message.length <= Messenger.MAX_MESSAGE_SIZE, "Texture pack URL is too long");

        getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(new net.minecraft.network.packet.Packet250CustomPayload/*was:Packet250CustomPayload*/("MC|TPack", message));
    }

    public void addChannel(String channel) {
        if (channels.add(channel)) {
            server.getPluginManager().callEvent(new PlayerRegisterChannelEvent(this, channel));
        }
    }

    public void removeChannel(String channel) {
        if (channels.remove(channel)) {
            server.getPluginManager().callEvent(new PlayerUnregisterChannelEvent(this, channel));
        }
    }

    public Set<String> getListeningPluginChannels() {
        return ImmutableSet.copyOf(channels);
    }

    public void sendSupportedChannels() {
        if (getHandle().playerNetServerHandler/*was:playerConnection*/ == null) return;
        Set<String> listening = server.getMessenger().getIncomingChannels();

        if (!listening.isEmpty()) {
            net.minecraft.network.packet.Packet250CustomPayload/*was:Packet250CustomPayload*/ packet = new net.minecraft.network.packet.Packet250CustomPayload/*was:Packet250CustomPayload*/();

            packet.channel/*was:tag*/ = "REGISTER";
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            for (String channel : listening) {
                try {
                    stream.write(channel.getBytes("UTF8"));
                    stream.write((byte) 0);
                } catch (IOException ex) {
                    Logger.getLogger(CraftPlayer.class.getName()).log(Level.SEVERE, "Could not send Plugin Channel REGISTER to " + getName(), ex);
                }
            }

            packet.data/*was:data*/ = stream.toByteArray();
            packet.length/*was:length*/ = packet.data/*was:data*/.length;

            getHandle().playerNetServerHandler/*was:playerConnection*/.sendPacketToPlayer/*was:sendPacket*/(packet);
        }
    }

    public EntityType getType() {
        return EntityType.PLAYER;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        server.getPlayerMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        server.getPlayerMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    @Override
    public boolean setWindowProperty(Property prop, int value) {
        net.minecraft.inventory.Container/*was:Container*/ container = getHandle().openContainer/*was:activeContainer*/;
        if (container.getBukkitView().getType() != prop.getType()) {
            return false;
        }
        getHandle().sendProgressBarUpdate/*was:setContainerData*/(container, prop.getId(), value);
        return true;
    }

    public void disconnect(String reason) {
        conversationTracker.abandonAllConversations();
        perm.clearPermissions();
    }

    public boolean isFlying() {
        return getHandle().capabilities/*was:abilities*/.isFlying/*was:isFlying*/;
    }

    public void setFlying(boolean value) {
        if (!getAllowFlight() && value) {
            throw new IllegalArgumentException("Cannot make player fly if getAllowFlight() is false");
        }

        getHandle().capabilities/*was:abilities*/.isFlying/*was:isFlying*/ = value;
        getHandle().sendPlayerAbilities/*was:updateAbilities*/();
    }

    public boolean getAllowFlight() {
        return getHandle().capabilities/*was:abilities*/.allowFlying/*was:canFly*/;
    }

    public void setAllowFlight(boolean value) {
        if (isFlying() && !value) {
            getHandle().capabilities/*was:abilities*/.isFlying/*was:isFlying*/ = false;
        }

        getHandle().capabilities/*was:abilities*/.allowFlying/*was:canFly*/ = value;
        getHandle().sendPlayerAbilities/*was:updateAbilities*/();
    }

    @Override
    public int getNoDamageTicks() {
        if (getHandle().initialInvulnerability/*was:invulnerableTicks*/ > 0) {
            return Math.max(getHandle().initialInvulnerability/*was:invulnerableTicks*/, getHandle().hurtResistantTime/*was:noDamageTicks*/);
        } else {
            return getHandle().hurtResistantTime/*was:noDamageTicks*/;
        }
    }

    public void setFlySpeed(float value) {
        validateSpeed(value);
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ player = getHandle();
        player.capabilities/*was:abilities*/.flySpeed/*was:flySpeed*/ = value / 2f;
        player.sendPlayerAbilities/*was:updateAbilities*/();

    }

    public void setWalkSpeed(float value) {
        validateSpeed(value);
        net.minecraft.entity.player.EntityPlayerMP/*was:EntityPlayer*/ player = getHandle();
        player.capabilities/*was:abilities*/.walkSpeed/*was:walkSpeed*/ = value / 2f;
        player.sendPlayerAbilities/*was:updateAbilities*/();
    }

    public float getFlySpeed() {
        return getHandle().capabilities/*was:abilities*/.flySpeed/*was:flySpeed*/ * 2f;
    }

    public float getWalkSpeed() {
        return getHandle().capabilities/*was:abilities*/.walkSpeed/*was:walkSpeed*/ * 2f;
    }

    private void validateSpeed(float value) {
        if (value < 0) {
            if (value < -1f) {
                throw new IllegalArgumentException(value + " is too low");
            }
        } else {
            if (value > 1f) {
                throw new IllegalArgumentException(value + " is too high");
            }
        }
    }

    public void setMaxHealth(int amount) {
        super.setMaxHealth(amount);
        getHandle().setPlayerHealthUpdated/*was:triggerHealthUpdate*/();
    }

    public void resetMaxHealth() {
        super.resetMaxHealth();
        getHandle().setPlayerHealthUpdated/*was:triggerHealthUpdate*/();
    }
}