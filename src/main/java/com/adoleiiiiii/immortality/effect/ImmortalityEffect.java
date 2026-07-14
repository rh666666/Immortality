package com.adoleiiiiii.immortality.effect;

import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 不屈状态效果：buff 期间抵抗死亡并获得叠乘减伤与击退抗性，结束时按死亡次数扣除生命上限。
 * <p>
 * 效果结束回调见 {@link com.adoleiiiiii.immortality.Immortality#onMobEffectRemoved(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove)}
 */
public class ImmortalityEffect extends MobEffect {

    public ImmortalityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // 不屈效果是被动触发的，不需要每 tick 更新
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        return true;
    }

    @Override
    public void onEffectAdded(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectAdded(entity, amplifier);

        if (entity instanceof Player player) {
            ImmortalityPenaltyHandler.handleEffectApplied(player);
        }
    }
}
