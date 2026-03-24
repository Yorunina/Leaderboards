package com.leclowndu93150.leaderboards.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class KubeEvents {
    public static EventGroup LEADERBOARDS_GROUP = EventGroup.of("LeaderboardsEvents");
    public static EventHandler REGISTRY_LEADERBOARDS_EVENT = LEADERBOARDS_GROUP
            .server("registryLeaderboards", () -> LeaderboardRegistryEventJS.class);
}
