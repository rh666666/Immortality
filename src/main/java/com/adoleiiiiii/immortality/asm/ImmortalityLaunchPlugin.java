package com.adoleiiiiii.immortality.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.EnumSet;
import java.util.Set;

/**
 * 向读血 / 存活实例方法返回处注入真正的力量钩子。
 * <p>
 * 不变换静态方法；不在变换期 {@code Class.forName}。
 */
public final class ImmortalityLaunchPlugin implements ILaunchPluginService {

	private static final String HOOKS = "com/adoleiiiiii/immortality/asm/TruePowerAsmHooks";

	private static final Set<String> GET_HEALTH_NAMES = Set.of("getHealth", "m_21223_");
	private static final Set<String> IS_ALIVE_NAMES = Set.of("isAlive", "m_6084_");
	private static final Set<String> IS_DEAD_OR_DYING_NAMES = Set.of("isDeadOrDying", "m_21224_");

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
		return EnumSet.of(Phase.BEFORE);
	}

	@Override
	public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
		if (phase != Phase.BEFORE) {
			return false;
		}
		boolean changed = false;
		for (MethodNode method : classNode.methods) {
			if (method.instructions == null || isStatic(method)) {
				continue;
			}
			if ("()F".equals(method.desc) && GET_HEALTH_NAMES.contains(method.name)) {
				changed |= injectFloatHook(method);
			} else if ("()Z".equals(method.desc) && IS_ALIVE_NAMES.contains(method.name)) {
				changed |= injectBooleanHook(method, "afterIsAlive");
			} else if ("()Z".equals(method.desc) && IS_DEAD_OR_DYING_NAMES.contains(method.name)) {
				changed |= injectBooleanHook(method, "afterIsDeadOrDying");
			}
		}
		return changed;
	}

	/** @return 静态方法时为 true */
	private static boolean isStatic(MethodNode method) {
		return (method.access & Opcodes.ACC_STATIC) != 0;
	}

	/**
	 * 在 {@code FRETURN} 前插入 {@link TruePowerAsmHooks#afterGetHealth}。
	 *
	 * @param method 目标实例方法
	 * @return 是否改写了指令
	 */
	private static boolean injectFloatHook(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.FRETURN) {
				InsnList inject = new InsnList();
				inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
				inject.add(new MethodInsnNode(
						Opcodes.INVOKESTATIC, HOOKS, "afterGetHealth", "(FLjava/lang/Object;)F", false));
				method.instructions.insertBefore(insn, inject);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * 在 {@code IRETURN} 前插入对应布尔钩子。
	 *
	 * @param method   目标实例方法
	 * @param hookName {@link TruePowerAsmHooks} 静态方法名
	 * @return 是否改写了指令
	 */
	private static boolean injectBooleanHook(MethodNode method, String hookName) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.IRETURN) {
				InsnList inject = new InsnList();
				inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
				inject.add(new MethodInsnNode(
						Opcodes.INVOKESTATIC, HOOKS, hookName, "(ZLjava/lang/Object;)Z", false));
				method.instructions.insertBefore(insn, inject);
				changed = true;
			}
		}
		return changed;
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
