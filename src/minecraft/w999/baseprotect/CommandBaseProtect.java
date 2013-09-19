package w999.baseprotect;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.entity.Player;

public class CommandBaseProtect extends VanillaCommand {
	
	public CommandBaseProtect() {
        super("baseprotect");
        this.description = "Base Protect control";
        this.usageMessage = ChatColor.RED + "basecontrol <inspect, help>";
        this.setPermission("mcpc.command.baseprotect");
    }

	@Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
		if (!testPermission(sender)) return true;
		
		if (args.length == 0) {
            sender.sendMessage(usageMessage.split("\n"));
            return false;
        }
		
		String action = args[0];
		
		if(action.equalsIgnoreCase("inspect"))
		{
			if(!(sender instanceof Player))
			{
				sender.sendMessage(ChatColor.RED + "This command can only be used by a player");
				return false;
			}
			
			//Get player data
			PlayerData player = BaseProtect.instance.getPlayerData(((Player)sender).getName());
			
			//Switch the BaseProtect inspect mode for the player
			if(!player.inspect)
			{
				player.inspect = true;
				sender.sendMessage(ChatColor.GREEN + "BaseProtect now in inspect mode!");
				return true;
			} else {
				player.inspect = false;
				sender.sendMessage(ChatColor.GREEN + "BaseProtect is now " + ChatColor.RED + " NOT " + ChatColor.GREEN + " in inspect mode!");
				return true;
			}
			
			
			
		} else if(action.equalsIgnoreCase("help"))
		{
			sender.sendMessage(ChatColor.RED + "Help not yet implemented!");
			return false;
		} else if(action.equalsIgnoreCase("crash"))
		{
			while(true) {} //Crash server 
			//TODO: REMOVE!!!!!
		}
		

		sender.sendMessage(ChatColor.RED + " Unknown action!");
		return false;
	}

}
