package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

@Mixin(LivingEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void onTryUseTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 只对玩家生效
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        // 检查玩家是否拥有不屈buff
        if (player.hasStatusEffect(ModEffects.IMMORTALITY)) {
            // 触发不死图腾效果
            triggerImmortalityEffect(player, source);
            // 取消原方法执行，防止消耗不死图腾
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void triggerImmortalityEffect(PlayerEntity player, DamageSource source) {
        // 保存不屈buff的剩余时长
        StatusEffectInstance immortalityEffect = player.getStatusEffect(ModEffects.IMMORTALITY);
        int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : -1;

        // 保存力量buff的等级
        StatusEffectInstance strengthEffect = player.getStatusEffect(StatusEffects.STRENGTH);
        int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier(): -1;

        // 设置生命值为1
        player.setHealth(1.0f);

        // 清除所有负面效果
        player.clearStatusEffects();

        // 重新添加不屈buff，使用原来的剩余时长
        player.addStatusEffect(new StatusEffectInstance(ModEffects.IMMORTALITY, remainingDuration, 0, false, false, true));

        // 添加伤害吸收和生命恢复效果（与原不死图腾效果相同）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 45 * TICKS_PER_SECOND, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 5 * TICKS_PER_SECOND, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40 * TICKS_PER_SECOND, 0));

        // 再添加一点神奇的效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 3 * TICKS_PER_MINUTE, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, TICKS_PER_MINUTE, strengthAmplifier + 2));

        // 播放不死图腾使用音效和粒子效果
        World world = player.getWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // 触发统计和成就
        player.incrementStat(Stats.USED.getOrCreateStat(net.minecraft.item.Items.TOTEM_OF_UNDYING));

        if (player instanceof ServerPlayerEntity serverPlayer) {
            Criteria.USED_TOTEM.trigger(serverPlayer, new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING));
        }

        // 生成不死图腾粒子效果
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + player.getHeight() / 2.0, player.getZ(),
                    100, 0.5, 0.5, 0.5, 0.5
            );
        }
    }
}
