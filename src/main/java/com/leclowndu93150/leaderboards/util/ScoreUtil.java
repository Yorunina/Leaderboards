package com.leclowndu93150.leaderboards.util;

import com.leclowndu93150.leaderboards.data.Leaderboard;
import com.leclowndu93150.leaderboards.data.PlayerStatsWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.UUID;

public class ScoreUtil {

    public static int getPlayerScore(Leaderboard leaderboard, PlayerStatsWrapper player) {
        return leaderboard.getIntValue(player);
    }

    public static ServerStatsCounter loadPlayerStats(MinecraftServer server, UUID uuid) {
        File statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        File statsFile = new File(statsDir, uuid.toString() + ".json");
        return new ServerStatsCounter(server, statsFile);
    }
}
