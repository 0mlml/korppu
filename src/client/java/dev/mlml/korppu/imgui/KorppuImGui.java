package dev.mlml.korppu.imgui;

import dev.mlml.korppu.config.LiveConfig;
import dev.mlml.korppu.imgui.windows.*;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import lombok.Getter;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.mlml.korppu.KorppuMod.mc;

public final class KorppuImGui {
    public static final int QUICK_SHOW_KEY =GLFW.GLFW_KEY_RIGHT_ALT;

    public static KeyMapping TOGGLE_KEY;
    public static KeyMapping CONSOLE_KEY;
    public static KeyMapping EDITOR_KEY;
    public static KeyMapping CONTEXT_KEY_1;
    public static KeyMapping CONTEXT_KEY_2;
    public static KeyMapping CONTEXT_KEY_3;

    private static boolean wasReleasingMouse = false;
    private static boolean quickActive = false;
    private static boolean quickKeyWasDown = false;
    private static boolean wasInteractive = false;

    /** The main window doubles as "the menu": while it is open the keyboard is trapped and the mouse is free. */
    public static boolean isMenuOpen() {
        return moduleListWindow.isVisible();
    }

    /** Holding the quick key frees the mouse for ImGui while movement keys keep reaching the game. */
    public static boolean isQuickHeld() {
        return quickActive;
    }

    /**
     * Quick mode only starts if the key goes down while nothing is being typed, so AltGr (reported as right alt on
     * many layouts) can be used for text input. Once started it lasts until the key is released.
     */
    private static void updateQuick() {
        boolean down = mc.getWindow() != null && GLFW.glfwGetKey(mc.getWindow().handle(), QUICK_SHOW_KEY) == GLFW.GLFW_PRESS;
        if (down && !quickKeyWasDown) {
            boolean typing = ImGuiManager.isInitialized() && imgui.ImGui.getIO().getWantTextInput();
            quickActive = mc.screen == null && !typing;
        } else if (!down) {
            quickActive = false;
        }
        quickKeyWasDown = down;
    }

    public static boolean shouldReleaseMouse() {
        return isMenuOpen() || isQuickHeld();
    }

    /** Whether ImGui should see the mouse at all; false while the game has the cursor locked for gameplay. */
    public static boolean isInteractive() {
        return shouldReleaseMouse() || !mc.mouseHandler.isMouseGrabbed();
    }

    public static boolean isCapturingMouse() {
        if (mc.screen == null && shouldReleaseMouse()) return true;
        if (!ImGuiManager.isInitialized() || mc.mouseHandler.isMouseGrabbed()) return false;
        return imgui.ImGui.getIO().getWantCaptureMouse();
    }

    public static boolean isKeyboardBlocked() {
        if (isMenuOpen()) return true;
        return ImGuiManager.isInitialized() && imgui.ImGui.getIO().getWantCaptureKeyboard();
    }

    public static void toggleMenu() {
        if (isMenuOpen()) {
            closeMenu();
        } else {
            moduleListWindow.show();
        }
    }

    public static void closeMenu() {
        moduleListWindow.hide();
        keybindingsWindow.hide();
    }

    private static final ModuleConfigWindow moduleConfigWindow = new ModuleConfigWindow();
    private static final KeybindingsWindow keybindingsWindow = new KeybindingsWindow();
    private static final ModuleListWindow moduleListWindow = new ModuleListWindow(moduleConfigWindow, keybindingsWindow);

    private static final PlayerListWindow playerListWindow = new PlayerListWindow();
    @Getter
    private static final EntityDetailWindow entityDetailWindow = new EntityDetailWindow();
    @Getter
    private static final EntityListWindow entityListWindow = new EntityListWindow(entityDetailWindow);
    private static final Map<Module, ModuleStatusWindow> statusWindows = new IdentityHashMap<>();
    private static final ConsoleWindow consoleWindow = new ConsoleWindow();
    private static final LocateResultsWindow locateResultsWindow = new LocateResultsWindow();
    private static final TextEditorWindow textEditorWindow = new TextEditorWindow();
    private static final NotificationWindow notificationWindow = new NotificationWindow();
    private static final UncleanShutdownWindow uncleanShutdownWindow = new UncleanShutdownWindow();
    @Getter
    private static final PlayerManagerWindow playerManagerWindow = new PlayerManagerWindow();
    private static final ItemRegistryWindow itemRegistryWindow = new ItemRegistryWindow();
    private static final ItemPresetsWindow itemPresetsWindow = new ItemPresetsWindow();
    private static final ItemModal itemModal = new ItemModal();
    static final ImGuiStatusBar statusBar = new ImGuiStatusBar();

