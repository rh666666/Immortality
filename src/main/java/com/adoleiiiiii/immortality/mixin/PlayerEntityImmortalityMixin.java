package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.ImmortalityConstants;
import com.adoleiiiiii.immortality.effect.ModEffects;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import com.adoleiiiiii.immortality.player.ImmortalityPlayerNbt;
import com.adoleiiiiii.immortality.util.ImmortalityDamageHelper;
import com.adoleiiiiii.immortality.util.ImmortalityPenaltyHandler;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为玩家实体附加不屈 buff 期间的死亡计数与生命上限惩罚状态。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityImmortalityMixin implements ImmortalityPlayerAccess {

	@Unique
	private int immortality$deathCount;

	@Unique
	private boolean immortality$refreshingBuff;

	@Unique
	private boolean immortality$buffSessionActive;

	@Unique
	private boolean immortality$effectEndSettled;

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void immortality$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		NbtCompound data = new NbtCompound();
		data.putInt(ImmortalityPlayerNbt.DEATH_COUNT, immortality$deathCount);
		data.putBoolean(ImmortalityPlayerNbt.BUFF_SESSION_ACTIVE, immortality$buffSessionActive);
		data.putBoolean(ImmortalityPlayerNbt.EFFECT_END_SETTLED, immortality$effectEndSettled);
		nbt.put(ImmortalityPlayerNbt.ROOT, data);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void immortality$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		immortality$refreshingBuff = false;

		if (!nbt.contains(ImmortalityPlayerNbt.ROOT)) {
			if (!player.getWorld().isClient) {
				immortality$clearSessionState();
			}
			return;
		}

		NbtCompound data = nbt.getCompound(ImmortalityPlayerNbt.ROOT);
		immortality$deathCount = Math.max(0, data.getInt(ImmortalityPlayerNbt.DEATH_COUNT));
		immortality$buffSessionActive = data.getBoolean(ImmortalityPlayerNbt.BUFF_SESSION_ACTIVE);
		immortality$effectEndSettled = data.getBoolean(ImmortalityPlayerNbt.EFFECT_END_SETTLED);

		if (player.getWorld().isClient) {
			return;
		}

		if (player.hasStatusEffect(ModEffects.IMMORTALITY)) {
			immortality$syncKnockbackResistance();
			return;
		}

		if (immortality$buffSessionActive && !immortality$effectEndSettled) {
			ImmortalityPenaltyHandler.handleEffectEnd(player);
		}

		if (!player.hasStatusEffect(ModEffects.IMMORTALITY)) {
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
		PlayerEntity player = (PlayerEntity) (Object) this;
		EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		immortality$clearMaxHealthPenalty();
		maxHealth.addPersistentModifier(new EntityAttributeModifier(
				ImmortalityConstants.MAX_HEALTH_PENALTY_MODIFIER_ID,
				"immortality_max_health_penalty",
				-penalty,
				EntityAttributeModifier.Operation.ADDITION
		));
	}

	@Override
	public void immortality$clearMaxHealthPenalty() {
		PlayerEntity player = (PlayerEntity) (Object) this;
		EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(ImmortalityConstants.MAX_HEALTH_PENALTY_MODIFIER_ID);
		}
	}

	/**
	 * 按当前死亡次数与最大生命值，同步不屈 buff 对应的击退抗性修饰符。
	 */
	@Unique
	private void immortality$syncKnockbackResistance() {
		PlayerEntity player = (PlayerEntity) (Object) this;
		EntityAttributeInstance knockbackResistance =
				player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
		if (knockbackResistance == null) {
			return;
		}

		knockbackResistance.removeModifier(ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID);
		if (!player.hasStatusEffect(ModEffects.IMMORTALITY) || immortality$deathCount <= 0) {
			return;
		}

		float resistance = ImmortalityDamageHelper.computeKnockbackResistance(
				immortality$deathCount, player.getMaxHealth());
		if (resistance <= 0.0f) {
			return;
		}

		knockbackResistance.addTemporaryModifier(new EntityAttributeModifier(
				ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID,
				"immortality_knockback_resistance",
				resistance,
				EntityAttributeModifier.Operation.ADDITION
		));
	}

	/**
	 * 移除不屈 buff 附加的击退抗性修饰符。
	 */
	@Unique
	private void immortality$clearKnockbackResistance() {
		PlayerEntity player = (PlayerEntity) (Object) this;
		EntityAttributeInstance knockbackResistance =
				player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
		if (knockbackResistance != null) {
			knockbackResistance.removeModifier(ImmortalityConstants.KNOCKBACK_RESISTANCE_MODIFIER_ID);
		}
	}
}
