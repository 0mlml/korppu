package dev.mlml.korppu.module;

import dev.mlml.korppu.imgui.windows.NotificationWindow;

public final class NotifierApi {
    private NotifierApi() {}

    public static void push(String title, String message) {
        NotificationWindow.push(title, message);
    }

    public static void push(String title, String message, long durationMs) {
        NotificationWindow.push(title, message, durationMs);
    }
}
