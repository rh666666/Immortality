package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.mixin.LivingEntityAccessor;
import com.adoleiiiiii.immortality.mixin.SynchedEntityDataAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;

/**
 * 通用清理：抹掉玩家同步数据里会把读血压到空血的「荒谬负向浮点」。
 * <p>
 * 不识别具体模组；只按数值形态处理（非有限，或绝对值不低于最大生命）。
 * 原版 {@code DATA_HEALTH_ID} 永不改写。
 */
public final class ImmortalityHealthDataSanitizer {

	private ImmortalityHealthDataSanitizer() {
	}

	/**
	 * 清除荒谬负向浮点同步项，使后续读血改写无法再靠偏移把正 DATA 压成空血。
	 *
	 * @param player 玩家
	 * @return 是否改写过至少一项
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static boolean clearAbsurdFloatOffsets(Player player) {
		if (player == null) {
			return false;
		}
		SynchedEntityData data = player.getEntityData();
		if (!(data instanceof SynchedEntityDataAccessor accessor)) {
			return false;
		}
		EntityDataAccessor<Float> healthId = LivingEntityAccessor.immortality$getHealthDataId();
		float max = player.getMaxHealth();
		float absurdThreshold = max > 0.0F ? max : 20.0F;
		boolean changed = false;
		Int2ObjectMap<SynchedEntityData.DataItem<?>> items = accessor.immortality$getItemsById();
		for (SynchedEntityData.DataItem<?> item : items.values()) {
			Object value = item.getValue();
			if (!(value instanceof Float f)) {
				continue;
			}
			EntityDataAccessor key = item.getAccessor();
			if (key.equals(healthId)) {
				continue;
			}
			if (!isAbsurdHealthOffset(f, absurdThreshold)) {
				continue;
			}
			data.set(key, 0.0F);
			changed = true;
		}
		return changed;
	}

	/**
	 * 是否为会毒害读血的荒谬偏移。
	 *
	 * @param value     同步浮点
	 * @param threshold 通常为最大生命
	 * @return 应清除时为 true
	 */
	private static boolean isAbsurdHealthOffset(float value, float threshold) {
		if (!Float.isFinite(value)) {
			return true;
		}
		return value <= -threshold;
	}
}
