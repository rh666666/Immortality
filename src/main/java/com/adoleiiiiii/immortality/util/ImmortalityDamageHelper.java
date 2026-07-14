package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.ImmortalityConfig;

/**
 * 不屈效果的伤害减免计算工具。
 * <p>
 * 减伤比例 R = 1 - 1/(1 + k·D/H_max)，系数 k 见 {@link ImmortalityConfig}。
 */
public final class ImmortalityDamageHelper {

    private ImmortalityDamageHelper() {
    }

    /**
     * 根据 buff 期间死亡次数与当前最大生命值，计算减伤比例。
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
     */
    public static float computeDamageMultiplier(int deathCount, float maxHealth) {
        return 1.0f - computeDamageReduction(deathCount, maxHealth);
    }

    /**
     * 根据减伤比例计算击退抗性：与减伤比例 R 相同，上限 1。
     */
    public static float computeKnockbackResistance(int deathCount, float maxHealth) {
        return Math.min(1.0f, computeDamageReduction(deathCount, maxHealth));
    }
}
