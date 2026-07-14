package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 不屈图腾触发时的状态效果处理工具。
 */
public final class ImmortalityEffectHelper {

    private ImmortalityEffectHelper() {
    }

    /**
     * 仅移除负面状态效果，保留不屈等增益效果。
     */
    public static void clearHarmfulStatusEffects(Player player) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                toRemove.add(effect);
            }
        }
        for (Holder<MobEffect> effect : toRemove) {
            player.removeEffect(effect);
        }
    }

    /**
     * 图腾免死后将生命恢复至当前真实上限。
     */
    public static void restoreHealthToMax(Player player) {
        if (player instanceof ImmortalityPlayerAccess access) {
            access.immortality$clearMaxHealthPenalty();
        }
        float maxHealth = player.getMaxHealth();
        player.setHealth(maxHealth);
    }
}
