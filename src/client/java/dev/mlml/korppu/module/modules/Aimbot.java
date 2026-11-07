package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.imgui.windows.AimbotControlWindow;
import dev.mlml.korppu.imgui.windows.ModuleControlWindow;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.TargetProtection;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Aimbot extends Module {

    private final ListSetting<Priority> priority = config.add(
            new ListSetting<>("Priority", "How to pick the target", Priority.AngleDist));
    private final DoubleSetting maxRange = config.add(
            new DoubleSetting("Max Range", "Maximum target distance in blocks", 60d, 1d, 200d, 0));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets",
            EntityTargetSelector.FilterMode.Allowlist, Set.of("PLAYERS", "HOSTILE"));
    private final TargetProtection protection = new TargetProtection(config);
    private final ListSetting<RotationMode> rotationMode = config.add(
            new ListSetting<>("Rotation Mode", "How to rotate toward the target", RotationMode.Instant));
    private final DoubleSetting smoothness = config.add(
            new DoubleSetting("Smoothness", "Laziness divisor for smooth rotation (higher = slower)", 3d, 1d, 20d, 1));
    private final DoubleSetting slackRadius = config.add(
            new DoubleSetting("Slack Radius", "Min angle to target (degrees) before aiming kicks in", 0d, 0d, 30d, 1));
    private final BooleanSetting skipOnCooldown = config.add(
            new BooleanSetting("Skip on Cooldown", "Suppress aim while melee weapon is on attack cooldown", true));
    private final BooleanSetting requireFullCharge = config.add(
            new BooleanSetting("Require Full Charge", "Bow only aims when fully charged", false));
    private final BooleanSetting showTrajectory = config.add(
            new BooleanSetting("Trajectory", "Always show simulated projectile path when holding a throwable item", true));
    private final BooleanSetting autofireWhenLocked = config.add(
            new BooleanSetting("Autofire When Locked", "Automatically fires once when a valid aim solution is found", false));
    private final BooleanSetting crossbowHoldAutofire = config.add(
            new BooleanSetting("Crossbow Hold Autofire", "Allow autofire while right-click is held on crossbow", false));
    private final BooleanSetting noSlow = config.add(
            new BooleanSetting("No Slow", "Remove movement slowdown while using items when aiming", false));

    // Physics constants from Meteor's ProjectileEntitySimulator
    private static final double GRAVITY_ARROW    = 0.05;
    private static final double GRAVITY_THROWN   = 0.03;
    private static final double GRAVITY_EXP_BOTTLE = 0.07;
    private static final double GRAVITY_POTION   = 0.05;
    private static final double DRAG_ARROW       = 0.99;
    private static final double DRAG_THROWN      = 0.99;
    private static final int    SIM_STEPS        = 200;

    private record Physics(double speed, double gravity, double drag) {}

    private Item cachedPhysicsItem;
    private Physics cachedPhysics;

    private Entity target;
    private float currentCharge;
    private List<Vec3> solvedTrajectory;   // aimbot-solved path (shown in green)
    private List<Vec3> previewTrajectory;  // always-on crosshair path (shown in yellow/grey)
    private AimMode currentMode;
    private float solvedPitch;
    private float solvedYaw;
    private boolean targetLocked;
    private boolean angleLocked;
    private boolean autofireLatched;
    private int lastCandidateCount;
    private UUID manualTargetId;

    public Aimbot() {
        super("Aimbot", "Automatically aims at targets based on held item", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        clearAimState();
        clearTargetControlState();
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            clearAimState();
            clearTargetControlState();
            return;
        }

        var stack = KorppuMod.mc.player.getMainHandItem();
        var item = stack.getItem();

        currentMode = detectMode(item, stack);
        currentCharge = 0;

        switch (currentMode) {
            case Bow -> {
                if (!KorppuMod.mc.player.isUsingItem()) {
                    clearAimState();
                    updatePreviewTrajectory(item, stack);
                    return;
                }
                int pullback = ((BowItem) item).getUseDuration(stack, KorppuMod.mc.player)
                        - KorppuMod.mc.player.getUseItemRemainingTicks();
                currentCharge = BowItem.getPowerForTime(pullback);
                float minCharge = requireFullCharge.getValue() ? 1.0f : 0.1f;
                if (currentCharge < minCharge) {
                    clearAimState();
                    return;
                }
            }
            case Crossbow -> {
                if (!CrossbowItem.isCharged(stack)) {
                    clearAimState();
                    return;
                }
                currentCharge = 1.0f;
            }
            case Trident -> {
                if (!KorppuMod.mc.player.isUsingItem()) {
                    clearAimState();
                    updatePreviewTrajectory(item, stack);
                    return;
                }
                currentCharge = 1.0f;
            }
            case Throwable -> currentCharge = 1.0f;
            case Melee -> {
                if (skipOnCooldown.getValue() && KorppuMod.mc.player.getAttackStrengthScale(0.5f) < 1.0f) {
                    clearAimState();
                    return;
                }
            }
            default -> {}
        }

        Vec3 eyePos = KorppuMod.mc.player.getEyePosition();
        List<Entity> candidates = findCandidates(eyePos, resolveTargetRange());
        lastCandidateCount = candidates.size();
        target = selectTarget(candidates);

        // Always update preview trajectory for throwables/bow-while-charging
        if (showTrajectory.getValue() && currentMode != AimMode.Melee && currentMode != AimMode.Default) {
            updatePreviewTrajectory(item, stack);
        } else {
            previewTrajectory = null;
        }

        if (target == null) {
            clearAimState();
            return;
        }

        double slack = slackRadius.getValue();
        if (slack > 0 && angleTo(target) <= slack) {
            solvedTrajectory = null;
            return;
        }

        Vec3 aimPos = getCenterMassPoint(target);
        Physics phys = resolvePhysics(currentMode, item, stack);
        if (phys != null) {
            aimBallistic(eyePos, aimPos, phys);
        } else {
            aimDirect(aimPos);
        }
    }

    // --- Physics resolution ---

    private Physics resolvePhysics(AimMode mode, Item item, ItemStack stack) {
        return switch (mode) {
            case Bow      -> new Physics(currentCharge * 3.0, GRAVITY_ARROW, DRAG_ARROW);
            case Crossbow -> new Physics(3.15, GRAVITY_ARROW, DRAG_ARROW);
            case Trident  -> new Physics(2.5, GRAVITY_ARROW, DRAG_ARROW);
            case Throwable -> {
                if (item == cachedPhysicsItem && cachedPhysics != null) yield cachedPhysics;
                double gravity = GRAVITY_THROWN;
                double speed   = 1.5;
                double drag    = DRAG_THROWN;
                if (item instanceof ExperienceBottleItem) {
                    gravity = GRAVITY_EXP_BOTTLE;
                    speed   = 0.7;
                } else if (item instanceof ThrowablePotionItem || item instanceof LingeringPotionItem) {
                    gravity = GRAVITY_POTION;
                    speed   = 0.5;
                }
                cachedPhysicsItem = item;
                cachedPhysics     = new Physics(speed, gravity, drag);
                yield cachedPhysics;
            }
            default -> null;
        };
    }

    private AimMode detectMode(Item item, ItemStack stack) {
        if (item instanceof BowItem) return AimMode.Bow;
        if (item instanceof CrossbowItem) return AimMode.Crossbow;
        if (item instanceof TridentItem) return AimMode.Trident;
        if (item instanceof ProjectileItem) return AimMode.Throwable;
        if (item instanceof AxeItem || item instanceof MaceItem
                || stack.is(ItemTags.SWORDS)) return AimMode.Melee;
        return AimMode.Default;
    }

    // --- Aiming ---

    private void aimDirect(Vec3 aimPos) {
        solvedTrajectory = null;
        angleLocked = true;
        if (currentMode == AimMode.Melee) {
            if (Rotations.isEnabled()) Rotations.disable();
            if (rotationMode.getValue() == RotationMode.Smooth) {
                Rotations.lookAtPositionSmooth(aimPos, smoothness.getValue().floatValue());
            } else {
                var rot = Rotations.getPitchYaw(aimPos);
                KorppuMod.mc.player.setYRot(rot.yaw());
                KorppuMod.mc.player.setXRot(rot.pitch());
            }
            solvedYaw = KorppuMod.mc.player.getYRot();
            solvedPitch = KorppuMod.mc.player.getXRot();
            return;
        }
        if (rotationMode.getValue() == RotationMode.Smooth) {
            Rotations.enable();
            Rotations.lookAtPositionSmoothServerSide(aimPos, smoothness.getValue());
        } else {
            Rotations.lookAtV3(aimPos);
        }
    }

    private void aimBallistic(Vec3 eyePos, Vec3 aimPos, Physics phys) {
        float[] angles = solveBallistic(eyePos, aimPos, phys);
        if (angles == null) {
            angleLocked = false;
            autofireLatched = false;
            solvedTrajectory = null;
            if (Rotations.isEnabled()) Rotations.disable();
            return;
        }

        angleLocked = true;
        solvedPitch = angles[1];
        solvedYaw   = angles[0];

        if (rotationMode.getValue() == RotationMode.Smooth) {
            Rotations.enable();
            float laziness = smoothness.getValue().floatValue();
            float dyaw   = Mth.wrapDegrees(angles[0] - Rotations.getClientYaw());
            float dpitch = Mth.wrapDegrees(angles[1] - Rotations.getClientPitch());
            Rotations.setClientYaw(Rotations.getClientYaw() + dyaw / laziness);
            Rotations.setClientPitch(Rotations.getClientPitch() + dpitch / laziness);
        } else {
            Rotations.enable();
            Rotations.setClientYaw(angles[0]);
            Rotations.setClientPitch(angles[1]);
        }

        solvedTrajectory = showTrajectory.getValue()
                ? simulateTrajectory(eyePos, solvedPitch, solvedYaw, phys)
                : null;

        if (autofireWhenLocked.getValue() && !autofireLatched) {
            triggerAutofire();
            autofireLatched = true;
        }
    }

    /**
     * Simulation-based ballistic solver. Sweeps pitch in 1° steps, refines to 0.1°.
     * Each candidate pitch is evaluated by simulating the full trajectory (with drag,
     * gravity, and player velocity) and measuring closest approach to the predicted
     * target position. This is accurate at all ranges and correctly handles drag.
     *
     * Target position is predicted by running the same simulation to find actual flight
     * time, then advancing the target by its current velocity * that tick count.
     */
    private float[] solveBallistic(Vec3 eyePos, Vec3 targetPos, Physics phys) {
        Vec3 playerVel = KorppuMod.mc.player.getDeltaMovement();

        // Iterative target lead: simulate flight time to un-led target, use that to
        // compute led position, then solve for that. Two iterations is sufficient.
        Vec3 aimAt = targetPos;
        for (int iter = 0; iter < 2; iter++) {
            double dx = aimAt.x - eyePos.x;
            double dz = aimAt.z - eyePos.z;
            float yawGuess = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
            int flightTicks = estimateFlightTicks(eyePos, aimAt, yawGuess, 0f, phys, playerVel);
            Vec3 targetVel = target.getDeltaMovement();
            aimAt = targetPos.add(targetVel.x * flightTicks, targetVel.y * flightTicks, targetVel.z * flightTicks);
        }

        double dx = aimAt.x - eyePos.x;
        double dz = aimAt.z - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;

        // Coarse 1° sweep
        float bestPitch = 0f;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i <= 178; i++) {
            float pitch = -89f + i;
            double d = closestApproachSq(eyePos, aimAt, yaw, pitch, phys, playerVel);
            if (d < bestDist) { bestDist = d; bestPitch = pitch; }
        }

        // Refine ±2° at 0.1° resolution
        float from = Math.max(-89f, bestPitch - 2f);
        float to   = Math.min( 89f, bestPitch + 2f);
        for (float pitch = from; pitch <= to; pitch += 0.1f) {
            double d = closestApproachSq(eyePos, aimAt, yaw, pitch, phys, playerVel);
            if (d < bestDist) { bestDist = d; bestPitch = pitch; }
        }

        // Reject if closest approach is more than 1 block — target truly out of range
        if (bestDist > 1.0) return null;

        return new float[]{ yaw, Mth.clamp(bestPitch, -90f, 90f) };
    }

    /** Returns squared distance of closest approach between simulated trajectory and target. */
    private double closestApproachSq(Vec3 eye, Vec3 target, float yaw, float pitch,
                                     Physics phys, Vec3 playerVel) {
        double[] vel = initialVelocity(yaw, pitch, phys.speed(), playerVel);
        double vx = vel[0], vy = vel[1], vz = vel[2];
        double x = eye.x, y = eye.y, z = eye.z;
        double best = Double.MAX_VALUE;

        for (int t = 0; t < SIM_STEPS; t++) {
            x += vx; y += vy; z += vz;
            vx *= phys.drag(); vy *= phys.drag(); vz *= phys.drag();
            vy -= phys.gravity();

            double dx = x - target.x, dy = y - target.y, dz = z - target.z;
            double dsq = dx*dx + dy*dy + dz*dz;
            if (dsq < best) best = dsq;
            else if (dsq > best * 4) break; // diverging — stop early

            if (y < KorppuMod.mc.level.getMinY()) break;
            if (KorppuMod.mc.level.getBlockState(BlockPos.containing(x, y, z)).isSolid()) break;
        }
        return best;
    }

    /** Estimates flight tick count to reach a target via a rough sim along the given yaw/pitch. */
    private int estimateFlightTicks(Vec3 eye, Vec3 tgt, float yaw, float pitch,
                                    Physics phys, Vec3 playerVel) {
        double[] vel = initialVelocity(yaw, pitch, phys.speed(), playerVel);
        double vx = vel[0], vy = vel[1], vz = vel[2];
        double x = eye.x, y = eye.y, z = eye.z;
        double prevDsq = Double.MAX_VALUE;

        for (int t = 1; t <= SIM_STEPS; t++) {
            x += vx; y += vy; z += vz;
            vx *= phys.drag(); vy *= phys.drag(); vz *= phys.drag();
            vy -= phys.gravity();

            double dx = x - tgt.x, dy = y - tgt.y, dz = z - tgt.z;
            double dsq = dx*dx + dy*dy + dz*dz;
            if (dsq > prevDsq) return t - 1; // past closest point
            prevDsq = dsq;

            if (y < KorppuMod.mc.level.getMinY()) return t;
            if (KorppuMod.mc.level.getBlockState(BlockPos.containing(x, y, z)).isSolid()) return t;
        }
        return SIM_STEPS;
    }

    private double[] initialVelocity(float yaw, float pitch, double speed, Vec3 playerVel) {
        double yawR   = Math.toRadians(yaw);
        double pitchR = Math.toRadians(pitch);
        double px = -Math.sin(yawR) * Math.cos(pitchR);
        double py = -Math.sin(pitchR);
        double pz =  Math.cos(yawR) * Math.cos(pitchR);
        return new double[]{
            px * speed + playerVel.x,
            py * speed + playerVel.y,
            pz * speed + playerVel.z
        };
    }

    // --- Trajectory simulation (for rendering only) ---

    private void updatePreviewTrajectory(Item item, ItemStack stack) {
        if (!showTrajectory.getValue() || currentMode == null) {
            previewTrajectory = null;
            return;
        }
        Physics phys = resolvePhysics(currentMode, item, stack);
        if (phys == null) {
            previewTrajectory = null;
            return;
        }
        Vec3 eyePos = KorppuMod.mc.player.getEyePosition();
        float pitch = KorppuMod.mc.player.getXRot();
        float yaw   = KorppuMod.mc.player.getYRot();
        previewTrajectory = simulateTrajectory(eyePos, pitch, yaw, phys);
    }

    private List<Vec3> simulateTrajectory(Vec3 eyePos, float pitch, float yaw, Physics phys) {
        Vec3 playerVel = KorppuMod.mc.player.getDeltaMovement();
        double px = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double py = -Math.sin(Math.toRadians(pitch));
        double pz =  Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double vx = px * phys.speed() + playerVel.x;
        double vy = py * phys.speed() + playerVel.y;
        double vz = pz * phys.speed() + playerVel.z;

        double x = eyePos.x, y = eyePos.y, z = eyePos.z;
        List<Vec3> pts = new ArrayList<>();
        pts.add(new Vec3(x, y, z));

        for (int i = 0; i < SIM_STEPS; i++) {
            // AbstractArrow order: pos += vel, then vel *= drag, then vel.y -= gravity
            double nx = x + vx;
            double ny = y + vy;
            double nz = z + vz;
            vx *= phys.drag();
            vy *= phys.drag();
            vz *= phys.drag();
            vy -= phys.gravity();

            Vec3 next = new Vec3(nx, ny, nz);
            pts.add(next);

            if (KorppuMod.mc.level.getBlockState(BlockPos.containing(nx, ny, nz)).isSolid()) break;
            if (ny < KorppuMod.mc.level.getMinY()) break;

            x = nx; y = ny; z = nz;
        }
        return pts;
    }

    private Vec3 getCenterMassPoint(Entity e) {
        AABB box = e.getBoundingBox();
        return new Vec3(
                (box.minX + box.maxX) * 0.5,
                Mth.lerp(0.45, box.minY, box.maxY),
                (box.minZ + box.maxZ) * 0.5
        );
    }

    // --- State management ---

    private void clearAimState() {
        target = null;
        solvedTrajectory = null;
        previewTrajectory = null;
        currentCharge = 0;
        currentMode = null;
        solvedPitch = 0;
        solvedYaw = 0;
        angleLocked = false;
        autofireLatched = false;
        if (Rotations.isEnabled()) Rotations.disable();
    }

    private void clearTargetControlState() {
        manualTargetId = null;
        targetLocked = false;
        lastCandidateCount = 0;
    }

    // --- Autofire ---

    private void triggerAutofire() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.gameMode == null) return;

        switch (currentMode) {
            case Bow, Trident -> {
                sendSolvedRotationPacket();
                if (KorppuMod.mc.player.isUsingItem()) {
                    KorppuMod.mc.gameMode.releaseUsingItem(KorppuMod.mc.player);
                }
            }
            case Crossbow, Throwable -> {
                if (currentMode == AimMode.Crossbow && KorppuMod.mc.player.isUsingItem() && !crossbowHoldAutofire.getValue()) return;
                applySolvedLocalRotation();
                sendSolvedRotationPacket();
                KorppuMod.mc.gameMode.useItem(KorppuMod.mc.player, InteractionHand.MAIN_HAND);
            }
            case Melee -> {
                if (KorppuMod.mc.player.getAttackStrengthScale(0.5f) < 1.0f || target == null) return;
                KorppuMod.mc.gameMode.attack(KorppuMod.mc.player, target);
                KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
            }
            default -> {}
        }
    }

    private void applySolvedLocalRotation() {
        if (!angleLocked || KorppuMod.mc.player == null) return;
        KorppuMod.mc.player.setYRot(solvedYaw);
        KorppuMod.mc.player.setXRot(solvedPitch);
    }

    private void sendSolvedRotationPacket() {
        if (!angleLocked || KorppuMod.mc.player == null || KorppuMod.mc.player.connection == null) return;
        KorppuMod.mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                KorppuMod.mc.player.getX(),
                KorppuMod.mc.player.getY(),
                KorppuMod.mc.player.getZ(),
                solvedYaw,
                solvedPitch,
                KorppuMod.mc.player.onGround(),
                KorppuMod.mc.player.horizontalCollision
        ));
    }

    // --- Range/target selection ---

    private double resolveTargetRange() {
        if (KorppuMod.mc.player == null) return maxRange.getValue();
        if (currentMode != AimMode.Melee) return maxRange.getValue();
        double reach = KorppuMod.mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        return reach > 0 ? reach : maxRange.getValue();
    }

    /**
     * Meteor's KillAura clamped-AABB range check: clamps the player's feet position into the
     * entity's bounding box before measuring distance. This prevents distant large entities
     * (or entities at very different Y) from appearing in range via eye-to-eye distance.
     */
    private boolean isInRange(Entity e, double range) {
        AABB box = e.getBoundingBox();
        double cx = Mth.clamp(KorppuMod.mc.player.getX(), box.minX, box.maxX);
        double cy = Mth.clamp(KorppuMod.mc.player.getY(), box.minY, box.maxY);
        double cz = Mth.clamp(KorppuMod.mc.player.getZ(), box.minZ, box.maxZ);
        double dx = KorppuMod.mc.player.getX() - cx;
        double dy = KorppuMod.mc.player.getY() - cy;
        double dz = KorppuMod.mc.player.getZ() - cz;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    private List<Entity> findCandidates(Vec3 eyePos, double range) {
        List<Entity> candidates = new ArrayList<>();
        for (Entity e : KorppuMod.mc.level.entitiesForRendering()) {
            if (e == KorppuMod.mc.player) continue;
            if (!e.isAlive() || !e.isAttackable()) continue;
            if (!targets.matches(e) || protection.isProtected(e)) continue;
            if (!isInRange(e, range)) continue;
            candidates.add(e);
        }
        candidates.sort(getTargetComparator());
        return candidates;
    }

    private Comparator<Entity> getTargetComparator() {
        return switch (priority.getValue()) {
            case Near  -> Comparator.comparingDouble(e -> KorppuMod.mc.player.distanceToSqr(e));
            case Angle -> Comparator.comparingDouble(this::angleTo);
            default    -> Comparator.comparingDouble(e ->
                    Math.pow(angleTo(e), 2) + KorppuMod.mc.player.distanceToSqr(e));
        };
    }

    private Entity findByUuid(List<Entity> candidates, UUID id) {
        if (id == null) return null;
        for (Entity candidate : candidates) {
            if (candidate.getUUID().equals(id)) return candidate;
        }
        return null;
    }

    private Entity selectTarget(List<Entity> candidates) {
        if (candidates.isEmpty()) return null;
        Entity manualTarget = findByUuid(candidates, manualTargetId);
        if (manualTarget != null) return manualTarget;
        if (manualTargetId != null) {
            manualTargetId = null;
            targetLocked = false;
        }
        return candidates.getFirst();
    }

    private double angleTo(Entity e) {
        Vec3 toTarget = getCenterMassPoint(e)
                .subtract(KorppuMod.mc.player.getEyePosition()).normalize();
        Vec3 look = KorppuMod.mc.player.getViewVector(1f);
        return Math.toDegrees(Math.acos(Mth.clamp(look.dot(toTarget), -1.0, 1.0)));
    }

    // --- Manual target control ---

    public void toggleTargetLock() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;
        if (targetLocked) {
            targetLocked = false;
            manualTargetId = null;
            return;
        }
        if (manualTargetId == null) {
            if (target != null) {
                manualTargetId = target.getUUID();
            } else {
                List<Entity> candidates = findCandidates(KorppuMod.mc.player.getEyePosition(), resolveTargetRange());
                if (!candidates.isEmpty()) {
                    manualTargetId = candidates.getFirst().getUUID();
                    this.target = candidates.getFirst();
                    lastCandidateCount = candidates.size();
                }
            }
        }
        targetLocked = manualTargetId != null;
    }

    public void cycleTarget(int direction) {
        if (direction == 0 || KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;
        List<Entity> candidates = findCandidates(KorppuMod.mc.player.getEyePosition(), resolveTargetRange());
        lastCandidateCount = candidates.size();
        if (candidates.isEmpty()) {
            manualTargetId = null;
            targetLocked = false;
            target = null;
            return;
        }
        UUID currentId = manualTargetId != null ? manualTargetId : (target != null ? target.getUUID() : null);
        int index = -1;
        if (currentId != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).getUUID().equals(currentId)) { index = i; break; }
            }
        }
        int nextIndex = index == -1
                ? (direction > 0 ? 0 : candidates.size() - 1)
                : Math.floorMod(index + direction, candidates.size());
        Entity next = candidates.get(nextIndex);
        manualTargetId = next.getUUID();
        targetLocked = true;
        target = next;
    }

    // --- Rendering ---

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        // Trajectory: show solved (green) if aimbot has a solution, otherwise preview (yellow)
        if (solvedTrajectory != null && solvedTrajectory.size() >= 2) {
            RenderUtils.drawCurvedLine(matrices, solvedTrajectory, 0xFF55FF55, false);
        } else if (previewTrajectory != null && previewTrajectory.size() >= 2) {
            RenderUtils.drawCurvedLine(matrices, previewTrajectory, 0xFFFFDD00, false);
        }

        if (target == null) return;
        AABB box = target.getBoundingBox();
        int solid   = angleLocked ? 0x8044FF44 : 0x80FF4444;
        int outline = angleLocked ? 0xFF00FF00 : 0xFFFF0000;
        RenderUtils.drawSolidBox(matrices, box, solid, true);
        RenderUtils.drawOutlinedBox(matrices, box, outline, true);
    }

    // --- Status ---

    @Override
    public String getStatus() {
        if (target == null) return "no target";
        String name = target instanceof Player p
                ? p.getDisplayName().getString()
                : target.getType().getDescription().getString();
        if (currentMode == AimMode.Bow && currentCharge < 1f)
            return "Charging " + (int) (currentCharge * 100) + "% → " + name;
        return (currentMode != null ? currentMode.name() : "?") + " → " + name;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.text("Priority", priority.getValue().name()));
        entries.add(StatusEntry.text("Mode", currentMode != null ? currentMode.name() : "None"));
        entries.add(StatusEntry.indicator("Target Lock", targetLocked));
        entries.add(StatusEntry.indicator("Angle Lock", angleLocked));
        entries.add(StatusEntry.text("Candidates", Integer.toString(lastCandidateCount)));
        if (target == null) {
            entries.add(StatusEntry.text("Target", "none"));
        } else {
            entries.add(StatusEntry.text("Target", getTargetName()));
            if (currentMode == AimMode.Bow)
                entries.add(StatusEntry.bar("Charge", (int) (currentCharge * 100) + "%", currentCharge));
            if (angleLocked)
                entries.add(StatusEntry.text("Angle", String.format("%.1f° / %.1f°", solvedYaw, solvedPitch)));
        }
        return entries;
    }

    public boolean isTargetLocked()  { return targetLocked; }
    public boolean isAngleLocked()   { return angleLocked; }
    public boolean isNoSlow()        { return isEnabled() && noSlow.getValue(); }
    public int     getCandidateCount() { return lastCandidateCount; }

    public String getTargetName() {
        if (target instanceof Player p) return p.getDisplayName().getString();
        if (target != null) return target.getType().getDescription().getString();
        return "none";
    }

    public enum Priority { Near, Angle, AngleDist }
    public enum RotationMode { Instant, Smooth }
    public enum AimMode { Bow, Crossbow, Trident, Throwable, Melee, Default }

    @Override
    public boolean hasControlWindow() { return true; }

    @Override
    public ModuleControlWindow createControlWindow() { return new AimbotControlWindow(this); }
}
