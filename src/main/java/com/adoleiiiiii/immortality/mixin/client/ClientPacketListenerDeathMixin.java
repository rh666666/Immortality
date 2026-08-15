package com.adoleiiiiii.immortality.mixin.client;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 不屈期间废除原版战斗击杀包打开的死亡界面。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerDeathMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	/**
	 * 死亡链路返空时取消 {@link ClientboundPlayerCombatKillPacket} 打开死亡界面。
	 */
	@Inject(method = "handlePlayerCombatKill", at = @At("HEAD"), cancellable = true)
	private void immortality$cancelCombatKillDeathScreen(
			ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
		if (this.minecraft.player != null
				&& ImmortalityDeathGate.shouldVoidDeath(this.minecraft.player)) {
			ci.cancel();
		}
	}
}
