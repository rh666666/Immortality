package com.adoleiiiiii.immortality.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;

/**
 * 向读血 / 存活实例方法返回处注入通用钩子（{@link Phase#AFTER}）。
 * <p>
 * 不针对任何第三方模组或实体类型；凡匹配方法签名的类均可注入。
 */
public final class ImmortalityLaunchPlugin implements ILaunchPluginService {

	@Override
	public String name() {
		return "immortality_true_power";
	}

	@Override
	public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
		if (isEmpty) {
			return EnumSet.noneOf(Phase.class);
		}
		String name = classType.getClassName();
		if (shouldSkip(name)) {
			return EnumSet.noneOf(Phase.class);
		}
		return EnumSet.of(Phase.AFTER);
	}

	@Override
	public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
		if (phase != Phase.AFTER) {
			return false;
		}
		return ImmortalityHealthHookInjector.inject(classNode);
	}

	private static boolean shouldSkip(String className) {
		return className.startsWith("java.")
				|| className.startsWith("javax.")
				|| className.startsWith("sun.")
				|| className.startsWith("jdk.")
				|| className.startsWith("com.sun.")
				|| className.startsWith("org.objectweb.")
				|| className.startsWith("org.spongepowered.")
				|| className.startsWith("cpw.mods.")
				|| className.startsWith("com.google.")
				|| className.startsWith("com.mojang.")
				|| className.startsWith("com.adoleiiiiii.immortality.asm.");
	}
}
