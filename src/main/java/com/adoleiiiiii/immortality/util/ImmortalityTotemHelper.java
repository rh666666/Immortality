package com.adoleiiiiii.immortality.util;

import com.adoleiiiiii.immortality.advancement.ModAdvancements;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.mixin.LivingEntityAccessor;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import static net.minecraft.SharedConstants.TICKS_PER_MINUTE;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

/**
 * 不屈期间原版死亡链路已全部返空后的图腾式结算（计数、回血、增益、粒子）。
 * <p>
 * 不重建玩家实体；存活语义由死亡闸门与读血/存活 Mixin 保证。
 */
public final class ImmortalityTotemHelper {

	private ImmortalityTotemHelper() {
	}

	/**
	 * 原版 {@code checkTotemDeathProtection} 路径：无视闩锁，每次正式免死均计数。
	 *
	 * @param player 持有不屈的玩家
	 */
	public static void triggerFromTotemCheck(Player player) {
		if (player.level().isClientSide) {
			return;
		}
		if (!ImmortalityDeathGate.shouldVoidDeath(player)) {
			return;
		}
		trigger(player);
	}

	/**
	 * {@code setHealth}/{@code die} 等致死尝试：短时闩锁内只维持满血；否则完整图腾结算并进入闩锁冷却。
	 *
	 * @param player 持有不屈的玩家
	 * @return 是否已执行完整图腾触发
	 */
	public static boolean tryTriggerLethalResist(Player player) {
		if (player.level().isClientSide) {
			return false;
		}
		if (!(player instanceof ImmortalityPlayerAccess access)) {
			return false;
		}
		if (!ImmortalityDeathGate.shouldVoidDeath(player)) {
			return false;
		}
		if (access.immortality$isDeathResistLatched()) {
			ensureSurviving(player);
			ImmortalityEffectHelper.restoreHealthToMax(player);
			syncHealthPacket(player);
			return false;
		}
		trigger(player);
		access.immortality$setDeathResistLatched(true);
		return true;
	}

