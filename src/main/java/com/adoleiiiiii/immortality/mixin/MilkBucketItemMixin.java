package com.adoleiiiiii.immortality.mixin;

import com.adoleiiiiii.immortality.player.ImmortalityPlayerAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 牛奶桶饮用前撤销保护标记 {@code protected = false}，使 {@link LivingEntity#clearStatusEffects()}
 * 可以正常移除不屈/TRUE_POWER，并触发惩罚结算。
 * <p>
 * 对标 Forge 版 {@code onCurePotionEffects} 中 {@code curativeItem.is(Items.MILK_BUCKET)} 的处理。
 */
@Mixin(MilkBucketItem.class)
public class MilkBucketItemMixin {

	@Inject(method = "finishUsing", at = @At("HEAD"))
	private void onFinishUsingHead(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
		if (user instanceof PlayerEntity player && player instanceof ImmortalityPlayerAccess access) {
			access.immortality$setProtected(false);
		}
	}
}
