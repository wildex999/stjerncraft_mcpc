package cpw.mods.fml.common.event;

import org.bukkit.command.Command;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.LoaderState.ModState;

public class FMLServerStartingEvent extends FMLStateEvent
{

    private MinecraftServer server;

    public FMLServerStartingEvent(Object... data)
    {
        super(data);
        this.server = (MinecraftServer) data[0];
    }
    @Override
    public ModState getModState()
    {
        return ModState.AVAILABLE;
    }

    public MinecraftServer getServer()
    {
        return server;
    }

    public void registerServerCommand(ICommand command)
    {
        CommandHandler ch = (CommandHandler) getServer().getCommandManager();
        ch.registerCommand(command);
    }

    // MCPC start - used for mods to register a Bukkit command
    public void registerServerCommand(String fallbackPrefix, Command command)
    {
        org.bukkit.command.SimpleCommandMap commandMap = getServer().server.getCommandMap();
        commandMap.register(fallbackPrefix, command);
    }
    // MCPC end 
}
