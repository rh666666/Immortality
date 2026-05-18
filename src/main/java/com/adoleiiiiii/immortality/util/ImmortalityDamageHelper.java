package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.config.ImmortalityConfig;

/**
 * 不屈效果的伤害减免计算工具。
 * <p>
 * 减伤比例 {@code R = 1 - 1/(1 + k·D/H_max)}，系数 {@code k} 见 {@link ImmortalityConfig}。
 * 反解公式 {@code k = H_max·R / (D·(1-R))} 见 {@code config/immortality.yml} 注释。
 */
public final class ImmortalityDamageHelper {

	private ImmortalityDamageHelper() {
	}

	/**
	 * 根据 buff 期间死亡次数与当前最大生命值，计算减伤比例。
	 *
	 * @param deathCount buff 期间累计的死亡次数
	 * @param maxHealth  玩家当前最大生命值
	 * @return 减伤比例 R，范围 [0, 1)
	 */
	public static float computeDamageReduction(int deathCount, float maxHealth) {
		if (deathCount <= 0 || maxHealth <= 0.0f) {
			return 0.0f;
		}
		float k = ImmortalityConfig.getDamageReductionK();
		float ratio = k * deathCount / maxHealth;
		return 1.0f - 1.0f / (1.0f + ratio);
	}

	/**
	 * 计算实际受到的伤害倍率（1 - R）。
	 *
	 * @param deathCount buff 期间累计的死亡次数
	 * @param maxHealth  玩家当前最大生命值
	 * @return 伤害倍率，范围 (0, 1]
	 */
	public static float computeDamageMultiplier(int deathCount, float maxHealth) {
		return 1.0f - computeDamageReduction(deathCount, maxHealth);
	}
}
