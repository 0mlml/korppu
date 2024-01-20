package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.misc.KorppuMath;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class InstaBow extends Module
{
    private final DoubleSetting charge = config.add(new DoubleSetting("Charge", "How many attempts to spoof velocity", 30d, 5d, 150d, 0));
    private final BooleanSetting aimAssist = config.add(new BooleanSetting("Auto Fire", "Automatically release when aiming at an entity", true));
    private final LivingEntity target = null;

    public InstaBow()
    {
        super("InstaBow", "Shoot bows instantly", ModuleType.PLAYER, GLFW.GLFW_KEY_APOSTROPHE);

        InstaBowPacketHandler packetHandler = new InstaBowPacketHandler();
        KorppuMod.eventManager.register(packetHandler);
    }

    @Override
    public void onTick()
    {
        if (!aimAssist.getValue() || KorppuMod.mc.player == null)
        {
            return;
        }

        if (!KorppuMod.mc.player.isUsingItem() || KorppuMod.mc.player.getMainHandStack().getItem() != Items.BOW)
        {
            return;
        }

        BowItem bowItem = (BowItem) KorppuMod.mc.player.getMainHandStack().getItem();
        int pullback = bowItem.getMaxUseTime(KorppuMod.mc.player.getMainHandStack()) - KorppuMod.mc.player.getItemUseTimeLeft();

        if (BowItem.getPullProgress(pullback) < 0.1f)
        {
            return;
        }

        Vec3d eyePosition = KorppuMod.mc.player.getEyePos();
        Entity bestTarget = null;
        for (Entity entity : KorppuMod.mc.world.getEntities())
        {
            if (entity.equals(KorppuMod.mc.player) || !(entity instanceof LivingEntity ent) || !ent.isAttackable() || !ent.isAlive())
            {
                continue;
            }

            if (entity.getType() == EntityType.ENDERMAN)
            {
                continue;
            }

            Vec3d origin = entity.getPos();
            float h = entity.getHeight();
            Vec3d upper = origin.add(0, h, 0);
            if (!KorppuMath.hasLOS(eyePosition, entity.getPos().add(0, h / 2f, 0), KorppuMod.mc.world, KorppuMod.mc.player))
            {
                continue;
            }
            if (eyePosition.y < upper.y && eyePosition.y > origin.y)
            {
                if (bestTarget == null || bestTarget.distanceTo(KorppuMod.mc.player) > origin.distanceTo(KorppuMod.mc.player.getPos()))
                {
                    bestTarget = entity;
                }
            }
        }

        if (bestTarget == null)
        {
            return;
        }

        Rotations.lookAtV3(bestTarget.getPos().add(0, bestTarget.getHeight() / 2f, 0));
        Objects.requireNonNull(KorppuMod.mc.getNetworkHandler()).sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(Rotations.getClientYaw(), Rotations.getClientPitch(), KorppuMod.mc.player.isOnGround()));
        Objects.requireNonNull(KorppuMod.mc.interactionManager).stopUsingItem(KorppuMod.mc.player);
    }

    public class InstaBowPacketHandler
    {
        public void onPacketSend(PacketEvent.Sent event)
        {
            if (!isEnabled())
            {
                return;
            }

            if (event.getPacket() instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM)
            {
                if (KorppuMod.mc.player == null || KorppuMod.mc.getNetworkHandler() == null || !ModuleManager.isSendPackets())
                {
                    return;
                }

                Vec3d start = KorppuMod.mc.player.getPos().subtract(0, 1e-10, 0);
                Vec3d end = KorppuMod.mc.player.getPos().add(0, 1e-10, 0);
                KorppuMod.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(KorppuMod.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                for (int i = 0; i < charge.getValue(); i++)
                {
                    PlayerMoveC2SPacket p = new PlayerMoveC2SPacket.PositionAndOnGround(start.x, start.y, start.z, true);
                    PlayerMoveC2SPacket p1 = new PlayerMoveC2SPacket.PositionAndOnGround(end.x, end.y, end.z, false);
                    KorppuMod.mc.getNetworkHandler().sendPacket(p);
                    KorppuMod.mc.getNetworkHandler().sendPacket(p1);
                }
            }
        }
    }
}
