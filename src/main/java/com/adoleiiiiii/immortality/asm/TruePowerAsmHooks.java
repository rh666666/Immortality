package com.adoleiiiiii.immortality.asm;

import com.adoleiiiiii.immortality.mixin.LivingEntityAccessor;
import com.adoleiiiiii.immortality.player.TruePowerVictimAccess;
import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityHealthDataSanitizer;
import com.adoleiiiiii.immortality.util.ImmortalityHealthProbe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * ASM 运行时钩子：在其它模组读血/存活改写之后执行。
 * <p>
 * 真正的力量致命标记下强制死亡语义；不屈闸门下强制存活读血/存活语义。
 */
public final class TruePowerAsmHooks {

	private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private TruePowerAsmHooks() {
	}

	/**
	 * {@code getHealth} 返回钩子（应尽量位于其它读血改写之后）。
	 *
	 * @param health 原返回值
	 * @param self   实体实例
	 * @return 致命为 {@code 0}；不屈期间无效读血回退 DATA/{@code 1}；否则原值
	 */
	public static float afterGetHealth(float health, Object self) {
		if (Boolean.TRUE.equals(REENTRY.get()) || ImmortalityHealthProbe.isProbing()) {
			return health;
		}
		if (isLethal(self)) {
			return 0.0F;
		}
		if (!shouldVoidDeath(self)) {
			return health;
		}
		if (self instanceof Player player) {
			ImmortalityHealthDataSanitizer.clearAbsurdFloatOffsets(player);
		}
		if (health > 0.0F && !Float.isNaN(health)) {
			return health;
		}
		REENTRY.set(Boolean.TRUE);
		try {
			return readDataHealth(self);
		} finally {
			REENTRY.set(Boolean.FALSE);
		}
	}

	/**
	 * {@code isAlive} 返回钩子。
	 *
	 * @param alive 原返回值
	 * @param self  实体实例
	 * @return 致命为 {@code false}；不屈且未移除为 {@code true}；否则原值
	 */
	public static boolean afterIsAlive(boolean alive, Object self) {
		if (Boolean.TRUE.equals(REENTRY.get()) || ImmortalityHealthProbe.isProbing()) {
			return alive;
		}
		if (isLethal(self)) {
			return false;
		}
		if (!shouldVoidDeath(self)) {
			return alive;
		}
		if (self instanceof LivingEntity living && living.isRemoved()) {
			return false;
		}
		return true;
	}

	/**
	 * {@code isDeadOrDying} 返回钩子。
	 *
	 * @param deadOrDying 原返回值
	 * @param self        实体实例
	 * @return 致命为 {@code true}；不屈期间为 {@code false}；否则原值
	 */
	public static boolean afterIsDeadOrDying(boolean deadOrDying, Object self) {
		if (Boolean.TRUE.equals(REENTRY.get()) || ImmortalityHealthProbe.isProbing()) {
			return deadOrDying;
		}
		if (isLethal(self)) {
			return true;
		}
		if (shouldVoidDeath(self)) {
			return false;
		}
		return deadOrDying;
	}

	private static boolean isLethal(Object self) {
		return self instanceof TruePowerVictimAccess victim && victim.immortality$isTruePowerLethal();
	}

	private static boolean shouldVoidDeath(Object self) {
		return self instanceof Player player && ImmortalityDeathGate.shouldVoidDeath(player);
	}

	/**
	 * 直读 DATA 血量，不调用 {@code getHealth()}。
	 *
	 * @param self 实体
	 * @return 正血量，缺失时为 {@code 1}
	 */
	private static float readDataHealth(Object self) {
		if (!(self instanceof LivingEntity living)) {
			return 1.0F;
		}
		float data = living.getEntityData().get(LivingEntityAccessor.immortality$getHealthDataId());
		if (data > 0.0F && !Float.isNaN(data)) {
			return data;
		}
		float max = living.getMaxHealth();
		return max > 0.0F ? max : 1.0F;
	}
}
