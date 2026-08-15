package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityHealthDataSanitizer;
import com.adoleiiiiii.immortality.util.ImmortalityHealthProbe;
import com.adoleiiiiii.immortality.util.ImmortalityTotemHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 不屈期间原版死亡链路全部返空：致死写入钳制、{@code die}/{@code tickDeath} 取消、
 * 存活判定强制为生，并保护效果不被非自然移除。
 */
@Mixin(value = LivingEntity.class, priority = 10000)
public class PlayerEntityMixin {

	/** 不屈效果移除前的备份，用于 {@code removeAllEffects} 后恢复。 */
	@Unique
	private MobEffectInstance immortality$backup;

	/** TRUE_POWER 效果移除前的备份。 */
	@Unique
	private MobEffectInstance truePower$backup;

	/**
	 * 不屈期间读血在入口直接返存活值，跳过可能被其它模组改写的方法体。
	 */
	@Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
	private void immortality$voidGetHealth(CallbackInfoReturnable<Float> cir) {
		if (ImmortalityHealthProbe.isProbing()) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		ImmortalityHealthDataSanitizer.clearAbsurdFloatOffsets(player);
		float real = self.getEntityData().get(LivingEntityAccessor.immortality$getHealthDataId());
		cir.setReturnValue(real > 0.0F && !Float.isNaN(real) ? real : 1.0F);
	}

	/**
	 * 致死 {@code setHealth} 写入返空（钳制为正值并触发图腾结算）。
	 */
	@ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
	private float immortality$clampSetHealth(float health) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return health;
		}
		return ImmortalityTotemHelper.clampHealthWhileImmortal(player, health);
	}

	/**
	 * {@code die} 返空，并做一次图腾式结算。
	 */
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
	private void immortality$voidDie(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		ci.cancel();
		ImmortalityTotemHelper.ensureSurviving(player);
		if (!player.level().isClientSide) {
			ImmortalityTotemHelper.tryTriggerLethalResist(player);
		}
	}

	/**
	 * {@code tickDeath} 返空。
	 */
	@Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
	private void immortality$voidTickDeath(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		ImmortalityTotemHelper.ensureSurviving(player);
		ci.cancel();
	}

	/**
	 * {@code isDeadOrDying} 在入口直接返 false。
	 */
	@Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
	private void immortality$voidDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		cir.setReturnValue(false);
	}

	/**
	 * {@code isAlive} 在入口直接返 true（未从世界剥离时）。
	 */
	@Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
	private void immortality$forceAlive(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		if (player.isRemoved()) {
			return;
		}
		cir.setReturnValue(true);
	}

	/**
	 * {@code isImmobile} 返空为 false。
	 */
	@Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true)
	private void immortality$voidImmobile(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		cir.setReturnValue(false);
	}

	/** 牛奶桶授权：饮用牛奶时撤销保护标记。 */
	@Inject(method = "curePotionEffects", at = @At("HEAD"), remap = false)
	private void onCurePotionEffects(ItemStack curativeItem, CallbackInfoReturnable<Boolean> cir) {
		if (!curativeItem.is(Items.MILK_BUCKET)) {
			return;
		}
		if (((LivingEntity) (Object) this) instanceof Player player
				&& player instanceof ImmortalityPlayerAccess access) {
			access.immortality$setProtected(false);
		}
	}

	@Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
	private void onRemoveEffect(MobEffect type, CallbackInfoReturnable<Boolean> cir) {
		if (!((LivingEntity) (Object) this instanceof Player player)) {
			return;
		}
		if (type == ModEffects.IMMORTALITY_EFFECT) {
			if (player instanceof ImmortalityPlayerAccess access && access.immortality$isRefreshingBuff()) {
				return;
			}
			cir.setReturnValue(true);
			cir.cancel();
			return;
		}
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
			if (player instanceof ImmortalityPlayerAccess access) {
				access.immortality$setRefreshingBuff(true);
			}
		}
	}

	@Inject(method = "removeAllEffects", at = @At("RETURN"))
	private void onRemoveAllEffectsReturn(CallbackInfoReturnable<Boolean> cir) {
		if (!(((LivingEntity) (Object) this) instanceof Player player)) {
			return;
		}
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

	/**
	 * {@code checkTotemDeathProtection} 直接视为成功并走不屈结算（原版图腾逻辑返空替换）。
	 */
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void onCheckTotemDeathProtection(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!(entity instanceof Player player) || !ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		if (!player.level().isClientSide) {
			ImmortalityTotemHelper.triggerFromTotemCheck(player);
		}
		cir.setReturnValue(true);
	}
}
