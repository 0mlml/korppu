package dev.mlml.korppu.imgui.windows;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

public class NotificationWindow extends ImGuiWindow {
    private record Notification(String title, String message, long expiryMs) {}

    private static NotificationWindow instance;
    private final Deque<Notification> queue = new ArrayDeque<>();

    public NotificationWindow() {
        instance = this;
    }

    public static void push(String title, String message) {
        push(title, message, 4000);
    }

    public static void push(String title, String message, long durationMs) {
        if (instance != null) {
            instance.queue.addLast(new Notification(title, message, System.currentTimeMillis() + durationMs));
        }
    }

    @Override
    public boolean isVisible() {
        return !queue.isEmpty();
    }

    @Override
    public void render() {
        queue.removeIf(n -> System.currentTimeMillis() > n.expiryMs());
        if (queue.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        float sw = mc.getWindow().getWidth();
        float sh = mc.getWindow().getHeight();
        float width = 260f;
        float height = 58f;
        float padding = 8f;

        int flags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoFocusOnAppearing
                | ImGuiWindowFlags.NoNav;

        int shown = 0;
        for (Notification n : queue) {
            if (shown >= 4) break;
            float x = sw - width - padding;
            float y = sh - padding - (shown + 1) * (height + padding);
            ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
            ImGui.setNextWindowSize(width, height, ImGuiCond.Always);
            if (ImGui.begin("##notif_" + shown, flags)) {
                ImGui.textColored(1f, 0.8f, 0.2f, 1f, n.title());
                ImGui.textWrapped(n.message());
            }
            ImGui.end();
            shown++;
        }
    }
}
