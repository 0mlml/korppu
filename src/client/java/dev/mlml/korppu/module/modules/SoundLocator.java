package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PlayedSoundEvent;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.module.NotifierApi;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SoundLocator extends Module {
    private final BooleanSetting whitelist = config.add(new BooleanSetting("Whitelist", "Only track selected sounds", false));
    private final MultiSelectSetting sounds = config.add(new MultiSelectSetting(
            "Sounds",
            "Sound ids to track",
            () -> BuiltInRegistries.SOUND_EVENT.stream()
                    .map(sound -> BuiltInRegistries.SOUND_EVENT.getKey(sound).toString())
                    .sorted()
                    .collect(Collectors.toList()),
            true,
            false
    ).showOnlyIf(() -> whitelist.getValue()));
    private final BooleanSetting logChat = config.add(new BooleanSetting("Log Chat", "Print tracked sound positions to chat", false));
    private final BooleanSetting popupNotify = config.add(new BooleanSetting("Popup Notify", "Show notification popup for tracked sounds", false));
    private final BooleanSetting renderPositions = config.add(new BooleanSetting("Render Positions", "Render boxes at recent sound positions", true));
    private final DoubleSetting keepTicks = config.add(new DoubleSetting("Keep Ticks", "How long to keep sound markers", 60d, 1d, 600d, 0)
            .showOnlyIf(() -> renderPositions.getValue()));

    private final List<Entry> entries = new ArrayList<>();

    public SoundLocator() {
        super("SoundLocator", "Tracks and renders played sound positions", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        entries.clear();
    }

    @Listener
    public void onSound(PlayedSoundEvent event) {
        if (!isEnabled()) return;

        if (whitelist.getValue()) {
            Set<String> selected = sounds.getValue();
            if (!selected.contains(event.getSoundId())) {
                return;
            }
        }

        Entry existing = null;
        for (Entry entry : entries) {
            if (entry.soundId.equals(event.getSoundId()) && entry.pos.distanceTo(event.getPosition()) < 1.0d) {
                existing = entry;
                break;
            }
        }

        int ttl = keepTicks.getValue().intValue();
        if (existing != null) {
            existing.ticks = ttl;
            existing.pos = event.getPosition();
        } else {
            entries.add(new Entry(event.getPosition(), event.getSoundId(), ttl));
        }

        if (logChat.getValue()) {
            Vec3 p = event.getPosition();
            ChatHelper.message(String.format("Sound %s at %.1f %.1f %.1f", event.getSoundId(), p.x, p.y, p.z));
        }

        if (popupNotify.getValue()) {
            NotifierApi.push("SoundLocator", event.getSoundId(), 1200);
        }
    }

    @Override
    public void onTick() {
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            entry.ticks--;
            if (entry.ticks <= 0) {
                iterator.remove();
            }
        }
    }

    @Override
    public void onWorldRender(com.mojang.blaze3d.vertex.PoseStack matrices, float tickProgress) {
        if (!renderPositions.getValue()) return;
        for (Entry entry : entries) {
            AABB box = new AABB(entry.pos.x - 0.5, entry.pos.y - 0.5, entry.pos.z - 0.5, entry.pos.x + 0.5, entry.pos.y + 0.5, entry.pos.z + 0.5);
            RenderUtils.drawOutlinedBox(matrices, box, 0xFFFF0000, false);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Whitelist", whitelist.getValue()),
                StatusEntry.indicator("Render", renderPositions.getValue()),
                StatusEntry.indicator("Popup", popupNotify.getValue()),
                StatusEntry.text("Markers", String.valueOf(entries.size()))
        );
    }

    private static class Entry {
        private Vec3 pos;
        private final String soundId;
        private int ticks;

        private Entry(Vec3 pos, String soundId, int ticks) {
            this.pos = pos;
            this.soundId = soundId;
            this.ticks = ticks;
        }
    }
}
