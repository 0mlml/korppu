package dev.mlml.korppu.module;

import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityTargetSelector {
    private final ListSetting<FilterMode> mode;
    private final MultiSelectSetting presets;
    private final MultiSelectSetting entityIds;

    public EntityTargetSelector(dev.mlml.korppu.config.Config config, String namePrefix, Set<String> defaults) {
        this(config, namePrefix, FilterMode.NoFilter, defaults);
    }

    public EntityTargetSelector(dev.mlml.korppu.config.Config config, String namePrefix, FilterMode defaultMode, Set<String> defaults) {
        this.mode = config.add(new ListSetting<>(namePrefix + " Mode", "Target filter strategy", defaultMode));
        this.presets = config.add(new MultiSelectSetting(
                namePrefix + " Presets",
                "Common target presets used by the selected mode",
                () -> List.of("PLAYERS", "HOSTILE", "MOBS", "ANIMALS", "LIVESTOCK", "VILLAGERS", "GOLEMS", "PIGMEN", "ITEMS", "OTHER"),
                true,
                false,
                defaults
        ).showOnlyIf(() -> mode.getValue() != FilterMode.NoFilter));

        this.entityIds = config.add(new MultiSelectSetting(
                namePrefix + " Entity IDs",
                "Manual entity type ids to include/exclude in addition to presets",
                () -> BuiltInRegistries.ENTITY_TYPE.stream()
                        .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())
                        .sorted()
                        .toList(),
                true,
                true
        ).showOnlyIf(() -> mode.getValue() != FilterMode.NoFilter));
    }

    public boolean matches(Entity entity) {
        if (entity == null) return false;
        if (mode.getValue() == FilterMode.NoFilter) return true;

        boolean inPreset = categories(entity).stream().anyMatch(presets::isSelected);
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        boolean inManualIds = entityIds.isSelected(typeId);
        boolean match = inPreset || inManualIds;

        return switch (mode.getValue()) {
            case Allowlist -> match;
            case Denylist -> !match;
            default -> true;
        };
    }

    public String summary() {
        return mode.getValue().name() + " (P:" + presets.getValue().size() + ", ID:" + entityIds.getValue().size() + ")";
    }

    private static List<String> categories(Entity e) {
        List<String> base = baseCategories(e);
        if (!(e instanceof Enemy)) return base;
        List<String> withHostile = new ArrayList<>(base);
        withHostile.add("HOSTILE");
        return withHostile;
    }

    private static List<String> baseCategories(Entity e) {
        if (e instanceof Player) return List.of("PLAYERS");
        if (e instanceof ItemEntity) return List.of("ITEMS");

        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();

        if (e instanceof Monster) {
            if (typeId.contains("piglin") || typeId.contains("pigman") || typeId.contains("zombified_piglin")) {
                return List.of("MOBS", "PIGMEN");
            }
            return List.of("MOBS");
        }

        if (e instanceof Animal) {
            if (typeId.contains("villager") || typeId.contains("trader")) {
                return List.of("ANIMALS", "VILLAGERS");
            }
            if (typeId.contains("golem")) {
                return List.of("ANIMALS", "GOLEMS");
            }
            if (typeId.contains("cow") || typeId.contains("pig") || typeId.contains("sheep") || typeId.contains("chicken")
                    || typeId.contains("goat") || typeId.contains("horse") || typeId.contains("llama") || typeId.contains("camel")
                    || typeId.contains("mooshroom") || typeId.contains("rabbit")) {
                return List.of("ANIMALS", "LIVESTOCK");
            }
            return List.of("ANIMALS");
        }

        if (typeId.contains("golem")) return List.of("GOLEMS", "OTHER");
        if (typeId.contains("villager") || typeId.contains("trader")) return List.of("VILLAGERS", "OTHER");
        if (typeId.contains("piglin") || typeId.contains("pigman") || typeId.contains("zombified_piglin")) return List.of("PIGMEN", "OTHER");
        return List.of("OTHER");
    }

    public enum FilterMode {
        NoFilter,
        Allowlist,
        Denylist
    }
}
