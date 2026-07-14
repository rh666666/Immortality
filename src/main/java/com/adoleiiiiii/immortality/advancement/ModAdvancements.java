package com.adoleiiiiii.immortality.advancement;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * 模组进度（成就）授予工具。
 */
public final class ModAdvancements {

    /** 进度 ID：燃尽此身。 */
    public static final ResourceLocation BURN_OUT_BODY =
            ResourceLocation.fromNamespaceAndPath(Immortality.MODID, "burn_out_body");

    private static final String BURN_OUT_BODY_CRITERION = "burn_out_body";

    private ModAdvancements() {
    }

    /**
     * 授予「燃尽此身」进度（不屈 buff 惩罚致死时调用）。
     *
     * @param player 服务端玩家
     */
    public static void grantBurnOutBody(ServerPlayer player) {
        MinecraftServer server = Objects.requireNonNull(player.getServer());
        AdvancementHolder advancement = server.getAdvancements().get(BURN_OUT_BODY);
        if (advancement != null) {
            player.getAdvancements().award(advancement, BURN_OUT_BODY_CRITERION);
        }
    }
}
