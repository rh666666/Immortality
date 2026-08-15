package com.adoleiiiiii.immortality.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;

/**
 * 向读血 / 存活实例方法的返回点注入 {@link TruePowerAsmHooks}。
 * <p>
 * 可重复调用：若返回点前已是本模组钩子则跳过，否则在 {@code *RETURN} 前再插一层。
 */
public final class ImmortalityHealthHookInjector {

	private static final String HOOKS = "com/adoleiiiiii/immortality/asm/TruePowerAsmHooks";

	private static final Set<String> GET_HEALTH_NAMES = Set.of("getHealth", "m_21223_");
	private static final Set<String> IS_ALIVE_NAMES = Set.of("isAlive", "m_6084_");
	private static final Set<String> IS_DEAD_OR_DYING_NAMES = Set.of("isDeadOrDying", "m_21224_");

	private ImmortalityHealthHookInjector() {
	}

	/**
	 * 扫描并注入目标类中的读血 / 存活方法。
	 *
	 * @param classNode 类节点
	 * @return 是否改写了字节码
	 */
	public static boolean inject(ClassNode classNode) {
		boolean changed = false;
		for (MethodNode method : classNode.methods) {
			if (method.instructions == null || isStatic(method)) {
				continue;
			}
			if ("()F".equals(method.desc) && GET_HEALTH_NAMES.contains(method.name)) {
				changed |= injectBeforeReturn(method, Opcodes.FRETURN, "afterGetHealth", "(FLjava/lang/Object;)F");
			} else if ("()Z".equals(method.desc) && IS_ALIVE_NAMES.contains(method.name)) {
				changed |= injectBeforeReturn(method, Opcodes.IRETURN, "afterIsAlive", "(ZLjava/lang/Object;)Z");
			} else if ("()Z".equals(method.desc) && IS_DEAD_OR_DYING_NAMES.contains(method.name)) {
				changed |= injectBeforeReturn(method, Opcodes.IRETURN, "afterIsDeadOrDying", "(ZLjava/lang/Object;)Z");
			}
		}
		return changed;
	}

	/**
	 * 在指定返回操作码前插入钩子；若已紧邻本模组钩子则不重复插入。
	 *
	 * @param method     方法
	 * @param returnOpcode {@link Opcodes#FRETURN} 或 {@link Opcodes#IRETURN}
	 * @param hookName   钩子方法名
	 * @param hookDesc   钩子描述符
	 * @return 是否改写
	 */
	private static boolean injectBeforeReturn(MethodNode method, int returnOpcode, String hookName, String hookDesc) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() != returnOpcode) {
				continue;
			}
			if (isAlreadyOurHook(insn, hookName, hookDesc)) {
				continue;
			}
			InsnList inject = new InsnList();
			inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hookName, hookDesc, false));
			method.instructions.insertBefore(insn, inject);
			changed = true;
		}
		return changed;
	}

	/**
	 * 判断返回指令前是否已是本模组钩子调用。
	 *
	 * @param returnInsn 返回指令
	 * @param hookName   钩子名
	 * @param hookDesc   钩子描述符
	 * @return 已是最后钩子时为 true
	 */
	private static boolean isAlreadyOurHook(AbstractInsnNode returnInsn, String hookName, String hookDesc) {
		AbstractInsnNode prev = skipNop(returnInsn.getPrevious());
		if (!(prev instanceof MethodInsnNode call)) {
			return false;
		}
		if (call.getOpcode() != Opcodes.INVOKESTATIC
				|| !HOOKS.equals(call.owner)
				|| !hookName.equals(call.name)
				|| !hookDesc.equals(call.desc)) {
			return false;
		}
		AbstractInsnNode aload = skipNop(prev.getPrevious());
		return aload != null && aload.getOpcode() == Opcodes.ALOAD && aload instanceof VarInsnNode v && v.var == 0;
	}

	private static AbstractInsnNode skipNop(AbstractInsnNode node) {
		while (node != null && (node.getOpcode() == -1 || node.getOpcode() == Opcodes.NOP)) {
			node = node.getPrevious();
		}
		return node;
	}

	private static boolean isStatic(MethodNode method) {
		return (method.access & Opcodes.ACC_STATIC) != 0;
	}
}
