package dev.mlml.korppu.imgui.windows;

public abstract class ImGuiWindow {
    public enum WindowType { MENU, OVERLAY }

    protected boolean visible = false;

    public WindowType getWindowType() {
        return WindowType.OVERLAY;
    }

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public void toggle() {
        visible = !visible;
    }

    public abstract void render();
}
