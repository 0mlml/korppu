package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.imgui.EntityInfoRenderer;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.EspManager;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRelation;
import lombok.Getter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class WallHack extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "How far to see", 64d, 1d, 128d, 1));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets",
            EntityTargetSelector.FilterMode.Allowlist, Set.of("PLAYERS"));
    private final BooleanSetting colorRelations = config.add(new BooleanSetting("Color Relations", "Use green/red for friends/enemies", true));
    @Getter
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "How to render entities", Mode.Glowing));
    private final BooleanSetting nametags = config.add(new BooleanSetting("Nametags", "Render name/type label above entities", false));
    private final BooleanSetting skeletonEsp = config.add(new BooleanSetting("Skeleton ESP", "Draw player skeletons through walls", false));
    private final BooleanSetting skeletonDistanceColors = config.add(new BooleanSetting("Skeleton Distance CommonColors", "Color skeletons by distance", false)
            .showOnlyIf(() -> skeletonEsp.getValue()));

    public WallHack() {
        super("WallHack", "See through walls",  GLFW.GLFW_KEY_U);
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        if (KorppuMod.mc.level == null) return;

        if (mode.getValue() != Mode.Glowing) {
            for (Entity ent : KorppuMod.mc.level.entitiesForRendering()) {
                if (!checkShouldRender(ent)) continue;
                int outline = getOutlineColor(ent);
                int fill = (outline & 0x00FFFFFF) | 0x50000000;
                EspManager.register(ent, fill, outline, getEspPriority(ent));
            }
        }

        if (nametags.getValue()) {
            Vec3 camPos = RenderUtils.getCameraPos();
            for (Entity ent : KorppuMod.mc.level.entitiesForRendering()) {
                if (!checkShouldRender(ent)) continue;
                int color = getOutlineColor(ent);
                double x = ent.getX() - camPos.x;
                double y = ent.getY() + ent.getBbHeight() + 0.5 - camPos.y;
                double z = ent.getZ() - camPos.z;
                RenderUtils.drawText3d(matrices, EntityInfoRenderer.entityName(ent), x, y, z, color);
            }
        }

        if (skeletonEsp.getValue()) {
            for (Player player : KorppuMod.mc.level.players()) {
                if (player == KorppuMod.mc.player || !checkShouldRender(player)) continue;
                int color = skeletonDistanceColors.getValue() ? getDistanceColor(player) : getOutlineColor(player);
                drawPlayerSkeleton(matrices, player, color);
            }
        }
    }

    private int getDistanceColor(LivingEntity entity) {
        if (KorppuMod.mc.player == null) return 0xFFFFFFFF;

        double distance = KorppuMod.mc.player.distanceTo(entity);
        double percent = Math.clamp(distance / 60d, 0d, 1d);

        int red;
        int green;
        if (percent < 0.5d) {
            red = 255;
            green = (int) (255d * percent / 0.5d);
        } else {
            green = 255;
            red = 255 - (int) (255d * (percent - 0.5d) / 0.5d);
        }

        return (0xFF << 24) | (red << 16) | (green << 8);
    }

    private void drawPlayerSkeleton(PoseStack matrices, Player player, int color) {
        Vec3 origin = player.position();
        double bodyYaw = Math.toRadians(player.yBodyRot);

        double height = player.getBbHeight();
        double width = player.getBbWidth();
        boolean sneaking = player.isCrouching();

        double hipY = sneaking ? height * 0.45d : height * 0.5d;
        double shoulderY = sneaking ? height * 0.67d : height * 0.74d;
        double neckY = sneaking ? height * 0.75d : height * 0.8d;
        double headTopY = sneaking ? height * 0.9d : height * 0.95d;
        double armEndY = sneaking ? height * 0.4d : height * 0.38d;
        double footY = 0.05d;

        Vec3 neck = localPoint(origin, bodyYaw, 0d, 0d, neckY);
        Vec3 headTop = localPoint(origin, bodyYaw, 0d, 0d, headTopY);
        Vec3 pelvis = localPoint(origin, bodyYaw, 0d, sneaking ? 0.08d : 0d, hipY);

        Vec3 shoulderLeft = localPoint(origin, bodyYaw, -width * 0.55d, 0d, shoulderY);
        Vec3 shoulderRight = localPoint(origin, bodyYaw, width * 0.55d, 0d, shoulderY);
        Vec3 handLeft = localPoint(origin, bodyYaw, -width * 0.72d, 0.04d, armEndY);
        Vec3 handRight = localPoint(origin, bodyYaw, width * 0.72d, 0.04d, armEndY);

        Vec3 hipLeft = localPoint(origin, bodyYaw, -width * 0.3d, sneaking ? 0.08d : 0d, hipY);
        Vec3 hipRight = localPoint(origin, bodyYaw, width * 0.3d, sneaking ? 0.08d : 0d, hipY);
        Vec3 footLeft = localPoint(origin, bodyYaw, -width * 0.22d, 0d, footY);
        Vec3 footRight = localPoint(origin, bodyYaw, width * 0.22d, 0d, footY);

        RenderUtils.drawLine(matrices, neck, headTop, color, false);
        RenderUtils.drawLine(matrices, neck, pelvis, color, false);
        RenderUtils.drawLine(matrices, shoulderLeft, shoulderRight, color, false);
        RenderUtils.drawLine(matrices, shoulderLeft, handLeft, color, false);
        RenderUtils.drawLine(matrices, shoulderRight, handRight, color, false);
        RenderUtils.drawLine(matrices, hipLeft, hipRight, color, false);
        RenderUtils.drawLine(matrices, hipLeft, footLeft, color, false);
        RenderUtils.drawLine(matrices, hipRight, footRight, color, false);
    }

    private Vec3 localPoint(Vec3 origin, double yawRad, double sideOffset, double forwardOffset, double yOffset) {
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double x = origin.x + sideOffset * cos - forwardOffset * sin;
        double z = origin.z + sideOffset * sin + forwardOffset * cos;
        return new Vec3(x, origin.y + yOffset, z);
    }

    private int getOutlineColor(Entity ent) {
        if (ent instanceof Player p && colorRelations.getValue()) {
            PlayerRelation rel = PlayerManager.getInstance().getRelation(p.getUUID());
            if (rel == PlayerRelation.FRIEND) return 0xFF00FF77;
            if (rel == PlayerRelation.ENEMY) return 0xFFFF3333;
        }
        if (ent instanceof Player) return 0xFF00B7FF;
        if (ent instanceof Monster) return 0xFFFF5555;
        if (ent instanceof Animal) return 0xFF55FF55;
        if (ent instanceof ItemEntity) return 0xFFFFFF55;
        return 0xFFFFFFFF;
    }

    private int getEspPriority(Entity ent) {
        if (ent instanceof Player p && colorRelations.getValue()) {
            PlayerRelation rel = PlayerManager.getInstance().getRelation(p.getUUID());
            if (rel != PlayerRelation.NEUTRAL) return EspManager.PRIORITY_RELATION;
        }
        return EspManager.PRIORITY_DEFAULT;
    }

    public boolean checkShouldRender(Entity ent) {
        if (KorppuMod.mc.player == null) {
            return false;
        }

        if (ent == KorppuMod.mc.player) {
            return false;
        }

        if (KorppuMod.mc.player.position().distanceTo(ent.position()) > range.getValue()) {
            return false;
        }

        if (ent instanceof Player && AntiBot.isBot((Player) ent)) {
            return false;
        }

        return targets.matches(ent);
    }

    public enum Mode {
        Glowing,
        Boxes,
        GlowingAndBoxes
    }
}
