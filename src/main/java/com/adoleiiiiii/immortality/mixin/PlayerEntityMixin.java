package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
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
import net.minecraft.world.effect.MobEffect;
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
 * 拦截图腾免死逻辑，并保护不屈效果不被非自然移除。
 */
@Mixin(LivingEntity.class)
public class PlayerEntityMixin {

	/** 不屈效果移除前的备份，用于 {@code removeAllEffects} 后恢复。 */
	@Unique
	private MobEffectInstance immortality$backup;

	/** TRUE_POWER 效果移除前的备份。 */
	@Unique
	private MobEffectInstance truePower$backup;

	/** 牛奶桶授权：饮用牛奶时撤销保护标记（其他模组的 cure 调用不受影响）。 */
	@Inject(method = "curePotionEffects", at = @At("HEAD"), remap = false)
	private void onCurePotionEffects(ItemStack curativeItem, CallbackInfoReturnable<Boolean> cir) {
		if (!curativeItem.is(Items.MILK_BUCKET)) return;
		if (((LivingEntity) (Object) this) instanceof Player player
				&& player instanceof ImmortalityPlayerAccess access) {
			access.immortality$setProtected(false);
		}
	}

	@Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
	private void onRemoveEffect(MobEffect type, CallbackInfoReturnable<Boolean> cir) {
		if (!((LivingEntity) (Object) this instanceof Player player)) return;
		// 不屈 → 非刷新中则拒绝移除。
		if (type == ModEffects.IMMORTALITY_EFFECT) {
			if (player instanceof ImmortalityPlayerAccess access && access.immortality$isRefreshingBuff()) return;
			cir.setReturnValue(true);
			cir.cancel();
			return;
		}
		// TRUE_POWER → 与不屈同生死，直接拒绝移除。
		if (type == ModEffects.TRUE_POWER_EFFECT) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

	@Inject(method = "removeAllEffects", at = @At("HEAD"))
	private void onRemoveAllEffectsHead(CallbackInfoReturnable<Boolean> cir) {
		if (((LivingEntity) (Object) this) instanceof Player player) {
			this.immortality$backup = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
			this.truePower$backup = player.getEffect(ModEffects.TRUE_POWER_EFFECT);
			// 防止 penalty 结算。
			if (player instanceof ImmortalityPlayerAccess access) {
				access.immortality$setRefreshingBuff(true);
			}
		}
	}

	@Inject(method = "removeAllEffects", at = @At("RETURN"))
	private void onRemoveAllEffectsReturn(CallbackInfoReturnable<Boolean> cir) {
		if (!(((LivingEntity) (Object) this) instanceof Player player)) return;
		// 恢复 refreshingBuff 状态。
		if (player instanceof ImmortalityPlayerAccess access) {
			access.immortality$setRefreshingBuff(false);
		}
		if (this.immortality$backup != null && !player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			player.addEffect(this.immortality$backup);
		}
		if (this.truePower$backup != null && !player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			player.addEffect(this.truePower$backup);
		}
		this.immortality$backup = null;
		this.truePower$backup = null;
	}

	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void onCheckTotemDeathProtection(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;

		if (!(entity instanceof Player player)) {
			return;
		}

		if (player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			if (player instanceof ImmortalityPlayerAccess access) {
				if (access.immortality$isEffectEndSettled() || access.immortality$isRefreshingBuff()) {
					return;
				}
			}
			if (!player.level().isClientSide) {
				immortality$trigger(player);
			}
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

	@Unique
	private void immortality$trigger(Player player) {
		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) player;
		access.immortality$incrementDeathCount();

		// 死亡计数达到 3 时授予「愈战愈勇」进度。
		if (access.immortality$getDeathCount() == 3 && player instanceof ServerPlayer sp) {
			ModAdvancements.grantEverStronger(sp);
		}

		MobEffectInstance immortalityEffect = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : -1;

		MobEffectInstance strengthEffect = player.getEffect(MobEffects.DAMAGE_BOOST);
		int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier() : -1;
		int immortalityAmplifier = immortalityEffect != null ? immortalityEffect.getAmplifier() : 0;

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
		MobEffectInstance currentImmortality = player.getEffect(ModEffects.IMMORTALITY_EFFECT);
		int immDuration = currentImmortality != null ? currentImmortality.getDuration() : TICKS_PER_MINUTE;
		if (player.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			player.removeEffect(MobEffects.DAMAGE_BOOST);
			player.addEffect(new MobEffectInstance(
					ModEffects.TRUE_POWER_EFFECT, immDuration, 0, false, false, true));
		} else {
			int newStrengthAmp = strengthAmplifier + immortalityAmplifier * 2 + 2;
			if (newStrengthAmp >= 39) {
				player.removeEffect(MobEffects.DAMAGE_BOOST);
				player.addEffect(new MobEffectInstance(
						ModEffects.TRUE_POWER_EFFECT, immDuration, 0, false, false, true));
			} else {
				player.addEffect(new MobEffectInstance(
						MobEffects.DAMAGE_BOOST, TICKS_PER_MINUTE, newStrengthAmp));
			}
		}

		// 首次获得「真正的力量」时授予进度。
		if (player.hasEffect(ModEffects.TRUE_POWER_EFFECT) && player instanceof ServerPlayer sp) {
			ModAdvancements.grantTruePower(sp);
		}

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
