package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.MultiSelectSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SurvivalQoL extends Module {
    private final MultiSelectSetting modes = config.add(new MultiSelectSetting(
            "Modes",
            "Enable one or more automation groups",
            SurvivalQoL::modeOptions,
            true,
            false,
            Set.of(Mode.FARM.name())));

    private final DoubleSetting farmRange = config.add(new DoubleSetting(
            "Farm Range", "Range used by farm automation", 4d, 1d, 8d, 1
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmTill = config.add(new BooleanSetting(
            "Farm Till", "Till dirt and grass for crops", true
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmHarvest = config.add(new BooleanSetting(
            "Farm Harvest", "Harvest mature crops", true
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmPlant = config.add(new BooleanSetting(
            "Farm Plant", "Plant available seeds", true
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmAlternateRows = config.add(new BooleanSetting(
            "Farm Alternate Rows", "Avoid planting next to crowded same-crop clusters", false
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmRowPriority = config.add(new BooleanSetting(
            "Farm Row Priority", "Prefer straight row continuation over diagonal patterns", true
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));
    private final BooleanSetting farmBonemeal = config.add(new BooleanSetting(
            "Farm Bonemeal", "Use bonemeal on supported crops", false
    ).showOnlyIf(() -> isEnabled(Mode.FARM)));

    private final MultiSelectSetting craftItems = config.add(new MultiSelectSetting(
            "Craft Items", "Items to auto-craft",
            () -> BuiltInRegistries.ITEM.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .filter(id -> !"minecraft:air".equals(id))
                    .sorted()
                    .collect(Collectors.toList()),
            true, false
    ).showOnlyIf(() -> isEnabled(Mode.CRAFT)));
    private final BooleanSetting craftAntiDesync = config.add(new BooleanSetting(
            "Craft Anti Desync", "Slow down actions to reduce ghost-crafts", true
    ).showOnlyIf(() -> isEnabled(Mode.CRAFT)));
    private final BooleanSetting craftAll = config.add(new BooleanSetting(
            "Craft All", "Shift-click outputs to craft full stacks", true
    ).showOnlyIf(() -> isEnabled(Mode.CRAFT)));
    private final BooleanSetting craftAutoDrop = config.add(new BooleanSetting(
            "Craft Auto Drop", "Drop crafted items after crafting", false
    ).showOnlyIf(() -> isEnabled(Mode.CRAFT)));

    private final DoubleSetting enchantLevel = config.add(new DoubleSetting(
            "Enchant Level", "Preferred enchant table level", 3d, 1d, 3d, 0
    ).showOnlyIf(() -> isEnabled(Mode.ENCHANT)));
    private final DoubleSetting enchantDelay = config.add(new DoubleSetting(
            "Enchant Delay", "Delay between enchant attempts in ticks", 2d, 0d, 20d, 0
    ).showOnlyIf(() -> isEnabled(Mode.ENCHANT)));
    private final MultiSelectSetting enchantItems = config.add(new MultiSelectSetting(
            "Enchant Items", "Whitelist of items to enchant",
            () -> BuiltInRegistries.ITEM.stream()
                    .filter(item -> item == Items.BOOK || new ItemStack(item).isEnchantable())
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .sorted()
                    .collect(Collectors.toList()),
            true, false
    ).showOnlyIf(() -> isEnabled(Mode.ENCHANT)));
    private final BooleanSetting enchantAutoDrop = config.add(new BooleanSetting(
            "Enchant Auto Drop", "Drop items when enchanting is complete", false
    ).showOnlyIf(() -> isEnabled(Mode.ENCHANT)));

    private final MultiSelectSetting grindItemWhitelist = config.add(new MultiSelectSetting(
            "Grind Item Whitelist", "Items to disenchant (empty = all enchanted items)",
            () -> BuiltInRegistries.ITEM.stream()
                    .filter(item -> item.components().get(DataComponents.DAMAGE) != null
                            || item == Items.ENCHANTED_BOOK)
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .sorted()
                    .collect(Collectors.toList()),
            true, false
    ).showOnlyIf(() -> isEnabled(Mode.GRIND)));
    private final MultiSelectSetting grindEnchantBlacklist = config.add(new MultiSelectSetting(
            "Grind Enchant Blacklist", "Enchantments that should not be removed",
            () -> {
                if (KorppuMod.mc.getConnection() == null) return List.of();
                return KorppuMod.mc.getConnection().registryAccess()
                        .lookup(Registries.ENCHANTMENT)
                        .map(reg -> reg.listElementIds()
                                .map(k -> k.identifier().toString())
                                .sorted()
                                .collect(Collectors.toList()))
                        .orElse(List.of());
            },
            true, false
    ).showOnlyIf(() -> isEnabled(Mode.GRIND)));
    private final DoubleSetting grindDelay = config.add(new DoubleSetting(
            "Grind Delay", "Delay between grindstone actions in ticks", 2d, 0d, 20d, 0
    ).showOnlyIf(() -> isEnabled(Mode.GRIND)));

    private int farmCooldown;
    private int craftCooldown;
    private int enchantCooldown;
    private int grindCooldown;

    public SurvivalQoL() {
        super("SurvivalQoL", "Combined survival automation module", GLFW.GLFW_KEY_UNKNOWN);
    }

    private boolean isEnabled(Mode mode) {
        return modes.isSelected(mode.name());
    }

    @Override
    public void onDisable() {
        farmCooldown = 0;
        craftCooldown = 0;
        enchantCooldown = 0;
        grindCooldown = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.gameMode == null) return;

        if (farmCooldown > 0) farmCooldown--;
        if (craftCooldown > 0) craftCooldown--;
        if (enchantCooldown > 0) enchantCooldown--;
        if (grindCooldown > 0) grindCooldown--;

        if (isEnabled(Mode.FARM)) tickFarm();
        if (isEnabled(Mode.CRAFT)) tickCraft();
        if (isEnabled(Mode.ENCHANT)) tickEnchant();
        if (isEnabled(Mode.GRIND)) tickGrind();
    }

    private void tickFarm() {
        if (farmCooldown > 0) return;

        int range = farmRange.getValue().intValue();
        BlockPos origin = KorppuMod.mc.player.blockPosition();

        for (int x = -range; x <= range; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > farmRange.getValue() * farmRange.getValue()) continue;

                    if (farmHarvest.getValue() && tryHarvest(pos)) return;
                    if (farmPlant.getValue() && tryPlant(pos)) return;
                    if (farmTill.getValue() && tryTill(pos)) return;
                    if (farmBonemeal.getValue() && tryBonemeal(pos)) return;
                }
            }
        }
    }

    private boolean tryHarvest(BlockPos pos) {
        BlockState state = KorppuMod.mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) return false;
        if (!crop.isMaxAge(state)) return false;

        Direction dir = Direction.UP;
        KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
        KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        farmCooldown = 2;
        return true;
    }

    private boolean tryPlant(BlockPos pos) {
        BlockState state = KorppuMod.mc.level.getBlockState(pos);
        if (state.getBlock() != Blocks.FARMLAND) return false;
        BlockPos above = pos.above();
        if (!KorppuMod.mc.level.getBlockState(above).isAir()) return false;

        int seedSlot = findSeedSlot(above);
        if (seedSlot < 0) return false;

        return interactWithHotbarItem(seedSlot, pos, Direction.UP, above);
    }

    private boolean tryTill(BlockPos pos) {
        BlockState state = KorppuMod.mc.level.getBlockState(pos);
        Block block = state.getBlock();
        if (block != Blocks.DIRT && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT_PATH) return false;
        BlockPos above = pos.above();
        if (!KorppuMod.mc.level.getBlockState(above).isAir()) return false;

        int hoeSlot = findHoeSlot();
        if (hoeSlot < 0) return false;

        return interactWithHotbarItem(hoeSlot, pos, Direction.UP, above);
    }

    private boolean tryBonemeal(BlockPos pos) {
        BlockState state = KorppuMod.mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) return false;
        if (crop.isMaxAge(state)) return false;

        int bonemealSlot = findItemSlot(Items.BONE_MEAL);
        if (bonemealSlot < 0) return false;

        return interactWithHotbarItem(bonemealSlot, pos, Direction.UP, pos);
    }

    private boolean interactWithHotbarItem(int slot, BlockPos targetBlock, Direction face, BlockPos hitPosBlock) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int previous = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        Vec3 hit = hitPosBlock.getCenter();
        BlockHitResult bhr = new BlockHitResult(hit, face, targetBlock, false);
        KorppuMod.mc.gameMode.useItemOn(KorppuMod.mc.player, InteractionHand.MAIN_HAND, bhr);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);

        inv.setSelectedSlot(previous);
        farmCooldown = 2;
        return true;
    }

    private int findHoeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HoeItem) return i;
        }
        return -1;
    }

    private int findSeedSlot(BlockPos plantPos) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            Block crop = cropBlockForItem(item);
            if (crop == null || isCrowdedForCrop(plantPos, crop)) continue;

            if (!farmAlternateRows.getValue() || !farmRowPriority.getValue()) {
                return i;
            }

            int score = rowPlantScore(plantPos, crop);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private Block cropBlockForItem(Item item) {
        if (item == Items.WHEAT_SEEDS) return Blocks.WHEAT;
        if (item == Items.BEETROOT_SEEDS) return Blocks.BEETROOTS;
        if (item == Items.CARROT) return Blocks.CARROTS;
        if (item == Items.POTATO) return Blocks.POTATOES;
        if (item == Items.NETHER_WART) return Blocks.NETHER_WART;
        if (item == Items.PUMPKIN_SEEDS) return Blocks.PUMPKIN_STEM;
        if (item == Items.MELON_SEEDS) return Blocks.MELON_STEM;
        if (item == Items.TORCHFLOWER_SEEDS) return Blocks.TORCHFLOWER_CROP;
        if (item == Items.PITCHER_POD) return Blocks.PITCHER_CROP;
        return null;
    }

    private boolean isCrowdedForCrop(BlockPos plantPos, Block cropBlock) {
        if (!farmAlternateRows.getValue()) return false;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = plantPos.relative(direction);
            if (KorppuMod.mc.level.getBlockState(adjacent).getBlock() != cropBlock) continue;
            if (countSameCropNeighbors(adjacent, cropBlock) >= 2) {
                return true;
            }
        }

        return false;
    }

    private int countSameCropNeighbors(BlockPos center, Block cropBlock) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos neighbor = center.offset(dx, 0, dz);
                if (KorppuMod.mc.level.getBlockState(neighbor).getBlock() == cropBlock) {
                    count++;
                }
            }
        }
        return count;
    }

    private int rowPlantScore(BlockPos plantPos, Block cropBlock) {
        int xAxis = 0;
        if (KorppuMod.mc.level.getBlockState(plantPos.east()).getBlock() == cropBlock) xAxis++;
        if (KorppuMod.mc.level.getBlockState(plantPos.west()).getBlock() == cropBlock) xAxis++;

        int zAxis = 0;
        if (KorppuMod.mc.level.getBlockState(plantPos.north()).getBlock() == cropBlock) zAxis++;
        if (KorppuMod.mc.level.getBlockState(plantPos.south()).getBlock() == cropBlock) zAxis++;

        int diagonal = 0;
        if (KorppuMod.mc.level.getBlockState(plantPos.north().east()).getBlock() == cropBlock) diagonal++;
        if (KorppuMod.mc.level.getBlockState(plantPos.north().west()).getBlock() == cropBlock) diagonal++;
        if (KorppuMod.mc.level.getBlockState(plantPos.south().east()).getBlock() == cropBlock) diagonal++;
        if (KorppuMod.mc.level.getBlockState(plantPos.south().west()).getBlock() == cropBlock) diagonal++;

        int major = Math.max(xAxis, zAxis);
        int minor = Math.min(xAxis, zAxis);

        int score = major * 12 - minor * 10 - diagonal * 3;

        if (major == 0 && diagonal > 0) score -= 10;
        if (major >= 1 && minor == 0 && diagonal == 0) score += 8;
        if (major == 2 && minor == 0) score += 4;

        return score;
    }

    private int findItemSlot(Item wanted) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == wanted) return i;
        }
        return -1;
    }

    private void tickCraft() {
        if (craftCooldown > 0) return;
        if (craftItems.getValue().isEmpty()) return;
        if (!(KorppuMod.mc.player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu craftHandler)) return;

        var ctx = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(KorppuMod.mc.level);
        for (net.minecraft.client.gui.screens.recipebook.RecipeCollection collection : KorppuMod.mc.player.getRecipeBook().getCollections()) {
            if (!collection.hasCraftable()) continue;
            for (net.minecraft.world.item.crafting.display.RecipeDisplayEntry recipe : collection.getSelectedRecipes(net.minecraft.client.gui.screens.recipebook.RecipeCollection.CraftableStatus.CRAFTABLE)) {
                boolean matches = recipe.resultItems(ctx).stream().anyMatch(stack ->
                        craftItems.isSelected(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
                if (!matches) continue;

                KorppuMod.mc.gameMode.handlePlaceRecipe(craftHandler.containerId, recipe.id(), craftAll.getValue());
                KorppuMod.mc.gameMode.handleContainerInput(craftHandler.containerId, 0, 1,
                        craftAutoDrop.getValue() ? ContainerInput.THROW : ContainerInput.QUICK_MOVE,
                        KorppuMod.mc.player);
                craftCooldown = craftAntiDesync.getValue() ? 4 : 1;
                return;
            }
        }
    }

    private void tickEnchant() {
        if (enchantCooldown > 0) return;
        if (!(KorppuMod.mc.player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu handler)) return;

        ItemStack itemSlot = handler.getSlot(0).getItem();
        ItemStack lapisSlot = handler.getSlot(1).getItem();

        if (!itemSlot.isEmpty() && itemSlot.isEnchanted()) {
            KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, 0,
                    enchantAutoDrop.getValue() ? 1 : 0,
                    enchantAutoDrop.getValue() ? ContainerInput.THROW : ContainerInput.QUICK_MOVE,
                    KorppuMod.mc.player);
            enchantCooldown = enchantDelay.getValue().intValue();
            return;
        }

        if (itemSlot.isEmpty()) {
            for (int i = 2; i < handler.slots.size(); i++) {
                ItemStack stack = handler.getSlot(i).getItem();
                if (stack.isEmpty()) continue;
                if (!enchantItems.getValue().isEmpty() && !enchantItems.isSelected(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) continue;
                KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, i, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
                enchantCooldown = enchantDelay.getValue().intValue();
                return;
            }
            return;
        }

        if (lapisSlot.getCount() < enchantLevel.getValue().intValue()) {
            for (int i = 2; i < handler.slots.size(); i++) {
                ItemStack stack = handler.getSlot(i).getItem();
                if (stack.getItem() == Items.LAPIS_LAZULI) {
                    KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, i, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
                    enchantCooldown = enchantDelay.getValue().intValue();
                    return;
                }
            }
            return;
        }

        KorppuMod.mc.gameMode.handleInventoryButtonClick(handler.containerId, Math.max(0, Math.min(2, enchantLevel.getValue().intValue() - 1)));
        enchantCooldown = enchantDelay.getValue().intValue();
    }

    private void tickGrind() {
        if (grindCooldown > 0) return;
        if (!(KorppuMod.mc.player.containerMenu instanceof net.minecraft.world.inventory.GrindstoneMenu handler)) return;

        ItemStack output = handler.getSlot(2).getItem();
        if (!output.isEmpty()) {
            KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, 2, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
            grindCooldown = grindDelay.getValue().intValue();
            return;
        }

        if (handler.getSlot(0).getItem().isEmpty()) {
            for (int i = 3; i < handler.slots.size(); i++) {
                if (canGrind(handler.getSlot(i).getItem())) {
                    KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, i, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
                    grindCooldown = grindDelay.getValue().intValue();
                    return;
                }
            }
        }
    }

    private boolean canGrind(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!grindItemWhitelist.getValue().isEmpty() && !grindItemWhitelist.isSelected(itemId)) return false;

        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty() && stored.isEmpty()) return false;

        for (var entry : enchants.entrySet()) {
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder = entry.getKey();
            String enchId = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (!enchId.isEmpty() && grindEnchantBlacklist.isSelected(enchId)) return false;
        }
        for (var entry : stored.entrySet()) {
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder = entry.getKey();
            String enchId = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (!enchId.isEmpty() && grindEnchantBlacklist.isSelected(enchId)) return false;
        }
        return true;
    }

    @Override
    public String getStatus() {
        long enabledModes = Arrays.stream(Mode.values()).filter(this::isEnabled).count();
        return enabledModes + " modes";
    }

    private static List<String> modeOptions() {
        return Arrays.stream(Mode.values()).map(Enum::name).toList();
    }

    public enum Mode {
        FARM,
        CRAFT,
        ENCHANT,
        GRIND
    }
}
