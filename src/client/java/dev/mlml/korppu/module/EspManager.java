package dev.mlml.korppu.module;

import dev.mlml.korppu.misc.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EspManager {
    public static final int PRIORITY_DEFAULT = -1;
    public static final int PRIORITY_SPECIALIZED = 0;
    public static final int PRIORITY_RELATION = 1;

    private record Entry(AABB box, int fill, int outline) {}
    private static final Map<UUID, Entry> entries = new HashMap<>();
    private static final Map<UUID, Integer> priorities = new HashMap<>();

    public static void beginFrame() {
        entries.clear();
        priorities.clear();
    }

    public static void register(Entity entity, int fill, int outline, int priority) {
        UUID id = entity.getUUID();
        if (priorities.getOrDefault(id, Integer.MIN_VALUE) < priority) {
            priorities.put(id, priority);
            entries.put(id, new Entry(entity.getBoundingBox(), fill, outline));
        }
    }

    public static void render(PoseStack matrices) {
        if (entries.isEmpty()) return;

        List<RenderUtils.ColoredBox> solids = new ArrayList<>(entries.size());
        List<RenderUtils.ColoredBox> outlines = new ArrayList<>(entries.size());
        for (Entry e : entries.values()) {
            solids.add(new RenderUtils.ColoredBox(e.box(), e.fill()));
            outlines.add(new RenderUtils.ColoredBox(e.box(), e.outline()));
        }
        RenderUtils.drawSolidBoxes(matrices, solids, false);
        RenderUtils.drawOutlinedBoxes(matrices, outlines, false);
    }
}
