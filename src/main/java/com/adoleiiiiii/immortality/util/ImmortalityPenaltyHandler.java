package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.damage.ImmortalityDamageTypes;
import com.adoleiiiiii.immortality.mixin.LivingEntityAccessor;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 不屈 buff 结束时的生命上限惩罚逻辑。
 */
public final class ImmortalityPenaltyHandler {

	private ImmortalityPenaltyHandler() {
	}

	/**
	 * 在不屈 buff 获得时初始化会话：清除残留惩罚，新开 buff 时重置死亡计数。
	 *
	 * @param player 玩家实体
	 */
	public static void handleEffectApplied(Player player) {
		if (player.level().isClientSide || !(player instanceof ImmortalityPlayerAccess access)) {
			return;
		}

		access.immortality$setEffectEndSettled(false);
		access.immortality$clearMaxHealthPenalty();
		access.immortality$setDeathResistLatched(false);

		if (!access.immortality$isBuffSessionActive()) {
			access.immortality$setDeathCount(0);
		}

		access.immortality$setBuffSessionActive(true);
	}

	/**
	 * 在不屈效果被移除时结算生命上限惩罚。
	 * <p>
	 * 先维持存活态再结算；若仍空血则走带播报的正式 {@code die}。
	 *
	 * @param player 玩家实体
	 */
	public static void handleEffectEnd(Player player) {
		if (player.level().isClientSide) {
			return;
		}

		if (!(player instanceof ImmortalityPlayerAccess access)) {
			return;
		}

		if (access.immortality$isEffectEndSettled() || access.immortality$isRefreshingBuff()) {
			return;
		}

		int deathCount = access.immortality$getDeathCount();

		// 结算前清荒谬偏移，避免效果刚结束时读血仍被压空而误判 / 假死残留
		ImmortalityHealthDataSanitizer.clearAbsurdFloatOffsets(player);
		ImmortalityTotemHelper.ensureSurviving(player);
		ImmortalityEffectHelper.restoreHealthToMax(player);
		ImmortalityTotemHelper.syncHealthPacket(player);

		access.immortality$clearMaxHealthPenalty();
		AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		double baseMax = maxHealthAttr != null ? maxHealthAttr.getBaseValue() : player.getMaxHealth();

		access.immortality$setEffectEndSettled(true);
		access.immortality$setBuffSessionActive(false);
		access.immortality$setDeathResistLatched(false);
		access.immortality$setDeathCount(0);

		if (deathCount <= 0) {
			ImmortalityEffectHelper.restoreHealthToMax(player);
			ensureAliveOrAnnounceDeath(player);
			return;
		}

		if (baseMax - deathCount <= 0.0) {
			access.immortality$applyMaxHealthPenalty(deathCount);
			killFromBurnOutPenalty(player);
			return;
		}

		access.immortality$applyMaxHealthPenalty(deathCount);
		ImmortalityEffectHelper.restoreHealthToMax(player);
		ImmortalityTotemHelper.syncHealthPacket(player);
		ensureAliveOrAnnounceDeath(player);
	}

	/**
	 * 以「燃尽」伤害类型击杀并保证死亡播报。
	 *
	 * @param player 玩家实体
	 */
	private static void killFromBurnOutPenalty(Player player) {
		Holder<DamageType> burnOut = player.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(ImmortalityDamageTypes.BURN_OUT);
		forceAnnounceableDeath(player, new DamageSource(burnOut));
	}

	/**
	 * 结算后若 DATA 血仍为空，则强制正式死亡播报。
	 * <p>
	 * 以同步 DATA 为准，避免依赖可能被改写的 {@code getHealth()} 返回值。
	 *
	 * @param player 玩家
	 */
	private static void ensureAliveOrAnnounceDeath(Player player) {
		float data = player.getEntityData().get(LivingEntityAccessor.immortality$getHealthDataId());
		if (data > 0.0F && !Float.isNaN(data)) {
			return;
		}
		Holder<DamageType> generic = player.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(DamageTypes.GENERIC);
		forceAnnounceableDeath(player, new DamageSource(generic));
	}

	/**
	 * 写入战斗记录并 {@code die}，保证聊天栏死亡信息。
	 *
	 * @param player 玩家
	 * @param source 死亡来源
	 */
	private static void forceAnnounceableDeath(Player player, DamageSource source) {
		player.getCombatTracker().recordDamage(source, Float.MAX_VALUE);
		player.setHealth(0.0F);
		player.die(source);
	}
}