	/**
	 * 执行一次完整图腾式免死（死亡计数 +1、清负面、回满血、音效粒子等）。
	 * <p>
	 * 读档阶段（{@code connection == null}）仅写安全 DATA 血量。
	 *
	 * @param player 玩家实体
	 */
	public static void trigger(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (serverPlayer.connection == null) {
			writeSafeHealthData(serverPlayer);
			return;
		}

		ImmortalityPlayerAccess access = (ImmortalityPlayerAccess) serverPlayer;
		access.immortality$incrementDeathCount();

		if (access.immortality$getDeathCount() == 3) {
			ModAdvancements.grantEverStronger(serverPlayer);
		}

		MobEffectInstance immortalityEffect = serverPlayer.getEffect(ModEffects.IMMORTALITY_EFFECT);
		int remainingDuration = immortalityEffect != null ? immortalityEffect.getDuration() : TICKS_PER_MINUTE;
		int immortalityAmplifier = immortalityEffect != null ? immortalityEffect.getAmplifier() : 0;

		MobEffectInstance strengthEffect = serverPlayer.getEffect(MobEffects.DAMAGE_BOOST);
		int strengthAmplifier = strengthEffect != null ? strengthEffect.getAmplifier() : -1;
		boolean hadTruePower = serverPlayer.hasEffect(ModEffects.TRUE_POWER_EFFECT);

		access.immortality$setRefreshingBuff(true);
		try {
			ImmortalityEffectHelper.clearHarmfulStatusEffects(serverPlayer);
		} finally {
			access.immortality$setRefreshingBuff(false);
		}

		access.immortality$setProtected(true);
		access.immortality$setImmortalityDuration(remainingDuration);
		if (!serverPlayer.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			serverPlayer.addEffect(new MobEffectInstance(
					ModEffects.IMMORTALITY_EFFECT, remainingDuration, immortalityAmplifier, false, false, true));
		}

		serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45 * TICKS_PER_SECOND, 1));
		serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * TICKS_PER_SECOND, 1));
		serverPlayer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40 * TICKS_PER_SECOND, 0));
		serverPlayer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3 * TICKS_PER_MINUTE, 0));

		int immDuration = remainingDuration;
		if (hadTruePower) {
			serverPlayer.removeEffect(MobEffects.DAMAGE_BOOST);
			serverPlayer.addEffect(new MobEffectInstance(
					ModEffects.TRUE_POWER_EFFECT, immDuration, 0, false, false, true));
		} else {
			int newStrengthAmp = strengthAmplifier + immortalityAmplifier * 2 + 2;
			if (newStrengthAmp >= 39) {
				serverPlayer.removeEffect(MobEffects.DAMAGE_BOOST);
				serverPlayer.addEffect(new MobEffectInstance(
						ModEffects.TRUE_POWER_EFFECT, immDuration, 0, false, false, true));
			} else {
				serverPlayer.addEffect(new MobEffectInstance(
						MobEffects.DAMAGE_BOOST, TICKS_PER_MINUTE, newStrengthAmp));
			}
		}

		if (serverPlayer.hasEffect(ModEffects.TRUE_POWER_EFFECT)) {
			ModAdvancements.grantTruePower(serverPlayer);
		}

		ensureSurviving(serverPlayer);
		ImmortalityEffectHelper.restoreHealthToMax(serverPlayer);
		syncHealthPacket(serverPlayer);

		Level level = serverPlayer.level();
		level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

		serverPlayer.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
		CriteriaTriggers.USED_TOTEM.trigger(serverPlayer, new ItemStack(Items.TOTEM_OF_UNDYING));
		level.broadcastEntityEvent(serverPlayer, (byte) 35);

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(
					ParticleTypes.TOTEM_OF_UNDYING,
					serverPlayer.getX(), serverPlayer.getY() + serverPlayer.getBbHeight() / 2.0, serverPlayer.getZ(),
					100, 0.5, 0.5, 0.5, 0.5
			);
		}
	}

	/**
	 * 维持可操控存活态：清原版死亡残留并保证 DATA 血为正。
	 *
	 * @param player 玩家
	 */
	public static void ensureSurviving(Player player) {
		if (player.isRemoved()) {
			player.revive();
		}
		if (player instanceof LivingEntityAccessor accessor) {
			accessor.immortality$setDead(false);
			accessor.immortality$setDeathTime(0);
		}
		if (player.getPose() == Pose.DYING) {
			player.setPose(Pose.STANDING);
		}
		writeSafeHealthData(player);
		ImmortalityHealthDataSanitizer.clearAbsurdFloatOffsets(player);
	}

	/**
	 * 将 DATA 血量写为安全正值。
	 *
	 * @param player 玩家
	 */
	public static void writeSafeHealthData(Player player) {
		float data = player.getEntityData().get(LivingEntityAccessor.immortality$getHealthDataId());
		if (data > 0.0F && !Float.isNaN(data)) {
			return;
		}
		float max = player.getMaxHealth();
		player.getEntityData().set(
				LivingEntityAccessor.immortality$getHealthDataId(),
				max > 0.0F ? max : 1.0F);
	}

	/**
	 * 向客户端同步生命/饥饿（不经过可能被改写的 {@code getHealth}）。
	 *
	 * @param player 玩家
	 */
	public static void syncHealthPacket(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) {
			return;
		}
		serverPlayer.connection.send(new ClientboundSetHealthPacket(
				serverPlayer.getMaxHealth(),
				serverPlayer.getFoodData().getFoodLevel(),
				serverPlayer.getFoodData().getSaturationLevel()));
	}

	/**
	 * 不屈期间将拟写入的生命值钳制为安全值（致死写入返空）。
	 *
	 * @param player 玩家
	 * @param health 拟写入生命值
	 * @return 钳制后的生命值
	 */
	public static float clampHealthWhileImmortal(Player player, float health) {
		if (health > 0.0F && !Float.isNaN(health)) {
			return health;
		}
		if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) {
			float max = player.getMaxHealth();
			return max > 0.0F ? max : 1.0F;
		}
		tryTriggerLethalResist(player);
		float max = player.getMaxHealth();
		if (max > 0.0F) {
			return max;
		}
		float data = player.getEntityData().get(LivingEntityAccessor.immortality$getHealthDataId());
		return data > 0.0F ? data : 1.0F;
	}
}
