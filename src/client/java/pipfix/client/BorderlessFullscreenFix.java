package pipfix.client;

import com.mojang.blaze3d.platform.Window;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

public final class BorderlessFullscreenFix {
	private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");

	private static long lastHandle = -1L;
	private static int lastWidth = -1;
	private static int lastHeight = -1;
	private static boolean softFullscreenActive = false;

	private BorderlessFullscreenFix() {
	}

	public static void markSoftFullscreenActive() {
		softFullscreenActive = true;
	}

	public static boolean consumeSoftFullscreenActive() {
		boolean wasActive = softFullscreenActive;
		softFullscreenActive = false;
		return wasActive;
	}

	public static void tick(Minecraft client) {
		if (!WINDOWS) {
			return;
		}

		Window window = client.getWindow();
		boolean shouldHideTaskbar = window.isFullscreen() && window.isFocused();
		if (shouldHideTaskbar) {
			TaskbarVisibility.hide();
		} else {
			TaskbarVisibility.show();
		}

		if (!window.isFullscreen()) {
			return;
		}

		long handle = window.handle();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer widthBuf = stack.mallocInt(1);
			IntBuffer heightBuf = stack.mallocInt(1);
			GLFW.glfwGetWindowSize(handle, widthBuf, heightBuf);
			int width = widthBuf.get(0);
			int height = heightBuf.get(0);

			if (handle == lastHandle && width == lastWidth && height == lastHeight) {
				return;
			}

			lastHandle = handle;
			lastWidth = width;
			lastHeight = height;

			IntBuffer xBuf = stack.mallocInt(1);
			IntBuffer yBuf = stack.mallocInt(1);
			GLFW.glfwGetWindowPos(handle, xBuf, yBuf);

			GLFWVidMode mode = findVideoModeAt(xBuf.get(0), yBuf.get(0));
			if (mode != null && width == mode.width() && height == mode.height()) {
				GLFW.glfwSetWindowSize(handle, width, height + 1);
			}
		}
	}

	private static GLFWVidMode findVideoModeAt(int windowX, int windowY) {
		PointerBuffer monitors = GLFW.glfwGetMonitors();
		if (monitors == null) {
			return null;
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer xBuf = stack.mallocInt(1);
			IntBuffer yBuf = stack.mallocInt(1);

			for (int i = 0; i < monitors.limit(); i++) {
				long monitor = monitors.get(i);
				GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
				if (mode == null) {
					continue;
				}

				GLFW.glfwGetMonitorPos(monitor, xBuf, yBuf);
				int monitorX = xBuf.get(0);
				int monitorY = yBuf.get(0);

				boolean withinX = windowX >= monitorX && windowX < monitorX + mode.width();
				boolean withinY = windowY >= monitorY && windowY < monitorY + mode.height();
				if (withinX && withinY) {
					return mode;
				}
			}
		}

		return null;
	}
}
