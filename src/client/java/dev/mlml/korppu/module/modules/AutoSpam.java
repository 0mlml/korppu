package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.config.StringSetting;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.ChatSendEvent;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoSpam extends Module {
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "What type of message to send", Mode.Chat));
    private final StringSetting message = config.add(new StringSetting("Message", "Message or command to send", "Hello!"));
    private final DoubleSetting intervalTicks = config.add(new DoubleSetting(
            "Interval", "Ticks between messages (20 ticks = 1 second)", 200d, 20d, 6000d, 0));

    private int tickCount = 0;

    public AutoSpam() {
        super("AutoSpam", "Automatically sends chat messages or commands on a timer", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        tickCount = 0;
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;

        tickCount++;
        if (tickCount >= intervalTicks.getValue().intValue()) {
            tickCount = 0;
            sendMessage();
        }
    }

    private void sendMessage() {
        String msg = message.getValue();
        if (msg.isEmpty()) return;

        switch (mode.getValue()) {
            case Chat -> KorppuMod.mc.getConnection().sendChat(msg);
            case Command -> {
                String cmd = msg.startsWith("/") ? msg.substring(1) : msg;
                KorppuMod.mc.getConnection().sendCommand(cmd);
            }
            case Title -> KorppuMod.mc.getConnection().sendCommand("title @a title {\"text\":\"" + msg + "\"}");
            case Scoreboard -> KorppuMod.mc.getConnection().sendCommand("scoreboard objectives add spam dummy " + msg);
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        int interval = intervalTicks.getValue().intValue();
        return List.of(
                StatusEntry.text("Mode", mode.getValue().name()),
                StatusEntry.bar("Next", tickCount + " / " + interval, (float) tickCount / interval),
                StatusEntry.text("Message", message.getValue().length() > 20
                        ? message.getValue().substring(0, 20) + "..." : message.getValue())
        );
    }

    @Override
    public String getStatus() {
        return mode.getValue().name() + " every " + intervalTicks.getValue().intValue() + " ticks";
    }

    public enum Mode {
        Chat, Command, Title, Scoreboard
    }
}
