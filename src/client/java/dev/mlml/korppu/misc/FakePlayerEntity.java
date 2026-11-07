package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;

public class FakePlayerEntity extends RemotePlayer {
    public FakePlayerEntity() {
        super(Objects.requireNonNull(KorppuMod.mc.level), Objects.requireNonNull(KorppuMod.mc.player).getGameProfile());
        setUUID(UUID.randomUUID());
        copyPosition(KorppuMod.mc.player);

        copyInventory();
        copyRotation();

        spawn();
    }

    private void copyInventory() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        getInventory().replaceWith(KorppuMod.mc.player.getInventory());
    }

    private void copyRotation() {
        if (KorppuMod.mc.player == null) {
            return;
        }

        yHeadRot = KorppuMod.mc.player.yHeadRot;
        yBodyRot = KorppuMod.mc.player.yBodyRot;
    }

    private void spawn() {
        if (KorppuMod.mc.level == null) {
            return;
        }

        KorppuMod.mc.level.addEntity(this);
    }

    public void despawn() {
        discard();
    }

    public void resetPlayerPosition() {
        if (KorppuMod.mc.player == null) {
            return;
        }
        KorppuMod.mc.player.setPos(getX(), getY(), getZ());
        KorppuMod.mc.player.setYRot(getYRot());
        KorppuMod.mc.player.setXRot(getXRot());
    }
}