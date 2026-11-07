package dev.mlml.korppu.imgui;

import dev.mlml.korppu.module.modules.EntityInfo;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EntityInfoRenderer {
    private EntityInfoRenderer() {}

    public static String entityName(Entity e) {
        if (e.hasCustomName() && e.getCustomName() != null)
            return e.getCustomName().getString();
        if (e instanceof Player p)
            return p.getDisplayName().getString();
        return e.getType().getDescription().getString();
    }

    public static void pushArgbColor(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        ImGui.pushStyleColor(ImGuiCol.PlotHistogram, r, g, b, a);
    }

    public static void renderEntity(Minecraft mc, Entity entity, HpTracker tracker) {
        if (EntityInfo.showIdentityEnabled()) {
            String typeName = entity.getType().getDescription().getString();
            String displayName = entityName(entity);
            if (!displayName.equals(typeName)) {
                ImGui.text("Name: " + displayName);
                ImGui.text("Type: " + typeName);
            } else {
                ImGui.text("Type: " + typeName);
            }
            ImGui.text("Entity ID: " + entity.getId());
        }

        if (EntityInfo.showPositionEnabled()) {
            ImGui.separator();
            ImGui.text(String.format("Pos: %.2f, %.2f, %.2f", entity.getX(), entity.getY(), entity.getZ()));
            if (mc.player != null)
                ImGui.text(String.format("Distance: %.2f m", mc.player.distanceTo(entity)));
            var vel = entity.getDeltaMovement();
            double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            ImGui.text(String.format("Vel: %.3f, %.3f, %.3f  (%.3f m/t)", vel.x, vel.y, vel.z, speed));
            ImGui.text(String.format("Yaw: %.1f  Pitch: %.1f", entity.getYRot(), entity.getXRot()));
        }

        if (entity instanceof LivingEntity le) {
            if (EntityInfo.showHealthEnabled()) {
                ImGui.separator();
                renderHealth(le, tracker);

                if (le.getAbsorptionAmount() > 0)
                    ImGui.text(String.format("Absorption: %.1f", le.getAbsorptionAmount()));
                if (le.isOnFire())
                    ImGui.text("On fire!");
                if (le.fallDistance > 0.5f)
                    ImGui.text(String.format("Fall: %.1f", le.fallDistance));
            }

            if (EntityInfo.showPotionEffectsEnabled()) {
                renderEffects(le);
            }
            if (EntityInfo.showEquipmentEnabled()) {
                renderEquipment(le, "ent_eq_" + entity.getId());
            }

            if (entity instanceof Player player && EntityInfo.showPlayerDetailsEnabled()) {
                ImGui.separator();
                ImGui.text("UUID: " + player.getUUID());
                ImGui.text(String.format("Food: %d  Sat: %.1f",
                        player.getFoodData().getFoodLevel(),
                        player.getFoodData().getSaturationLevel()));
                ClientPacketListener net = mc.getConnection();
                if (net != null) {
                    UUID uuid = player.getUUID();
                    PlayerInfo ple = net.getPlayerInfo(uuid);
                    if (ple != null) {
                        ImGui.text("Ping: " + ple.getLatency() + " ms");
                        if (ple.getGameMode() != null)
                            ImGui.text("Gamemode: " + ple.getGameMode().name());
                    }
                }
            }
        }
    }

    public static void renderSnapshot(TrackedEntity tracked) {
        if (EntityInfo.showIdentityEnabled()) {
            String typeName = tracked.lastKnownType != null ? tracked.lastKnownType : "unknown";
            String displayName = tracked.getDisplayName();
            if (!displayName.equals(typeName)) {
                ImGui.text("Name: " + displayName);
                ImGui.text("Type: " + typeName);
            } else {
                ImGui.text("Type: " + typeName);
            }
            if (tracked.lastKnownEntityId >= 0) {
                ImGui.text("Entity ID: " + tracked.lastKnownEntityId + " (last seen)");
            }
        }

        if (EntityInfo.showPositionEnabled()) {
            ImGui.separator();
            ImGui.text(String.format("Pos: %.2f, %.2f, %.2f", tracked.lastKnownX, tracked.lastKnownY, tracked.lastKnownZ));
            double speed = Math.sqrt(tracked.lastKnownVx * tracked.lastKnownVx + tracked.lastKnownVz * tracked.lastKnownVz);
            ImGui.text(String.format("Vel: %.3f, %.3f, %.3f  (%.3f m/t)", tracked.lastKnownVx, tracked.lastKnownVy, tracked.lastKnownVz, speed));
            ImGui.text(String.format("Yaw: %.1f  Pitch: %.1f", tracked.lastKnownYaw, tracked.lastKnownPitch));
        }

        if (EntityInfo.showHealthEnabled() && tracked.lastKnownHp >= 0 && tracked.lastKnownMaxHp > 0) {
            ImGui.separator();
            float frac = tracked.lastKnownHp / tracked.lastKnownMaxHp;
            ImGui.text(String.format("Last HP: %.1f / %.1f", tracked.lastKnownHp, tracked.lastKnownMaxHp));
            int barCol = frac > 0.6f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
            pushArgbColor(barCol);
            ImGui.progressBar(frac, -1f, 6f, "");
            ImGui.popStyleColor();
        }
    }

    public static void renderHealth(LivingEntity le, HpTracker tracker) {
        float hp = le.getHealth();
        float maxHp = le.getMaxHealth();
        float frac = maxHp > 0 ? hp / maxHp : 0;

        float delta = tracker != null ? tracker.getDelta() : 0f;
        if (delta != 0f) {
            ImGui.text(String.format("HP %.1f/%.1f ", hp, maxHp));
            ImGui.sameLine();
            pushArgbColor(delta < 0 ? 0xFFFF5555 : 0xFF55FF55);
            ImGui.text(String.format("%+.1f", delta));
            ImGui.popStyleColor();
        } else {
            ImGui.text(String.format("HP %.1f/%.1f", hp, maxHp));
        }

        int barCol = frac > 0.6f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
        pushArgbColor(barCol);
        ImGui.progressBar(frac, -1f, 6f, "");
        ImGui.popStyleColor();
    }

    public static void renderEffects(LivingEntity le) {
        var effects = le.getActiveEffects();
        if (effects.isEmpty()) return;
        ImGui.separator();
        ImGui.textDisabled("Effects");
        for (MobEffectInstance fx : effects) {
            String name = fx.getEffect().value().getDisplayName().getString();
            int amp = fx.getAmplifier();
            int dur = fx.getDuration();
            String label = amp > 0 ? name + " " + (amp + 1) : name;
            ImGui.text(String.format("%s  %ds", label, dur / 20));
        }
    }

    public static void renderEquipment(LivingEntity le, String tableId) {
        boolean hasAny = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!le.getItemBySlot(slot).isEmpty()) { hasAny = true; break; }
        }
        if (!hasAny) return;

        ImGui.separator();
        ImGui.textDisabled("Equipment");
        if (!ImGui.beginTable("##" + tableId, 2,
                ImGuiTableFlags.SizingStretchProp | ImGuiTableFlags.RowBg)) return;
        ImGui.tableSetupColumn("Slot", 0, 0.3f);
        ImGui.tableSetupColumn("Item", 0, 0.7f);
        ImGui.tableHeadersRow();
        renderSlot("Head",      le.getItemBySlot(EquipmentSlot.HEAD));
        renderSlot("Chest",     le.getItemBySlot(EquipmentSlot.CHEST));
        renderSlot("Legs",      le.getItemBySlot(EquipmentSlot.LEGS));
        renderSlot("Feet",      le.getItemBySlot(EquipmentSlot.FEET));
        renderSlot("Main Hand", le.getItemBySlot(EquipmentSlot.MAINHAND));
        renderSlot("Off Hand",  le.getItemBySlot(EquipmentSlot.OFFHAND));
        ImGui.endTable();
    }

    public static void renderSlot(String label, ItemStack stack) {
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(0);
        ImGui.textDisabled(label);
        ImGui.tableSetColumnIndex(1);
        if (stack.isEmpty()) { ImGui.textDisabled("(empty)"); return; }

        String name = stack.getHoverName().getString();
        boolean hasEnchants = !stack.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
        if (hasEnchants) name += " *";
        if (stack.getCount() > 1) name += " x" + stack.getCount();
        ImGui.text(name);
        if (ImGui.isItemClicked()) KorppuImGui.openItemModal(stack);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.textDisabled("Click to inspect");
            ImGui.endTooltip();
        }
        if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
            float rem = (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
            int c = rem < 0.25f ? 0xFFFF5555 : rem < 0.6f ? 0xFFFFAA00 : 0xFF55FF55;
            pushArgbColor(c);
            ImGui.progressBar(rem, -1f, 6f, "");
            ImGui.popStyleColor();
        }
        if (hasEnchants && ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                    .entrySet()
                    .forEach(e -> {
                        String en = e.getKey().value().description().getString();
                        int lvl = e.getIntValue();
                        ImGui.text(lvl == 1 ? en : en + " " + lvl);
                    });
            ImGui.endTooltip();
        }
    }
}
