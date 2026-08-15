package com.adoleiiiiii.immortality.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 合法访问 {@link LivingEntity} 死亡流程与生命值同步字段。
 * <p>
 * 供真正的力量旁路击杀在 {@code die()} 被重写拦截时补完死亡态，
 * 并在 {@code setHealth} 被其它模组限幅时直写生命值数据。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

	/**
	 * 读取实体是否已进入死亡态（原版 {@code dead} 字段）。
	 *
	 * @return 若为 true，表示死亡流程已启动
	 */
	@Accessor("dead")
	boolean immortality$isDead();

	/**
	 * 设置实体死亡态标记。
	 * <p>
	 * 副作用：为 true 后实体将走 {@code tickDeath}，不再按存活逻辑处理。
	 *
	 * @param dead 是否标记为已死亡
	 */
	@Accessor("dead")
	void immortality$setDead(boolean dead);

	/**
	 * 生命值同步数据键（原版 {@code DATA_HEALTH_ID}）。
	 *
	 * @return 生命值 {@link EntityDataAccessor}
	 */
	@Accessor("DATA_HEALTH_ID")
	static EntityDataAccessor<Float> immortality$getHealthDataId() {
		throw new AssertionError();
	}

	/**
	 * 读取死亡动画计时（原版 {@code deathTime}）。
	 *
	 * @return 死亡动画已进行的 tick 数
	 */
	@Accessor("deathTime")
	int immortality$getDeathTime();

	/**
	 * 设置死亡动画计时。
	 *
	 * @param deathTime 死亡动画 tick；不屈强制存活时应置 0
	 */
	@Accessor("deathTime")
	void immortality$setDeathTime(int deathTime);

	/**
	 * 调用原版掉落结算（经验、装备、自定义掉落等）。
	 * <p>
	 * 调用前提：通常在服务端且已确定本次击杀应产生掉落时调用。
	 *
	 * @param source 致死伤害来源
	 */
	@Invoker("dropAllDeathLoot")
	void immortality$dropAllDeathLoot(DamageSource source);
}
