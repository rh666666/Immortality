package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.handler.TruePowerHandler;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityEffectHelper;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

/**
 * 拦截图腾免死逻辑：拥有不屈 buff 时触发与图腾等效的免死效果。
 */
@Mixin(LivingEntity.class)
public class PlayerEntityMixin {

	@Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
	private void onTryUseTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;

		if (!(entity instanceof PlayerEntity player)) {
			return;
		}

		if (player.hasStatusEffect(ModEffects.IMMORTALITY)) {
			if (player instanceof ImmortalityPlayerAccess access) {
				// 效果结束结算或 buff 刷新过程中不应再次触发免死，避免与 removeAllEffects 等批量移除冲突
				if (access.immortality$isEffectEndSettled() || access.immortality$isRefreshingBuff()) {
					return;
				}
			}
			if (!player.getWorld().isClient) {
				triggerImmortalityEffect(player);
			}
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

	@Unique
	private void triggerImmortalityEffect(PlayerEntity player) {
		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) player;
		access.immortality$incrementDeathCount();

		// 单次不屈会话中死亡 3 次时授予「愈战愈勇」进度。
		if (access.immortality$getDeathCount() == 3 && player instanceof ServerPlayerEntity sp) {
			ModAdvancements.grantEverStronger(sp);
		}

		StatusEffectInstance immortalityEffect = player.getStatusEffect(ModEffects.IMMORTALITY);
		int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : -1;

		StatusEffectInstance strengthEffect = player.getStatusEffect(StatusEffects.STRENGTH);
		int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier() : -1;
		int immortalityAmplifier = immortalityEffect != null ? immortalityEffect.getAmplifier() : 0;

		access.immortality$setRefreshingBuff(true);
		try {
			ImmortalityEffectHelper.clearHarmfulStatusEffects(player);
		} finally {
			access.immortality$setRefreshingBuff(false);
		}

		if (!player.hasStatusEffect(ModEffects.IMMORTALITY)) {
			player.addStatusEffect(new StatusEffectInstance(
					ModEffects.IMMORTALITY, remainingDuration, 0, false, false, true));
		}

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 45 * TICKS_PER_SECOND, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 5 * TICKS_PER_SECOND, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40 * TICKS_PER_SECOND, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 3 * TICKS_PER_MINUTE, 0));

		// 力量等级 = 原力量代码值 + 不屈代码值 * 2 + 2
		StatusEffectInstance currentImmortality = player.getStatusEffect(ModEffects.IMMORTALITY);
		int immDuration = currentImmortality != null ? currentImmortality.getDuration() : TICKS_PER_MINUTE;
		if (player.hasStatusEffect(ModEffects.TRUE_POWER)) {
			player.removeStatusEffect(StatusEffects.STRENGTH);
			player.addStatusEffect(new StatusEffectInstance(
					ModEffects.TRUE_POWER, immDuration, 0, false, false, true));
		} else {
			int newStrengthAmp = strengthAmplifier + immortalityAmplifier * 2 + 2;
			if (newStrengthAmp >= 39) {
				player.removeStatusEffect(StatusEffects.STRENGTH);
				player.addStatusEffect(new StatusEffectInstance(
						ModEffects.TRUE_POWER, immDuration, 0, false, false, true));
			} else {
				player.addStatusEffect(new StatusEffectInstance(
						StatusEffects.STRENGTH, TICKS_PER_MINUTE, newStrengthAmp));
			}
		}

		// 首次获得「真正的力量」时授予进度。
		if (player.hasStatusEffect(ModEffects.TRUE_POWER) && player instanceof ServerPlayerEntity sp) {
			ModAdvancements.grantTruePower(sp);
		}

		ImmortalityEffectHelper.restoreHealthToMax(player);

		World world = player.getWorld();
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

		player.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));

		if (player instanceof ServerPlayerEntity serverPlayer) {
			Criteria.USED_TOTEM.trigger(serverPlayer, new ItemStack(Items.TOTEM_OF_UNDYING));
		}

		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(
					net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + player.getHeight() / 2.0, player.getZ(),
					100, 0.5, 0.5, 0.5, 0.5
			);
		}
	}

	/**
	 * 每 tick 处理 TRUE_POWER 待击杀目标的多层击杀流程。
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof PlayerEntity player) {
			TruePowerHandler.processPending(player);
		}
	}

	/**
	 * 食用不死图腾时叠加不屈等级与时长。
	 * <p>
	 * 每吃一个，不屈等级 +1（最高 VIII 级），时长按等比数列递减。
	 * TRUE_POWER 同步刷新时长。
	 */
	@Inject(method = "consumeItem", at = @At("HEAD"))
	private void onConsumeItem(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!(entity instanceof PlayerEntity player)) return;
		if (player.getWorld().isClient) return;
		ItemStack activeStack = player.getActiveItem();
		if (!activeStack.isOf(Items.TOTEM_OF_UNDYING)) return;
		if (!(player instanceof ImmortalityPlayerAccess access)) return;

		StatusEffectInstance current = player.getStatusEffect(ModEffects.IMMORTALITY);
		int curAmp = current != null ? current.getAmplifier() : -1;
		int oldDuration = current != null ? current.getDuration() : 0;

		int addDuration = (int) Math.ceil(6000.0 / Math.pow(2, curAmp + 1));
		int newAmp = Math.min(curAmp + 1, 7);

		access.immortality$setRefreshingBuff(true);
		try {
			player.removeStatusEffect(ModEffects.IMMORTALITY);
			player.addStatusEffect(new StatusEffectInstance(
					ModEffects.IMMORTALITY, oldDuration + addDuration, newAmp, false, false, true));
			if (player.hasStatusEffect(ModEffects.TRUE_POWER)) {
				player.addStatusEffect(new StatusEffectInstance(
						ModEffects.TRUE_POWER, oldDuration + addDuration, 0, false, false, true));
			}
		} finally {
			access.immortality$setRefreshingBuff(false);
		}
	}
}
