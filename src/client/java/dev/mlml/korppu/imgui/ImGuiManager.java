package dev.mlml.korppu.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11C;

public final class ImGuiManager {
    private static final ImGuiImplGlfw implGlfw = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 implGl3 = new ImGuiImplGl3();
    @Getter
    private static boolean initialized = false;

    private ImGuiManager() {}

    private static void init() {
        long handle = Minecraft.getInstance().getWindow().handle();
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(Minecraft.getInstance().gameDirectory.toPath().resolve("korppu_imgui.ini").toString());
        implGlfw.init(handle, true);
        implGl3.init();
        initialized = true;
    }

    public static void startFrame() {
        if (!initialized) {
            init();
        }
        final com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
        GL11C.glViewport(0, 0, window.getWidth(), window.getHeight());

        implGl3.newFrame();
        implGlfw.newFrame();

        if (!KorppuImGui.isInteractive()) {
            ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        ImGui.newFrame();
    }

    public static void endFrame() {
        if (!initialized) return;
        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());
    }

    public static void dispose() {
        if (!initialized) return;
        implGl3.shutdown();
        implGlfw.shutdown();
        ImGui.destroyContext();
        initialized = false;
    }
}
