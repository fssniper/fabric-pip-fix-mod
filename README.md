# PIP Fix

A Fabric mod for Minecraft that keeps browser Picture-in-Picture windows (and other
always-on-top windows) visible over Minecraft's fullscreen mode on Windows.

## The problem

On Windows, Minecraft's non-exclusive fullscreen mode attaches the game window to a monitor
through GLFW. Windows then treats it like a true fullscreen application: it hides other apps'
always-on-top windows (such as a browser's Picture-in-Picture player) instead of just placing
them behind it, and it force-minimizes the game on Alt+Tab.

## What this mod does

- Replaces the monitor-attached fullscreen with a plain, undecorated window resized to cover
  the screen, so Windows no longer treats Minecraft as an exclusive fullscreen app.
- Nudges the window one pixel taller than the monitor to avoid Windows' borderless-fullscreen
  compositor optimization, which has the same hiding effect based on window geometry alone.
- Manually hides the taskbar while Minecraft is fullscreen and focused (restoring it on
  Alt+Tab or when leaving fullscreen), since that's normally something Windows does for "real"
  fullscreen apps only.

Client-side only; has no effect on servers.

## License

MIT — see [LICENSE](LICENSE).
