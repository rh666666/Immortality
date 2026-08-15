package com.adoleiiiiii.immortality.mixin.client;

import com.adoleiiiiii.immortality.util.ImmortalityDeathGate;
import com.adoleiiiiii.immortality.util.ImmortalityHealthProbe;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 不屈期间废除原版死亡界面：仅拦截死亡 UI 及其在 {@code setScreen(null)} 时的复活替换，
 * 不劫持普通关界面，避免每次免死都卡顿清屏。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftDeathScreenMixin {

	@Shadow
	public LocalPlayer player;

	@Shadow
	public Screen screen;

	@Shadow
	public MouseHandler mouseHandler;

	@Shadow
	private SoundManager soundManager;

	@Shadow
	public abstract void updateTitle();

	/**
	 * 不屈时拒绝打开死亡界面；仅当原版会用死亡界面替换 {@code null} 时才特殊清屏。
	 */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void immortality$abolishDeathScreen(Screen screen, CallbackInfo ci) {
		if (!immortality$hasImmortality()) {
			return;
		}
		if (immortality$isDeathUi(screen)) {
			immortality$clearToGameWithoutDeath();
			ci.cancel();
			return;
		}
		if (screen == null && immortality$wouldVanillaOpenDeathScreen()) {
			immortality$clearToGameWithoutDeath();
			ci.cancel();
		}
	}

	/**
	 * 每 tick 兜底：不屈期间若仍停在死亡 UI 则清屏。
	 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void immortality$dismissDeathScreenWhileImmortal(CallbackInfo ci) {
		if (!immortality$hasImmortality()) {
			return;
		}
		if (immortality$isDeathUi(this.screen)) {
			immortality$clearToGameWithoutDeath();
		}
	}

	@Unique
	private boolean immortality$hasImmortality() {
		return this.player != null && ImmortalityDeathGate.shouldVoidDeath(this.player);
	}

	/**
	 * 原版 {@code setScreen(null)} 在玩家判死时会改开 {@link DeathScreen}。
	 *
	 * @return 若此时清屏会被替换成死亡界面则为 true
	 */
	@Unique
	private boolean immortality$wouldVanillaOpenDeathScreen() {
		if (this.player == null) {
			return false;
		}
		ImmortalityHealthProbe.push();
		try {
			return this.player.isDeadOrDying();
		} finally {
			ImmortalityHealthProbe.pop();
		}
	}

	/**
	 * 是否为原版死亡相关界面。
	 *
	 * @param screen 目标界面
	 * @return 死亡界面或其标题确认框时为 true
	 */
	@Unique
	private static boolean immortality$isDeathUi(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (screen instanceof DeathScreen) {
			return true;
		}
		return screen instanceof DeathScreen.TitleConfirmScreen;
	}

	/**
	 * 关闭当前界面并回到游戏，且不经过 {@code setScreen(null)} 的死亡替换逻辑。
	 */
	@Unique
	private void immortality$clearToGameWithoutDeath() {
		Screen old = this.screen;
		if (old != null) {
			MinecraftForge.EVENT_BUS.post(new ScreenEvent.Closing(old));
			old.removed();
		}
		this.screen = null;
		BufferUploader.reset();
		this.soundManager.resume();
		this.mouseHandler.grabMouse();
		this.updateTitle();
	}
}
