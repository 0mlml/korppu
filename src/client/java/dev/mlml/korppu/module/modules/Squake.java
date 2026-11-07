package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Squake extends Module {

    final DoubleSetting airAccelerate = config.add(new DoubleSetting("Air Accel", "sv_airaccelerate", 14.0, 1.0, 200.0, 1));
    final DoubleSetting maxAirAccelPerTick = config.add(
            new DoubleSetting("Air Accel Cap", "Max speed added per tick by air accel (blocks/tick)", 0.045, 0.001, 1.0, 3));
    final DoubleSetting groundAccelerate = config.add(new DoubleSetting("Ground Accel", "sv_accelerate", 10.0, 1.0, 100.0, 1));
    final BooleanSetting autoBhop = config.add(
            new BooleanSetting("Auto Bhop", "Treat onGround+jumpHeld as airborne", true));
    final BooleanSetting uncappedBhop = config.add(
            new BooleanSetting("Uncapped Bhop", "No soft/hard cap on jump", true));
    final BooleanSetting trimpEnabled = config.add(
            new BooleanSetting("Trimp", "Shift+jump converts horizontal speed into vertical boost", true));
    final DoubleSetting trimpMultiplier = config.add(
            new DoubleSetting("Trimp Mult", "Vertical multiplier for trimp", 1.4, 0.1, 5.0, 1));
    final DoubleSetting softCapThreshold = config.add(
            new DoubleSetting("Soft Cap", "Soft cap as multiple of base move speed (uncapped bhop ignores)", 1.4, 0.1, 10.0, 1));
    final DoubleSetting softCapDegen = config.add(
            new DoubleSetting("Soft Cap Degen", "Fraction of excess speed removed by soft cap", 0.65, 0.0, 1.0, 2));
    final DoubleSetting hardCapThreshold = config.add(
            new DoubleSetting("Hard Cap", "Hard cap as multiple of base move speed (0 = off)", 2.0, 0.0, 20.0, 1));

    public Squake() {
        super("Squake", "Quake movement", GLFW.GLFW_KEY_UNKNOWN);
    }

    public boolean doTravel(LocalPlayer player, Vec3 input) {
        if (player.onClimbable()) return false;
        if (player.isInLava()) return false;
        if (player.isInWater()) return false;
        if (player.getAbilities().flying) return false;
//        if (player.isFallFlying()) return false;

        float sidemove = (float) input.x;
        float forwardmove = (float) input.z;

        float wishspeed = (sidemove != 0f || forwardmove != 0f) ? quake_getMoveSpeed(player) : 0f;
        float[] wishdir = getMovementDirection(player, sidemove, forwardmove);

        boolean jumpHeld = KorppuMod.mc.options.keyJump.isDown();
        boolean onGroundForReal = player.onGround() && !(autoBhop.getValue() && jumpHeld);

        if (onGroundForReal) {
            float slip = getSlipperiness(player);
            applyFriction(player, slip);
            if (wishspeed != 0f) {
                double accel = groundAccelerate.getValue() * minecraft_getMoveSpeed(player) * 2.15f / wishspeed;
                quake_Accelerate(player, wishspeed, wishdir[0], wishdir[1], accel);
            }
        } else {
            quake_AirAccelerate(player, wishspeed, wishdir[0], wishdir[1], airAccelerate.getValue());
        }
        return true;
    }

    public void afterJump(LocalPlayer player) {
        if (player.isSprinting()) {
            float rad = player.getYRot() * 0.017453292f;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x + Mth.sin(rad) * 0.2f, v.y, v.z - Mth.cos(rad) * 0.2f);
        }

        float movespeed = quake_getMaxMoveSpeed(player);
        applySoftCap(player, movespeed);
        if (!doTrimp(player, movespeed)) {
            applyHardCap(player, movespeed);
        }
    }

    private float quake_getMoveSpeed(LocalPlayer player) {
        float base = player.getSpeed();
        return player.isCrouching() ? base * 1.11f : base * 2.15f;
    }

    private float quake_getMaxMoveSpeed(LocalPlayer player) {
        return player.getSpeed() * 2.15f;
    }

    private float minecraft_getMoveSpeed(LocalPlayer player) {
        float slip = getSlipperiness(player);
        float f3 = 0.16277136f / (slip * slip * slip);
        return player.getSpeed() * f3;
    }

    private float[] getMovementDirection(LocalPlayer player, float sidemove, float forwardmove) {
        float f3 = sidemove * sidemove + forwardmove * forwardmove;
        if (f3 < 1e-4f) return new float[]{0f, 0f};
        f3 = 1f / Math.max(Mth.sqrt(f3), 1f);
        sidemove *= f3;
        forwardmove *= f3;
        float f4 = Mth.sin(player.getYRot() * (float) Math.PI / 180f);
        float f5 = Mth.cos(player.getYRot() * (float) Math.PI / 180f);
        return new float[]{sidemove * f5 - forwardmove * f4,
                           forwardmove * f5 + sidemove * f4
        };
    }

    void quake_AirAccelerate(LocalPlayer player, float wishspeed, float wishX, float wishZ, double accel) {
        float wishspd = Math.min(wishspeed, maxAirAccelPerTick.getValue().floatValue());
        Vec3 vel = player.getDeltaMovement();
        double current = vel.x * wishX + vel.z * wishZ;
        double addspeed = wishspd - current;
        if (addspeed <= 0) return;
        double accelspeed = Math.min(accel * wishspeed * 0.05, addspeed);
        player.setDeltaMovement(vel.x + accelspeed * wishX, vel.y, vel.z + accelspeed * wishZ);
    }

    void quake_Accelerate(LocalPlayer player, float wishspeed, float wishX, float wishZ, double accel) {
        Vec3 vel = player.getDeltaMovement();
        double current = vel.x * wishX + vel.z * wishZ;
        double addspeed = wishspeed - current;
        if (addspeed <= 0) return;
        double accelspeed = Math.min(accel * wishspeed * 0.05, addspeed);
        player.setDeltaMovement(vel.x + accelspeed * wishX, vel.y, vel.z + accelspeed * wishZ);
    }

    private void applyFriction(LocalPlayer player, float slipperiness) {
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * slipperiness, v.y, v.z * slipperiness);
    }

    public void applyGravity(LocalPlayer player) {
        double vy = player.getDeltaMovement().y - player.getGravity();
        vy *= 0.9800000190734863;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, vy, v.z);
    }

    private float getSlipperiness(LocalPlayer player) {
        if (!player.onGround()) return 0.91f;
        BlockPos pos = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 1, player.getZ());
        BlockState state = player.level().getBlockState(pos);
        return state.getBlock().getFriction();
    }

    private double getHSpeed(LocalPlayer player) {
        Vec3 v = player.getDeltaMovement();
        return Mth.sqrt((float) (v.x * v.x + v.z * v.z));
    }

    private void applySoftCap(LocalPlayer player, float movespeed) {
        if (uncappedBhop.getValue()) return;
        float softCap = movespeed * softCapThreshold.getValue().floatValue();
        double speed = getHSpeed(player);
        if (speed > softCap) {
            double degen = softCapDegen.getValue();
            if (degen != 1.0) {
                float applied = (float) ((speed - softCap) * degen + softCap);
                float multi = applied / (float) speed;
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * multi, v.y, v.z * multi);
            }
        }
    }

    private boolean doTrimp(LocalPlayer player, float movespeed) {
        if (!trimpEnabled.getValue() || !player.isCrouching()) return false;
        double speed = getHSpeed(player);
        if (speed <= movespeed) return false;
        double bonus = Math.min(speed / movespeed * 0.5, 1.0);
        double mult = trimpMultiplier.getValue();
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x / mult, v.y + bonus * speed * mult, v.z / mult);
        return true;
    }

    private void applyHardCap(LocalPlayer player, float movespeed) {
        if (uncappedBhop.getValue()) return;
        float hardCap = movespeed * hardCapThreshold.getValue().floatValue();
        if (hardCap <= 0) return;
        double speed = getHSpeed(player);
        if (speed > hardCap) {
            float multi = hardCap / (float) speed;
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * multi, v.y, v.z * multi);
        }
    }

    @Override
    public String getStatus() {
        if (KorppuMod.mc.player == null) return "idle";
        Vec3 v = KorppuMod.mc.player.getDeltaMovement();
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        return String.format("%.2f b/t", h);
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.indicator("Auto Bhop", autoBhop.getValue()));
        entries.add(StatusEntry.indicator("Uncapped", uncappedBhop.getValue()));
        entries.add(StatusEntry.indicator("Trimp", trimpEnabled.getValue()));
        if (KorppuMod.mc.player != null) {
            Vec3 v = KorppuMod.mc.player.getDeltaMovement();
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            entries.add(StatusEntry.text("H Speed", String.format("%.3f b/t", h)));
        }
        return entries;
    }
}
