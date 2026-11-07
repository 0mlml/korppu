package dev.mlml.korppu.player;

import dev.mlml.korppu.KorppuMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton player management system. Tracks UUIDs with friend/neutral/enemy relations.
 * Persisted to the Minecraft run directory as korppu_players.txt.
 */
public class PlayerManager {
    private static final String DATA_FILE = "korppu_players.txt";
    private static PlayerManager instance;

    private final Map<UUID, PlayerRecord> records = new ConcurrentHashMap<>();

    private PlayerManager() {}

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    public void add(UUID uuid, String name, PlayerRelation relation) {
        records.put(uuid, new PlayerRecord(uuid, name, relation, ""));
    }

    public void add(UUID uuid, String name, PlayerRelation relation, String notes) {
        records.put(uuid, new PlayerRecord(uuid, name, relation, notes));
    }

    public void remove(UUID uuid) {
        records.remove(uuid);
    }

    public PlayerRecord get(UUID uuid) {
        return records.get(uuid);
    }

    public PlayerRelation getRelation(UUID uuid) {
        PlayerRecord r = records.get(uuid);
        return r != null ? r.getRelation() : PlayerRelation.NEUTRAL;
    }

    public boolean isFriend(UUID uuid) {
        return getRelation(uuid) == PlayerRelation.FRIEND;
    }

    public boolean isEnemy(UUID uuid) {
        return getRelation(uuid) == PlayerRelation.ENEMY;
    }

    public Collection<PlayerRecord> getAll() {
        return Collections.unmodifiableCollection(records.values());
    }

    public void updateRelation(UUID uuid, PlayerRelation relation) {
        PlayerRecord r = records.get(uuid);
        if (r != null) r.setRelation(relation);
    }

    public void updateNotes(UUID uuid, String notes) {
        PlayerRecord r = records.get(uuid);
        if (r != null) r.setNotes(notes);
    }

    public void save() {
        Path path = Path.of(DATA_FILE);
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (PlayerRecord r : records.values()) {
                w.write(r.serialize());
                w.newLine();
            }
        } catch (IOException e) {
            KorppuMod.LOGGER.error("Failed to save player manager data: {}", e.getMessage());
        }
    }

    public void load() {
        Path path = Path.of(DATA_FILE);
        if (!Files.exists(path)) return;
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                PlayerRecord record = PlayerRecord.deserialize(line);
                if (record != null) {
                    records.put(record.getUuid(), record);
                }
            }
        } catch (IOException e) {
            KorppuMod.LOGGER.error("Failed to load player manager data: {}", e.getMessage());
        }
    }
}
