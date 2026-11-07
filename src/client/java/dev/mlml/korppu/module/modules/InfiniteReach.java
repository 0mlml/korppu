package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InfiniteReach extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "Vanilla or Paper compatibility", Mode.Vanilla));
    private final DoubleSetting vanillaRange = config.add(new DoubleSetting("Vanilla Range", "Maximum reach in vanilla mode", 22d, 1d, 22d, 1));
    private final DoubleSetting paperRange = config.add(new DoubleSetting("Paper Range", "Maximum reach in paper mode", 59d, 1d, 99d, 1));
    private final DoubleSetting vanillaPackets = config.add(new DoubleSetting("Vanilla Spam", "Pre-teleport movement packets", 4d, 1d, 6d, 0));
    private final DoubleSetting paperPackets = config.add(new DoubleSetting("Paper Spam", "Pre-teleport movement packets", 7d, 1d, 10d, 0));
    private final BooleanSetting clipUp = config.add(new BooleanSetting("Paper Clip Up", "Clip up before teleport in Paper mode", true));
    private final BooleanSetting phoneHome = config.add(new BooleanSetting("Phone Home", "Teleport back after action", true));
    private final BooleanSetting swing = config.add(new BooleanSetting("Swing", "Send swing packet", true));
    private final BooleanSetting onlyMace = config.add(new BooleanSetting("Only Mace", "Only do entity attack while holding mace", false));
    private final DoubleSetting horizontalOffset = config.add(new DoubleSetting("Horizontal Offset", "Return offset horizontal", 0.05d, 0.01d, 0.99d, 2));
    private final DoubleSetting yOffset = config.add(new DoubleSetting("Y Offset", "Return offset vertical", 0.01d, 0.01d, 0.99d, 2));
    private final BooleanSetting miningPacket = config.add(new BooleanSetting("Mine Blocks", "Send start/stop mine packets", true));
    private final DoubleSetting miningDelay = config.add(new DoubleSetting("Mine Delay", "Ticks between mine packets", 5d, 1d, 20d, 0));
    private final BooleanSetting itemUsePacket = config.add(new BooleanSetting("Use Blocks", "Send interact block packets", true));
    private final DoubleSetting useDelay = config.add(new DoubleSetting("Use Delay", "Ticks between use packets", 5d, 1d, 20d, 0));
    private final BooleanSetting attackPacket = config.add(new BooleanSetting("Attack Entities", "Send attack/interact packets", true));
    private final DoubleSetting entityDelay = config.add(new DoubleSetting("Entity Delay", "Ticks between entity packets", 5d, 1d, 20d, 0));

    private int mineTicks;
    private int useTicks;
    private int entityTicks;

    public InfiniteReach() {
        super("InfiniteReach", "Teleports to hit/use at long range and teleports back", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        mineTicks = 0;
        useTicks = 0;
        entityTicks = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;

        mineTicks++;
        useTicks++;
        entityTicks++;

        double maxDistance = mode.getValue() == Mode.Vanilla ? vanillaRange.getValue() : paperRange.getValue();

        Vec3 camera = KorppuMod.mc.player.getEyePosition(1f);
        Vec3 rotation = KorppuMod.mc.player.getViewVector(1f);
        Vec3 end = camera.add(rotation.scale(maxDistance));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                KorppuMod.mc.player,
                camera,
                end,
                KorppuMod.mc.player.getBoundingBox().inflate(maxDistance),
                e -> e.isAlive() && e.isAttackable() && !e.isInvulnerable() && e != KorppuMod.mc.player,
                maxDistance * maxDistance
        );

        BlockHitResult blockHit = null;
        if (entityHit == null) {
            HitResult hit = KorppuMod.mc.getCameraEntity().pick(maxDistance, 0, false);
            if (hit instanceof BlockHitResult bhr && !KorppuMod.mc.level.getBlockState(bhr.getBlockPos()).isAir()) {
                blockHit = bhr;
            }
        }

        boolean attackPressed = KorppuMod.mc.options.keyAttack.isDown();
        boolean usePressed = KorppuMod.mc.options.keyUse.isDown();

        if (!attackPressed && !usePressed) return;

        if (entityHit != null && attackPressed && attackPacket.getValue() && entityTicks >= entityDelay.getValue().intValue()) {
            entityTicks = 0;
            hitEntity(entityHit.getEntity(), true, maxDistance);
            return;
        }

        if (entityHit != null && usePressed && itemUsePacket.getValue() && entityTicks >= entityDelay.getValue().intValue()) {
            entityTicks = 0;
            hitEntity(entityHit.getEntity(), false, maxDistance);
            return;
        }

        if (blockHit != null && attackPressed && miningPacket.getValue() && mineTicks >= miningDelay.getValue().intValue()) {
            mineTicks = 0;
            hitBlock(blockHit, true, maxDistance);
            return;
        }

        if (blockHit != null && usePressed && itemUsePacket.getValue() && useTicks >= useDelay.getValue().intValue() && !KorppuMod.mc.player.isUsingItem()) {
            useTicks = 0;
            hitBlock(blockHit, false, maxDistance);
        }
    }

    private void hitEntity(Entity target, boolean attack, double maxDistance) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;
        if (onlyMace.getValue() && KorppuMod.mc.player.getMainHandItem().getItem() != Items.MACE) return;
        if (onlyMace.getValue() && target instanceof Player player && player.isBlocking()) return;

        Entity self = KorppuMod.mc.player.isPassenger() ? KorppuMod.mc.player.getVehicle() : KorppuMod.mc.player;
        Vec3 start = self.position();

        Vec3 targetPos = target.position();
        double y = KorppuMod.mc.player.getVehicle() != null ? target.getBoundingBox().maxY + 0.3 : targetPos.y;
        Vec3 desired = new Vec3(targetPos.x, y, targetPos.z);
        Vec3 finalPos = invalid(desired, self) ? findNearestPos(desired, self) : desired;
        if (finalPos == null) return;

        Vec3 diff = start.subtract(targetPos);
        double flat = Math.sqrt(Math.max(0.0, maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z)));
        Vec3 aboveSelf = start.add(0, maxDistance, 0);
        Vec3 aboveTarget = finalPos.add(0, flat + diff.y, 0);

        if (invalid(finalPos, self)) return;
        if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
            if (invalid(aboveSelf, self) || invalid(aboveTarget, self) || !hasClearPath(aboveSelf, aboveTarget, self)) return;
        }

        sendSpamPackets();
        if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
            sendMove(self, aboveSelf);
            sendMove(self, aboveTarget);
        }
        sendMove(self, finalPos);
        if (!phoneHome.getValue()) self.setPos(finalPos);

        if (attack) {
            KorppuMod.mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundAttackPacket(target.getId()));
        } else {
            KorppuMod.mc.getConnection().send(new ServerboundInteractPacket(target.getId(), InteractionHand.MAIN_HAND, null, KorppuMod.mc.player.isCrouching()));
        }
        doSwing();

        if (phoneHome.getValue()) {
            if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
                sendMove(self, aboveTarget.add(0, 0.01, 0));
                sendMove(self, aboveSelf.add(0, 0.01, 0));
            }
            sendMove(self, start);
            Vec3 offset = getOffset(start, self);
            sendMove(self, offset);
            self.setPos(offset);
        }
    }

    private void hitBlock(BlockHitResult hit, boolean attack, double maxDistance) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;

        Entity self = KorppuMod.mc.player.isPassenger() ? KorppuMod.mc.player.getVehicle() : KorppuMod.mc.player;
        Vec3 start = self.position();

        Vec3 targetCenter = hit.getBlockPos().getCenter();
        BlockPos actionPos = hit.getBlockPos().relative(hit.getDirection());
        Vec3 insideTarget = new Vec3(targetCenter.x, KorppuMod.mc.player.getVehicle() != null ? hit.getBlockPos().getY() + 1.3 : targetCenter.y, targetCenter.z);
        Vec3 finalPos = findNearestPosBlock(insideTarget, actionPos, hit, self);
        if (finalPos == null || invalid(finalPos, self)) return;

        Vec3 diff = start.subtract(targetCenter);
        double flat = Math.sqrt(Math.max(0.0, maxDistance * maxDistance - (diff.x * diff.x + diff.z * diff.z)));
        Vec3 aboveSelf = start.add(0, maxDistance, 0);
        Vec3 aboveTarget = finalPos.add(0, flat + diff.y, 0);

        if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
            if (invalid(aboveSelf, self) || invalid(aboveTarget, self) || !hasClearPath(aboveSelf, aboveTarget, self)) return;
        }

        sendSpamPackets();
        if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
            sendMove(self, aboveSelf);
            sendMove(self, aboveTarget);
        }
        sendMove(self, finalPos);
        if (!phoneHome.getValue()) self.setPos(finalPos);

        if (attack) {
            KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, hit.getBlockPos(), hit.getDirection()));
            KorppuMod.mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, hit.getBlockPos(), hit.getDirection()));
        } else {
            KorppuMod.mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
        }
        doSwing();

        if (phoneHome.getValue()) {
            if (mode.getValue() == Mode.Paper && clipUp.getValue()) {
                sendMove(self, aboveTarget.add(0, 0.01, 0));
                sendMove(self, aboveSelf.add(0, 0.01, 0));
            }
            sendMove(self, start);
            Vec3 offset = getOffset(start, self);
            sendMove(self, offset);
            self.setPos(offset);
        }
    }

    private void sendSpamPackets() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.player.connection == null) return;
        int amount = mode.getValue() == Mode.Vanilla ? vanillaPackets.getValue().intValue() : paperPackets.getValue().intValue();
        for (int i = 0; i < amount; i++) {
            if (KorppuMod.mc.player.isPassenger()) {
                KorppuMod.mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(KorppuMod.mc.player.getVehicle()));
            } else {
                KorppuMod.mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, KorppuMod.mc.player.horizontalCollision));
            }
        }
    }

    private void doSwing() {
        if (!swing.getValue() || KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;
        KorppuMod.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void sendMove(Entity entity, Vec3 pos) {
        if (KorppuMod.mc.getConnection() == null || KorppuMod.mc.player == null) return;
        if (entity == KorppuMod.mc.player) {
            KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    pos.x,
                    pos.y,
                    pos.z,
                    false,
                    KorppuMod.mc.player.horizontalCollision
            ));
        } else {
            KorppuMod.mc.getConnection().send(new ServerboundMoveVehiclePacket(pos, entity.getYRot(), entity.getXRot(), false));
        }
    }

    private Vec3 findNearestPos(Vec3 desired, Entity entity) {
        if (!invalid(desired, entity)) return desired;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Vec3 test = desired.add(dx, dy, dz);
                    if (invalid(test, entity)) continue;
                    double dist = test.distanceToSqr(desired);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }
        return best;
    }

    private Vec3 findNearestPosBlock(Vec3 preferred, BlockPos actionBlockPos, BlockHitResult hit, Entity entity) {
        if (!invalid(preferred, entity) && !BlockPos.containing(preferred).equals(actionBlockPos)) {
            return preferred;
        }

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    Vec3 test = preferred.add(dx * 0.4, dy * 0.4, dz * 0.4);
                    if (BlockPos.containing(test).equals(actionBlockPos)) continue;
                    if (invalid(test, entity)) continue;
                    double dist = test.distanceToSqr(preferred);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }

        if (best == null) {
            for (Direction dir : Direction.values()) {
                if (dir == hit.getDirection()) continue;
                Vec3 fallback = preferred.add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
                if (!invalid(fallback, entity) && !BlockPos.containing(fallback).equals(actionBlockPos)) {
                    return fallback;
                }
            }
        }
        return best;
    }

    private Vec3 getOffset(Vec3 base, Entity entity) {
        double dx = horizontalOffset.getValue();
        double dy = yOffset.getValue();

        Vec3[] offsets = new Vec3[]{
                base.add(dx, dy, 0),
                base.add(-dx, dy, 0),
                base.add(0, dy, dx),
                base.add(0, dy, -dx),
                base.add(dx, dy, dx),
                base.add(-dx, dy, -dx),
                base.add(-dx, dy, dx),
                base.add(dx, dy, -dx)
        };
        Collections.shuffle(Arrays.asList(offsets));
        for (Vec3 pos : offsets) {
            if (!invalid(pos, entity)) return pos;
        }

        Vec3 verticalOnly = base.add(0, dy, 0);
        if (!invalid(verticalOnly, entity)) return verticalOnly;
        return base;
    }

    private boolean invalid(Vec3 pos, Entity entity) {
        if (KorppuMod.mc.level == null) return true;
        if (KorppuMod.mc.level.getChunk(BlockPos.containing(pos)) == null) return true;

        AABB targetBox = entity.getBoundingBox().move(pos.x - entity.getX(), pos.y - entity.getY(), pos.z - entity.getZ());

        for (BlockPos bp : BlockPos.betweenClosed(
                BlockPos.containing(targetBox.minX, targetBox.minY, targetBox.minZ),
                BlockPos.containing(targetBox.maxX, targetBox.maxY, targetBox.maxZ)
        )) {
            BlockState state = KorppuMod.mc.level.getBlockState(bp);
            if (state.is(Blocks.LAVA) || !state.getCollisionShape(KorppuMod.mc.level, bp).isEmpty()) {
                return true;
            }
        }

        for (Entity other : KorppuMod.mc.level.getEntities(entity, targetBox)) {
            if (other.canCollideWith(entity)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasClearPath(Vec3 start, Vec3 end, Entity entity) {
        if (invalid(start, entity) || invalid(end, entity)) return false;
        int steps = Math.max(10, (int) (start.distanceTo(end) * 2.5));
        for (int i = 1; i < steps; i++) {
            Vec3 sample = start.lerp(end, i / (double) steps);
            if (invalid(sample, entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        double maxDistance = mode.getValue() == Mode.Vanilla ? vanillaRange.getValue() : paperRange.getValue();
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.text("Range", String.format("%.1f", maxDistance)),
                StatusEntry.text("Home", phoneHome.getValue() ? "On" : "Off")
        );
    }

    public enum Mode {
        Vanilla,
        Paper
    }
}
