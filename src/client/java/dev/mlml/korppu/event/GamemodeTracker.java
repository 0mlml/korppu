package dev.mlml.korppu.event;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.PlayerGamemodeChangeEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;

import java.util.*;

/**
 * Always-active service that polls the player list each tick and fires
 * {@link PlayerGamemodeChangeEvent} whenever a player's game mode changes.
 *
 * <p>Because this runs unconditionally (not as a module), any module or
 * other listener can react to {@code PlayerGamemodeChangeEvent} regardless
 * of whether the {@link dev.mlml.korppu.module.modules.Notifier} module is
 * enabled.
 *
 * <p>Register via {@code ClientTickEvents.START_CLIENT_TICK} during
 * {@link dev.mlml.korppu.module.ModuleManager#init()}.
 */
public final class GamemodeTracker {
    private static final Map<UUID, GameType> snapshot = new HashMap<>();

    private GamemodeTracker() {}

    public static void tick() {
        ClientPacketListener handler = KorppuMod.mc.getConnection();
        if (handler == null) {
            snapshot.clear();
            return;
        }

        Collection<PlayerInfo> entries = handler.getListedOnlinePlayers();
        Set<UUID> currentIds = new HashSet<>();

        for (PlayerInfo entry : entries) {
            UUID id = entry.getProfile().id();
            currentIds.add(id);

            GameType current = entry.getGameMode();
            if (current == null) continue;

            GameType previous = snapshot.put(id, current);
            if (previous != null && !previous.equals(current)) {
                String name = entry.getProfile().name();
                KorppuMod.eventManager.trigger(
                        new PlayerGamemodeChangeEvent(name, id, previous, current));
            }
        }

        snapshot.keySet().retainAll(currentIds);
    }
}
