package net.minecraft.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;

// MCPC+ start
import org.bukkit.craftbukkit.command.CraftSimpleCommandMap;
import org.bukkit.craftbukkit.command.ModCustomCommand;

import cpw.mods.fml.common.FMLCommonHandler;
// MCPC+ end

public class CommandHandler implements ICommandManager
{
    /** Map of Strings to the ICommand objects they represent */
    private final Map commandMap = new HashMap();

    /** The set of ICommand objects currently loaded. */
    private final Set commandSet = new HashSet();

    public void executeCommand(ICommandSender par1ICommandSender, String par2Str)
    {
        if (par2Str.startsWith("/"))
        {
            par2Str = par2Str.substring(1);
        }

        String[] astring = par2Str.split(" ");
        String s1 = astring[0];
        astring = dropFirstString(astring);
        ICommand icommand = (ICommand)this.commandMap.get(s1);
        int i = this.getUsernameIndex(icommand, astring);

        try
        {
            if (icommand == null)
            {
                throw new CommandNotFoundException();
            }

            // MCPC+ start - disable check for permissions since we handle it on Bukkit side
            //if (icommand.canCommandSenderUseCommand(par1ICommandSender))
            //{
            CommandEvent event = new CommandEvent(icommand, par1ICommandSender, astring);
            if (MinecraftForge.EVENT_BUS.post(event))
            {
                if (event.exception != null)
                {
                    throw event.exception;
                }
                return;
            }

            if (i > -1)
            {
                EntityPlayerMP[] aentityplayermp = PlayerSelector.matchPlayers(par1ICommandSender, astring[i]);
                String s2 = astring[i];
                EntityPlayerMP[] aentityplayermp1 = aentityplayermp;
                int j = aentityplayermp.length;

                for (int k = 0; k < j; ++k)
                {
                    EntityPlayerMP entityplayermp = aentityplayermp1[k];
                    astring[i] = entityplayermp.getEntityName();

                    try
                    {
                        icommand.processCommand(par1ICommandSender, astring);
                    }
                    catch (PlayerNotFoundException playernotfoundexception)
                    {
                        par1ICommandSender.sendChatToPlayer("\u00a7c" + par1ICommandSender.translateString(playernotfoundexception.getMessage(), playernotfoundexception.getErrorOjbects()));
                    }
                }

                astring[i] = s2;
            }
            else
            {
                icommand.processCommand(par1ICommandSender, astring);
            }
            /*}
            else
            {
                par1ICommandSender.sendChatToPlayer("\u00a7cYou do not have permission to use this command.");
            }*/
            // MCPC+ end
        }
        catch (WrongUsageException wrongusageexception)
        {
            par1ICommandSender.sendChatToPlayer("\u00a7c" + par1ICommandSender.translateString("commands.generic.usage", new Object[] {par1ICommandSender.translateString(wrongusageexception.getMessage(), wrongusageexception.getErrorOjbects())}));
        }
        catch (CommandException commandexception)
        {
            par1ICommandSender.sendChatToPlayer("\u00a7c" + par1ICommandSender.translateString(commandexception.getMessage(), commandexception.getErrorOjbects()));
        }
        catch (Throwable throwable)
        {
            par1ICommandSender.sendChatToPlayer("\u00a7c" + par1ICommandSender.translateString("commands.generic.exception", new Object[0]));
            throwable.printStackTrace();
        }
    }

    /**
     * adds the command and any aliases it has to the internal map of available commands
     */
    public ICommand registerCommand(ICommand par1ICommand)
    {
        // MCPC+ start - register commands with permission nodes, defaulting to class name
        return registerCommand(par1ICommand, par1ICommand.getClass().getName());
    }

    public ICommand registerCommand(String permissionGroup, ICommand par1ICommand)
    {
        return registerCommand(par1ICommand, permissionGroup + "." + par1ICommand.getCommandName());
    }

