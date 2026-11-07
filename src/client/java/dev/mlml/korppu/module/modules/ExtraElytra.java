package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ExtraElytra extends Module {
    private final BooleanSetting instantFly = config.add(new BooleanSetting("Instant Fly", "Trigger elytra flight while falling by holding jump", true));
    private final BooleanSetting speedControl = config.add(new BooleanSetting("Speed Control", "Use forward/back keys to control elytra speed", true));
    private final BooleanSetting heightControl = config.add(new BooleanSetting("Height Control", "Use jump/sneak keys to control elytra altitude", false));
    private final BooleanSetting stopInWater = config.add(new BooleanSetting("Stop In Water", "Stop fall flying if submerged", true));
    private final DoubleSetting speedStep = config.add(new DoubleSetting("Speed Step", "Speed control acceleration", 0.05d, 0.01d, 1d, 2)
            .showOnlyIf(() -> speedControl.getValue()));

    private int jumpTimer;

    public ExtraElytra() {
        super("ExtraElytra", "Easier and more controllable elytra flight", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        jumpTimer = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.options == null || KorppuMod.mc.getConnection() == null) return;
        if (jumpTimer > 0) jumpTimer--;

        ItemStack chest = KorppuMod.mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) return;

        if (KorppuMod.mc.player.isFallFlying()) {
            if (stopInWater.getValue() && KorppuMod.mc.player.isUnderWater()) {
                sendStartFallFlying();
                return;
            }

            if (speedControl.getValue()) {
                applySpeedControl();
            }

            if (heightControl.getValue()) {
                applyHeightControl();
            }

            return;
        }

        if (!instantFly.getValue()) return;
        if (!KorppuMod.mc.options.keyJump.isDown()) return;
        if (KorppuMod.mc.player.onGround() || KorppuMod.mc.player.getDeltaMovement().y > -0.05d) return;
        if (chest.getDamageValue() >= chest.getMaxDamage() - 1) return;

        if (jumpTimer <= 0) {
            jumpTimer = 20;
            KorppuMod.mc.player.setSprinting(true);
            KorppuMod.mc.player.jumpFromGround();
        }

        sendStartFallFlying();
    }

    private void sendStartFallFlying() {
        KorppuMod.mc.getConnection().send(new ServerboundPlayerCommandPacket(
                KorppuMod.mc.player,
                ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    private void applySpeedControl() {
        float yaw = (float) Math.toRadians(KorppuMod.mc.player.getYRot());
        double step = speedStep.getValue();
        Vec3 forward = new Vec3(-Mth.sin(yaw) * step, 0d, Mth.cos(yaw) * step);
        Vec3 velocity = KorppuMod.mc.player.getDeltaMovement();

        if (KorppuMod.mc.options.keyUp.isDown()) {
            KorppuMod.mc.player.setDeltaMovement(velocity.add(forward));
        } else if (KorppuMod.mc.options.keyDown.isDown()) {
            KorppuMod.mc.player.setDeltaMovement(velocity.subtract(forward));
        }
    }

    private void applyHeightControl() {
        Vec3 velocity = KorppuMod.mc.player.getDeltaMovement();
        if (KorppuMod.mc.options.keyJump.isDown()) {
            KorppuMod.mc.player.setDeltaMovement(velocity.x, velocity.y + 0.08d, velocity.z);
        } else if (KorppuMod.mc.options.keyShift.isDown()) {
            KorppuMod.mc.player.setDeltaMovement(velocity.x, velocity.y - 0.04d, velocity.z);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Instant Fly", instantFly.getValue()),
                StatusEntry.indicator("Speed Control", speedControl.getValue()),
                StatusEntry.indicator("Height Control", heightControl.getValue()),
                StatusEntry.indicator("Water Stop", stopInWater.getValue())
        );
    }
}
