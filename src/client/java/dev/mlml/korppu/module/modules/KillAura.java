package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.misc.RenderUtils;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.module.TargetProtection;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Set;

public class KillAura extends Module {
    final DoubleSetting range = config.add(new DoubleSetting("Range", "How far to attack", 5d, 1d, 7d, 1));
    final BooleanSetting multiMode = config.add(new BooleanSetting("Multi Mode", "Attack multiple entities", false));
    final DoubleSetting multiAmount = config.add(new DoubleSetting("Multi Amount", "How many entities to attack", 3d, 1d, 10d, 1).showOnlyIf(() -> multiMode.getValue()));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets",
            EntityTargetSelector.FilterMode.Allowlist, Set.of("PLAYERS", "HOSTILE"));
    private final TargetProtection protection = new TargetProtection(config);
    final ListSetting<AttackPriority> attackPrio = config.add(new ListSetting<>("Mode", "How to prioritize entities", AttackPriority.Near));
    final DoubleSetting additionalDelay = config.add(new DoubleSetting("Additional Delay", "Additional delay between hits", 0d, 0d, 1000d, 1));
    final BooleanSetting lookingCheck = config.add(new BooleanSetting("\"Looking at\" check", "Check if entity is looked at", false));
    final BooleanSetting drawTracers = config.add(new BooleanSetting("Tracers", "Draw tracers to targets", false));
    final BooleanSetting rotations = config.add(new BooleanSetting("Rotations", "Silently aim at target server-side without moving the camera", true));
    final ListSetting<RotationMode> rotationMode = config.add(new ListSetting<>("Rotation Mode", "How to rotate toward the target", RotationMode.Instant).showOnlyIf(() -> rotations.getValue()));
    final DoubleSetting rotationSmoothness = config.add(new DoubleSetting("Rotation Smoothness", "Laziness divisor for smooth rotations (higher = slower)", 3d, 1d, 20d, 1).showOnlyIf(() -> rotations.getValue() && rotationMode.getValue() == RotationMode.Smooth));
    final ListSetting<EvasionStrategy> evasionStrategy = config.add(new ListSetting<>("Evasion Strategy", "Teleport pattern before attacking", EvasionStrategy.None));
    final DoubleSetting evasionDistance = config.add(new DoubleSetting("Evasion Distance", "Teleport distance for evasion", 1.5d, 0d, 4d, 1).showOnlyIf(() -> evasionStrategy.getValue() != EvasionStrategy.None));

    public KillAura() {
        super("KillAura", "Automatically attacks entities", GLFW.GLFW_KEY_Y);
    }

    private long lastAttack = 0;
    private List<LivingEntity> lastTargets = List.of();

    @Override
    public List<StatusEntry> getStatusEntries() {
        List<StatusEntry> entries = new ArrayList<>();
        entries.add(StatusEntry.text("Range", String.format("%.1f", range.getValue())));
        entries.add(StatusEntry.text("Priority", attackPrio.getValue().name()));
        entries.add(StatusEntry.text("Evasion", evasionStrategy.getValue().name()));
        entries.add(StatusEntry.text("Evasion Dist", String.format("%.1f", evasionDistance.getValue())));
        entries.add(StatusEntry.text("Targets", targets.summary()));

        if (KorppuMod.mc.player != null) {
            float cooldown = KorppuMod.mc.player.getAttackStrengthScale(0);
            entries.add(StatusEntry.bar("Cooldown", String.format("%.0f%%", cooldown * 100), cooldown));
        }

        List<LivingEntity> alive = lastTargets.stream()
                .filter(e -> e != null && e.isAlive())
                .toList();

        if (alive.isEmpty()) {
            entries.add(StatusEntry.text("Target", "none"));
        } else if (!multiMode.getValue()) {
            LivingEntity t = alive.getFirst();
            String name = t instanceof net.minecraft.world.entity.player.Player p
                    ? p.getGameProfile().name()
                    : t.getType().getDescription().getString();
            float hp = t.getHealth();
            float maxHp = t.getMaxHealth();
            float dist = t.distanceTo(KorppuMod.mc.player);
            entries.add(StatusEntry.text("Target", String.format("%s (%.1f / %.1f HP)", name, hp, maxHp)));
            entries.add(StatusEntry.bar("HP", String.format("%.1f%%", hp / maxHp * 100), hp / Math.max(1, maxHp),
                    hp / maxHp < 0.25f ? 0xFFFF5555 : hp / maxHp < 0.6f ? 0xFFFFAA00 : 0xFF55FF55));
            entries.add(StatusEntry.text("Distance", String.format("%.1f", dist)));
        } else {
            entries.add(StatusEntry.text("Targets", alive.size() + " active"));
            for (LivingEntity t : alive) {
                String name = t instanceof net.minecraft.world.entity.player.Player p
                        ? p.getGameProfile().name()
                        : t.getType().getDescription().getString();
                float hp = t.getHealth();
                float maxHp = t.getMaxHealth();
                int color = hp / maxHp < 0.25f ? 0xFFFF5555 : hp / maxHp < 0.6f ? 0xFFFFAA00 : 0xFF55FF55;
                entries.add(StatusEntry.bar(name, String.format("%.1f / %.1f", hp, maxHp),
                        hp / Math.max(1, maxHp), color));
            }
        }

        return entries;
    }

    @Override
    public String getStatus() {
        return String.format("<%.1f; %s; %s", range.getValue(), multiMode.getValue() ? "Multi: " + multiAmount.getValue() : "Single", targets.summary());
    }

    private double getYawDelta(LivingEntity le) {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return 999f;
        }

        double pYaw = KorppuMod.mc.player.getYRot();
        double deltaX = le.getX() - KorppuMod.mc.player.getX();
        double deltaZ = le.getZ() - KorppuMod.mc.player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90f;

        return Math.abs(targetYaw - pYaw);
    }

    private List<LivingEntity> sortTargets(List<LivingEntity> targets) {
        return switch (attackPrio.getValue()) {
            case Near ->
                    targets.stream().sorted(Comparator.comparingDouble(a -> a.distanceTo(KorppuMod.mc.player))).collect(Collectors.toList());
            case Far ->
                    targets.stream().sorted(Comparator.comparingDouble(a -> -a.distanceTo(KorppuMod.mc.player))).collect(Collectors.toList());
            case LowHealth ->
                    targets.stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth)).collect(Collectors.toList());
            case HighHealth ->
                    targets.stream().sorted(Comparator.comparingDouble(a -> -a.getHealth())).collect(Collectors.toList());
            case Yaw ->
                    targets.stream().sorted(Comparator.comparingDouble(this::getYawDelta)).collect(Collectors.toList());
            case YawInverse ->
                    targets.stream().sorted(Comparator.comparingDouble(a -> -getYawDelta(a))).collect(Collectors.toList());
            case Random ->
                    targets.stream().sorted(Comparator.comparingDouble(a -> Math.random())).collect(Collectors.toList());
        };
    }

    private List<LivingEntity> getTargets() {
        return StreamSupport.stream(KorppuMod.mc.level.entitiesForRendering().spliterator(), false).filter(e -> e instanceof LivingEntity)
                            .filter(e -> !e.getUUID().equals(KorppuMod.mc.player.getUUID())).filter(Entity::isAlive)
                            .filter(Entity::isAttackable).filter(targets::matches).filter(e -> !protection.isProtected(e)).filter(e -> {
                    if (e instanceof Player player && AntiBot.isBot(player)) {
                        return false;
                    }
                    if (lookingCheck.getValue()) {
                        Vec3 rayDir = KorppuMod.mc.player.getViewVector(0).scale(range.getValue());
                        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(
                                KorppuMod.mc.player,
                                KorppuMod.mc.player.getEyePosition(0),
                                KorppuMod.mc.player.getEyePosition(0).add(rayDir),
                                KorppuMod.mc.player.getBoundingBox().expandTowards(rayDir).inflate(1, 1, 1),
                                Entity::isAttackable,
                                range.getValue() * range.getValue()
                        );
                        return Objects.nonNull(ehr) && ehr.getEntity().equals(e);
                    }
                    return true;
                }).filter(e -> e.distanceTo(KorppuMod.mc.player) <= range.getValue()).map(e -> (LivingEntity) e)
                            .collect(Collectors.toList());
    }

    @Override
    public void onDisable() {
        Rotations.disable();
        lastTargets = List.of();
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) {
            return;
        }

        List<LivingEntity> targets = getTargets();
        if (targets.isEmpty()) {
            Rotations.disable();
            lastTargets = List.of();
            return;
        }

        List<LivingEntity> sorted = sortTargets(targets);
        if (multiMode.getValue()) {
            targets = sorted.stream().limit(multiAmount.getValue().intValue()).collect(Collectors.toList());
        } else {
            targets = sorted.stream().limit(1).collect(Collectors.toList());
        }

        if (rotations.getValue()) {
            Vec3 aimPoint = targets.getFirst().getBoundingBox().getCenter();
            if (rotationMode.getValue() == RotationMode.Smooth) {
                Rotations.enable();
                Rotations.lookAtPositionSmoothServerSide(aimPoint, rotationSmoothness.getValue());
            } else {
                Rotations.lookAtV3(aimPoint);
            }
        }

        applyEvasion(targets.getFirst());

        if (System.currentTimeMillis() - lastAttack < additionalDelay.getValue()) {
            return;
        }

        if (KorppuMod.mc.player.getAttackStrengthScale(0) < 1) {
            return;
        }

        lastAttack = System.currentTimeMillis();
        lastTargets = targets;
        for (LivingEntity target : targets) {
            KorppuMod.mc.gameMode.attack(KorppuMod.mc.player, target);
            KorppuMod.mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void onWorldRender(PoseStack matrices, float tickProgress) {
        if (drawTracers.getValue()) {
            List<LivingEntity> targets = getTargets();
            for (LivingEntity target : targets) {
                RenderUtils.drawTracer(matrices, tickProgress, target.position(), 0xFF0000FF, false);
            }
        }
    }

    @Override
    public void onFastTick() {
        super.onFastTick();
    }

    private void applyEvasion(LivingEntity target) {
        if (target == null || KorppuMod.mc.player == null) return;
        if (evasionStrategy.getValue() == EvasionStrategy.None) return;

        double d = evasionDistance.getValue();
        Vec3 destination;
        switch (evasionStrategy.getValue()) {
            case Behind -> {
                Vec3 look = Rotations.getRotationVector(0, target.getYHeadRot()).normalize();
                destination = new Vec3(
                        target.getX() - look.x * d,
                        KorppuMod.mc.player.getY(),
                        target.getZ() - look.z * d
                );
            }
            case Above -> {
                double y = target.getY() + Math.min(4.0, d);
                destination = new Vec3(target.getX(), y, target.getZ());
            }
            case Circle -> {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
                double radius = 1.5;
                double x = target.getX() + Math.cos(angle) * radius;
                double z = target.getZ() + Math.sin(angle) * radius;
                double y = target.getY() + Math.min(4.0, d);
                destination = new Vec3(x, y, z);
            }
            default -> {
                return;
            }
        }

        KorppuMod.mc.player.setPos(destination.x, destination.y, destination.z);
        KorppuMod.mc.player.setDeltaMovement(
                KorppuMod.mc.player.getDeltaMovement().x,
                0,
                KorppuMod.mc.player.getDeltaMovement().z
        );

        if (KorppuMod.mc.getConnection() != null) {
            KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    destination.x,
                    destination.y,
                    destination.z,
                    KorppuMod.mc.player.getYRot(),
                    KorppuMod.mc.player.getXRot(),
                    KorppuMod.mc.player.onGround(),
                    KorppuMod.mc.player.horizontalCollision
            ));
        }
    }

    public enum AttackPriority {
        Near, Far, LowHealth, HighHealth, Yaw, YawInverse, Random;
    }

    public enum RotationMode {
        Instant, Smooth;
    }

    public enum EvasionStrategy {
        None, Behind, Above, Circle;
    }
}
