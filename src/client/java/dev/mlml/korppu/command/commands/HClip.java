package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.arg.DoubleArgParser;
import dev.mlml.korppu.command.coloring.ArgType;
import dev.mlml.korppu.command.coloring.PossibleArg;
import dev.mlml.korppu.command.coloring.StaticArgServer;
import dev.mlml.korppu.command.exception.CommandException;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class HClip extends Command {
    public HClip() {
        super("HClip", "Teleport horizontally", "hclip");
    }

    @Override
    public PossibleArg getSuggestionsWithType(int index, String[] args) {
        return StaticArgServer.serveFromStatic(index, new PossibleArg(ArgType.NUMBER, "<amount>"));
    }

    @Override
    public void execute(String[] args) throws CommandException {
        validateArgsLength(args, 1, "Provide distance");

        double dist = new DoubleArgParser().parse(args[0]);
        Vec3 forward = Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot()).normalize();

        if (KorppuMod.mc.player.getAbilities().flying) {
            KorppuMod.mc.player.setPos(KorppuMod.mc.player.getX() + forward.x * dist, KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ() + forward.z * dist);
        } else {
            clip(dist);
        }
    }

    private void clip(double blocks) {
        Vec3 pos = KorppuMod.mc.player.position();
        Vec3 forward = Vec3.directionFromRotation(0, KorppuMod.mc.player.getYRot()).normalize();
        float oldy = KorppuMod.mc.player.getYRot();
        float oldp = KorppuMod.mc.player.getXRot();
        sendPosition(pos.x, pos.y + 9, pos.z);
        sendPosition(pos.x, pos.y + 18, pos.z);
        sendPosition(pos.x, pos.y + 27, pos.z);
        sendPosition(pos.x, pos.y + 36, pos.z);
        sendPosition(pos.x, pos.y + 45, pos.z);
        sendPosition(pos.x, pos.y + 54, pos.z);
        sendPosition(pos.x, pos.y + 63, pos.z);
        sendPosition(pos.x + forward.x * blocks, KorppuMod.mc.player.getY(), pos.z + forward.z * blocks);
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY() - 8.9, KorppuMod.mc.player.getZ());
        sendPosition(KorppuMod.mc.player.getX(), KorppuMod.mc.player.getY(), KorppuMod.mc.player.getZ());
        KorppuMod.mc.player.setYRot(oldy);
        KorppuMod.mc.player.setXRot(oldp);
    }


    private void sendPosition(double x, double y, double z) {
        KorppuMod.mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true, KorppuMod.mc.player.horizontalCollision));
    }
}
