package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.HudRenderEvent;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRelation;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.CommonColors;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HeadsUpDisplay extends Module  {
    public enum DisplayMode { SHOW_ALL, ALLOWLIST, DENYLIST }

    private final BooleanSetting showWatermark = config.add(new BooleanSetting("Show Watermark", "Shows the Korppu watermark", true));
    private final BooleanSetting showFPS = config.add(new BooleanSetting("Show FPS", "Shows your current FPS", true));
    private final BooleanSetting showCoordinates = config.add(new BooleanSetting("Show Coordinates", "Shows your current coordinates", false));
    private final BooleanSetting showVelocity = config.add(new BooleanSetting("Show Velocity", "Shows your current velocity", false));
    private final BooleanSetting showPing = config.add(new BooleanSetting("Show Ping", "Shows your current ping", false));
    private final BooleanSetting showPlayerCount = config.add(new BooleanSetting("Show Player Count", "Shows online player count from tab list", false));
    private final BooleanSetting showBiome = config.add(new BooleanSetting("Show Biome", "Shows current biome id", false));
    private final BooleanSetting showElytraStats = config.add(new BooleanSetting("Show Elytra Stats", "Shows equipped and inventory elytra info", false));
    private final BooleanSetting showTotemCount = config.add(new BooleanSetting("Show Totems", "Shows total totem count in inventory", true));
    private final BooleanSetting showRadar = config.add(new BooleanSetting("Show Radar", "Shows nearby players on a mini radar", false));
    private final ListSetting<RadarCorner> radarCorner = config.add(new ListSetting<>("Radar Corner", "Radar anchor corner", RadarCorner.TOP_RIGHT)
            .showOnlyIf(() -> showRadar.getValue()));
    private final dev.mlml.korppu.config.DoubleSetting radarRange = config.add(new dev.mlml.korppu.config.DoubleSetting("Radar Range", "Radar max range in blocks", 64d, 8d, 256d, 0)
            .showOnlyIf(() -> showRadar.getValue()));
    private final dev.mlml.korppu.config.DoubleSetting radarSize = config.add(new dev.mlml.korppu.config.DoubleSetting("Radar Size", "Radar size in pixels", 96d, 48d, 220d, 0)
            .showOnlyIf(() -> showRadar.getValue()));
    private final BooleanSetting radarFriends = config.add(new BooleanSetting("Radar Friends", "Render friends on radar", true)
            .showOnlyIf(() -> showRadar.getValue()));
    private final BooleanSetting radarEnemies = config.add(new BooleanSetting("Radar Enemies", "Render enemies on radar", true)
            .showOnlyIf(() -> showRadar.getValue()));
    private final BooleanSetting radarNeutrals = config.add(new BooleanSetting("Radar Neutrals", "Render neutrals on radar", true)
            .showOnlyIf(() -> showRadar.getValue()));
    private final dev.mlml.korppu.config.DoubleSetting radarMaxPlayers = config.add(new dev.mlml.korppu.config.DoubleSetting("Radar Max Players", "Max rendered radar points", 64d, 1d, 200d, 0)
            .showOnlyIf(() -> showRadar.getValue()));

    private final BooleanSetting alwaysShowEnabled = config.add(new BooleanSetting("Always Show Enabled", "Always show enabled " +
            "modules", true));
    private final ListSetting<DisplayMode> moduleDisplayMode = config.add(
            new ListSetting<>("Module Display", "How to filter which modules appear on the HUD", DisplayMode.ALLOWLIST));
    private final MultiSelectSetting moduleFilter = config.add(new MultiSelectSetting(
            "Module Filter", "Modules for the allowlist or denylist",
            () -> ModuleManager.getModules().stream().map(Module::getName).collect(Collectors.toList()),
            true, false,
            Set.of("Flight", "Speed", "NoFall", "ExtraElytra", "KillAura", "WallHack", "BlockESP", "Freecam", "FastMine")));
    private final BooleanSetting simplify = config.add(new BooleanSetting("Simplify", "Simplify the HUD", false));

    private final BooleanSetting keyControls = config.add(new BooleanSetting("Key Controls", "Allow controlling with keys", true));

    public final KeyMapping toggleKey = new KeyMapping("key.korppumod.hud_toggle", GLFW.GLFW_KEY_LEFT, kbCategory);
    public final KeyMapping upKey = new KeyMapping("key.korppumod.hud_up", GLFW.GLFW_KEY_UP, kbCategory);
    public final KeyMapping downKey = new KeyMapping("key.korppumod.hud_down", GLFW.GLFW_KEY_DOWN, kbCategory);
    public final KeyMapping settingsKey = new KeyMapping("key.korppumod.hud_settings", GLFW.GLFW_KEY_RIGHT, kbCategory);

    private int kcSelection = 0;
    private long kcShowingTicks = 0;
    private Module kcSelected = null;

    public HeadsUpDisplay() {
        super("HeadsUpDisplay", "Displays information on your screen", GLFW.GLFW_KEY_EQUAL);

        KeyMappingHelper.registerKeyMapping(toggleKey);
        KeyMappingHelper.registerKeyMapping(upKey);
        KeyMappingHelper.registerKeyMapping(downKey);
        KeyMappingHelper.registerKeyMapping(settingsKey);

        KorppuMod.eventManager.register(this);
    }

    private void renderText(GuiGraphicsExtractor drawContext, String text, Corner corner, int index) {
        int[] base = corner.getBase(KorppuMod.mc.font.width(text), KorppuMod.mc.font.lineHeight, index);

        drawContext.centeredText(KorppuMod.mc.font, text, base[0], base[1], CommonColors.BLACK);
    }

    @Override
    public void onTick() {
        if (kcShowingTicks > 0) {
            kcShowingTicks--;
        }

        if (!keyControls.getValue() || simplify.getValue()) {
            return;
        }

        boolean isVisible = kcShowingTicks > 0;
        int visibleCount = getVisibleModules().size();
        clampSelection(visibleCount);

        if (toggleKey.consumeClick()) {
            if (isVisible && kcSelected != null) {
                kcSelected.toggle();
            }
            kcShowingTicks = 100;
        }

        if (upKey.consumeClick()) {
            if (isVisible) {
                kcSelection--;
                wrapSelection(visibleCount);
            }
            kcShowingTicks = 100;
        }

        if (downKey.consumeClick()) {
            if (isVisible) {
                kcSelection++;
                wrapSelection(visibleCount);
            }
            kcShowingTicks = 100;
        }

        if (settingsKey.consumeClick()) {
            if (isVisible && kcSelected != null) {
                KorppuImGui.openModuleConfig(kcSelected);
            }
            kcShowingTicks = 100;
        }
    }

    @Listener
    public void render(HudRenderEvent hre) {
        if (!isEnabled()) {
            return;
        }

        GuiGraphicsExtractor drawContext = hre.getContext();

        List<Module> modules = getVisibleModules();
        clampSelection(modules.size());
        kcSelected = null;

        int count = 0;
        if (!simplify.getValue()) {
            for (Module m : modules) {


                String status = m.getStatus();
                String selection = "";
                if (kcSelection == count && kcShowingTicks > 0) {
                    kcSelected = m;
                    selection = TextFormatter.format("%2%3>%1 ", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GOLD);
                }

                String kb = m.getKeybind().getTranslatedKeyMessage().getString();
                if (m.getKeybind().isUnbound()) {
                    kb = "N/A";
                }
                String format = TextFormatter.format("%2%3[%4%s%3]%1 %5%s%s", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, TextFormatter.Code.WHITE, m.isEnabled() ? TextFormatter.Code.GREEN : TextFormatter.Code.RED, kb, m.getName(), status.isEmpty() ? "" : " : " + status);

                renderText(drawContext, selection + format, Corner.TOP_LEFT, count);
                count++;
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(TextFormatter.format("%1", TextFormatter.Code.RESET));
            int width = 0;
            for (Module m : modules) {


                sb.append(TextFormatter.format("%2%s%1, ", TextFormatter.Code.WHITE, m.getName(), m.isEnabled() ? TextFormatter.Code.GREEN : TextFormatter.Code.RED));
                width += KorppuMod.mc.font.width(m.getName() + ", ");

                if (width > KorppuMod.mc.getWindow().getGuiScaledWidth() / 3) {
                    renderText(drawContext, sb.toString(), Corner.TOP_LEFT, count);
                    count++;
                    sb = new StringBuilder();
                    width = 0;
                }
            }

            if (sb.toString().isEmpty()) {
                return;
            }

            sb.delete(sb.length() - 2, sb.length());
            renderText(drawContext, sb.toString(), Corner.TOP_LEFT, count);
        }

        if (!showWatermark.getValue()) {
            return;
        }
        renderText(drawContext, TextFormatter.format("[%2%4Fa%5rtWa%4re%1 v%2%3%s%1]", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, TextFormatter.Code.WHITE, TextFormatter.Code.BLUE, KorppuMod.VERSION), Corner.TOP_RIGHT, 0);
        count = 1;
        if (showFPS.getValue()) {
            renderText(drawContext, TextFormatter.format("FPS: %2%3%d", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.getFps()), Corner.TOP_RIGHT, count++);
        }
        if (showCoordinates.getValue()) {
            renderText(drawContext, TextFormatter.format("X: %2%3%.2f%1 Y: %2%3%.2f%1 Z: %2%3%.2f", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ()), Corner.TOP_RIGHT, count++);
        }
        if (showVelocity.getValue()) {
            renderText(drawContext, TextFormatter.format("Vel: %2%3%.2f", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY, KorppuMod.mc.player.getDeltaMovement().length()), Corner.TOP_RIGHT, count++);
        }
        if (showPing.getValue() && KorppuMod.mc.getConnection() != null) {
            var entry = KorppuMod.mc.getConnection().getPlayerInfo(KorppuMod.mc.player.getUUID());
            int ping = entry == null ? -1 : entry.getLatency();
            renderText(drawContext, TextFormatter.format("Ping: %2%3%s", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                    ping < 0 ? "N/A" : ping + " ms"), Corner.TOP_RIGHT, count++);
        }
        if (showPlayerCount.getValue() && KorppuMod.mc.getConnection() != null) {
            int online = KorppuMod.mc.getConnection().getListedOnlinePlayers().size();
            renderText(drawContext, TextFormatter.format("Players: %2%3%d", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                    online), Corner.TOP_RIGHT, count++);
        }
        if (showBiome.getValue() && KorppuMod.mc.level != null) {
            String biome = KorppuMod.mc.level.getBiome(KorppuMod.mc.player.blockPosition())
                    .unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
            renderText(drawContext, TextFormatter.format("Biome: %2%3%s", TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                    biome), Corner.TOP_RIGHT, count++);
        }
        if (showElytraStats.getValue()) {
            ItemStack equipped = KorppuMod.mc.player.getItemBySlot(EquipmentSlot.CHEST);
            int invElytras = 0;
            for (int i = 0; i < 36; i++) {
                if (KorppuMod.mc.player.getInventory().getItem(i).getItem() == Items.ELYTRA) {
                    invElytras++;
                }
            }

            if (equipped.getItem() == Items.ELYTRA) {
                int max = equipped.getMaxDamage();
                int damage = equipped.getDamageValue();
                int dur = Math.max(0, max - damage);
                int pct = max <= 0 ? 0 : (int) ((dur * 100.0) / max);
                renderText(drawContext, TextFormatter.format("Elytra: %2%3%d%% %1(%2%3%d%1/%2%3%d%1) Inv:%2%3%d",
                        TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                        pct, dur, max, invElytras), Corner.TOP_RIGHT, count++);
            } else {
                renderText(drawContext, TextFormatter.format("Elytra: %2%3none%1 Inv:%2%3%d",
                        TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                        invElytras), Corner.TOP_RIGHT, count++);
            }
        }
        if (showTotemCount.getValue()) {
            int countTotems = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    countTotems += stack.getCount();
                }
            }
            renderText(drawContext, TextFormatter.format("Totems: %2%3%d",
                    TextFormatter.Code.RESET, TextFormatter.Code.BOLD, TextFormatter.Code.GRAY,
                    countTotems), Corner.TOP_RIGHT, count++);
        }

        if (showRadar.getValue() && KorppuMod.mc.player != null && KorppuMod.mc.level != null) {
            renderRadar(drawContext);
        }
    }

    private void renderRadar(GuiGraphicsExtractor dc) {
        int size = radarSize.getValue().intValue();
        int pad = 6;
        int x = radarCorner.getValue().x(size, pad);
        int y = radarCorner.getValue().y(size, pad);
        int centerX = x + size / 2;
        int centerY = y + size / 2;

        dc.fill(x, y, x + size, y + size, 0x80000000);
        dc.fill(x, y, x + size, y + 1, 0xCCFFFFFF);
        dc.fill(x, y + size - 1, x + size, y + size, 0xCCFFFFFF);
        dc.fill(x, y, x + 1, y + size, 0xCCFFFFFF);
        dc.fill(x + size - 1, y, x + size, y + size, 0xCCFFFFFF);
        dc.fill(centerX - 1, y + 2, centerX + 1, y + size - 2, 0x55FFFFFF);
        dc.fill(x + 2, centerY - 1, x + size - 2, centerY + 1, 0x55FFFFFF);
        dc.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFFFFFFF);

        double maxRange = radarRange.getValue();
        float yawRad = (float) Math.toRadians(KorppuMod.mc.player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double scale = (size * 0.45d) / maxRange;

        int rendered = 0;
        for (var player : KorppuMod.mc.level.players()) {
            if (player.getUUID().equals(KorppuMod.mc.player.getUUID())) continue;
            if (rendered >= radarMaxPlayers.getValue().intValue()) break;

            double dx = player.getX() - KorppuMod.mc.player.getX();
            double dz = player.getZ() - KorppuMod.mc.player.getZ();
            double distance = Math.hypot(dx, dz);
            if (distance > maxRange) continue;

            double rx = -(dx * cos + dz * sin);
            double rz = dz * cos - dx * sin;
            int px = centerX + (int) Math.max(-size * 0.45d, Math.min(size * 0.45d, rx * scale));
            int py = centerY + (int) Math.max(-size * 0.45d, Math.min(size * 0.45d, rz * scale));

            int color = 0xFFFF5555;
            PlayerRelation rel = PlayerManager.getInstance().getRelation(player.getUUID());
            if (rel == PlayerRelation.FRIEND) {
                if (!radarFriends.getValue()) continue;
                color = 0xFF55FF55;
            } else if (rel == PlayerRelation.ENEMY) {
                if (!radarEnemies.getValue()) continue;
                color = 0xFFFF5555;
            } else {
                if (!radarNeutrals.getValue()) continue;
                color = 0xFFFFFF55;
            }

            dc.fill(px - 2, py - 2, px + 2, py + 2, color);
            rendered++;
        }
    }

    private List<Module> getVisibleModules() {
        return ModuleManager.getModules().stream()
                .filter(m -> switch (moduleDisplayMode.getValue()) {
                    case SHOW_ALL -> true;
                    case ALLOWLIST -> moduleFilter.isSelected(m.getName()) || (alwaysShowEnabled.getValue() && m.isEnabled());
                    case DENYLIST -> !moduleFilter.isSelected(m.getName()) || (alwaysShowEnabled.getValue() && m.isEnabled());
                })
                .collect(Collectors.toList());
    }

    private void clampSelection(int size) {
        if (size <= 0) {
            kcSelection = 0;
            return;
        }
        if (kcSelection < 0) kcSelection = 0;
        if (kcSelection >= size) kcSelection = size - 1;
    }

    private void wrapSelection(int size) {
        if (size <= 0) {
            kcSelection = 0;
            return;
        }
        if (kcSelection < 0) kcSelection = size - 1;
        if (kcSelection >= size) kcSelection = 0;
    }

    @Override
    public String getStatus() {
        int enabled = ModuleManager.getModules().stream().filter(Module::isEnabled).toArray().length;

        return TextFormatter.format("%1%s | %2%s", TextFormatter.Code.GREEN, enabled, TextFormatter.Code.RED, ModuleManager.getModules().size() - enabled);
    }

    private enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public int[] getBase(int textWidth, int textHeight, int row) {
            int gap = 2;

            return switch (this) {
                case TOP_LEFT -> new int[]{gap + textWidth / 2, gap + (textHeight + gap) * row};
                case TOP_RIGHT ->
                        new int[]{KorppuMod.mc.getWindow().getGuiScaledWidth() - textWidth / 2 - gap, gap + (textHeight + gap) * row};
                case BOTTOM_LEFT ->
                        new int[]{gap + textWidth / 2, KorppuMod.mc.getWindow().getGuiScaledHeight() + gap + (textHeight + gap) * row};
                case BOTTOM_RIGHT ->
                        new int[]{KorppuMod.mc.getWindow().getGuiScaledWidth() - textWidth / 2 - gap, KorppuMod.mc.getWindow().getGuiScaledHeight() + gap + (textHeight + gap) * row};
            };
        }
    }

    public enum RadarCorner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        private int x(int size, int pad) {
            return switch (this) {
                case TOP_LEFT, BOTTOM_LEFT -> pad;
                case TOP_RIGHT, BOTTOM_RIGHT -> KorppuMod.mc.getWindow().getGuiScaledWidth() - size - pad;
            };
        }

        private int y(int size, int pad) {
            return switch (this) {
                case TOP_LEFT, TOP_RIGHT -> pad;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> KorppuMod.mc.getWindow().getGuiScaledHeight() - size - pad;
            };
        }
    }
}
