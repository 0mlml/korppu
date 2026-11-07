package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.event.events.WorldRenderEvent;
import dev.mlml.korppu.misc.KorppuMath;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.player.PlayerManager;
import dev.mlml.korppu.player.PlayerRelation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.HitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VelocitySpoof extends Module {

    private Entity spearTarget = null;
    private boolean spearActive = false;
    private int spearBurstCount = 0;
    private String spearIdleReason = "idle";
    private boolean spearControllingRotations = false;
    private final List<ServerboundMovePlayerPacket> spearBlinkPackets = new ArrayList<>();
    private boolean spearBlinking = false;
    private boolean spearBlinkFlushing = false;
    private Vec3 spearBlinkStartPos = null;
    private double spearBlinkLastDistance = Double.MAX_VALUE;
    private boolean spearBlinkWasApproaching = false;
    private int spearBlinkChargeTicks = 0;
    private Vec3 spearAboveTargetPos = null;
    private boolean spearAbovePhase = false;
    private int spearBlinkFlushCooldown = 0;
    private boolean wasNoFallEnabled = false;
    private boolean noFallToggled = false;

    private LivingEntity bowTarget = null;
    private boolean bowBurstInFlight = false;

    private final DoubleSetting strength = config.add(
            new DoubleSetting("Bow Strength", "Bow/trident fake displacement (blocks)", 10d, 1d, 10d, 1));
    private final DoubleSetting bowCharge = config.add(
            new DoubleSetting("Bow Packets", "Position packet pairs sent on bow release", 30d, 1d, 100d, 0));
    private final BooleanSetting bowEnabled = config.add(new BooleanSetting("Bow", "Apply to bows", true));
    private final BooleanSetting bowAutoAim = config.add(
            new BooleanSetting("Bow Auto Aim", "Auto-aim and release bow at nearest target", false));
    private final BooleanSetting tridentEnabled = config.add(new BooleanSetting("Trident", "Apply to tridents", true));
    private final BooleanSetting spearEnabled = config.add(
            new BooleanSetting("Spear", "Boost spear damage every tick while right-click held", true));
    private final EntityTargetSelector spearTargets = new EntityTargetSelector(config, "Spear Targets", Set.of("PLAYERS", "MOBS", "ANIMALS"));
    private final BooleanSetting spearIgnoreFriends = config.add(
            new BooleanSetting("Spear Ignore Friends", "Do not target friends in spear modes", true)
                    .showOnlyIf(() -> spearEnabled.getValue()));
    private final BooleanSetting spearDisableNoFall = config.add(
            new BooleanSetting("Spear Disable NoFall", "Disable NoFall while charging spear", true)
                    .showOnlyIf(() -> spearEnabled.getValue()));
    private final ListSetting<SpearMode> spearMode = config.add(new ListSetting<>(
            "Spear Mode",
            "Legacy packet burst, Lunge velocity, or Blink packet delay",
            SpearMode.Legacy
    ).showOnlyIf(() -> spearEnabled.getValue()));
    private final DoubleSetting spearVelocity = config.add(
            new DoubleSetting("Spear Velocity", "Faked approach speed in blocks/tick (>~3.5 may rubber-band)", 2d, 0.5d, 10d, 1));
    private final DoubleSetting spearPackets = config.add(
            new DoubleSetting("Spear Packets", "Packet pairs sent per tick for spear", 5d, 1d, 30d, 0));
    private final BooleanSetting spearAim = config.add(
            new BooleanSetting("Spear Auto Aim", "Point camera at spear target (required for max range)", true));
    private final DoubleSetting spearRange = config.add(
            new DoubleSetting("Spear Range", "Range to auto-target entities for spear (blocks)", 5d, 2d, 10d, 1));
    private final DoubleSetting spearReadyTicks = config.add(
            new DoubleSetting("Spear Ready Ticks", "Minimum use ticks before lunge/blink movement", 14d, 0d, 30d, 0)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() != SpearMode.Legacy));
    private final BooleanSetting spearStopOnTarget = config.add(
            new BooleanSetting("Spear Stop On Target", "Stops velocity when you reach target", true)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Lunge));
    private final DoubleSetting spearStopDistance = config.add(
            new DoubleSetting("Spear Stop Distance", "Distance to stop at target", 2d, 0d, 10d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Lunge));
    private final BooleanSetting spearFromAbove = config.add(
            new BooleanSetting("Spear From Above", "First move above target before diving", false)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Lunge));
    private final DoubleSetting spearAboveHeight = config.add(
            new DoubleSetting("Spear Above Height", "Blocks above target for first phase", 10d, 3d, 50d, 0)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Lunge && spearFromAbove.getValue()));
    private final DoubleSetting spearAboveTrigger = config.add(
            new DoubleSetting("Spear Above Trigger", "Distance from above point to dive", 3d, 1d, 10d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Lunge && spearFromAbove.getValue()));
    private final DoubleSetting spearBlinkFlushRange = config.add(
            new DoubleSetting("Spear Blink Flush", "Flush delayed packets under this target distance", 3d, 1d, 10d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink));
    private final DoubleSetting spearBlinkForceFlush = config.add(
            new DoubleSetting("Spear Blink Force", "Force flush after traveling this distance", 9.5d, 1d, 25d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink));
    private final DoubleSetting spearBlinkDistanceBoost = config.add(
            new DoubleSetting("Spear Blink Distance Boost", "Extra blocks added to blink flush start", 0d, 0d, 10d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink));
    private final BooleanSetting spearBlinkAimbot = config.add(
            new BooleanSetting("Spear Blink Aimbot", "Keep aim locked while blinking", true)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink));
    private final BooleanSetting spearBlinkLunge = config.add(
            new BooleanSetting("Spear Blink Lunge", "Apply lunge velocity while charging in blink", false)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink));
    private final DoubleSetting spearBlinkLungeStrength = config.add(
            new DoubleSetting("Spear Blink Lunge Strength", "Velocity during blink charge", 1d, 0.1d, 3d, 1)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink && spearBlinkLunge.getValue()));
    private final DoubleSetting spearBlinkLungeDelay = config.add(
            new DoubleSetting("Spear Blink Lunge Delay", "Charge ticks before applying blink lunge", 15d, 1d, 30d, 0)
                    .showOnlyIf(() -> spearEnabled.getValue() && spearMode.getValue() == SpearMode.Blink && spearBlinkLunge.getValue()));
    private final BooleanSetting showTrajectory = config.add(
            new BooleanSetting("Trajectory", "Show simulated arrow trajectory while drawing bow", true));

    public VelocitySpoof() {
        super("VelocitySpoof", "Fakes position packets to boost projectile/spear velocity damage", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onDisable() {
        clearSpear();
    }

    private void clearSpear() {
        flushSpearBlinkPackets();
        spearActive = false;
        spearTarget = null;
        spearBurstCount = 0;
        spearIdleReason = "idle";
        if (spearControllingRotations && Rotations.isEnabled()) {
            Rotations.disable();
        }
        spearControllingRotations = false;
        spearBlinkPackets.clear();
        spearBlinking = false;
        spearBlinkFlushing = false;
        spearBlinkStartPos = null;
        spearBlinkLastDistance = Double.MAX_VALUE;
        spearBlinkWasApproaching = false;
        spearBlinkChargeTicks = 0;
        spearAboveTargetPos = null;
        spearAbovePhase = false;
        spearBlinkFlushCooldown = 0;

        if (noFallToggled && wasNoFallEnabled) {
            NoFall noFall = ModuleManager.getModule(NoFall.class);
            if (noFall != null && !noFall.isEnabled()) {
                noFall.setEnabled(true);
            }
        }
        noFallToggled = false;
        wasNoFallEnabled = false;
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.player == null) return;

        if (spearEnabled.getValue()
                && spearMode.getValue() == SpearMode.Blink
                && spearBlinking
                && !spearBlinkFlushing
                && event.getPacket() instanceof ServerboundMovePlayerPacket movePacket) {
            event.cancel();
            if (spearBlinkPackets.isEmpty() || !isSameMovePacket(movePacket, spearBlinkPackets.get(spearBlinkPackets.size() - 1))) {
                spearBlinkPackets.add(movePacket);
            }
            return;
        }

        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)) return;
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return;

        if (spearEnabled.getValue()
                && spearMode.getValue() == SpearMode.Blink
                && KorppuMod.mc.player.getActiveItem().is(ItemTags.SPEARS)) {
            flushSpearBlinkPackets();
            return;
        }

        var item = KorppuMod.mc.player.getMainHandItem().getItem();
        boolean applyBow = item instanceof BowItem && bowEnabled.getValue();
        boolean applyTrident = (item instanceof TridentItem || KorppuMod.mc.player.getActiveItem().is(ItemTags.SPEARS))
                && tridentEnabled.getValue();
        boolean applySpear = spearEnabled.getValue()
                && KorppuMod.mc.player.getActiveItem().is(ItemTags.SPEARS)
                && spearMode.getValue() != SpearMode.Blink;

        if (!applyBow && !applyTrident && !applySpear) return;

        if (bowBurstInFlight) return;
        bowBurstInFlight = true;
        Entity burstTarget = spearTarget;
        if (applySpear && burstTarget != null) {
            sendSpearBurst(burstTarget);
        } else {
            sendProjectileBurst(horizontalLookDir());
        }
        bowBurstInFlight = false;
    }

    @Listener
    public void onPacketReceive(PacketEvent.Received event) {
        if (!isEnabled() || !spearEnabled.getValue() || spearMode.getValue() != SpearMode.Blink) return;
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            spearBlinkPackets.clear();
            spearBlinkStartPos = KorppuMod.mc.player != null ? KorppuMod.mc.player.position() : null;
            spearBlinkLastDistance = spearTarget != null && KorppuMod.mc.player != null ? KorppuMod.mc.player.distanceTo(spearTarget) : Double.MAX_VALUE;
            spearBlinkWasApproaching = false;
        }
    }

    @Listener
    public void onWorldRender(WorldRenderEvent event) {
        if (!isEnabled() || !showTrajectory.getValue()) return;
        if (KorppuMod.mc.player == null) return;
        if (!KorppuMod.mc.player.isUsingItem()) return;
        if (!(KorppuMod.mc.player.getMainHandItem().getItem() instanceof BowItem bowItem)) return;

        int pullback = bowItem.getUseDuration(KorppuMod.mc.player.getMainHandItem(),
                                             KorppuMod.mc.player) - KorppuMod.mc.player.getUseItemRemainingTicks();
        float charge = BowItem.getPowerForTime(pullback);
        if (charge < 0.05f) return;

        List<Vec3> points = simulateArrow(charge);
        if (points.size() < 2) return;

        int color = bowTarget != null ? 0xFF55FF55 : 0xFFFFAA00;
        RenderUtils.drawCurvedLine(event.getMatrixStack(), points, color, false);
    }

    private List<Vec3> simulateArrow(float charge) {
        Vec3 pos = KorppuMod.mc.player.getEyePosition();
        Vec3 playerVel = KorppuMod.mc.player.getDeltaMovement();
        Vec3 vel = lookDir().scale(charge * 3.0).add(playerVel.x, 0, playerVel.z);

        List<Vec3> points = new ArrayList<>();
        points.add(pos);

        for (int i = 0; i < 200; i++) {
            vel = new Vec3(vel.x * 0.99, vel.y * 0.99 - 0.05, vel.z * 0.99);
            pos = pos.add(vel);
            points.add(pos);
            if (KorppuMod.mc.level != null && KorppuMod.mc.level.getBlockState(net.minecraft.core.BlockPos.containing(pos))
                                                                .isSolid()) {
                break;
            }
        }
        return points;
    }

    @Override
    public void onTick() {
        bowTarget = null;

        boolean drawingBow = bowEnabled.getValue() && KorppuMod.mc.player != null && KorppuMod.mc.player.isUsingItem() && KorppuMod.mc.player.getMainHandItem()
                                                                                                                                             .getItem() instanceof BowItem;

        if (drawingBow) {
            BowItem bowItem = (BowItem) KorppuMod.mc.player.getMainHandItem().getItem();
            int pullback = bowItem.getUseDuration(KorppuMod.mc.player.getMainHandItem(),
                                                 KorppuMod.mc.player) - KorppuMod.mc.player.getUseItemRemainingTicks();
            if (BowItem.getPowerForTime(pullback) >= 0.2f) {
                bowTarget = findBowTarget();
                if (bowAutoAim.getValue() && bowTarget != null) {
                    Rotations.lookAtV3(bowTarget.getBoundingBox().getCenter());
                    if (BowItem.getPowerForTime(pullback) >= 1.0f) {
                        KorppuMod.mc.player.stopUsingItem();
                    }
                } else if (bowAutoAim.getValue()) {
                    Rotations.disable();
                }
            }
        } else if (bowAutoAim.getValue() && Rotations.isEnabled() && !spearControllingRotations) {
            Rotations.disable();
        }

        if (!spearEnabled.getValue()) {
            clearSpear();
            spearIdleReason = "disabled";
            return;
        }
        if (KorppuMod.mc.player == null) {
            clearSpear();
            spearIdleReason = "no player";
            return;
        }
        if (!KorppuMod.mc.player.isUsingItem()) {
            boolean wasActive = spearActive;

            if (noFallToggled && wasNoFallEnabled) {
                NoFall noFall = ModuleManager.getModule(NoFall.class);
                if (noFall != null && !noFall.isEnabled()) {
                    noFall.setEnabled(true);
                }
                noFallToggled = false;
            }

            clearSpear();
            if (wasActive) {
                spearIdleReason = "released";
            } else {
                var heldItem = KorppuMod.mc.player.getMainHandItem().getItem();
                spearIdleReason = "not using item (" + heldItem.getDescriptionId() + ")";
            }
            return;
        }
        if (!KorppuMod.mc.player.getActiveItem().is(ItemTags.SPEARS)) {
            if (noFallToggled && wasNoFallEnabled) {
                NoFall noFall = ModuleManager.getModule(NoFall.class);
                if (noFall != null && !noFall.isEnabled()) {
                    noFall.setEnabled(true);
                }
                noFallToggled = false;
            }

            clearSpear();
            var activeItem = KorppuMod.mc.player.getActiveItem().getItem();
            spearIdleReason = "item not a spear (" + activeItem.getDescriptionId() + ")";
            return;
        }

        if (spearDisableNoFall.getValue() && !noFallToggled) {
            NoFall noFall = ModuleManager.getModule(NoFall.class);
            if (noFall != null) {
                wasNoFallEnabled = noFall.isEnabled();
                if (wasNoFallEnabled) {
                    noFall.setEnabled(false);
                    noFallToggled = true;
                }
            }
        }

        spearTarget = findSpearTarget();
        spearActive = true;
        spearBlinkChargeTicks++;

        if (spearAim.getValue() && spearTarget != null) {
            Rotations.lookAtV3(spearTarget.getBoundingBox().getCenter());
            spearControllingRotations = true;
        } else if (spearControllingRotations) {
            Rotations.disable();
            spearControllingRotations = false;
        }

        if (spearTarget == null) {
            spearIdleReason = "no target in range";
        } else {
            spearIdleReason = "";
        }

        if (spearMode.getValue() == SpearMode.Blink) {
            tickSpearBlink();
        }
    }

    private LivingEntity findBowTarget() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return null;
        Vec3 eye = KorppuMod.mc.player.getEyePosition();
        LivingEntity best = null;
        float bestYawDelta = Float.MAX_VALUE;

        for (Entity e : KorppuMod.mc.level.entitiesForRendering()) {
            if (e == KorppuMod.mc.player) continue;
            if (!(e instanceof LivingEntity le) || !le.isAttackable() || !le.isAlive()) continue;
            if (e.getType() == EntityType.ENDERMAN) continue;

            if (eye.y <= le.getY() || eye.y >= le.getY() + le.getBbHeight()) continue;

            Vec3 center = le.getBoundingBox().getCenter();
            if (!KorppuMath.hasLOS(eye, center, KorppuMod.mc.level, KorppuMod.mc.player)) continue;

            float yawDelta = Math.abs(yawDeltaTo(le));
            if (yawDelta < bestYawDelta) {
                bestYawDelta = yawDelta;
                best = le;
            }
        }
        return best;
    }

    private float yawDeltaTo(LivingEntity target) {
        Vec3 playerPos = KorppuMod.mc.player.position();
        Vec3 targetPos = new Vec3(target.getX(), target.getY(), target.getZ());
        double deltaX = targetPos.x - playerPos.x;
        double deltaZ = targetPos.z - playerPos.z;
        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        return Mth.wrapDegrees((float) (targetYaw - KorppuMod.mc.player.getYRot()));
    }

    private Entity findSpearTarget() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.player == null) return null;
        double range = spearRange.getValue();
        Vec3 eye = KorppuMod.mc.player.getEyePosition();
        Vec3 look = lookDir();
        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : KorppuMod.mc.level.entitiesForRendering()) {
            if (e == KorppuMod.mc.player) continue;
            if (!(e instanceof LivingEntity le) || !le.isAlive() || !le.isAttackable()) continue;
            if (!spearTargets.matches(e)) continue;
            if (e instanceof Player pe && spearIgnoreFriends.getValue()) {
                if (PlayerManager.getInstance().getRelation(pe.getUUID()) == PlayerRelation.FRIEND) {
                    continue;
                }
            }
            double dist = KorppuMod.mc.player.distanceTo(e);
            if (dist > range) continue;
            Vec3 toEntity = new Vec3(e.getX() - KorppuMod.mc.player.getX(), e.getY() - KorppuMod.mc.player.getY(),
                                       e.getZ() - KorppuMod.mc.player.getZ()).normalize();
            if (look.dot(toEntity) <= 0) continue;
            if (!KorppuMath.hasLOS(eye, e.getBoundingBox().getCenter(), KorppuMod.mc.level, KorppuMod.mc.player)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private void sendProjectileBurst(Vec3 hDir) {
        double x = KorppuMod.mc.player.getX();
        double y = KorppuMod.mc.player.getY();
        double z = KorppuMod.mc.player.getZ();

        KorppuMod.mc.player.connection.send(
                new ServerboundPlayerCommandPacket(KorppuMod.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

        double disp = strength.getValue() / 10.0 * Math.sqrt(500);
        int iters = bowCharge.getValue().intValue();

        for (int i = 0; i < iters; i++) {
            KorppuMod.mc.player.connection.send(
                    new ServerboundMovePlayerPacket.Pos(x - hDir.x * disp, y, z - hDir.z * disp, true,
                                                                KorppuMod.mc.player.horizontalCollision));
            KorppuMod.mc.player.connection.send(
                    new ServerboundMovePlayerPacket.Pos(x, y, z, false, KorppuMod.mc.player.horizontalCollision));
        }
    }

    private void sendSpearBurst(Entity target) {
        double px = KorppuMod.mc.player.getX();
        double py = KorppuMod.mc.player.getY();
        double pz = KorppuMod.mc.player.getZ();

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double dx = targetCenter.x - px;
        double dz = targetCenter.z - pz;
        double hLen = Math.sqrt(dx * dx + dz * dz);
        if (hLen < 0.01) return;
        double nx = dx / hLen;
        double nz = dz / hLen;

        double disp = spearVelocity.getValue();
        int iters = spearPackets.getValue().intValue();

        KorppuMod.mc.player.connection.send(
                new ServerboundPlayerCommandPacket(KorppuMod.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));

        for (int i = 0; i < iters; i++) {
            KorppuMod.mc.player.connection.send(
                    new ServerboundMovePlayerPacket.Pos(px - nx * disp, py, pz - nz * disp, true,
                                                                KorppuMod.mc.player.horizontalCollision));
            KorppuMod.mc.player.connection.send(
                    new ServerboundMovePlayerPacket.Pos(px, py, pz, false, KorppuMod.mc.player.horizontalCollision));
        }
    }

    @Override
    public void afterTick() {
        if (KorppuMod.mc.player == null) return;
        if (!spearEnabled.getValue() || !spearActive || spearTarget == null) return;

        if (spearMode.getValue() == SpearMode.Legacy) {
            sendSpearBurst(spearTarget);
            spearBurstCount++;
        }
    }

    @Override
    public void onFastTick() {
        if (KorppuMod.mc.player == null) return;
        Entity target = spearTarget;
        if (!spearEnabled.getValue() || !spearActive || target == null) return;
        if (spearMode.getValue() == SpearMode.Lunge) {
            tickSpearLunge(target);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.indicator("Bow", bowEnabled.getValue()));
        entries.add(StatusEntry.indicator("Trident", tridentEnabled.getValue()));
        entries.add(StatusEntry.indicator("Spear", spearEnabled.getValue()));

        if (bowEnabled.getValue() && KorppuMod.mc.player != null && KorppuMod.mc.player.isUsingItem() && KorppuMod.mc.player.getMainHandItem()
                                                                                                                            .getItem() instanceof BowItem) {
            if (bowTarget != null) {
                String name = bowTarget instanceof Player p ? p.getName().getString() : bowTarget.getType().getDescription()
                                                                                                       .getString();
                double dist = KorppuMod.mc.player.distanceTo(bowTarget);
                entries.add(StatusEntry.text("Bow Target", String.format("%s (%.1fm)", name, dist), 0xFF55FF55));
            } else {
                entries.add(StatusEntry.text("Bow Target", "none", 0xFFFFAA00));
            }
        }

        if (spearEnabled.getValue() && KorppuMod.mc.player != null) {
            boolean usingItem = KorppuMod.mc.player.isUsingItem();
            if (usingItem) {
                var activeItem = KorppuMod.mc.player.getActiveItem().getItem();
                boolean isSpear = KorppuMod.mc.player.getActiveItem().is(ItemTags.SPEARS);
                entries.add(StatusEntry.text("Active Item",
                                             KorppuMod.mc.player.getActiveItem().getHoverName().getString() + (isSpear ? " [SPEAR]" : " [NOT SPEAR]"),
                                             isSpear ? 0xFF55FF55 : 0xFFFF5555));
            } else {
                var heldItem = KorppuMod.mc.player.getMainHandItem().getItem();
                entries.add(StatusEntry.text("Held Item", KorppuMod.mc.player.getMainHandItem().getHoverName().getString() + " (not using)", 0xFFAAAAAA));
            }

            if (spearActive) {
                if (spearTarget != null) {
                    String name = spearTarget instanceof Player p ? p.getName().getString() : spearTarget.getType().getDescription().getString();
                    double dist = KorppuMod.mc.player.distanceTo(spearTarget);
                    Vec3 toTarget = spearTarget.getBoundingBox().getCenter().subtract(KorppuMod.mc.player.getEyePosition())
                                                .normalize();
                    double dot = lookDir().dot(toTarget);
                    double angleDeg = Math.toDegrees(Math.acos(Mth.clamp((float) dot, -1f, 1f)));
                    entries.add(StatusEntry.text("Spear Target", String.format("%s %.1fm %.0f°", name, dist, angleDeg),
                                                 angleDeg < 30 ? 0xFF55FF55 : 0xFFFFAA00));

                    double disp = spearVelocity.getValue();
                    String burstInfo = String.format("%.1f blk/tick × %d pkt | x%d", disp, spearPackets.getValue().intValue(),
                                                     spearBurstCount);
                    entries.add(StatusEntry.text("Spear Burst", burstInfo, spearBurstCount > 0 ? 0xFF55FF55 : 0xFFFFFF55));

                    entries.add(StatusEntry.text("Spear Mode", spearMode.getValue().name(), 0xFFAAAAFF));
                    entries.add(StatusEntry.text("Spear Targets", spearTargets.summary(), 0xFFAAAAFF));
                    if (spearMode.getValue() == SpearMode.Blink) {
                        entries.add(StatusEntry.text("Spear Blink", "queued=" + spearBlinkPackets.size() + " charging=" + spearBlinkChargeTicks,
                                spearBlinking ? 0xFF55FF55 : 0xFFFFFF55));
                    }

                    entries.add(StatusEntry.text("Spear Aim", spearControllingRotations ? "auto-aimed" : "manual",
                                                 spearControllingRotations ? 0xFF55FF55 : 0xFFAAAAAA));
                } else {
                    entries.add(StatusEntry.text("Spear Target", "none in range", 0xFFFF5555));
                    entries.add(StatusEntry.text("Spear State", "using spear, scanning...", 0xFFFFAA00));
                }
            } else if (!spearIdleReason.isEmpty() && !spearIdleReason.equals("disabled")) {
                entries.add(StatusEntry.text("Spear State", spearIdleReason, 0xFFAAAAAA));
            }
        }
        return entries;
    }

    private Vec3 lookDir() {
        float pitch = Rotations.isEnabled() ? Rotations.getClientPitch() : KorppuMod.mc.player.getXRot();
        float yaw = Rotations.isEnabled() ? Rotations.getClientYaw() : KorppuMod.mc.player.getYRot();
        return Rotations.getRotationVector(pitch, yaw).normalize();
    }

    private Vec3 horizontalLookDir() {
        Vec3 d = lookDir();
        double hLen = Math.sqrt(d.x * d.x + d.z * d.z);
        if (hLen < 0.01) return new Vec3(0, 0, 1);
        return new Vec3(d.x / hLen, 0, d.z / hLen);
    }

    private void tickSpearLunge(Entity target) {
        if (KorppuMod.mc.player == null) return;
        if (( KorppuMod.mc.player.getUseItem().getItem().getUseDuration(KorppuMod.mc.player.getUseItem(), KorppuMod.mc.player) - KorppuMod.mc.player.getUseItemRemainingTicks()) < spearReadyTicks.getValue().intValue()) return;

        Vec3 currentPos = KorppuMod.mc.player.position();
        Vec3 targetCenter = target.getBoundingBox().getCenter();

        if (spearStopOnTarget.getValue() && KorppuMod.mc.player.distanceTo(target) <= spearStopDistance.getValue()) {
            KorppuMod.mc.player.setDeltaMovement(0, 0, 0);
            KorppuMod.mc.player.setSprinting(false);
            return;
        }

        Vec3 desired;
        if (spearFromAbove.getValue()) {
            if (!spearAbovePhase || spearAboveTargetPos == null) {
                spearAboveTargetPos = new Vec3(targetCenter.x, targetCenter.y + spearAboveHeight.getValue(), targetCenter.z);
                spearAbovePhase = true;
            }

            if (currentPos.distanceTo(spearAboveTargetPos) <= spearAboveTrigger.getValue()) {
                spearAbovePhase = false;
                desired = targetCenter;
            } else {
                desired = spearAboveTargetPos;
            }
        } else {
            desired = targetCenter;
        }

        Vec3 direction = desired.subtract(currentPos).normalize();
        KorppuMod.mc.player.setSprinting(true);
        KorppuMod.mc.player.setDeltaMovement(direction.scale(spearVelocity.getValue()));
    }

    private void tickSpearBlink() {
        if (KorppuMod.mc.player == null) return;

        if (!spearBlinking) {
            startSpearBlink();
        }

        if (spearTarget != null && spearBlinkAimbot.getValue()) {
            Rotations.lookAtV3(spearTarget.getBoundingBox().getCenter());
            spearControllingRotations = true;
        }

        if (spearBlinkFlushCooldown > 0) {
            spearBlinkFlushCooldown--;
        }

        if (spearBlinkLunge.getValue() && spearTarget != null && spearBlinkFlushCooldown <= 0
                && spearBlinkChargeTicks >= spearBlinkLungeDelay.getValue().intValue()) {
            Vec3 dir = spearTarget.getBoundingBox().getCenter().subtract(KorppuMod.mc.player.position()).normalize();
            KorppuMod.mc.player.setSprinting(true);
            KorppuMod.mc.player.setDeltaMovement(dir.scale(spearBlinkLungeStrength.getValue()));
        }

        if (spearTarget == null) return;

        double currentDistance = KorppuMod.mc.player.distanceTo(spearTarget);
        boolean approaching = currentDistance < spearBlinkLastDistance;
        boolean shouldFlush = false;

        if (currentDistance <= spearBlinkFlushRange.getValue()) {
            shouldFlush = true;
        } else if (spearBlinkWasApproaching && !approaching && currentDistance < 8.0) {
            shouldFlush = true;
        } else if (!spearBlinkLunge.getValue() && spearBlinkStartPos != null && KorppuMod.mc.player.position().distanceTo(spearBlinkStartPos) >= spearBlinkForceFlush.getValue()) {
            shouldFlush = true;
        }

        if (shouldFlush) {
            flushSpearBlinkPackets();
            if (spearBlinkLunge.getValue()) {
                spearBlinkFlushCooldown = spearBlinkLungeDelay.getValue().intValue();
            }
            startSpearBlink();
        } else {
            spearBlinkLastDistance = currentDistance;
            spearBlinkWasApproaching = approaching;
        }
    }

    private void startSpearBlink() {
        if (KorppuMod.mc.player == null) return;
        spearBlinking = true;
        spearBlinkStartPos = KorppuMod.mc.player.position();
        spearBlinkPackets.clear();
        spearBlinkLastDistance = spearTarget != null ? KorppuMod.mc.player.distanceTo(spearTarget) : Double.MAX_VALUE;
        spearBlinkWasApproaching = false;
    }

    private void flushSpearBlinkPackets() {
        if (!spearBlinking || KorppuMod.mc.player == null || KorppuMod.mc.player.connection == null) return;

        Vec3 currentPos = KorppuMod.mc.player.position();
        double distance = spearBlinkStartPos != null ? spearBlinkStartPos.distanceTo(currentPos) : 0;
        if (distance < spearBlinkFlushRange.getValue()) {
            spearBlinkPackets.clear();
            spearBlinking = false;
            return;
        }

        spearBlinkFlushing = true;
        try {
            Vec3 startPos = spearBlinkStartPos;
            double boost = spearBlinkDistanceBoost.getValue();
            Vec3 sendStartPos = startPos;
            if (startPos != null && boost > 0 && KorppuMod.mc.level != null) {
                Vec3 direction = currentPos.subtract(startPos);
                Vec3 horizontalDir = new Vec3(direction.x, 0, direction.z);
                if (horizontalDir.length() > 0.01) {
                    horizontalDir = horizontalDir.normalize();
                    Vec3 targetPos = startPos.subtract(horizontalDir.scale(boost));
                    HitResult hit = KorppuMod.mc.level.clip(new ClipContext(
                            startPos, targetPos,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            KorppuMod.mc.player
                    ));
                    if (hit.getType() == HitResult.Type.MISS) {
                        sendStartPos = targetPos;
                    } else {
                        sendStartPos = hit.getLocation().add(horizontalDir.scale(0.5));
                    }
                }
            }

            if (sendStartPos != null) {
                KorppuMod.mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                        sendStartPos.x, sendStartPos.y, sendStartPos.z,
                        KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(),
                        false, false
                ));
            }

            spearBlinkPackets.clear();

            KorppuMod.mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    currentPos.x, currentPos.y, currentPos.z,
                    KorppuMod.mc.player.getYRot(), KorppuMod.mc.player.getXRot(),
                    KorppuMod.mc.player.onGround(), KorppuMod.mc.player.horizontalCollision
            ));
        } finally {
            spearBlinkPackets.clear();
            spearBlinkFlushing = false;
            spearBlinking = false;
        }
    }

    private static boolean isSameMovePacket(ServerboundMovePlayerPacket a, ServerboundMovePlayerPacket b) {
        return a.isOnGround() == b.isOnGround()
                && a.getYRot(-1) == b.getYRot(-1)
                && a.getXRot(-1) == b.getXRot(-1)
                && a.getX(-1) == b.getX(-1)
                && a.getY(-1) == b.getY(-1)
                && a.getZ(-1) == b.getZ(-1);
    }

    public enum SpearMode {
        Legacy,
        Lunge,
        Blink
    }
}
