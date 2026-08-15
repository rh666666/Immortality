package com.adoleiiiiii.immortality.asm;

import com.adoleiiiiii.immortality.player.TruePowerVictimAccess;

/**
 * ASM 运行时钩子：致命标记下改写读血 / 存活返回值；禁止再入 {@code getHealth}/{@code isAlive}。
 */
public final class TruePowerAsmHooks {

	private TruePowerAsmHooks() {
	}

	/**
	 * {@code getHealth} 返回钩子。
	 *
	 * @param health 原返回值
	 * @param self   实体实例
	 * @return 致命时为 {@code 0}，否则为原值
	 */
	public static float afterGetHealth(float health, Object self) {
		if (isLethal(self)) {
			return 0.0F;
		}
		return health;
	}

	/**
	 * {@code isAlive} 返回钩子。
	 *
	 * @param alive 原返回值
	 * @param self  实体实例
	 * @return 致命时为 {@code false}，否则为原值
	 */
	public static boolean afterIsAlive(boolean alive, Object self) {
		if (isLethal(self)) {
			return false;
		}
		return alive;
	}

	/**
	 * {@code isDeadOrDying} 返回钩子。
	 *
	 * @param deadOrDying 原返回值
	 * @param self        实体实例
	 * @return 致命时为 {@code true}，否则为原值
	 */
	public static boolean afterIsDeadOrDying(boolean deadOrDying, Object self) {
		if (isLethal(self)) {
			return true;
		}
		return deadOrDying;
	}

	private static boolean isLethal(Object self) {
		return self instanceof TruePowerVictimAccess victim && victim.immortality$isTruePowerLethal();
	}
}
