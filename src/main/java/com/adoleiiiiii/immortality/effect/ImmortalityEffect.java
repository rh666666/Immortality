package com.adoleiiiiii.immortality.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;

public class ImmortalityEffect extends StatusEffect {

    public ImmortalityEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        // 不屈效果是被动触发的，不需要每tick更新
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);

        // 效果结束时，如果是玩家则恢复血量至上限
        if (entity instanceof PlayerEntity player) {
            float maxHealth = player.getMaxHealth();
            player.setHealth(maxHealth);
        }
    }
}
