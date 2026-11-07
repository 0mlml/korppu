package dev.mlml.korppu.config;

import dev.mlml.korppu.KorppuMod;
import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mirrors the in-memory config to korppu.live.txt during a session. The file is deleted on a clean
 * shutdown, so finding it on launch means the previous session ended without saving korppu.txt.
 */
public final class LiveConfig {
    public static final Path LIVE_FILE = Path.of("korppu.live.txt");
    private static final int WRITE_INTERVAL_TICKS = 20;

    public record Change(String key, String saved, String session) {
    }

    @Getter
    private static String recovered;
    private static String lastWritten;
    private static int ticks;

    private LiveConfig() {
    }

    /**
     * Must run after korppu.txt has been loaded. Returns the differences between the loaded config and the
     * leftover session file, or null if the previous shutdown was clean.
     */
    public static List<Change> init() {
        List<Change> changes = null;
        if (Files.exists(LIVE_FILE)) {
            try {
                recovered = Files.readString(LIVE_FILE, StandardCharsets.UTF_8);
                changes = diff(ConfigWriter.generateConfig(), recovered);
                KorppuMod.LOGGER.warn("Previous session did not shut down cleanly ({} setting(s) differ)", changes.size());
            } catch (IOException e) {
                KorppuMod.LOGGER.error("Failed to read {}:", LIVE_FILE, e);
            }
        }
        write();
        return changes;
    }

    public static void tick() {
        if (++ticks < WRITE_INTERVAL_TICKS) return;
        ticks = 0;
        write();
    }

    public static void restoreRecovered() {
        if (recovered == null) return;
        ConfigWriter.deserializeConfig(recovered);
        write();
    }

    public static void onCleanShutdown() {
        try {
            Files.deleteIfExists(LIVE_FILE);
        } catch (IOException e) {
            KorppuMod.LOGGER.error("Failed to delete {}:", LIVE_FILE, e);
        }
    }

    private static void write() {
        String config = ConfigWriter.generateConfig();
        if (config.equals(lastWritten)) return;
        try {
            ConfigWriter.writeAtomically(LIVE_FILE, config);
            lastWritten = config;
        } catch (IOException e) {
            KorppuMod.LOGGER.error("Failed to write {}:", LIVE_FILE, e);
        }
    }

    private static List<Change> diff(String saved, String session) {
        Map<String, String> before = ConfigWriter.flatten(saved);
        Map<String, String> after = ConfigWriter.flatten(session);

        Set<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());

        List<Change> changes = new ArrayList<>();
        for (String key : keys) {
            String a = before.get(key);
            String b = after.get(key);
            if (!Objects.equals(a, b)) {
                changes.add(new Change(key, a, b));
            }
        }
        return changes;
    }
}
