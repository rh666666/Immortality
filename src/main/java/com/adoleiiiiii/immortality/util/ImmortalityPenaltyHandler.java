package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 不屈 buff 结束时的生命上限惩罚逻辑。
 */
public final class ImmortalityPenaltyHandler {

    private ImmortalityPenaltyHandler() {
    }

    /**
     * 在不屈 buff 获得时初始化会话：清除残留惩罚，新开 buff 时重置死亡计数。
     */
    public static void handleEffectApplied(Player player) {
        if (player.level().isClientSide || !(player instanceof ImmortalityPlayerAccess access)) {
            return;
        }

        access.immortality$setEffectEndSettled(false);
        access.immortality$clearMaxHealthPenalty();

        if (!access.immortality$isBuffSessionActive()) {
            access.immortality$setDeathCount(0);
        }

        access.immortality$setBuffSessionActive(true);
    }

    /**
     * 在不屈效果被移除时尝试结算生命上限惩罚。
     */
    public static void handleEffectEnd(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        if (!(player instanceof ImmortalityPlayerAccess access)) {
            return;
        }

        if (access.immortality$isEffectEndSettled() || access.immortality$isRefreshingBuff()) {
            return;
        }

        int deathCount = access.immortality$getDeathCount();

        access.immortality$clearMaxHealthPenalty();
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        double baseMax = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : player.getMaxHealth();

        access.immortality$setEffectEndSettled(true);
        access.immortality$setBuffSessionActive(false);
        access.immortality$setDeathCount(0);

        if (deathCount <= 0) {
            return;
        }

        if (baseMax - deathCount <= 0.0) {
            access.immortality$applyMaxHealthPenalty(deathCount);
            killFromBurnOutPenalty(player);
            return;
        }

        access.immortality$applyMaxHealthPenalty(deathCount);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    /**
     * 以「燃尽」伤害类型击杀玩家（死亡信息：%1$s燃尽了）。
     * 结算前已设置 isEffectEndSettled，免死 mixin 不会再次触发。
     */
    private static void killFromBurnOutPenalty(Player player) {
        Holder<DamageType> burnOut = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ImmortalityDamageTypes.BURN_OUT);
        player.hurt(new DamageSource(burnOut), Float.MAX_VALUE);
    }
}
