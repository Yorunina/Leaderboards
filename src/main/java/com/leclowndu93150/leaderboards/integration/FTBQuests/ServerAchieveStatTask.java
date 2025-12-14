package com.leclowndu93150.leaderboards.integration.FTBQuests;

import com.leclowndu93150.leaderboards.LeaderboardRegistry;
import com.leclowndu93150.leaderboards.Leaderboards;
import com.leclowndu93150.leaderboards.VanillaStatsRegistry;
import com.leclowndu93150.leaderboards.data.PlayerDataTracker;
import com.leclowndu93150.leaderboards.data.Leaderboard;
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
import java.util.List;
import java.util.UUID;

public class ServerAchieveStatTask extends Task {
    private ResourceLocation leaderboard;
    private long value = 1;

    private static long cachedServerTotal = -1;
    private static long lastCalcTime = 0;
    private static final long CACHE_DURATION = 30 * 1000;
    private static final Object CALCULATION_LOCK = new Object();

    public ServerAchieveStatTask(long id, Quest quest) {
        super(id, quest);
        this.leaderboard = new ResourceLocation(Leaderboards.MODID, "mob_kills");
    }


    public TaskType getType() {
        return LeaderboardTaskTypes.SERVER_ACHIEVE_STAT_TASK;
    }


    public long getMaxProgress() {
        return this.value;
    }


    public String formatMaxProgress() {
        return Long.toString(this.value);
    }


    public String formatProgress(TeamData teamData, long progress) {
        return Long.toUnsignedString(progress);
    }


    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("leaderboard", this.leaderboard.toString());
        nbt.putLong("value", this.value);
    }


    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        this.leaderboard = ResourceLocation.tryParse(nbt.getString("leaderboard"));
        this.value = nbt.getLong("value");
    }


    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(this.leaderboard);
        buffer.writeLong(this.value);
    }


    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        this.leaderboard = buffer.readResourceLocation();
        this.value = buffer.readVarInt();
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
        config.addLong("value", this.value, v -> this.value = v, 1, -2147483647, 2147483647);
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

        if (leaderBoard == null) {
            return;
        }

        long serverTotal = getOrCalculateServerTotal(player.getServer(), leaderBoard);

        teamData.setProgress(this, serverTotal);
    }

    private long getOrCalculateServerTotal(MinecraftServer server, Leaderboard leaderboard) {
        long now = System.currentTimeMillis();
        if (now - lastCalcTime < CACHE_DURATION && cachedServerTotal != -1) {
            return cachedServerTotal;
        }

        synchronized (CALCULATION_LOCK) {
            if (now - lastCalcTime < CACHE_DURATION && cachedServerTotal != -1) {
                return cachedServerTotal;
            }

            long total = 0;

            for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
                total += ScoreUtil.getPlayerScore(leaderboard, new PlayerStatsWrapper(onlinePlayer));
            }

            for (UUID uuid : PlayerDataTracker.get(server.overworld()).getAllPlayerUUIDs()) {
                if (server.getPlayerList().getPlayer(uuid) != null) {
                    continue;
                }

                GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
                if (profile != null) {
                    ServerStatsCounter stats = ScoreUtil.loadPlayerStats(server, uuid);
                    total += ScoreUtil.getPlayerScore(leaderboard, new PlayerStatsWrapper(uuid, profile, stats, server));
                }
            }

            cachedServerTotal = total;
            lastCalcTime = System.currentTimeMillis();
        }

        return cachedServerTotal;
    }
}
