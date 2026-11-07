package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IPlayerInventoryMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InfiniteElytra extends Module {
    private final DoubleSetting elytraOnTicks = config.add(new DoubleSetting(
            "Elytra On Ticks", "Number of ticks to keep elytra equipped", 15d, 1d, 200d, 0));
    private final DoubleSetting elytraOffTicks = config.add(new DoubleSetting(
            "Elytra Off Ticks", "Number of ticks to keep elytra unequipped", 2d, 1d, 200d, 0));
    private final BooleanSetting fireRockets = config.add(new BooleanSetting(
            "Auto Fire Rockets", "Automatically use firework rockets to maintain flight", true));
    private final DoubleSetting timeBetweenRockets = config.add(new DoubleSetting(
            "Time Between Rockets", "Ticks between rocket fires", 40d, 1d, 200d, 0)
            .<DoubleSetting>showOnlyIf(() -> fireRockets.getValue()));

    private int tickCounter = 0;
    private boolean playerWasFlying = false;
    private boolean glidingTime = false;
    private int ticksSinceLastRocket = 0;

    public InfiniteElytra() {
        super("InfiniteElytra", "Automatically toggles elytra on/off to conserve durability and auto-uses rockets to maintain flight", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        tickCounter = 0;
        playerWasFlying = false;
        glidingTime = false;
        ticksSinceLastRocket = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;

        if (!playerWasFlying) playerWasFlying = KorppuMod.mc.player.isFallFlying();
        if (!playerWasFlying) return;

        tickCounter++;
        if (fireRockets.getValue()) ticksSinceLastRocket++;

        int onTicks = elytraOnTicks.getValue().intValue();
        int offTicks = elytraOffTicks.getValue().intValue();
        int total = onTicks + offTicks;
        int phase = tickCounter % total;

        if (phase == 0) tickCounter = 0;

        ItemStack chestStack = KorppuMod.mc.player.getItemBySlot(EquipmentSlot.CHEST);

        if (phase < onTicks) {
            glidingTime = true;
            if (chestStack.getItem() != Items.ELYTRA) {
                equipElytraFromInventory();
            }
            if (chestStack.getItem() == Items.ELYTRA && !KorppuMod.mc.player.onGround() && !KorppuMod.mc.player.isFallFlying()) {
                KorppuMod.mc.getConnection().send(
                        new ServerboundPlayerCommandPacket(KorppuMod.mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } else {
            glidingTime = false;
            if (chestStack.getItem() == Items.ELYTRA) {
                unequipElytra();
            }
        }

        if (glidingTime && fireRockets.getValue()) {
            if (ticksSinceLastRocket >= timeBetweenRockets.getValue().intValue()) {
                fireRocket();
            }
        }
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!(event.getPacket() instanceof ServerboundUseItemPacket interactPacket)) return;
        if (KorppuMod.mc.player == null) return;
        ItemStack stack = KorppuMod.mc.player.getItemInHand(interactPacket.getHand());
        if (stack.getItem() == Items.FIREWORK_ROCKET) {
            ticksSinceLastRocket = 0;
        }
    }

    private void equipElytraFromInventory() {
        var inventory = KorppuMod.mc.player.getInventory();
        var handler = KorppuMod.mc.player.containerMenu;
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i).getItem() == Items.ELYTRA) {
                int screenSlot = i < 9 ? 36 + i : i;
                KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, screenSlot, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
                break;
            }
        }
    }

    private void unequipElytra() {
        var inventory = KorppuMod.mc.player.getInventory();
        var handler = KorppuMod.mc.player.containerMenu;
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i).isEmpty()) {
                KorppuMod.mc.gameMode.handleContainerInput(handler.containerId, 6, 0, ContainerInput.QUICK_MOVE, KorppuMod.mc.player);
                break;
            }
        }
    }

    private void fireRocket() {
        var inventory = KorppuMod.mc.player.getInventory();
        int rocketSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).getItem() == Items.FIREWORK_ROCKET) {
                rocketSlot = i;
                break;
            }
        }
        if (rocketSlot < 0) return;

        IPlayerInventoryMixin inv = (IPlayerInventoryMixin) (Object) inventory;
        int currentSlot = inv.getSelectedSlot();
        if (rocketSlot != currentSlot) inv.setSelectedSlot(rocketSlot);
        KorppuMod.mc.gameMode.useItem(KorppuMod.mc.player, InteractionHand.MAIN_HAND);
        if (rocketSlot != currentSlot) inv.setSelectedSlot(currentSlot);
        ticksSinceLastRocket = 0;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.indicator("Active", playerWasFlying));
        entries.add(StatusEntry.indicator("Elytra On", glidingTime));
        entries.add(StatusEntry.indicator("Rockets", fireRockets.getValue()));
        return entries;
    }

    @Override
    public String getStatus() {
        if (!playerWasFlying) return "Waiting";
        return glidingTime ? "Elytra On" : "Elytra Off";
    }
}
