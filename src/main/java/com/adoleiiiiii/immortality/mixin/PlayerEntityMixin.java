package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityEffectHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

/**
 * 拦截图腾免死逻辑：拥有不屈 buff 时触发与图腾等效的免死效果。
 */
@Mixin(LivingEntity.class)
public class PlayerEntityMixin {

	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void onCheckTotemDeathProtection(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;

		if (!(entity instanceof Player player)) {
			return;
		}

		if (player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			if (player instanceof ImmortalityPlayerAccess access) {
				// 效果结束结算或 buff 刷新过程中不应再次触发免死，避免与 removeAllEffects 等批量移除冲突
				if (access.immortality$isEffectEndSettled() || access.immortality$isRefreshingBuff()) {
					return;
				}
			}
			if (!player.level().isClientSide) {
				triggerImmortalityEffect(player);
			}
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

	@Unique
	private void triggerImmortalityEffect(Player player) {
		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) player;
		access.immortality$incrementDeathCount();

		MobEffectInstance immortalityEffect = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : -1;

		MobEffectInstance strengthEffect = player.getEffect(MobEffects.DAMAGE_BOOST);
		int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier() : -1;

		access.immortality$setRefreshingBuff(true);
		try {
			ImmortalityEffectHelper.clearHarmfulStatusEffects(player);
		} finally {
			access.immortality$setRefreshingBuff(false);
		}

		if (!player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			player.addEffect(new MobEffectInstance(
					ModEffects.IMMORTALITY_EFFECT, remainingDuration, 0, false, false, true));
		}

		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45 * TICKS_PER_SECOND, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * TICKS_PER_SECOND, 1));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40 * TICKS_PER_SECOND, 0));
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3 * TICKS_PER_MINUTE, 0));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, TICKS_PER_MINUTE, strengthAmplifier + 2));

		ImmortalityEffectHelper.restoreHealthToMax(player);

		Level level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

		player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));

		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, new ItemStack(Items.TOTEM_OF_UNDYING));
		}

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
					ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(),
					100, 0.5, 0.5, 0.5, 0.5
			);
		}
	}
}
