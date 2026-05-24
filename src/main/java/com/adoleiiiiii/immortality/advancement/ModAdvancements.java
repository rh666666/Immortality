package com.adoleiiiiii.immortality.advancement;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * 模组进度（成就）授予工具。
 */
public final class ModAdvancements {

	/** 进度 ID：燃尽此身（「超越生死」的子进度）。 */
	public static final ResourceLocation BURN_OUT_BODY = new ResourceLocation(Immortality.MOD_ID, "burn_out_body");

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
		Advancement advancement = server.getAdvancements().getAdvancement(BURN_OUT_BODY);
		if (advancement != null) {
			player.getAdvancements().award(advancement, BURN_OUT_BODY_CRITERION);
		}
	}
}
