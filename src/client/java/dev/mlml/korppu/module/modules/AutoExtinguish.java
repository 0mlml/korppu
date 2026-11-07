package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoExtinguish extends Module {
    private final BooleanSetting requireBurning = config.add(new BooleanSetting("Require Burning", "Only trigger while on fire", true));
    private final BooleanSetting waterBucket = config.add(new BooleanSetting("Water Bucket", "Airplace water at feet to extinguish", true));
    private final BooleanSetting pickUpWater = config.add(new BooleanSetting("Pick Up Water", "Pick up placed water after extinguishing", true)
            .showOnlyIf(() -> waterBucket.getValue()));
    private final BooleanSetting useMilk = config.add(new BooleanSetting("Use Milk", "Use milk bucket as fallback for fire/effects", true));
    private final BooleanSetting cureNegativeEffects = config.add(new BooleanSetting("Cure Effects", "Use milk when harmful effects are present", true)
            .showOnlyIf(() -> useMilk.getValue()));
    private final BooleanSetting extinguishBlocks = config.add(new BooleanSetting("Extinguish Blocks", "Break fire blocks around the player", true));
    private final DoubleSetting extinguishRadius = config.add(new DoubleSetting("Extinguish Radius", "Radius to search for fire blocks", 4d, 1d, 8d, 0)
            .showOnlyIf(() -> extinguishBlocks.getValue()));
    private final DoubleSetting cooldownTicks = config.add(new DoubleSetting("Cooldown Ticks", "Minimum ticks between uses", 8d, 0d, 40d, 0));

    private int cooldown;
    private boolean hasPlacedWater = false;

    public AutoExtinguish() {
        super("AutoExtinguish", "Auto-uses water bucket from hotbar when burning", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        hasPlacedWater = false;
        cooldown = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null || KorppuMod.mc.level == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (hasPlacedWater && pickUpWater.getValue()) {
            int slot = findInHotbar(Items.BUCKET);
            if (slot >= 0) useItemLookingDown(slot);
            hasPlacedWater = false;
            cooldown = cooldownTicks.getValue().intValue();
            return;
        }

        boolean burning = KorppuMod.mc.player.isOnFire();
        boolean harmful = cureNegativeEffects.getValue() && hasHarmfulEffect();

        if (requireBurning.getValue() && !burning && !harmful) return;

        if (burning && waterBucket.getValue() && !hasFireResistance() && isNotNether()) {
            int slot = findInHotbar(Items.WATER_BUCKET);
            if (slot >= 0) {
                useItemLookingDown(slot);
                hasPlacedWater = true;
                cooldown = cooldownTicks.getValue().intValue();
                return;
            }
        }

        if (useMilk.getValue() && (burning || harmful)) {
            int slot = findInHotbar(Items.MILK_BUCKET);
            if (slot >= 0) {
                IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
                int prev = inv.getSelectedSlot();
                inv.setSelectedSlot(slot);
                KorppuMod.mc.gameMode.useItem(KorppuMod.mc.player, InteractionHand.MAIN_HAND);
                KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
                inv.setSelectedSlot(prev);
                cooldown = Math.max(10, cooldownTicks.getValue().intValue());
            }
        }

        if (extinguishBlocks.getValue()) {
            int radius = extinguishRadius.getValue().intValue();
            BlockPos origin = KorppuMod.mc.player.blockPosition();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = origin.offset(x, y, z);
                        var block = KorppuMod.mc.level.getBlockState(pos).getBlock();
                        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
                            KorppuMod.mc.getConnection().send(
                                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                            KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
                            KorppuMod.mc.getConnection().send(
                                    new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                        }
                    }
                }
            }
        }
    }

    private void useItemLookingDown(int slot) {
        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) KorppuMod.mc.player.getInventory();
        int prev = inv.getSelectedSlot();
        float prevPitch = KorppuMod.mc.player.getXRot();

        inv.setSelectedSlot(slot);
        KorppuMod.mc.player.setXRot(90f);
        KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                KorppuMod.mc.player.getYRot(), 90f,
                KorppuMod.mc.player.onGround(),
                KorppuMod.mc.player.horizontalCollision
        ));
        KorppuMod.mc.gameMode.useItem(KorppuMod.mc.player, InteractionHand.MAIN_HAND);
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        KorppuMod.mc.player.setXRot(prevPitch);

        inv.setSelectedSlot(prev);
    }

    private boolean isNotNether() {
        return KorppuMod.mc.level.dimension() != Level.NETHER;
    }

    private boolean hasFireResistance() {
        return KorppuMod.mc.player.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    private int findInHotbar(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = KorppuMod.mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) return slot;
        }
        return -1;
    }

    private boolean hasHarmfulEffect() {
        for (var effect : KorppuMod.mc.player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Require Burning", requireBurning.getValue()),
                StatusEntry.indicator("Water Bucket", waterBucket.getValue()),
                StatusEntry.indicator("Use Milk", useMilk.getValue()),
                StatusEntry.text("Cooldown", String.valueOf(Math.max(cooldown, 0)))
        );
    }
}
