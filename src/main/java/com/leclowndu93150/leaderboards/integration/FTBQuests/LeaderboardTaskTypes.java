package com.leclowndu93150.leaderboards.integration.FTBQuests;

import com.leclowndu93150.leaderboards.Leaderboards;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.resources.ResourceLocation;

public interface LeaderboardTaskTypes {
    TaskType RANK_TASK = TaskTypes.register(new ResourceLocation(Leaderboards.MODID, "rank_task"), RankTask::new, () -> Icon.getIcon(new ResourceLocation("minecraft:item/iron_sword")));
    TaskType SERVER_ACHIEVE_STAT_TASK = TaskTypes.register(new ResourceLocation(Leaderboards.MODID, "server_achieve_stat"), ServerAchieveStatTask::new, () -> Icon.getIcon(new ResourceLocation("minecraft:item/diamond_sword")));
    static void init() {}
}