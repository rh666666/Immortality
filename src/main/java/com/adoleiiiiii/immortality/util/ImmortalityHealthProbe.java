package com.adoleiiiiii.immortality.util;

/**
 * 读血探针：探测期间跳过不屈对读血/存活的掩盖，以便观察未改写语义（如死亡界面判定）。
 */
public final class ImmortalityHealthProbe {

	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	private ImmortalityHealthProbe() {
	}

	/**
	 * 当前是否处于探针读血（应跳过不屈 {@code getHealth} 回写）。
	 *
	 * @return 探测中为 true
	 */
	public static boolean isProbing() {
		return DEPTH.get() > 0;
	}

	/**
	 * 进入一层探针作用域。
	 */
	public static void push() {
		DEPTH.set(DEPTH.get() + 1);
	}

	/**
	 * 退出一层探针作用域。
	 */
	public static void pop() {
		int depth = DEPTH.get();
		if (depth <= 1) {
			DEPTH.remove();
		} else {
			DEPTH.set(depth - 1);
		}
	}
}
