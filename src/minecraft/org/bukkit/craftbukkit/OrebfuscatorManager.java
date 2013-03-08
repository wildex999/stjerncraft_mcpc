package org.bukkit.craftbukkit;


public class OrebfuscatorManager {

    // Used to keep track of which blocks to obfuscate
    public static boolean[] obfuscateBlocks = new boolean[Short.MAX_VALUE]; // MCPC+ - private -> public for custom ores

    // Default blocks
    static {
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.stone/*was:STONE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreGold/*was:GOLD_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreIron/*was:IRON_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreCoal/*was:COAL_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreLapis/*was:LAPIS_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.chest/*was:CHEST*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreDiamond/*was:DIAMOND_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreRedstone/*was:REDSTONE_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreRedstoneGlowing/*was:GLOWING_REDSTONE_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.oreEmerald/*was:EMERALD_ORE*/.blockID/*was:id*/] = true;
        obfuscateBlocks[net.minecraft.block.Block/*was:Block*/.enderChest/*was:ENDER_CHEST*/.blockID/*was:id*/] = true;
    }

    public static void updateNearbyBlocks(net.minecraft.world.World/*was:World*/ world, int x, int y, int z) {
        updateNearbyBlocks(world, x, y, z, world.getServer().orebfuscatorUpdateRadius);
    }

    public static void obfuscate(int chunkX, int chunkY, int bitmask, byte[] buffer, net.minecraft.world.World/*was:World*/ world) {
        if (world.getServer().orebfuscatorEnabled && world.getWorld().obfuscated) {
            int initialRadius = 1;
            int index = 0;
            int startX = chunkX << 4;
            int startZ = chunkY << 4;
            for (int i = 0; i < 16; i++) {
                // If the bitmask indicates this chunk is sent...
                if ((bitmask & 1 << i) != 0) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                byte data = buffer[index];
                                // Check if the block should be obfuscated for the default engine modes
                                if (obfuscateBlocks[data & 0xFF]) { // TODO: decode extended block IDs (4-bit 'add' field)
                                    if (initialRadius == 0 || !areAjacentBlocksTransparent(world, startX + x, (i << 4) + y, startZ + z, initialRadius)) {
                                        // Replace with stone
                                        buffer[index] = (byte) net.minecraft.block.Block/*was:Block*/.stone/*was:STONE*/.blockID/*was:id*/;
                                    }
                                }
                                if (++index >= buffer.length) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void updateNearbyBlocks(net.minecraft.world.World/*was:World*/ world, int x, int y, int z, int radius) {
        if (world.getServer().orebfuscatorEnabled && world.getWorld().obfuscated && world.blockExists/*was:isLoaded*/(x, y, z)) {
            // Get block id
            int id = world.getBlockId/*was:getTypeId*/(x, y, z);

            // See if it needs update
            if (obfuscateBlocks[id]) {
                // Send the update
                world.markBlockForUpdate/*was:notify*/(x, y, z);
            }

            // Check other blocks for updates
            if (radius != 0) {
                updateNearbyBlocks(world, x + 1, y, z, radius - 1);
                updateNearbyBlocks(world, x - 1, y, z, radius - 1);
                updateNearbyBlocks(world, x, y + 1, z, radius - 1);
                updateNearbyBlocks(world, x, y - 1, z, radius - 1);
                updateNearbyBlocks(world, x, y, z + 1, radius - 1);
                updateNearbyBlocks(world, x, y, z - 1, radius - 1);
            }
        }
    }

    private static boolean areAjacentBlocksTransparent(net.minecraft.world.World/*was:World*/ world, int x, int y, int z, int radius) {
        return y > 0 && y <= world.getHeight/*was:getHeight*/()
                && world.blockExists/*was:isLoaded*/(x, y, z)
                && !net.minecraft.block.Block/*was:Block*/.isNormalCube/*was:i*/(world.getBlockId/*was:getTypeId*/(x, y, z))
                || (radius > 0 && (areAjacentBlocksTransparent(world, x, y + 1, z, radius - 1)
                || areAjacentBlocksTransparent(world, x, y - 1, z, radius - 1)
                || areAjacentBlocksTransparent(world, x + 1, y, z, radius - 1)
                || areAjacentBlocksTransparent(world, x - 1, y, z, radius - 1)
                || areAjacentBlocksTransparent(world, x, y, z + 1, radius - 1)
                || areAjacentBlocksTransparent(world, x, y, z - 1, radius - 1)));
    }
}
