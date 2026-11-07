package dev.mlml.korppu.player;

import java.util.UUID;

public class PlayerRecord {
    private final UUID uuid;
    private String lastKnownName;
    private PlayerRelation relation;
    private String notes;

    public PlayerRecord(UUID uuid, String lastKnownName, PlayerRelation relation, String notes) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.relation = relation;
        this.notes = notes;
    }

    public UUID getUuid() { return uuid; }
    public String getLastKnownName() { return lastKnownName; }
    public PlayerRelation getRelation() { return relation; }
    public String getNotes() { return notes; }

    public void setLastKnownName(String lastKnownName) { this.lastKnownName = lastKnownName; }
    public void setRelation(PlayerRelation relation) { this.relation = relation; }
    public void setNotes(String notes) { this.notes = notes; }

    public String serialize() {
        return uuid.toString() + "|" + lastKnownName + "|" + relation.name() + "|" + notes.replace("|", "\\|");
    }

    public static PlayerRecord deserialize(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 3) return null;
        try {
            UUID uuid = UUID.fromString(parts[0]);
            String name = parts[1];
            PlayerRelation relation = PlayerRelation.valueOf(parts[2]);
            String notes = parts.length >= 4 ? parts[3].replace("\\|", "|") : "";
            return new PlayerRecord(uuid, name, relation, notes);
        } catch (Exception e) {
            return null;
        }
    }
}
