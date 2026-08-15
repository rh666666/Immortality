package com.adoleiiiiii.immortality.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 访问 {@link SynchedEntityData} 私有条目，以便绕过对 {@code set} 的限幅注入、直写生命值。
 */
@Mixin(SynchedEntityData.class)
public interface SynchedEntityDataAccessor {

	/**
	 * 取得指定键对应的数据项。
	 *
	 * @param key 同步数据键
	 * @param <T> 值类型
	 * @return 数据项
	 */
	@Invoker("getItem")
	<T> SynchedEntityData.DataItem<T> immortality$callGetItem(EntityDataAccessor<T> key);

	/**
	 * 标记整表为脏，以便向客户端同步。
	 *
	 * @param dirty 是否脏
	 */
	@Accessor("isDirty")
	void immortality$setDirty(boolean dirty);
}
