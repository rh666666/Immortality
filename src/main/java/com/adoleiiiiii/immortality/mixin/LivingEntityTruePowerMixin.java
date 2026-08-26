package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.player.TruePowerVictimAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 挂载真正的力量致命态；压制期内取消 {@code heal}，不介入 {@code setHealth}。 */
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

	/** 递减压制计时。 */
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
	}
}
