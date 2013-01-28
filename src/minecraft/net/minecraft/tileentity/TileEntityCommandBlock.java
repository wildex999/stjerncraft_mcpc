package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
// CraftBukkit start
import java.util.ArrayList;
import java.util.Arrays;
import com.google.common.base.Joiner;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
// CraftBukkit end
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class TileEntityCommandBlock extends TileEntity implements ICommandSender
{
    /** The command this block will execute when powered. */
    private String command = "";
    private final org.bukkit.command.BlockCommandSender sender;

    public TileEntityCommandBlock()
    {
        sender = new org.bukkit.craftbukkit.command.CraftBlockCommandSender(this);
    }

    /**
     * Sets the command this block will execute when powered.
     */
    public void setCommand(String par1Str)
    {
        this.command = par1Str;
        this.onInventoryChanged();
    }

    @SideOnly(Side.CLIENT)

    /**
     * Return the command this command block is set to execute.
     */
    public String getCommand()
    {
        return this.command;
    }

    /**
     * Execute the command, called when the command block is powered.
     */
    public void executeCommandOnPowered(World par1World)
    {
        if (!par1World.isRemote)
        {
            MinecraftServer var2 = MinecraftServer.getServer();

            if (var2 != null && var2.isCommandBlockEnabled())
            {
                // CraftBukkit start - handle command block as console
                org.bukkit.command.SimpleCommandMap commandMap = var2.server.getCommandMap();
                Joiner joiner = Joiner.on(" ");
                String command = this.command;

                if (this.command.startsWith("/"))
                {
                    command = this.command.substring(1);
                }

                String[] args = command.split(" ");
                ArrayList<String[]> commands = new ArrayList<String[]>();

                // block disallowed commands
                if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("op") ||
                        args[0].equalsIgnoreCase("deop") || args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("ban-ip") ||
                        args[0].equalsIgnoreCase("pardon") || args[0].equalsIgnoreCase("pardon-ip") || args[0].equalsIgnoreCase("reload"))
                {
                    return;
                }

                // make sure this is a valid command
                if (commandMap.getCommand(args[0]) == null)
                {
                    return;
                }

                // if the world has no players don't run
                if (this.worldObj.playerEntities.isEmpty())
                {
                    return;
                }

                commands.add(args);
                // find positions of command block syntax, if any
                ArrayList<String[]> newCommands = new ArrayList<String[]>();

                for (int i = 0; i < args.length; i++)
                {
                    if (PlayerSelector.hasArguments(args[i]))
                    {
                        for (int j = 0; j < commands.size(); j++)
                        {
                            newCommands.addAll(this.buildCommands(commands.get(j), i));
                        }

                        ArrayList<String[]> temp = commands;
                        commands = newCommands;
                        newCommands = temp;
                        newCommands.clear();
                    }
                }

                // now dispatch all of the commands we ended up with
                for (int i = 0; i < commands.size(); i++)
                {
                    commandMap.dispatch(sender, joiner.join(Arrays.asList(commands.get(i))));
                }

                // CraftBukkit end
            }
        }
    }

    // CraftBukkit start
    private ArrayList<String[]> buildCommands(String[] args, int pos)
    {
        ArrayList<String[]> commands = new ArrayList<String[]>();
        EntityPlayerMP[] players = PlayerSelector.matchPlayers(this, args[pos]);

        if (players != null)
        {
            for (EntityPlayerMP player : players)
            {
                String[] command = args.clone();
                command[pos] = player.getEntityName();
                commands.add(command);
            }
        }

        return commands;
    }
    // CraftBukkit end

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getCommandSenderName()
    {
        return "@";
    }

    public void sendChatToPlayer(String par1Str) {}

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        return par1 <= 2;
    }

    /**
     * Translates and formats the given string key with the given arguments.
     */
    public String translateString(String par1Str, Object ... par2ArrayOfObj)
    {
        return par1Str;
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setString("Command", this.command);
    }

    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        this.command = par1NBTTagCompound.getString("Command");
    }

    /**
     * Return the position for this command sender.
     */
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(this.xCoord, this.yCoord, this.zCoord);
    }

    /**
     * Overriden in a sign to provide the text.
     */
    public Packet getDescriptionPacket()
    {
        NBTTagCompound var1 = new NBTTagCompound();
        this.writeToNBT(var1);
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 2, var1);
    }
}
