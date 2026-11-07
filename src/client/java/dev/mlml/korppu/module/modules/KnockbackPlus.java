package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.module.EntityTargetSelector;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.registries.Registries;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

public class KnockbackPlus extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Direction", "Direction of outgoing knockback", Mode.Degrees));
    private final DoubleSetting degrees = config.add(new DoubleSetting("Degrees", "Yaw offset for Degrees mode", 180d, 0d, 360d, 0));
    private final EntityTargetSelector targets = new EntityTargetSelector(config, "Targets", Set.of("PLAYERS"));

    private boolean attacking;
    private float originalYaw;

    public KnockbackPlus() {
        super("KnockbackPlus", "Changes knockback direction by spoofing yaw on attack", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Listener
    public void onPacket(PacketEvent.Sent event) {
        if (!isEnabled() || attacking || KorppuMod.mc.player == null || KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null) return;
        if (!(event.getPacket() instanceof net.minecraft.network.protocol.game.ServerboundAttackPacket packet)) return;

        net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> enchLookup = KorppuMod.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        net.minecraft.core.Holder<Enchantment> knockbackHolder = enchLookup.getOrThrow(Enchantments.KNOCKBACK);
        int knockback = KorppuMod.mc.player.getMainHandItem().getEnchantments().getLevel(knockbackHolder);
        if (!KorppuMod.mc.player.isSprinting() && knockback <= 0) return;

        int entityId = packet.entityId();
        Entity entity = KorppuMod.mc.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity)) return;
        if (!targets.matches(entity)) return;

        attacking = true;
        event.cancel();

        originalYaw = KorppuMod.mc.player.getYRot();
        float modifiedYaw = switch (mode.getValue()) {
            case TowardSelf -> originalYaw + 180f;
            case Left -> originalYaw - 90f;
            case Right -> originalYaw + 90f;
            case Degrees -> (float) ((originalYaw + degrees.getValue()) % 360d);
        };

        KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                modifiedYaw,
                KorppuMod.mc.player.getXRot(),
                KorppuMod.mc.player.onGround(),
                KorppuMod.mc.player.horizontalCollision
        ));

        KorppuMod.mc.getConnection().send(packet);
    }

    @Override
    public void onTick() {
        if (!attacking || KorppuMod.mc.player == null) return;
        KorppuMod.mc.player.setYRot(originalYaw);
        KorppuMod.mc.player.yHeadRot = originalYaw;
        attacking = false;
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player != null && attacking) {
            KorppuMod.mc.player.setYRot(originalYaw);
            KorppuMod.mc.player.yHeadRot = originalYaw;
        }
        attacking = false;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Direction", mode.getValue().name()),
                StatusEntry.text("Targets", targets.summary())
        );
    }

    public enum Mode {
        Degrees,
        TowardSelf,
        Left,
        Right
    }
}
