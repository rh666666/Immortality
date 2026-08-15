package com.adoleiiiiii.immortality.player;

/**
 * 真正的力量目标态：压制禁抬血；致命标记供 ASM 改写存活语义。
 * 由 Mixin 实现于 {@link net.minecraft.world.entity.LivingEntity}。
 */
public interface TruePowerVictimAccess {

	/**
	 * 开始或刷新压制窗口。
	 *
	 * @param ticks 持续 tick；非正数立即结束并清除致命标记
	 */
	void immortality$beginTruePowerSuppress(int ticks);

	/**
	 * 开启致命标记并刷新压制。
	 *
	 * @param ticks 压制持续 tick
	 */
	void immortality$beginTruePowerLethal(int ticks);

	/**
	 * @return 压制中时为 true
	 */
	boolean immortality$isTruePowerSuppressing();

	/**
	 * @return 致命标记开启时为 true
	 */
	boolean immortality$isTruePowerLethal();

	/**
	 * @return 压制剩余 tick；未压制为 0
	 */
	int immortality$getTruePowerSuppressTicks();

	/** 递减压制；归零时清除致命标记。 */
	void immortality$tickTruePowerSuppress();
}
