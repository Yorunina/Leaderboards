package com.leclowndu93150.leaderboards.integration.kubejs;

import com.leclowndu93150.leaderboards.LeaderboardRegistry;
import com.leclowndu93150.leaderboards.VanillaStatsRegistry;
import com.leclowndu93150.leaderboards.data.Leaderboard;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;

public class LeaderboardRegistryEventJS extends EventJS {
    public LeaderboardRegistryEventJS() {
        super();
    }
    public void register(ResourceLocation id, Leaderboard leaderboard) {
        LeaderboardRegistry.LEADERBOARDS.put(id, leaderboard);
    }

    public void registerVanilla(ResourceLocation id, Leaderboard leaderboard) {
        VanillaStatsRegistry.VANILLA_STATS.put(id, leaderboard);
    }
}
