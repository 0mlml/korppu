package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.module.AirPlaceApi;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AirPlace extends Module {
    private final BooleanSetting render = config.add(new BooleanSetting("Render", "Renders a box where the block will be placed", true));
    private final BooleanSetting customRange = config.add(new BooleanSetting("Custom Range", "Use a custom placement range instead of the player's reach", false));
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "Custom placement range", 5d, 0d, 6d, 1)
            .showOnlyIf(customRange::getValue));

    public AirPlace() {
        super("AirPlace", "Places a block where your crosshair is pointing at", GLFW.GLFW_KEY_UNKNOWN);
    }

    public double getEffectiveRange() {
        if (customRange.getValue()) return range.getValue();
        if (KorppuMod.mc.player == null) return 4.5;
        return KorppuMod.mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickDelta) {
        if (!render.getValue()) return;
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;
        if (KorppuMod.mc.hitResult == null || KorppuMod.mc.hitResult.getType() != HitResult.Type.MISS) return;

        BlockHitResult target = AirPlaceApi.findPlacementTarget(KorppuMod.mc, getEffectiveRange());
        if (target == null) return;
        if (!KorppuMod.mc.level.getBlockState(target.getBlockPos()).canBeReplaced()) return;

        AABB box = new AABB(target.getBlockPos());
        RenderUtils.drawSolidBox(matrices, box, 0x10CC0000, false);
        RenderUtils.drawOutlinedBox(matrices, box, 0xFFCC0000, false);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Range", String.format("%.1f", getEffectiveRange()))
        );
    }
}
