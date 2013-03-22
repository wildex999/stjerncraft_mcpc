package org.bukkit.craftbukkit.scoreboard;


import org.bukkit.scoreboard.DisplaySlot;

import com.google.common.collect.ImmutableBiMap;

class CraftScoreboardTranslations {
    static final int MAX_DISPLAY_SLOT = 3;
    static ImmutableBiMap<DisplaySlot, String> SLOTS = ImmutableBiMap.of(
            DisplaySlot.BELOW_NAME, "belowName",
            DisplaySlot.PLAYER_LIST, "list",
            DisplaySlot.SIDEBAR, "sidebar");

    private CraftScoreboardTranslations() {}

    static DisplaySlot toBukkitSlot(int i) {
        return SLOTS.inverse().get(net.minecraft.scoreboard.Scoreboard.func_96517_b(i));
    }

    static int fromBukkitSlot(DisplaySlot slot) {
        return net.minecraft.scoreboard.Scoreboard.func_96537_j(SLOTS.get(slot));
    }

}
