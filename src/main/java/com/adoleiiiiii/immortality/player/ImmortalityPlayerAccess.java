package com.adoleiiiiii.immortality.player;

/**
 * 存储不屈 buff 期间玩家专属状态（死亡计数、生命上限惩罚等）的访问接口。
 */
public interface ImmortalityPlayerAccess {

	/**
	 * 获取 buff 期间累计的死亡次数。
	 *
	 * @return 死亡次数
	 */
	int immortality$getDeathCount();

	/**
	 * 设置 buff 期间累计的死亡次数。
	 *
	 * @param deathCount 死亡次数
	 */
	void immortality$setDeathCount(int deathCount);

	/**
	 * 将 buff 期间死亡次数加一（不屈抵抗死亡时调用）。
	 */
	void immortality$incrementDeathCount();

	/**
	 * 施加生命上限扣除（效果结束时调用，复活后需清除）。
	 *
	 * @param penalty 扣除的生命上限点数
	 */
	void immortality$applyMaxHealthPenalty(float penalty);

	/**
	 * 清除生命上限扣除修饰符（玩家复活后调用）。
	 */
	void immortality$clearMaxHealthPenalty();

	/**
	 * 是否正在刷新不屈 buff（图腾免死流程中，避免误触发结束惩罚）。
	 *
	 * @return 若为 true，表示处于免死刷新流程
	 */
	boolean immortality$isRefreshingBuff();

	/**
	 * 设置是否正在刷新不屈 buff。
	 *
	 * @param refreshing 是否刷新中
	 */
	void immortality$setRefreshingBuff(boolean refreshing);

	/**
	 * 当前是否处于一次完整的不屈 buff 会话中（获得 buff 至效果结束）。
	 *
	 * @return 是否在 buff 会话中
	 */
	boolean immortality$isBuffSessionActive();

	/**
	 * 设置不屈 buff 会话状态。
	 *
	 * @param active 是否激活
	 */
	void immortality$setBuffSessionActive(boolean active);

	/**
	 * 本次 buff 结束惩罚是否已结算（防止 onRemoved 与 removeStatusEffect 重复结算）。
	 *
	 * @return 是否已结算
	 */
	boolean immortality$isEffectEndSettled();

	/**
	 * 设置本次 buff 结束惩罚是否已结算。
	 *
	 * @param settled 是否已结算
	 */
	void immortality$setEffectEndSettled(boolean settled);

	/**
	 * 不屈效果是否受保护（防止非自然移除）。
	 *
	 * @return true 表示受保护，不允许外部移除
	 */
	boolean immortality$isProtected();

	/**
	 * 设置不屈效果保护状态。
	 *
	 * @param protect 是否保护
	 */
	void immortality$setProtected(boolean protect);

	/**
	 * 获取不屈效果备份时长（用于恢复）。
	 *
	 * @return 时长（tick）
	 */
	int immortality$getImmortalityDuration();

	/**
	 * 设置不屈效果备份时长。
	 *
	 * @param duration 时长（tick）
	 */
	void immortality$setImmortalityDuration(int duration);
}
