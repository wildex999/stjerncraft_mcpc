package w999.baseprotect;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.World;

public class ProxyEntityPlayerMP extends EntityPlayerMP {

	public ProxyEntityPlayerMP(MinecraftServer par1MinecraftServer,
			World par2World, String par3Str,
			ItemInWorldManager par4ItemInWorldManager) {
		super(par1MinecraftServer, par2World, par3Str, par4ItemInWorldManager);
	}
	
    public void sendChatToPlayer(String par1Str)
    {
    	//this.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(s1));
    }
    
    public void addChatMessage(String par1Str)
    {
        /*StringTranslate stringtranslate = StringTranslate.getInstance();
        String s1 = stringtranslate.translateKey(par1Str);
        this.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(s1));*/
    }

}
