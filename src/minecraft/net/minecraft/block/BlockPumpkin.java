package net.minecraft.block;

// CraftBukkit start
import org.bukkit.craftbukkit.util.BlockStateListPopulator;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
// CraftBukkit end

public class BlockPumpkin extends BlockDirectional
{
    /** Boolean used to seperate different states of blocks */
    private boolean blockType;

    protected BlockPumpkin(int par1, boolean par2)
    {
        super(par1, Material.pumpkin);
        this.setTickRandomly(true);
        this.blockType = par2;
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    /**
     * Called whenever the block is added into the world. Args: world, x, y, z
     */
    public void onBlockAdded(World par1World, int par2, int par3, int par4)
    {
        super.onBlockAdded(par1World, par2, par3, par4);

        if (par1World.getBlockId(par2, par3 - 1, par4) == Block.blockSnow.blockID && par1World.getBlockId(par2, par3 - 2, par4) == Block.blockSnow.blockID)
        {
            if (!par1World.isRemote)
            {
                // CraftBukkit start - Use BlockStateListPopulator
                BlockStateListPopulator blockList = new BlockStateListPopulator(par1World.getWorld());
                blockList.setTypeId(par2, par3, par4, 0);
                blockList.setTypeId(par2, par3 - 1, par4, 0);
                blockList.setTypeId(par2, par3 - 2, par4, 0);
                EntitySnowman entitysnowman = new EntitySnowman(par1World);
                entitysnowman.setLocationAndAngles((double)par2 + 0.5D, (double)par3 - 1.95D, (double)par4 + 0.5D, 0.0F, 0.0F);

                if (par1World.addEntity(entitysnowman, SpawnReason.BUILD_SNOWMAN))
                {
                    blockList.updateList();
                }

                // CraftBukkit end
            }

            for (int l = 0; l < 120; ++l)
            {
                par1World.spawnParticle("snowshovel", (double)par2 + par1World.rand.nextDouble(), (double)(par3 - 2) + par1World.rand.nextDouble() * 2.5D, (double)par4 + par1World.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }
        else if (par1World.getBlockId(par2, par3 - 1, par4) == Block.blockSteel.blockID && par1World.getBlockId(par2, par3 - 2, par4) == Block.blockSteel.blockID)
        {
            boolean flag = par1World.getBlockId(par2 - 1, par3 - 1, par4) == Block.blockSteel.blockID && par1World.getBlockId(par2 + 1, par3 - 1, par4) == Block.blockSteel.blockID;
            boolean flag1 = par1World.getBlockId(par2, par3 - 1, par4 - 1) == Block.blockSteel.blockID && par1World.getBlockId(par2, par3 - 1, par4 + 1) == Block.blockSteel.blockID;

            if (flag || flag1)
            {
                // CraftBukkit start - Use BlockStateListPopulator
                BlockStateListPopulator blockList = new BlockStateListPopulator(par1World.getWorld());
                blockList.setTypeId(par2, par3, par4, 0);
                blockList.setTypeId(par2, par3 - 1, par4, 0);
                blockList.setTypeId(par2, par3 - 2, par4, 0);

                if (flag)
                {
                    blockList.setTypeId(par2 - 1, par3 - 1, par4, 0);
                    blockList.setTypeId(par2 + 1, par3 - 1, par4, 0);
                }
                else
                {
                    blockList.setTypeId(par2, par3 - 1, par4 - 1, 0);
                    blockList.setTypeId(par2, par3 - 1, par4 + 1, 0);
                }

                EntityIronGolem entityirongolem = new EntityIronGolem(par1World);
                entityirongolem.setPlayerCreated(true);
                entityirongolem.setLocationAndAngles((double)par2 + 0.5D, (double)par3 - 1.95D, (double)par4 + 0.5D, 0.0F, 0.0F);

                if (par1World.addEntity(entityirongolem, SpawnReason.BUILD_IRONGOLEM))
                {
                    for (int i1 = 0; i1 < 120; ++i1)
                    {
                        par1World.spawnParticle("snowballpoof", (double) par2 + par1World.rand.nextDouble(), (double)(par3 - 2) + par1World.rand.nextDouble() * 3.9D, (double) par4 + par1World.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
                    }

                    blockList.updateList();
                }

                // CraftBukkit end
            }
        }
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    public boolean canPlaceBlockAt(World par1World, int par2, int par3, int par4)
    {
        int l = par1World.getBlockId(par2, par3, par4);
        return (l == 0 || Block.blocksList[l].blockMaterial.isReplaceable()) && par1World.doesBlockHaveSolidTopSurface(par2, par3 - 1, par4);
    }

    /**
     * Called when the block is placed in the world.
     */
    public void onBlockPlacedBy(World par1World, int par2, int par3, int par4, EntityLiving par5EntityLiving, ItemStack par6ItemStack)
    {
        int l = MathHelper.floor_double((double)(par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 2.5D) & 3;
        par1World.setBlockMetadataWithNotify(par2, par3, par4, l, 2);
    }

    // CraftBukkit start

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World world, int i, int j, int k, int l)
    {
        if (Block.blocksList[l] != null && Block.blocksList[l].canProvidePower())
        {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(i, j, k);
            int power = block.getBlockPower();
            BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(block, power, power);
            world.getServer().getPluginManager().callEvent(eventRedstone);
        }
    }
    // CraftBukkit end
}
