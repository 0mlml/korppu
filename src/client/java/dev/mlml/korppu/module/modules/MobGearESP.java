package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.EspManager;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class MobGearESP extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting(
            "Range", "Maximum detection range", 64d, 8d, 256d, 0));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets", Set.of("MOBS", "ANIMALS"));
    private final BooleanSetting showEmpty = config.add(new BooleanSetting("Show Unequipped", "Show entities with no gear", false));

    private static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public MobGearESP() {
        super("MobGearESP", "Highlights mobs wearing armor or holding items", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickDelta) {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return;

        StreamSupport.stream(KorppuMod.mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity)
                .filter(e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .filter(targets::matches)
                .filter(e -> e.distanceTo(KorppuMod.mc.player) <= range.getValue())
                .map(e -> (LivingEntity) e)
                .forEach(this::registerMobGear);
    }

    private void registerMobGear(LivingEntity entity) {
        boolean hasGear = false;
        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            if (!entity.getItemBySlot(slot).isEmpty()) {
                hasGear = true;
                break;
            }
        }

        if (!hasGear && !showEmpty.getValue()) return;

        int fill = hasGear ? 0xA0FFD700 : 0x40AAAAAA;
        int outline = hasGear ? 0xFFFFD700 : 0x60AAAAAA;
        EspManager.register(entity, fill, outline, EspManager.PRIORITY_SPECIALIZED);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return List.of();
        long count = StreamSupport.stream(KorppuMod.mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity && !(e instanceof net.minecraft.world.entity.player.Player))
                .filter(e -> e.distanceTo(KorppuMod.mc.player) <= range.getValue())
                .map(e -> (LivingEntity) e)
                .filter(e -> {
                    for (EquipmentSlot s : EQUIPMENT_SLOTS) if (!e.getItemBySlot(s).isEmpty()) return true;
                    return false;
                })
                .count();
        return List.of(StatusEntry.text("Equipped mobs", count + ""));
    }

    @Override
    public String getStatus() {
        return "Range: " + (range.getValue().intValue());
    }
}