    private static final List<WindowLayoutPreset> presets = WindowLayoutPreset.load();
    private static final LayoutPresetsWindow layoutPresetsWindow = new LayoutPresetsWindow(presets);

    private static ModuleControlWindow activeControlWindow;

    private KorppuImGui() {
    }

    public static void init() {
        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.imgui_toggle", GLFW.GLFW_KEY_INSERT, Module.kbCategory));
        CONSOLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.console_toggle", GLFW.GLFW_KEY_GRAVE_ACCENT, Module.kbCategory));
        EDITOR_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.editor_toggle", GLFW.GLFW_KEY_UNKNOWN, Module.kbCategory));
        CONTEXT_KEY_1 = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.context_1", GLFW.GLFW_KEY_UNKNOWN, Module.kbCategory));
        CONTEXT_KEY_2 = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.context_2", GLFW.GLFW_KEY_UNKNOWN, Module.kbCategory));
        CONTEXT_KEY_3 = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.korppumod.context_3", GLFW.GLFW_KEY_UNKNOWN, Module.kbCategory));
    }

    public static void tick() {
        if (TOGGLE_KEY.consumeClick()) {
            toggleMenu();
        }
        if (CONSOLE_KEY.consumeClick()) consoleWindow.toggle();
        if (EDITOR_KEY.consumeClick()) textEditorWindow.toggle();
        if (activeControlWindow != null) {
            if (CONTEXT_KEY_1.consumeClick()) activeControlWindow.onContextKey(0);
            if (CONTEXT_KEY_2.consumeClick()) activeControlWindow.onContextKey(1);
            if (CONTEXT_KEY_3.consumeClick()) activeControlWindow.onContextKey(2);
            if (!activeControlWindow.isVisible()) {
                activeControlWindow = null;
            }
        }
    }


    public static void hideAll() {
        moduleListWindow.hide();
        moduleConfigWindow.closeAll();
        keybindingsWindow.hide();
        consoleWindow.hide();
        textEditorWindow.hide();
        locateResultsWindow.hide();
        itemRegistryWindow.hide();
        itemModal.hide();
        if (activeControlWindow != null) activeControlWindow.hide();
        statusWindows.values().forEach(ImGuiWindow::hide);
        notificationWindow.hide();
        playerListWindow.hide();
        playerManagerWindow.hide();
        entityDetailWindow.hide();
        entityListWindow.hide();
    }

    public static void hideNonStatus() {
        moduleListWindow.hide();
        moduleConfigWindow.closeAll();
        keybindingsWindow.hide();
        consoleWindow.hide();
        textEditorWindow.hide();
        locateResultsWindow.hide();
        itemRegistryWindow.hide();
        itemModal.hide();
        if (activeControlWindow != null) activeControlWindow.hide();
        playerListWindow.hide();
    }

    public static void openModuleConfig(Module module) {
        moduleConfigWindow.openForModule(module);
    }

    public static void openConsole() {
        consoleWindow.show();
    }

    public static void openItemModal(net.minecraft.world.item.ItemStack stack) {
        ItemModal.openForStack(stack);
    }

    public static void openItemRegistry() {
        itemRegistryWindow.show();
    }

    public static void openItemPresets() {
        itemPresetsWindow.show();
    }

    public static void openEditorWithContent(String content) {
        textEditorWindow.loadContent(content);
    }

    public static void openLocateResults() {
        locateResultsWindow.show();
    }

    public static void setLocatePendingQuery(String query) {
        locateResultsWindow.setPendingQuery(query);
    }

    public static void clearLocatePendingQuery() {
        locateResultsWindow.clearPendingQuery();
    }

    public static void addLocateResult(String line) {
        locateResultsWindow.addEntry(line);
    }

    public static void openStatusWindow(Module module) {
        if (module.hasControlWindow()) {
            openControlWindow(module);
            return;
        }
        ModuleStatusWindow w = statusWindows.computeIfAbsent(module, ModuleStatusWindow::new);
        w.show();
    }

    public static void openControlWindow(Module module) {
        if (activeControlWindow != null) {
            activeControlWindow.hide();
        }
        ModuleControlWindow w = module.createControlWindow();
        if (w != null) {
            activeControlWindow = w;
            activeControlWindow.show();
        }
    }

