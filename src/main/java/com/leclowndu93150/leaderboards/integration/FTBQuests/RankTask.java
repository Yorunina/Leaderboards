package com.leclowndu93150.leaderboards.integration.FTBQuests;

import com.leclowndu93150.leaderboards.LeaderboardRegistry;
import com.leclowndu93150.leaderboards.Leaderboards;
import com.leclowndu93150.leaderboards.VanillaStatsRegistry;
import com.leclowndu93150.leaderboards.data.Leaderboard;
import com.leclowndu93150.leaderboards.data.PlayerDataTracker;
import com.leclowndu93150.leaderboards.data.PlayerStatsWrapper;
import com.leclowndu93150.leaderboards.util.ScoreUtil;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RankTask extends Task {
    private ResourceLocation leaderboard;
    private int min = 0;
    private int rank = 1;

    private record PlayerScore(UUID uuid, int score) {
    }

    private static final Map<ResourceLocation, List<PlayerScore>> RANKING_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Long> LAST_RANK_CALC_TIME = new ConcurrentHashMap<>();
    private static final long RANK_CACHE_DURATION = 30 * 1000;
    private static final Object RANK_CALCULATION_LOCK = new Object();

    public RankTask(long id, Quest quest) {
        super(id, quest);
        this.leaderboard = new ResourceLocation(Leaderboards.MODID, "mob_kills");
    }


    public TaskType getType() {
        return LeaderboardTaskTypes.RANK_TASK;
    }


    public long getMaxProgress() {
        return 1;
    }


    public String formatMaxProgress() {
        return Integer.toString(1);
    }


    public String formatProgress(TeamData teamData, long progress) {
        return Long.toUnsignedString(progress);
    }


    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("leaderboard", this.leaderboard.toString());
        nbt.putInt("min", this.min);
        nbt.putInt("rank", this.rank);
    }


    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        this.leaderboard = ResourceLocation.tryParse(nbt.getString("leaderboard"));
        this.min = nbt.getInt("min");
        this.rank = nbt.getInt("rank");
    }


    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(this.leaderboard);
        buffer.writeVarInt(this.min);
        buffer.writeVarInt(this.rank);
    }


    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        this.leaderboard = buffer.readResourceLocation();
        this.min = buffer.readVarInt();
        this.rank = buffer.readVarInt();

    }


    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        List<ResourceLocation> list = new ArrayList<>();
        LeaderboardRegistry.LEADERBOARDS.forEach((k, v) -> {
            list.add(k);
        });
        VanillaStatsRegistry.VANILLA_STATS.forEach((k, v) -> {
            list.add(k);
        });
        config.addEnum("leaderboard", this.leaderboard, v -> this.leaderboard = v,
                NameMap.of(new ResourceLocation(Leaderboards.MODID, "mob_kills"), list)
                        .nameKey(v -> "leaderboard." + v.getNamespace() + "." + v.getPath())
                        .icon(v -> ItemIcon.getItemIcon(Items.SPAWNER))
                        .create());
        config.addInt("min", this.min, v -> this.min = v, 0, -2147483647, 2147483647);
        config.addInt("rank", this.rank, v -> this.rank = v, 1, -2147483647, 2147483647);
    }


    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        return Component.translatable("leaderboard." + this.leaderboard.getNamespace() + "." + this.leaderboard.getPath());
    }


    public int autoSubmitOnPlayerTick() {
        return 100;
    }


    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this) || !checkTaskSequence(teamData)) return;

        Leaderboard leaderBoard = LeaderboardRegistry.LEADERBOARDS.containsKey(this.leaderboard) ? LeaderboardRegistry.LEADERBOARDS.get(this.leaderboard) : VanillaStatsRegistry.VANILLA_STATS.get(this.leaderboard);

        if (leaderBoard == null) return;
        if (checkIfMeetRankRequire(player, leaderBoard)) {
            teamData.addProgress(this, 1);
        }

    }


    public boolean checkIfMeetRankRequire(ServerPlayer player, Leaderboard leaderboard) {
        if (this.rank == 0) {
            return false;
        }

        int targetPlayerScore = ScoreUtil.getPlayerScore(leaderboard, new PlayerStatsWrapper(player));
        if (this.min > 0 && targetPlayerScore < this.min) {
            return false;
        }

        boolean ascending = this.rank < 0;
        List<PlayerScore> sortedScores = getSortedScores(player.getServer(), leaderboard, ascending);

        if (sortedScores.isEmpty()) {
            return false;
        }

        int effectiveTargetRank = Math.abs(this.rank);
        int playerRank = -1;

        // More efficient O(N) rank calculation
        int rankCounter = 1;
        int playersAtCurrentRank = 1;
        for (int i = 0; i < sortedScores.size(); i++) {
            PlayerScore current = sortedScores.get(i);

            if (i > 0 && current.score() != sortedScores.get(i - 1).score()) {
                rankCounter += playersAtCurrentRank;
                playersAtCurrentRank = 1;
            } else if (i > 0) {
                playersAtCurrentRank++;
            }

            if (current.uuid().equals(player.getUUID())) {
                playerRank = rankCounter;
                break;
            }
        }

        return playerRank != -1 && playerRank <= effectiveTargetRank;
    }

    private List<PlayerScore> getSortedScores(MinecraftServer server, Leaderboard leaderboard, boolean ascending) {
        long now = System.currentTimeMillis();
        if (now - LAST_RANK_CALC_TIME.getOrDefault(leaderboard.id, 0L) < RANK_CACHE_DURATION && RANKING_CACHE.containsKey(leaderboard.id)) {
            return RANKING_CACHE.getOrDefault(leaderboard.id, List.of());
        }

        synchronized (RANK_CALCULATION_LOCK) {
            // Double-checked locking
            if (now - LAST_RANK_CALC_TIME.getOrDefault(leaderboard.id, 0L) < RANK_CACHE_DURATION && RANKING_CACHE.containsKey(leaderboard.id)) {
                return RANKING_CACHE.getOrDefault(leaderboard.id, List.of());
            }

            List<PlayerScore> scores = new ArrayList<>();
            for (UUID uuid : PlayerDataTracker.get(server.overworld()).getAllPlayerUUIDs()) {
                GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
                if (profile != null) {
                    ServerStatsCounter stats = ScoreUtil.loadPlayerStats(server, uuid);
                    int score = ScoreUtil.getPlayerScore(leaderboard, new PlayerStatsWrapper(uuid, profile, stats, server));
                    scores.add(new PlayerScore(uuid, score));
                }
            }

            Comparator<PlayerScore> comparator = Comparator.comparingInt(PlayerScore::score);
            scores.sort(ascending ? comparator : comparator.reversed());

            RANKING_CACHE.put(leaderboard.id, scores);
            LAST_RANK_CALC_TIME.put(leaderboard.id, now);
            return scores;
        }
    }
}
