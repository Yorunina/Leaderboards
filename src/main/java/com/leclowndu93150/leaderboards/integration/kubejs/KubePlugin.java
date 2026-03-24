package com.leclowndu93150.leaderboards.integration.kubejs;

import com.leclowndu93150.leaderboards.LeaderboardRegistry;
import com.leclowndu93150.leaderboards.VanillaStatsRegistry;
import com.leclowndu93150.leaderboards.data.Leaderboard;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

import static com.leclowndu93150.leaderboards.integration.kubejs.KubeEvents.LEADERBOARDS_GROUP;


public class KubePlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        LEADERBOARDS_GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("LeaderboardRegistry", LeaderboardRegistry.class);
        event.add("VanillaStatsRegistry", VanillaStatsRegistry.class);
        event.add("Leaderboard", Leaderboard.class);
        event.add("LeaderboardFromStat", Leaderboard.FromStat.class);
    }
}