    public static void openPlayerManager() {
        playerManagerWindow.show();
    }

    public static void showUncleanShutdown(List<LiveConfig.Change> changes) {
        uncleanShutdownWindow.open(changes);
    }

    public static void openLayoutPresets() {
        layoutPresetsWindow.show();
    }

    public static WindowLayoutPreset capturePreset(String name) {
        WindowLayoutPreset p = new WindowLayoutPreset(name);
        p.moduleList = moduleListWindow.isVisible();
        p.console = consoleWindow.isVisible();
        p.entityList = entityListWindow.isVisible();
        p.textEditor = textEditorWindow.isVisible();
        p.itemRegistry = itemRegistryWindow.isVisible();
        for (Map.Entry<Module, ModuleStatusWindow> e : statusWindows.entrySet()) {
            if (e.getValue().isVisible()) p.statusModules.add(e.getKey().getName());
        }
        for (Module m : ModuleManager.getModules()) {
            if (moduleConfigWindow.isOpenFor(m)) p.configModules.add(m.getName());
            p.enabledModules.put(m.getName(), m.isEnabled());
            p.moduleConfigs.put(m.getName(), m.getConfig().serialize());
        }
        return p;
    }

    public static void applyPreset(WindowLayoutPreset p) {
        if (p.moduleConfigs != null) {
            for (Module m : ModuleManager.getModules()) {
                String cfg = p.moduleConfigs.get(m.getName());
                if (cfg != null && !cfg.isEmpty()) m.getConfig().deserialize(cfg);
            }
        }

        if (p.enabledModules != null) {
            for (Module m : ModuleManager.getModules()) {
                Boolean enabled = p.enabledModules.get(m.getName());
                if (enabled != null) m.setEnabled(enabled);
            }
        }

        if (p.moduleList) moduleListWindow.show();
        else moduleListWindow.hide();
        if (p.console) consoleWindow.show();
        else consoleWindow.hide();
        if (p.entityList) entityListWindow.show();
        else entityListWindow.hide();
        if (p.textEditor) textEditorWindow.show();
        else textEditorWindow.hide();
        if (p.itemRegistry) itemRegistryWindow.show();
        else itemRegistryWindow.hide();

        moduleConfigWindow.closeAll();
        for (Module m : ModuleManager.getModules()) {
            if (p.configModules.contains(m.getName())) moduleConfigWindow.openForModule(m);
        }

        statusWindows.values().forEach(ImGuiWindow::hide);
        for (Module m : ModuleManager.getModules()) {
            if (p.statusModules.contains(m.getName())) openStatusWindow(m);
        }
    }

    public static void render() {
        updateQuick();
        boolean quickHeld = isQuickHeld();
        if (quickHeld) {
            playerListWindow.show();
        } else {
            playerListWindow.hide();
        }

        boolean releaseMouse = shouldReleaseMouse();
        if (releaseMouse) {
            mc.mouseHandler.releaseMouse();
        } else if (wasReleasingMouse && mc.player != null && mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }
        wasReleasingMouse = releaseMouse;

        boolean interactive = isInteractive();
        if (wasInteractive && !interactive) {
            // Drop any focused text field so it doesn't keep eating game keys after the menu closes.
            imgui.ImGui.setWindowFocus(null);
        }
        wasInteractive = interactive;

        if (quickHeld) {
            imgui.ImGui.getIO().addKeyEvent(imgui.flag.ImGuiKey.LeftAlt, false);
            imgui.ImGui.getIO().addKeyEvent(imgui.flag.ImGuiKey.RightAlt, false);
        }

        statusBar.render();

        moduleListWindow.render();
        moduleConfigWindow.render();
        keybindingsWindow.render();

        playerListWindow.render();
        entityListWindow.render();
        entityDetailWindow.render();
        consoleWindow.render();
        textEditorWindow.render();
        locateResultsWindow.render();
        itemRegistryWindow.render();
        itemPresetsWindow.render();
        itemModal.render();
        layoutPresetsWindow.render();

        List<Module> closedStatus = new ArrayList<>();
        for (Map.Entry<Module, ModuleStatusWindow> entry : statusWindows.entrySet()) {
            entry.getValue().render();
            if (!entry.getValue().isVisible()) closedStatus.add(entry.getKey());
        }
        closedStatus.forEach(statusWindows::remove);

        notificationWindow.render();
        uncleanShutdownWindow.render();
        playerManagerWindow.render();

        if (activeControlWindow != null) {
            activeControlWindow.render();
        }
    }
}
