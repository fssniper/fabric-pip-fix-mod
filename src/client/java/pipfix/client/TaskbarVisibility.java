package pipfix.client;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import pipfix.PIPFixMod;

public final class TaskbarVisibility {
	private static final int SW_HIDE = 0;
	private static final int SW_SHOWNA = 8;

	private static final MethodHandle FIND_WINDOW_A;
	private static final MethodHandle FIND_WINDOW_EX_A;
	private static final MethodHandle SHOW_WINDOW;
	private static final boolean AVAILABLE;

	private static boolean hidden = false;
	private static boolean shutdownHookRegistered = false;

	static {
		MethodHandle findWindowA = null;
		MethodHandle findWindowExA = null;
		MethodHandle showWindow = null;
		boolean available = false;

		try {
			Linker linker = Linker.nativeLinker();
			SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());

			findWindowA = linker.downcallHandle(
				user32.findOrThrow("FindWindowA"),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
			);
			findWindowExA = linker.downcallHandle(
				user32.findOrThrow("FindWindowExA"),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
			);
			showWindow = linker.downcallHandle(
				user32.findOrThrow("ShowWindow"),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
			);
			available = true;
		} catch (Throwable t) {
			PIPFixMod.LOGGER.warn("[pipfix] Could not bind user32 taskbar functions, taskbar hiding disabled", t);
		}

		FIND_WINDOW_A = findWindowA;
		FIND_WINDOW_EX_A = findWindowExA;
		SHOW_WINDOW = showWindow;
		AVAILABLE = available;
	}

	private TaskbarVisibility() {
	}

	public static void hide() {
		if (!AVAILABLE || hidden) {
			return;
		}

		registerShutdownHook();
		hidden = true;
		for (MemorySegment taskbar : findTaskbarWindows()) {
			setVisible(taskbar, false);
		}
	}

	private static void registerShutdownHook() {
		if (shutdownHookRegistered) {
			return;
		}

		shutdownHookRegistered = true;
		Runtime.getRuntime().addShutdownHook(new Thread(TaskbarVisibility::show, "pipfix-taskbar-restore"));
	}

	public static void show() {
		if (!AVAILABLE || !hidden) {
			return;
		}

		hidden = false;
		for (MemorySegment taskbar : findTaskbarWindows()) {
			setVisible(taskbar, true);
		}
	}

	private static void setVisible(MemorySegment taskbar, boolean visible) {
		try {
			SHOW_WINDOW.invoke(taskbar, visible ? SW_SHOWNA : SW_HIDE);
		} catch (Throwable t) {
			PIPFixMod.LOGGER.warn("[pipfix] ShowWindow on taskbar failed", t);
		}
	}

	private static List<MemorySegment> findTaskbarWindows() {
		List<MemorySegment> windows = new ArrayList<>();

		try (Arena arena = Arena.ofConfined()) {
			MemorySegment primary = (MemorySegment) FIND_WINDOW_A.invoke(arena.allocateFrom("Shell_TrayWnd"), MemorySegment.NULL);
			if (primary.address() != 0L) {
				windows.add(primary);
			}

			MemorySegment secondaryClassName = arena.allocateFrom("Shell_SecondaryTrayWnd");
			MemorySegment previous = MemorySegment.NULL;
			while (true) {
				MemorySegment next = (MemorySegment) FIND_WINDOW_EX_A.invoke(MemorySegment.NULL, previous, secondaryClassName, MemorySegment.NULL);
				if (next.address() == 0L) {
					break;
				}

				windows.add(next);
				previous = next;
			}
		} catch (Throwable t) {
			PIPFixMod.LOGGER.warn("[pipfix] Failed to enumerate taskbar windows", t);
		}

		return windows;
	}
}
