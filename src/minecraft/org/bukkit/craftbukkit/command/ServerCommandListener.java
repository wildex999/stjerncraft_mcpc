package org.bukkit.craftbukkit.command;

import java.lang.reflect.Method;


import org.bukkit.command.CommandSender;

public class ServerCommandListener implements net.minecraft.command.ICommandSender {
    private final CommandSender commandSender;
    private final String prefix;

    public ServerCommandListener(CommandSender commandSender) {
        this.commandSender = commandSender;
        String[] parts = commandSender.getClass().getName().split("\\.");
        this.prefix = parts[parts.length - 1];
    }

    public void sendChatToPlayer(String msg) {
        this.commandSender.sendMessage(msg);
    }

    public CommandSender getSender() {
        return commandSender;
    }

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getCommandSenderName() {
        try {
            Method getName = commandSender.getClass().getMethod("getName");

            return (String) getName.invoke(commandSender);
        } catch (Exception e) {}

        return this.prefix;
    }

    /**
     * Translates and formats the given string key with the given arguments.
     */
    public String translateString(String s, Object... aobject) {
        return net.minecraft.util.StringTranslate.getInstance().translateKeyFormat(s, aobject);
    }

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int i, String s) {
        return true;
    }

    /**
     * Return the position for this command sender.
     */
    public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
        return new net.minecraft.util.ChunkCoordinates(0, 0, 0);
    }
}
