package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.module.Module;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.StatusEntry;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private final BooleanSetting nullListEntry = config.add(new BooleanSetting("Null List Entry", "Mark players missing tab-list entry as bots", true));
    private final BooleanSetting nullGamemode = config.add(new BooleanSetting("Null Gamemode", "Mark players with null gamemode as bots", true));
    private final BooleanSetting nullProfile = config.add(new BooleanSetting("Null Profile", "Mark players with null profile as bots", true));
    private final BooleanSetting invalidPing = config.add(new BooleanSetting("Invalid Ping", "Mark players with invalid ping as bots", true));
    private final DoubleSetting maxPing = config.add(new DoubleSetting("Max Ping", "Maximum valid ping before a player is flagged", 1000d, 1d, 100000d, 0)
            .showOnlyIf(() -> invalidPing.getValue()));

    private final Set<UUID> botPlayers = new LinkedHashSet<>();

    public AntiBot() {
        super("AntiBot", "Filters fake players from targeting/render modules", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onDisable() {
        botPlayers.clear();
    }

    @Override
    public void onTick() {
        if (KorppuMod.mc.level == null || KorppuMod.mc.getConnection() == null || KorppuMod.mc.player == null) {
            return;
        }

        botPlayers.clear();

        for (Player player : KorppuMod.mc.level.players()) {
            if (player.getUUID().equals(KorppuMod.mc.player.getUUID())) continue;

            PlayerInfo entry = KorppuMod.mc.getConnection().getPlayerInfo(player.getUUID());
            if (entry == null) {
                if (nullListEntry.getValue()) {
                    botPlayers.add(player.getUUID());
                }
                continue;
            }

            if (nullProfile.getValue() && entry.getProfile() == null) {
                botPlayers.add(player.getUUID());
                continue;
            }

            if (nullGamemode.getValue() && entry.getGameMode() == null) {
                botPlayers.add(player.getUUID());
                continue;
            }

            if (invalidPing.getValue()) {
                int ping = entry.getLatency();
                if (ping < 0 || ping > maxPing.getValue()) {
                    botPlayers.add(player.getUUID());
                }
            }
        }
    }

    @Override
    public List<StatusEntry> getStatusEntries() {
        return List.of(
                StatusEntry.indicator("Null Entry", nullListEntry.getValue()),
                StatusEntry.indicator("Null Gamemode", nullGamemode.getValue()),
                StatusEntry.indicator("Null Profile", nullProfile.getValue()),
                StatusEntry.indicator("Invalid Ping", invalidPing.getValue()),
                StatusEntry.text("Detected", String.valueOf(botPlayers.size()))
        );
    }

    @Override
    public String getStatus() {
        return botPlayers.size() + " bots";
    }

    public boolean isBot(UUID playerId) {
        return botPlayers.contains(playerId);
    }

    public static boolean isBot(Player player) {
        if (player == null) return false;
        AntiBot antiBot = ModuleManager.getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && antiBot.isBot(player.getUUID());
    }
}
