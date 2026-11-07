package dev.mlml.korppu.command.commands;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.exception.CommandException;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.PacketEvent;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.imgui.windows.ConsoleWindow;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Locate extends Command {
    private static final Pattern LOCATE_PATTERN = Pattern.compile("\\[(-?\\d+),\\s*~?[^,]*,\\s*(-?\\d+)\\]");
    private static final Pattern COORD_PAIR_PATTERN = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");

    private volatile boolean pendingLocate;
    private volatile long pendingUntilMs;

    public Locate() {
        super("locate", "Wrapper for /locate command", "find");
        KorppuMod.eventManager.register(this);
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (KorppuMod.mc.player == null) throw new CommandException("Not in world.");
        validateArgsLength(args, 1, "Usage: locate <args...>");

        String joined = String.join(" ", args);
        pendingLocate = true;
        pendingUntilMs = System.currentTimeMillis() + 15000;
        KorppuImGui.openLocateResults();
        KorppuImGui.setLocatePendingQuery(joined);
        KorppuImGui.addLocateResult("/locate " + joined);
        KorppuMod.mc.player.connection.sendCommand("locate " + joined);
        ConsoleWindow.print("Executed /locate " + joined);
    }

    @Listener
    public void onPacket(PacketEvent.Received event) {
        if (!pendingLocate || System.currentTimeMillis() > pendingUntilMs || event.getPacket() == null) return;

        String packetName = event.getPacket().getClass().getSimpleName();
        if (!packetName.contains("Message") && !packetName.contains("Chat")) return;

        String msg = extractMessageString(event.getPacket());
        if (msg == null || msg.isBlank()) return;

        String lower = msg.toLowerCase();
        if (!(lower.contains("located") || lower.contains("nearest") || lower.contains("found"))) return;

        Matcher m = LOCATE_PATTERN.matcher(msg);
        if (m.find()) {
            String line = "Locate result: x=" + m.group(1) + " z=" + m.group(2);
            ConsoleWindow.print(line);
            KorppuImGui.addLocateResult(line);
            KorppuImGui.clearLocatePendingQuery();
            pendingLocate = false;
            return;
        }

        Matcher pair = COORD_PAIR_PATTERN.matcher(msg);
        if (pair.find()) {
            String line = "Locate result: x=" + pair.group(1) + " z=" + pair.group(2);
            ConsoleWindow.print(line);
            KorppuImGui.addLocateResult(line);
            KorppuImGui.clearLocatePendingQuery();
            pendingLocate = false;
            return;
        }

        String line = "Locate response: " + msg;
        ConsoleWindow.print(line);
        KorppuImGui.addLocateResult(line);
        KorppuImGui.clearLocatePendingQuery();
        pendingLocate = false;
    }

    private static String extractMessageString(Object packet) {
        StringBuilder out = new StringBuilder();
        try {
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value == null) continue;

                if (value instanceof String s) {
                    if (!s.isBlank()) out.append(s).append(' ');
                } else {
                    String s = value.toString();
                    if (!s.isBlank() && s.length() < 1000) out.append(s).append(' ');
                }
            }
        } catch (Throwable ignored) {
        }
        return out.toString().trim();
    }
}
