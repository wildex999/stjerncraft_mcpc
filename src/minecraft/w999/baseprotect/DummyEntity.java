package w999.baseprotect;

import java.lang.reflect.Field;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.craftbukkit.*;

import net.minecraft.entity.EntityLiving;

//Dummy Entity used for Nonliving entities that need to be logged

public class DummyEntity extends EntityLiving {

	public DummyEntity(net.minecraft.world.World par1World) {
		super(par1World);
	}

	@Override
	public int getMaxHealth() {
		return 1;
	}

	
	
	


}
