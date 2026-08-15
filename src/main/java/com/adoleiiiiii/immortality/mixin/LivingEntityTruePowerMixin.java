package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.player.TruePowerVictimAccess;
import com.adoleiiiiii.immortality.util.TruePowerKillHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 挂载真正的力量压制 / 致命态，压制期内禁止抬血。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTruePowerMixin implements TruePowerVictimAccess {

	/** 压制剩余 tick。 */
	@Unique
	private int immortality$truePowerSuppressTicks;

	/** 致命标记；为 true 时 ASM 改写存活语义。 */
	@Unique
	private boolean immortality$truePowerLethal;

	@Override
	public void immortality$beginTruePowerSuppress(int ticks) {
		if (ticks <= 0) {
			this.immortality$truePowerSuppressTicks = 0;
			this.immortality$truePowerLethal = false;
			return;
		}
		this.immortality$truePowerSuppressTicks = Math.max(this.immortality$truePowerSuppressTicks, ticks);
	}

	@Override
	public void immortality$beginTruePowerLethal(int ticks) {
		this.immortality$truePowerLethal = true;
		this.immortality$beginTruePowerSuppress(ticks);
	}

	@Override
	public boolean immortality$isTruePowerSuppressing() {
		return this.immortality$truePowerSuppressTicks > 0;
	}

	@Override
	public boolean immortality$isTruePowerLethal() {
		return this.immortality$truePowerLethal;
	}

	@Override
	public int immortality$getTruePowerSuppressTicks() {
		return this.immortality$truePowerSuppressTicks;
	}

	@Override
	public void immortality$tickTruePowerSuppress() {
		if (this.immortality$truePowerSuppressTicks > 0) {
			this.immortality$truePowerSuppressTicks--;
			if (this.immortality$truePowerSuppressTicks <= 0) {
				this.immortality$truePowerLethal = false;
			}
		}
	}

	/** 压制期间取消 {@code heal}。 */
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void immortality$cancelHealWhileSuppressed(float amount, CallbackInfo ci) {
		if (this.immortality$isTruePowerSuppressing()) {
			ci.cancel();
		}
	}

	/**
	 * 压制期间禁止抬血；生命语义已 ≤0 时钉在 0。
	 *
	 * @param health 即将写入的生命值
	 * @return 约束后的生命值
	 */
	@ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
	private float immortality$clampHealthWhileSuppressed(float health) {
		if (!this.immortality$isTruePowerSuppressing()) {
			return health;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		float current = self.getHealth();
		if (current <= 0.0F) {
			return 0.0F;
		}
		if (health > current) {
			return current;
		}
		return health;
	}

	/** 递减压制；致命且未 {@code dead} 时维持钉血。 */
	@Inject(method = "baseTick", at = @At("TAIL"))
	private void immortality$tickTruePowerSuppressState(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide) {
			return;
		}
		if (!this.immortality$isTruePowerSuppressing()) {
			return;
		}
		this.immortality$tickTruePowerSuppress();
		if (this.immortality$isTruePowerLethal() && !((LivingEntityAccessor) self).immortality$isDead()) {
			TruePowerKillHelper.forceZeroHealth(self);
		}
	}
}
