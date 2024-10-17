package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    public FakePlayerEntity() {
        super(Objects.requireNonNull(KorppuMod.mc.world), Objects.requireNonNull(KorppuMod.mc.player).getGameProfile());
        copyPositionAndRotation(KorppuMod.mc.player);

        copyInventory();
        copyPlayerModel(KorppuMod.mc.player, this);
        copyRotation();
        resetCapeMovement();

        spawn();
    }

    private void copyInventory() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        getInventory().clone(KorppuMod.mc.player.getInventory());
    }

    private void copyPlayerModel(Entity from, Entity to) {
        DataTracker fromTracker = from.getDataTracker();
        DataTracker toTracker = to.getDataTracker();
        Byte playerModel = fromTracker.get(PlayerEntity.PLAYER_MODEL_PARTS);
        toTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
    }

    private void copyRotation() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        headYaw = KorppuMod.mc.player.headYaw;
        bodyYaw = KorppuMod.mc.player.bodyYaw;
    }

    private void resetCapeMovement() {
        capeX = getX();
        capeY = getY();
        capeZ = getZ();
    }

    private void spawn() {
        if (KorppuMod.mc.world == null) {
            return;
        }

        KorppuMod.mc.world.addEntity(this);
    }

    public void despawn() {
        discard();
    }

    public void resetPlayerPosition() {
        if (KorppuMod.mc.player == null) {
            return;
        }
        KorppuMod.mc.player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
    }
}
