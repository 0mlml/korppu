package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.modules.InteractionMenu;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class InteractionMenuControlWindow extends ModuleControlWindow {
    private final InteractionMenu menu;

    public InteractionMenuControlWindow(InteractionMenu menu) {
        super(menu);
        this.menu = menu;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(420, 230, ImGuiCond.Once);
        ImGui.begin(getTitle() + "##imctrl", openBool);

        ModuleStatusWindow.renderEntries(menu.getStatusEntries());
        ImGui.separator();

        if (ImGui.button("Previous")) menu.cycleTarget(-1);
        ImGui.sameLine();
        if (ImGui.button("Next")) menu.cycleTarget(1);
        ImGui.sameLine();
        if (ImGui.button("Primary")) menu.runPrimaryAction();

        if (ImGui.button("Friend")) menu.setFriend();
        ImGui.sameLine();
        if (ImGui.button("Enemy")) menu.setEnemy();
        ImGui.sameLine();
        if (ImGui.button("Neutral")) menu.setNeutral();

        if (ImGui.button("Teleport")) menu.teleportToTarget();
        ImGui.sameLine();
        if (ImGui.button("Invsee")) menu.invseeTarget();

        ImGui.textDisabled("Ctx1=Primary  Ctx2=Prev  Ctx3=Next");

        ImGui.end();
        if (!openBool.get()) {
            visible = false;
        }
    }

    @Override
    public void onContextKey(int index) {
        switch (index) {
            case 0 -> menu.runPrimaryAction();
            case 1 -> menu.cycleTarget(-1);
            case 2 -> menu.cycleTarget(1);
        }
    }
}
