package com.leclowndu93150.leaderboards.integration.kubejs;

import com.leclowndu93150.leaderboards.LeaderboardRegistry;
import com.leclowndu93150.leaderboards.VanillaStatsRegistry;
import com.leclowndu93150.leaderboards.data.Leaderboard;
import com.leclowndu93150.leaderboards.data.PlayerStatsWrapper;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

public class LeaderboardRegistryEventJS extends EventJS {
    public LeaderboardRegistryEventJS() {
        super();
    }

    public void register(ResourceLocation id, Component title, Function<PlayerStatsWrapper, Component> valueFunction, Function<PlayerStatsWrapper, Double> intValueFunction, Comparator<PlayerStatsWrapper> comparator, Predicate<PlayerStatsWrapper> validValue) {
        LeaderboardRegistry.LEADERBOARDS.put(id, new Leaderboard(
                id,
                title,
                valueFunction,
                (v) -> intValueFunction.apply(v).intValue(),
                comparator,
                validValue)
        );
    }

    public void registerByLeaderboard(ResourceLocation id,  Leaderboard leaderboard) {
        LeaderboardRegistry.LEADERBOARDS.put(id, leaderboard);
    }

}
