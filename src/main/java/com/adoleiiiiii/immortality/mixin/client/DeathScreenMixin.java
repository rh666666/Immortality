package com.adoleiiiiii.immortality.mixin.client;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 不屈期间若原版 {@link DeathScreen} 仍被打开，则立即请求清屏。
 */
@Mixin(DeathScreen.class)
public class DeathScreenMixin {

	/**
	 * 初始化时死亡链路返空则立刻关闭死亡界面。
	 */
	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void immortality$closeOnInit(CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && ImmortalityDeathGate.shouldVoidDeath(mc.player)) {
			mc.setScreen(null);
			ci.cancel();
		}
	}

	/**
	 * 每 tick 若仍应返空死亡链路则关闭界面。
	 */
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void immortality$closeOnTick(CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && ImmortalityDeathGate.shouldVoidDeath(mc.player)) {
			mc.setScreen(null);
			ci.cancel();
		}
	}
}
