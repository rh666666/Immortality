package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
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

		StatusEffectInstance immortalityEffect = player.getStatusEffect(ModEffects.IMMORTALITY);
		int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : -1;

		StatusEffectInstance strengthEffect = player.getStatusEffect(StatusEffects.STRENGTH);
		int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier() : -1;

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
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, TICKS_PER_MINUTE, strengthAmplifier + 2));

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
}
