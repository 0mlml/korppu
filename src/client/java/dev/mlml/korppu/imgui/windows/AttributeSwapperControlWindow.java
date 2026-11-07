package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.module.modules.AttributeSwapper;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class AttributeSwapperControlWindow extends ModuleControlWindow {
    private final AttributeSwapper swapper;

    public AttributeSwapperControlWindow(AttributeSwapper swapper) {
        super(swapper);
        this.swapper = swapper;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(420, 310, ImGuiCond.Once);
        ImGui.begin(getTitle() + "##asctrl", openBool);

        ModuleStatusWindow.renderEntries(swapper.getStatusEntries());

        ImGui.separator();

        if (ImGui.button("Lock current as Slot 1")) {
            swapper.lockSelectedAsFirst();
        }
        ImGui.sameLine();
        if (ImGui.button("Lock current as Slot 2")) {
            swapper.lockSelectedAsSecond();
        }
        ImGui.sameLine();
        if (ImGui.button("Clear Locks")) {
            swapper.clearLocks();
        }

        ImGui.textDisabled("Ctx1=Lock Slot1  Ctx2=Lock Slot2  Ctx3=Clear Locks");

        ImGui.end();

        if (!openBool.get()) {
            visible = false;
        }
    }

    @Override
    public void onContextKey(int index) {
        switch (index) {
            case 0 -> swapper.lockSelectedAsFirst();
            case 1 -> swapper.lockSelectedAsSecond();
            case 2 -> swapper.clearLocks();
        }
    }
}
