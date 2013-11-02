package w999.baseprotect;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class ProxyEntityPlayerMP extends EntityPlayerMP {

	public ProxyEntityPlayerMP(MinecraftServer par1MinecraftServer,
			World par2World, String par3Str,
			ItemInWorldManager par4ItemInWorldManager) {
		super(par1MinecraftServer, par2World, par3Str, par4ItemInWorldManager);
		// TODO Auto-generated constructor stub
	}

}
