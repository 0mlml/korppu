package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.mixin.IPlayerMoveC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TPFly extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting(
            "Range", "Teleport distance per tick (horizontal)", 5d, 1d, 7d, 1));
    private final DoubleSetting upRange = config.add(new DoubleSetting(
            "Up Range", "Teleport distance per tick (upward)", 4d, 1d, 6d, 1));
    private final DoubleSetting downRange = config.add(new DoubleSetting(
            "Down Range", "Teleport distance per tick (downward)", 4d, 1d, 6d, 1));
    private final BooleanSetting antiKick = config.add(new BooleanSetting(
            "Anti Kick", "Periodically descend slightly to prevent kick", true));
    private final DoubleSetting kickDelay = config.add(new DoubleSetting(
            "Kick Delay", "Ticks between anti-kick drops", 20d, 5d, 60d, 0));
    private final DoubleSetting kickOffTime = config.add(new DoubleSetting(
            "Kick Off Time", "Ticks to briefly descend for anti-kick", 3d, 1d, 20d, 0));

    private int delayLeft;
    private int offLeft;

    public TPFly() {
        super("TPFly", "Teleport-based flight that sends setPos packets each tick to move without velocity", GLFW.GLFW_KEY_UNKNOWN);
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void onEnable() {
        delayLeft = (kickDelay.getValue().intValue());
        offLeft = (kickOffTime.getValue().intValue());
    }

    @Override
    public void onDisable() {
        if (KorppuMod.mc.player == null) return;
        KorppuMod.mc.player.setDeltaMovement(0, 0.01, 0);
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled()) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) return;
        ((IPlayerMoveC2SPacketMixin) event.getPacket()).setOnGround(true);
    }

    @Override
    public void afterTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.level == null) return;
        var player = KorppuMod.mc.player;
        var world = KorppuMod.mc.level;
        var options = KorppuMod.mc.options;

        // Zero out velocity
        player.setDeltaMovement(0, 0, 0);

        BlockPos playerPos = player.blockPosition();
        int r = (range.getValue().intValue());
        int ur = upRange.getValue().intValue();
        int dr = downRange.getValue().intValue();
        float yaw = player.getYRot();

        // Anti-kick logic
        if (antiKick.getValue()) {
            BlockPos below = playerPos.offset(0, -1, 0);
            if (world.getBlockState(below).canBeReplaced()) {
                if (delayLeft > 0) {
                    delayLeft--;
                } else if (offLeft > 0) {
                    offLeft--;
                    player.setPos(player.getX(), player.getY() - 0.1, player.getZ());
                    return;
                } else {
                    delayLeft = (kickDelay.getValue().intValue());
                    offLeft = (kickOffTime.getValue().intValue());
                }
            }
        }

        // Movement: teleport in look direction
        Direction4 movDir = getMovementDirection(yaw);

        if (options.keyJump.isDown()) {
            // Move up
            if (canMoveTo(world, playerPos.offset(0, ur, 0))) {
                player.setPos(player.getX(), player.getY() + ur, player.getZ());
            }
        } else if (options.keySprint.isDown()) {
            // Move down
            if (canMoveTo(world, playerPos.offset(0, -dr, 0))) {
                player.setPos(player.getX(), player.getY() - dr, player.getZ());
            }
        }

        if (options.keyUp.isDown()) {
            double newX = player.getX() - Math.sin(Math.toRadians(yaw)) * r;
            double newZ = player.getZ() + Math.cos(Math.toRadians(yaw)) * r;
            BlockPos target = new BlockPos((int) newX, (int) player.getY(), (int) newZ);
            if (canMoveTo(world, target) && world.getBlockState(target).getBlock() != Blocks.LAVA) {
                player.setPos(newX, player.getY(), newZ);
            }
        } else if (options.keyDown.isDown()) {
            double newX = player.getX() + Math.sin(Math.toRadians(yaw)) * r;
            double newZ = player.getZ() - Math.cos(Math.toRadians(yaw)) * r;
            BlockPos target = new BlockPos((int) newX, (int) player.getY(), (int) newZ);
            if (canMoveTo(world, target) && world.getBlockState(target).getBlock() != Blocks.LAVA) {
                player.setPos(newX, player.getY(), newZ);
            }
        }

        if (options.keyLeft.isDown()) {
            double newX = player.getX() - Math.cos(Math.toRadians(yaw)) * r;
            double newZ = player.getZ() - Math.sin(Math.toRadians(yaw)) * r;
            BlockPos target = new BlockPos((int) newX, (int) player.getY(), (int) newZ);
            if (canMoveTo(world, target) && world.getBlockState(target).getBlock() != Blocks.LAVA) {
                player.setPos(newX, player.getY(), newZ);
            }
        } else if (options.keyRight.isDown()) {
            double newX = player.getX() + Math.cos(Math.toRadians(yaw)) * r;
            double newZ = player.getZ() + Math.sin(Math.toRadians(yaw)) * r;
            BlockPos target = new BlockPos((int) newX, (int) player.getY(), (int) newZ);
            if (canMoveTo(world, target) && world.getBlockState(target).getBlock() != Blocks.LAVA) {
                player.setPos(newX, player.getY(), newZ);
            }
        }
    }

    private boolean canMoveTo(net.minecraft.world.level.Level world, BlockPos pos) {
        if (world == null) return false;
        // Check a few blocks above for head clearance
        return world.getBlockState(pos).canBeReplaced()
                && world.getBlockState(pos.above()).canBeReplaced();
    }

    private Direction4 getMovementDirection(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 45 || yaw >= 315) return Direction4.SOUTH;
        if (yaw < 135) return Direction4.WEST;
        if (yaw < 225) return Direction4.NORTH;
        return Direction4.EAST;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Range", (range.getValue().intValue()) + " blocks/tick"),
                StatusEntry.indicator("Anti-kick", antiKick.getValue())
        );
    }

    @Override
    public String getStatus() {
        return "Range: " + (range.getValue().intValue());
    }

    private enum Direction4 { NORTH, SOUTH, EAST, WEST }
}
