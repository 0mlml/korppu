package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.EntityDeathEvent;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.TrackedEntity;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.NotifierApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EntityInfo extends Module {

    private final DoubleSetting range = config.add(
            new DoubleSetting("Range", "Max crosshair targeting range (blocks)", 10d, 1d, 64d, 1));
    private final BooleanSetting showCrosshair = config.add(
            new BooleanSetting("Show Crosshair Info", "Show detail window for entity under crosshair", true));
    private final BooleanSetting showList = config.add(
            new BooleanSetting("Show Entity List", "Show the tracked entity list window", true));
    private final BooleanSetting notifyDeath = config.add(
            new BooleanSetting("Notify Tracked Death", "Notify when a tracked entity dies", true));
    private final BooleanSetting notifyRenderDistance = config.add(
            new BooleanSetting("Notify Render Distance", "Notify when a tracked entity enters/leaves render distance", false));

    private final BooleanSetting showIdentity = config.add(
            new BooleanSetting("Show Identity", "Show name/type/entity id", true));
    private final BooleanSetting showPosition = config.add(
            new BooleanSetting("Show Position", "Show position, velocity, yaw and pitch", true));
    private final BooleanSetting showHealth = config.add(
            new BooleanSetting("Show Health", "Show health and absorption for living entities", true));
    private final BooleanSetting showPotionEffects = config.add(
            new BooleanSetting("Show Potion Effects", "Show active potion/effect list", true));
    private final BooleanSetting showEquipment = config.add(
            new BooleanSetting("Show Equipment", "Show equipped armor and held items", true));
    private final BooleanSetting showPlayerDetails = config.add(
            new BooleanSetting("Show Player Details", "Show UUID, food, ping and gamemode", true));

    public EntityInfo() {
        super("EntityInfo", "Track and inspect entities", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onEnable() {
        if (showList.getValue())      KorppuImGui.getEntityListWindow().show();
        if (showCrosshair.getValue()) KorppuImGui.getEntityDetailWindow().show();
    }

    @Override
    public void onDisable() {
        KorppuImGui.getEntityListWindow().hide();
        KorppuImGui.getEntityDetailWindow().hide();
    }

    @Override
    public void onTick() {
        var listWindow   = KorppuImGui.getEntityListWindow();
        var detailWindow = KorppuImGui.getEntityDetailWindow();

        if (!listWindow.isVisible() && !detailWindow.isVisible()) {
            setEnabled(false);
            return;
        }

        listWindow.tick(KorppuMod.mc);
        if (notifyRenderDistance.getValue()) {
            for (TrackedEntity te : listWindow.getTracked()) {
                boolean nowOnline = te.isOnline();
                if (nowOnline != te.wasOnline) {
                    if (nowOnline) {
                        NotifierApi.push("Entity Tracked", te.getDisplayName() + " entered render distance");
                    } else {
                        NotifierApi.push("Entity Tracked", te.getDisplayName() + " left render distance");
                    }
                    te.wasOnline = nowOnline;
                }
            }
        }

        if (showCrosshair.getValue() && detailWindow.isVisible()) {
            detailWindow.setCrosshairEntity(findTarget());
        }
    }

    @Listener
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isEnabled()) return;
        LivingEntity entity = event.getEntity();
        var listWindow = KorppuImGui.getEntityListWindow();
        TrackedEntity tracked = listWindow.getTracked(entity.getUUID());
        if (tracked == null) return;

        tracked.updateFrom(entity);

        if (!(entity instanceof Player)) {
            listWindow.removeTracked(entity.getUUID());
        }

        if (!notifyDeath.getValue()) return;
        String name = entity instanceof Player p
                ? p.getDisplayName().getString()
                : entity.getType().getDescription().getString();
        NotifierApi.push("Tracked Entity Died", name + " has died");
    }

    private Entity findTarget() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return null;
        double r = range.getValue();
        Vec3 eye  = KorppuMod.mc.player.getEyePosition();
        Vec3 look = KorppuMod.mc.player.getViewVector(1.0f);
        Vec3 end  = eye.add(look.scale(r));

        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : KorppuMod.mc.level.entitiesForRendering()) {
            if (e == KorppuMod.mc.player) continue;
            AABB box = e.getBoundingBox().inflate(0.1);
            var hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    private static EntityInfo getInstance() {
        return dev.mlml.korppu.module.ModuleManager.getModule(EntityInfo.class);
    }

    public static boolean showIdentityEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showIdentity.getValue();
    }

    public static boolean showPositionEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showPosition.getValue();
    }

    public static boolean showHealthEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showHealth.getValue();
    }

    public static boolean showPotionEffectsEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showPotionEffects.getValue();
    }

    public static boolean showEquipmentEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showEquipment.getValue();
    }

    public static boolean showPlayerDetailsEnabled() {
        EntityInfo module = getInstance();
        return module == null || module.showPlayerDetails.getValue();
    }
}
