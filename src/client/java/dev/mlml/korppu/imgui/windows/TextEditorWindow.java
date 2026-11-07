package dev.mlml.korppu.imgui.windows;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextEditorWindow extends ImGuiWindow {
    private static final int MAX_CONTENT = 1 << 20;

    private final ImBoolean openBool = new ImBoolean(true);
    private final ImString pathBuf = new ImString(512);
    private final ImString contentBuf = new ImString(MAX_CONTENT);
    private final ImString extCmdBuf = new ImString("{f}", 512);
    private String statusMsg = "";
    private String openedPath = null;

    private Path watchPath = null;
    private long watchMtime = 0;

    @Override
    public void render() {
        if (!visible) return;

        pollExternalFile();

        openBool.set(true);
        ImGui.setNextWindowSize(640, 480, ImGuiCond.Once);
        ImGui.begin("Text Editor##korppu_editor", openBool);

        ImGui.pushItemWidth(-1);
        ImGui.inputText("##epath", pathBuf);
        ImGui.popItemWidth();

        if (ImGui.button("Open##eopen")) {
            loadFile(pathBuf.get().trim());
        }
        ImGui.sameLine();
        if (ImGui.button("Save##esave")) {
            saveFile(openedPath != null ? openedPath : pathBuf.get().trim());
        }
        ImGui.sameLine();
        if (ImGui.button("Save As##esaveas")) {
            saveFile(pathBuf.get().trim());
        }
        ImGui.sameLine();
        if (ImGui.button("New##enew")) {
            contentBuf.set("");
            openedPath = null;
            statusMsg = "New file";
        }

        if (!statusMsg.isEmpty()) {
            ImGui.sameLine();
            ImGui.textDisabled(statusMsg);
        }

        ImGui.separator();

        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - 160);
        ImGui.inputText("##extcmd", extCmdBuf);
        ImGui.sameLine();
        if (ImGui.button("Open External##eext")) {
            openExternal();
        }
        if (watchPath != null) {
            ImGui.sameLine();
            ImGui.textColored(0.4f, 1f, 0.4f, 1f, "Watching...");
        }

        ImGui.separator();

        float availH = ImGui.getContentRegionAvailY();
        ImGui.inputTextMultiline("##econtent", contentBuf, -1f, availH,
                ImGuiInputTextFlags.AllowTabInput);

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
            cleanupWatch();
        }
    }

    private void pollExternalFile() {
        if (watchPath == null) return;
        try {
            long mtime = Files.getLastModifiedTime(watchPath).toMillis();
            if (mtime != watchMtime) {
                watchMtime = mtime;
                String updated = Files.readString(watchPath);
                if (updated.length() < MAX_CONTENT) {
                    contentBuf.set(updated);
                    statusMsg = "Reloaded from external";
                }
            }
        } catch (IOException e) {
            statusMsg = "Watch error: " + e.getMessage();
            cleanupWatch();
        }
    }

    private void openExternal() {
        String cmd = extCmdBuf.get().trim();
        if (cmd.isEmpty() || !cmd.contains("{f}")) {
            statusMsg = "Command must contain {f}";
            return;
        }
        try {
            Path tmp = Files.createTempFile("korppu_editor_", ".txt");
            Files.writeString(tmp, contentBuf.get());
            cleanupWatch();
            watchPath = tmp;
            watchMtime = Files.getLastModifiedTime(tmp).toMillis();

            String[] parts = cmd.replace("{f}", tmp.toAbsolutePath().toString()).split("\\s+");
            new ProcessBuilder(parts).start();
            statusMsg = "Opened in external editor";
        } catch (IOException e) {
            statusMsg = "Error: " + e.getMessage();
        }
    }

    private void cleanupWatch() {
        if (watchPath != null) {
            try { Files.deleteIfExists(watchPath); } catch (IOException ignored) {}
            watchPath = null;
        }
    }

    private void loadFile(String path) {
        if (path.isEmpty()) {
            statusMsg = "No path specified";
            return;
        }
        try {
            String content = Files.readString(Path.of(path));
            if (content.length() >= MAX_CONTENT) {
                statusMsg = "File too large (> 1 MiB)";
                return;
            }
            contentBuf.set(content);
            openedPath = path;
            pathBuf.set(path);
            statusMsg = "Opened";
        } catch (IOException e) {
            statusMsg = "Error: " + e.getMessage();
        }
    }

    public void loadContent(String content) {
        if (content == null) content = "";
        if (content.length() >= MAX_CONTENT) content = content.substring(0, MAX_CONTENT - 1);
        contentBuf.set(content);
        openedPath = null;
        statusMsg = "Loaded";
        visible = true;
    }

    private void saveFile(String path) {
        if (path == null || path.isEmpty()) {
            statusMsg = "No path specified";
            return;
        }
        try {
            Files.writeString(Path.of(path), contentBuf.get());
            openedPath = path;
            pathBuf.set(path);
            statusMsg = "Saved";
        } catch (IOException e) {
            statusMsg = "Error: " + e.getMessage();
        }
    }
}