    public ICommand registerCommand(ICommand par1ICommand, String permissionNode)
    {
        // MCPC+ end
        List list = par1ICommand.getCommandAliases();
        this.commandMap.put(par1ICommand.getCommandName(), par1ICommand);
        this.commandSet.add(par1ICommand);
        // MCPC+ start - register vanilla commands with Bukkit to support permissions.
        CraftSimpleCommandMap commandMap = FMLCommonHandler.instance().getMinecraftServerInstance().server.getCraftCommandMap();
        ModCustomCommand customCommand = new ModCustomCommand(par1ICommand.getCommandName());
        customCommand.setPermission(par1ICommand.getClass().getName());
        commandMap.register(par1ICommand.getCommandName(), customCommand);
        FMLCommonHandler.instance().getMinecraftServerInstance().server.getLogger().info("Registered command " + par1ICommand.getCommandName() + " with permission node " + permissionNode);
        // MCPC+ end
        if (list != null)
        {
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                String s1 = (String)iterator.next();
                ICommand icommand1 = (ICommand)this.commandMap.get(s1);

                if (icommand1 == null || !icommand1.getCommandName().equals(s1))
                {
                    this.commandMap.put(s1, par1ICommand);
                }
            }
        }

        return par1ICommand;
    }

    /**
     * creates a new array and sets elements 0..n-2 to be 0..n-1 of the input (n elements)
     */
    private static String[] dropFirstString(String[] par0ArrayOfStr)
    {
        String[] astring1 = new String[par0ArrayOfStr.length - 1];

        for (int i = 1; i < par0ArrayOfStr.length; ++i)
        {
            astring1[i - 1] = par0ArrayOfStr[i];
        }

        return astring1;
    }

    /**
     * Performs a "begins with" string match on each token in par2. Only returns commands that par1 can use.
     */
    public List getPossibleCommands(ICommandSender par1ICommandSender, String par2Str)
    {
        String[] astring = par2Str.split(" ", -1);
        String s1 = astring[0];

        if (astring.length == 1)
        {
            ArrayList arraylist = new ArrayList();
            Iterator iterator = this.commandMap.entrySet().iterator();

            while (iterator.hasNext())
            {
                Entry entry = (Entry)iterator.next();

                if (CommandBase.doesStringStartWith(s1, (String)entry.getKey()) && ((ICommand)entry.getValue()).canCommandSenderUseCommand(par1ICommandSender))
                {
                    arraylist.add(entry.getKey());
                }
            }

            return arraylist;
        }
        else
        {
            if (astring.length > 1)
            {
                ICommand icommand = (ICommand)this.commandMap.get(s1);

                if (icommand != null)
                {
                    return icommand.addTabCompletionOptions(par1ICommandSender, dropFirstString(astring));
                }
            }

            return null;
        }
    }

    /**
     * returns all commands that the commandSender can use
     */
    public List getPossibleCommands(ICommandSender par1ICommandSender)
    {
        ArrayList arraylist = new ArrayList();
        Iterator iterator = this.commandSet.iterator();

        while (iterator.hasNext())
        {
            ICommand icommand = (ICommand)iterator.next();

            if (icommand.canCommandSenderUseCommand(par1ICommandSender))
            {
                arraylist.add(icommand);
            }
        }

        return arraylist;
    }

    /**
     * returns a map of string to commads. All commands are returned, not just ones which someone has permission to use.
     */
    public Map getCommands()
    {
        return this.commandMap;
    }

    /**
     * Return a command's first parameter index containing a valid username.
     */
    private int getUsernameIndex(ICommand par1ICommand, String[] par2ArrayOfStr)
    {
        if (par1ICommand == null)
        {
            return -1;
        }
        else
        {
            for (int i = 0; i < par2ArrayOfStr.length; ++i)
            {
                if (par1ICommand.isUsernameIndex(i) && PlayerSelector.matchesMultiplePlayers(par2ArrayOfStr[i]))
                {
                    return i;
                }
            }

            return -1;
        }
    }
}
