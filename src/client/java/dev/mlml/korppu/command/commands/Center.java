package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

public class Center extends Command {
    public Center() {
        super("center", "Centers X/Z on the current block", "middle");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;

        double x = Mth.floor(KorppuMod.mc.player.getX()) + 0.5d;
        double z = Mth.floor(KorppuMod.mc.player.getZ()) + 0.5d;

        KorppuMod.mc.player.setPos(x, KorppuMod.mc.player.getY(), z);
        KorppuMod.mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                x,
                KorppuMod.mc.player.getY(),
                z,
                KorppuMod.mc.player.onGround(),
                KorppuMod.mc.player.horizontalCollision
        ));
    }
}
