package pipfix.client.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pipfix.client.BorderlessFullscreenFix;

@Mixin(Window.class)
public class WindowMixin {
	@Shadow
	private boolean isSoftScreen() {
		throw new AssertionError();
	}

	@Redirect(
		method = "setMode",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", ordinal = 0)
	)
	private void pipfix$attachBorderlessInsteadOfMonitor(long window, long monitor, int xpos, int ypos, int width, int height, int refreshRate) {
		if (this.isSoftScreen()) {
			GLFW.glfwSetWindowMonitor(window, 0L, xpos, ypos, width, height, GLFW.GLFW_DONT_CARE);
			GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
			GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);
			GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
			GLFW.glfwFocusWindow(window);
			BorderlessFullscreenFix.markSoftFullscreenActive();
		} else {
			GLFW.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate);
		}
	}

	@Redirect(
		method = "setMode",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", ordinal = 1)
	)
	private void pipfix$restoreDecorationsLeavingBorderless(long window, long monitor, int xpos, int ypos, int width, int height, int refreshRate) {
		if (BorderlessFullscreenFix.consumeSoftFullscreenActive()) {
			GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
			GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		}

		GLFW.glfwSetWindowMonitor(window, monitor, xpos, ypos, width, height, refreshRate);
	}
}
