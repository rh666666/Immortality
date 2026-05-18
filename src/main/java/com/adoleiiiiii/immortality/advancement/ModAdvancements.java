package com.adoleiiiiii.immortality.advancement;

import com.adoleiiiiii.immortality.Immortality;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * 模组进度（成就）授予工具。
 */
public final class ModAdvancements {

	/** 进度 ID：燃尽此身（「超越生死」的子进度）。 */
	public static final Identifier BURN_OUT_BODY = new Identifier(Immortality.MOD_ID, "burn_out_body");

	private static final String BURN_OUT_BODY_CRITERION = "burn_out_body";

	private ModAdvancements() {
	}

	/**
	 * 授予「燃尽此身」进度（不屈 buff 惩罚致死时调用）。
	 *
	 * @param player 服务端玩家
	 */
	public static void grantBurnOutBody(ServerPlayerEntity player) {
		MinecraftServer server = Objects.requireNonNull(player.getServer());
		Advancement advancement = server.getAdvancementLoader().get(BURN_OUT_BODY);
		if (advancement != null) {
			player.getAdvancementTracker().grantCriterion(advancement, BURN_OUT_BODY_CRITERION);
		}
	}
}
