package net.minecraft.command;

import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityCommandBlock;

public class ServerCommandManager extends CommandHandler implements IAdminCommand
{
    public ServerCommandManager()
    {
        CommandBase.setAdminCommander(this);
    }

    // MCPC+ start - moved commands to it's own method to be executed further in server startup + changed to registerVanillaCommand
    public void registerVanillaCommands()
    {
        // MCPC+ - do not register vanilla commands replaced by Bukkit (TODO: option to choose vanilla or Bukkit?)
        /*
        this.registerCommand("vanilla.command", new CommandTime());
        this.registerCommand("vanilla.command", new CommandGameMode());
        this.registerCommand("vanilla.command", new CommandDifficulty());
        this.registerCommand("vanilla.command", new CommandDefaultGameMode());
        this.registerCommand("vanilla.command", new CommandKill());
        this.registerCommand("vanilla.command", new CommandToggleDownfall());
        this.registerCommand("vanilla.command", new CommandWeather());
        this.registerCommand("vanilla.command", new CommandXP());
        this.registerCommand("vanilla.command", new CommandServerTp());
        this.registerCommand("vanilla.command", new CommandGive());
        this.registerCommand("vanilla.command", new CommandEnchant());
        this.registerCommand("vanilla.command", new CommandServerEmote());
        this.registerCommand("vanilla.command", new CommandShowSeed());
        this.registerCommand("vanilla.command", new CommandHelp());
        */
        this.registerCommand("vanilla.command", new CommandDebug());
        /*
        this.registerCommand("vanilla.command", new CommandServerMessage());
        this.registerCommand("vanilla.command", new CommandServerSay());
        this.registerCommand("vanilla.command", new CommandSetSpawnpoint());
        this.registerCommand("vanilla.command", new CommandGameRule());
        this.registerCommand("vanilla.command", new CommandClearInventory());
        */
        if (MinecraftServer.getServer().isDedicatedServer())
        {
            /*
            this.registerCommand("vanilla.command", new CommandServerOp());
            this.registerCommand("vanilla.command", new CommandServerDeop());
            this.registerCommand("vanilla.command", new CommandServerStop());
            this.registerCommand("vanilla.command", new CommandServerSaveAll());
            this.registerCommand("vanilla.command", new CommandServerSaveOff());
            this.registerCommand("vanilla.command", new CommandServerSaveOn());
            this.registerCommand("vanilla.command", new CommandServerBanIp());
            this.registerCommand("vanilla.command", new CommandServerPardonIp());
            this.registerCommand("vanilla.command", new CommandServerBan());
            this.registerCommand("vanilla.command", new CommandServerBanlist());
            this.registerCommand("vanilla.command", new CommandServerPardon());
            this.registerCommand("vanilla.command", new CommandServerKick());
            this.registerCommand("vanilla.command", new CommandServerList());
            this.registerCommand("vanilla.command", new CommandServerWhitelist());
            */
        }
        else
        {
            this.registerCommand("vanilla.command", new CommandServerPublishLocal());
        }
    }
    // MCPC+ end

    /**
     * Sends a message to the admins of the server from a given CommandSender with the given resource string and given
     * extra srings. If the int par2 is even or zero, the original sender is also notified.
     */
    public void notifyAdmins(ICommandSender par1ICommandSender, int par2, String par3Str, Object ... par4ArrayOfObj)
    {
        boolean var5 = true;

        if (par1ICommandSender instanceof TileEntityCommandBlock && !MinecraftServer.getServer().worldServers[0].getGameRules().getGameRuleBooleanValue("commandBlockOutput"))
        {
            var5 = false;
        }

        if (var5)
        {
            Iterator var6 = MinecraftServer.getServer().getConfigurationManager().playerEntityList.iterator();

            while (var6.hasNext())
            {
                EntityPlayerMP var7 = (EntityPlayerMP)var6.next();

                if (var7 != par1ICommandSender && MinecraftServer.getServer().getConfigurationManager().areCommandsAllowed(var7.username))
                {
                    var7.sendChatToPlayer("\u00a77\u00a7o[" + par1ICommandSender.getCommandSenderName() + ": " + var7.translateString(par3Str, par4ArrayOfObj) + "]");
                }
            }
        }

        if (par1ICommandSender != MinecraftServer.getServer())
        {
            MinecraftServer.logger.info("[" + par1ICommandSender.getCommandSenderName() + ": " + MinecraftServer.getServer().translateString(par3Str, par4ArrayOfObj) + "]");
        }

        if ((par2 & 1) != 1)
        {
            par1ICommandSender.sendChatToPlayer(par1ICommandSender.translateString(par3Str, par4ArrayOfObj));
        }
    }
}
