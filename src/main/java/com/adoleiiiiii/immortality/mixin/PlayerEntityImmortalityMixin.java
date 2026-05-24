package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.ImmortalityConstants;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerNbt;
import com.adoleiiiiii.immortality.util.ImmortalityDamageHelper;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为玩家实体附加不屈 buff 期间的死亡计数与生命上限惩罚状态。
 */
@Mixin(Player.class)
public abstract class PlayerEntityImmortalityMixin implements ImmortalityPlayerAccess {

	@Unique
	private int immortality$deathCount;

	@Unique
	private boolean immortality$refreshingBuff;

	@Unique
	private boolean immortality$buffSessionActive;

	@Unique
	private boolean immortality$effectEndSettled;

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void immortality$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		CompoundTag data = new CompoundTag();
		data.putInt(ImmortalityPlayerNbt.DEATH_COUNT, immortality$deathCount);
		data.putBoolean(ImmortalityPlayerNbt.BUFF_SESSION_ACTIVE, immortality$buffSessionActive);
		data.putBoolean(ImmortalityPlayerNbt.EFFECT_END_SETTLED, immortality$effectEndSettled);
		tag.put(ImmortalityPlayerNbt.ROOT, data);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void immortality$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		immortality$refreshingBuff = false;

		if (!tag.contains(ImmortalityPlayerNbt.ROOT)) {
			if (!player.level().isClientSide) {
				immortality$clearSessionState();
			}
			return;
		}

		CompoundTag data = tag.getCompound(ImmortalityPlayerNbt.ROOT);
		immortality$deathCount = Math.max(0, data.getInt(ImmortalityPlayerNbt.DEATH_COUNT));
		immortality$buffSessionActive = data.getBoolean(ImmortalityPlayerNbt.BUFF_SESSION_ACTIVE);
		immortality$effectEndSettled = data.getBoolean(ImmortalityPlayerNbt.EFFECT_END_SETTLED);

		if (player.level().isClientSide) {
			return;
		}

		if (player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			immortality$syncKnockbackResistance();
			return;
		}

		if (immortality$buffSessionActive && !immortality$effectEndSettled) {
			ImmortalityPenaltyHandler.handleEffectEnd(player);
		}

		if (!player.hasEffect(ModEffects.IMMORTALITY_EFFECT)) {
			immortality$clearSessionState();
		}
	}

	@Unique
	private void immortality$clearSessionState() {
		immortality$deathCount = 0;
		immortality$buffSessionActive = false;
		immortality$effectEndSettled = false;
		immortality$refreshingBuff = false;
		immortality$clearKnockbackResistance();
	}

	@Override
	public int immortality$getDeathCount() {
		return immortality$deathCount;
	}

	@Override
	public void immortality$setDeathCount(int deathCount) {
		immortality$deathCount = Math.max(0, deathCount);
		immortality$syncKnockbackResistance();
	}

	@Override
	public void immortality$incrementDeathCount() {
		immortality$deathCount++;
		immortality$syncKnockbackResistance();
	}

	@Override
	public boolean immortality$isRefreshingBuff() {
		return immortality$refreshingBuff;
	}

	@Override
	public void immortality$setRefreshingBuff(boolean refreshing) {
		immortality$refreshingBuff = refreshing;
	}

	@Override
	public boolean immortality$isBuffSessionActive() {
		return immortality$buffSessionActive;
	}

	@Override
	public void immortality$setBuffSessionActive(boolean active) {
		immortality$buffSessionActive = active;
	}

	@Override
	public boolean immortality$isEffectEndSettled() {
		return immortality$effectEndSettled;
	}

	@Override
	public void immortality$setEffectEndSettled(boolean settled) {
		immortality$effectEndSettled = settled;
	}

	@Override
	public void immortality$applyMaxHealthPenalty(float penalty) {
		if (penalty <= 0.0f) {
			return;
		}
		Player player = (Player) (Object) this;
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		immortality$clearMaxHealthPenalty();
		maxHealth.addPermanentModifier(new AttributeModifier(
				ImmortalityConstants.MAX_HEALTH_PENALTY_MODIFIER_ID,
				"immortality_max_health_penalty",
				-penalty,
				AttributeModifier.Operation.ADDITION
		));
	}

	@Override
	public void immortality$clearMaxHealthPenalty() {
		Player player = (Player) (Object) this;
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(ImmortalityConstants.MAX_HEALTH_PENALTY_MODIFIER_ID);
		}
	}

	/**
	 * 按当前死亡次数与最大生命值，同步不屈 buff 对应的击退抗性修饰符。
	 */
	@Unique
	private void immortality$syncKnockbackResistance() {
		Player player = (Player) (Object) this;
		AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockbackResistance == null) {
			return;
		}

		knockbackResistance.removeModifier(ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID);
		if (!player.hasEffect(ModEffects.IMMORTALITY_EFFECT) || immortality$deathCount <= 0) {
			return;
		}

		float resistance = ImmortalityDamageHelper.computeKnockbackResistance(
				immortality$deathCount, player.getMaxHealth());
		if (resistance <= 0.0f) {
			return;
		}

		knockbackResistance.addTransientModifier(new AttributeModifier(
				ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID,
				"immortality_knockback_resistance",
				resistance,
				AttributeModifier.Operation.ADDITION
		));
	}

	/**
	 * 移除不屈 buff 附加的击退抗性修饰符。
	 */
	@Unique
	private void immortality$clearKnockbackResistance() {
		Player player = (Player) (Object) this;
		AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockbackResistance != null) {
			knockbackResistance.removeModifier(ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID);
		}
	}
}
