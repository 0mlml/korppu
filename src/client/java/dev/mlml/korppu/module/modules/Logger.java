package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.gui.ChatHelper;
import dev.mlml.korppu.gui.TextFormatter;
import dev.mlml.korppu.mixin.IPlayerInteractEntityC2SPacketMixin;
import dev.mlml.korppu.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Logger extends Module {
    private final BooleanSetting logTaken = config.add(new BooleanSetting("Log Taken", "Logs damage taken", true));
    private final BooleanSetting logOtherTaken = config.add(new BooleanSetting("Log Other Taken", "Logs damage taken by others", true));
    private final BooleanSetting logDealt = config.add(new BooleanSetting("Log Dealt", "Logs damage dealt", true));
    private final BooleanSetting logPlayerEnterRender = config.add(new BooleanSetting("Log Player Enter Render", "Logs when a player enters render distance", true));
    private final BooleanSetting logPlayerLeaveRender = config.add(new BooleanSetting("Log Player Leave Render", "Logs when a player leaves render distance", true));
    private final BooleanSetting logGameModeChange = config.add(new BooleanSetting("Log Game Mode Change", "Logs when a player changes game mode", true));
    private final BooleanSetting debug = config.add(new BooleanSetting("Debug", "Logs debug information", false));

    private final BooleanSetting toChat = config.add(new BooleanSetting("To Chat", "Logs to chat (as opposed to just status)", true));

    public Logger() {
        super("DamageLogger", "Logs damage", -1);

        KorppuMod.eventManager.register(this);
    }

    private record QueuedEntity(LivingEntity entity, String damageSource, float health) {
    }

    private List<QueuedEntity> queuedEntitiesToProcess = new ArrayList<>();
    private List<QueuedEntity> queuedEntitiesThisTick = new ArrayList<>();

    @Override
    public void onTick() {
        if (KorppuMod.mc.world == null || KorppuMod.mc.player == null) {
            return;
        }

        int count = 0;
        for (QueuedEntity entity : queuedEntitiesToProcess) {
            String message = processEntity(entity);
            if (message != null) {
                logMessage(message);
                count++;
            }
        }
        if (debug.getValue() && count > 0) {
            logMessage("Processed " + count + " entities.");
        }
        queuedEntitiesToProcess.clear();
        queuedEntitiesToProcess.addAll(queuedEntitiesThisTick);
        queuedEntitiesThisTick.clear();
    }

    private String processEntity(QueuedEntity qe) {
        StringBuilder message = new StringBuilder();
        if (qe.entity == KorppuMod.mc.player) {
            if (!logTaken.getValue()) {
                return null;
            }
            message.append(TextFormatter.format("%1You were %2", TextFormatter.Code.RESET, TextFormatter.Code.RED));
        } else {
            if (!logOtherTaken.getValue()) {
                return null;
            }
            message.append(qe.entity.getName().getString());
            message.append(TextFormatter.format(" was %1", TextFormatter.Code.RED));
        }
        message.append(qe.damageSource);
        Entity entity = KorppuMod.mc.world.getEntityById(qe.entity.getId());
        if (entity == null || !(entity instanceof LivingEntity)) {
            message.append(" for an unknown amount of damage. Was ");
            message.append(TextFormatter.format("%2%.1f%1/%2%.1f%1 hp.", TextFormatter.Code.RESET, TextFormatter.Code.GREEN, qe.entity.getHealth(), qe.entity.getMaxHealth()));
            return message.toString();
        }
        LivingEntity le = (LivingEntity) entity;
        double delta = qe.health - le.getHealth();
        message.append(" for ");
        message.append(TextFormatter.format("%2%.1f%1 hp. Now ", TextFormatter.Code.RESET, TextFormatter.Code.RED, delta));
        message.append(TextFormatter.format("%2%.1f%1/%2%.1f%1 hp.", TextFormatter.Code.RESET, TextFormatter.Code.GREEN, le.getHealth(), le.getMaxHealth()));
        return message.toString();
    }

    private String status = "";

    private void logMessage(String message) {
        logMessage(message, false);
    }

    private void logMessage(String message, boolean canBeStatus) {
        if (toChat.getValue()) {
            ChatHelper.message(message);
        }
        if (canBeStatus) {
            status = message;
        }
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Listener
    public void onPacketSend(PacketEvent.Sent event) {
        if (!isEnabled() || KorppuMod.mc.world == null || KorppuMod.mc.player == null) {
            return;
        }

        if (logDealt.getValue() && event.getPacket() instanceof PlayerInteractEntityC2SPacket epacket) {
            handleLogDealt(epacket);
        }
    }

    private void handleLogDealt(PlayerInteractEntityC2SPacket epacket) {
        IPlayerInteractEntityC2SPacketMixin packet = (IPlayerInteractEntityC2SPacketMixin) epacket;
        Entity entity = KorppuMod.mc.world.getEntityById(packet.getEntityId());
        if (entity == null) {
            return;
        }

        final boolean[] isAttack = {false};
        epacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override
            public void interact(Hand hand) {

            }

            @Override
            public void interactAt(Hand hand, Vec3d pos) {

            }

            @Override
            public void attack() {
                isAttack[0] = true;
            }
        });
        if (!isAttack[0]) {
            return;
        }

        if (!(entity instanceof LivingEntity le)) {
            return;
        }

        double hitDistance = Math.sqrt(KorppuMod.mc.player.squaredDistanceTo(le));

        StringBuilder message = new StringBuilder();
        message.append(TextFormatter.format("%1Hit %2", TextFormatter.Code.RESET, TextFormatter.Code.YELLOW));
        message.append(le.getName().getString());
        message.append(TextFormatter.format(" at %2%.1f%1 blocks.", TextFormatter.Code.RESET, TextFormatter.Code.YELLOW, hitDistance));
        message.append(TextFormatter.format(" Was %2%.1f%1/%2%.1f%1 hp", TextFormatter.Code.RESET, TextFormatter.Code.GREEN, le.getHealth(), le.getMaxHealth()));
        logMessage(message.toString());
    }

    @Listener
    public void onPacketReceive(PacketEvent.Received event) {
        if (!isEnabled() || KorppuMod.mc.world == null || KorppuMod.mc.player == null) {
            return;
        }

        if ((logTaken.getValue() || logOtherTaken.getValue()) && event.getPacket() instanceof EntityDamageS2CPacket packet) {
            handleLogTaken(packet);
        }
    }

    private void handleLogTaken(EntityDamageS2CPacket packet) {
        Entity entity = KorppuMod.mc.world.getEntityById(packet.entityId());
        if (entity == null || !(entity instanceof LivingEntity le)) {
            return;
        }

        String type = packet.sourceType().value().msgId();
        int attackerId = packet.sourceDirectId();
        if (attackerId > 0) {
            Entity attacker = KorppuMod.mc.world.getEntityById(attackerId);
            if (attacker != null) {
                type += " by " + attacker.getName().getString();
            }
        }

        QueuedEntity qe = new QueuedEntity(le, type, le.getHealth());

        queuedEntitiesThisTick.add(qe);
    }
}
