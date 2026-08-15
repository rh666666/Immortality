package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.world.entity.player.Player;

/**
 * 不屈效果期间的死亡链路闸门：为 true 时原版死亡相关入口应直接返空/取消。
 */
public final class ImmortalityDeathGate {

	private ImmortalityDeathGate() {
	}

	/**
	 * 当前是否应废除原版死亡链路（持有不屈且未进入效果结束结算）。
	 *
	 * @param player 玩家
	 * @return 应返空死亡链路时为 true
	 */
	public static boolean shouldVoidDeath(Player player) {
		if (!player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			return false;
		}
		if (player instanceof ImmortalityPlayerAccess access) {
			return !access.immortality$isEffectEndSettled() && !access.immortality$isRefreshingBuff();
		}
		return true;
	}
}
