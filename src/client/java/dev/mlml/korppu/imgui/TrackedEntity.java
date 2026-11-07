package dev.mlml.korppu.imgui;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class TrackedEntity {
    public final UUID uuid;
    public String lastKnownName;
    public String lastKnownType;
    public int lastKnownEntityId = -1;
    public float lastKnownHp = -1f;
    public float lastKnownMaxHp = -1f;
    public boolean lastKnownDead = false;
    public double lastKnownX;
    public double lastKnownY;
    public double lastKnownZ;
    public double lastKnownVx;
    public double lastKnownVy;
    public double lastKnownVz;
    public float lastKnownYaw;
    public float lastKnownPitch;
    public long lastSeen = 0;
    public Entity liveEntity = null;
    public boolean wasOnline = false;
    public final HpTracker hpTracker = new HpTracker();

    public TrackedEntity(Entity entity) {
        this.uuid = entity.getUUID();
        updateFrom(entity);
        this.wasOnline = true;
    }

    public TrackedEntity(UUID uuid, String name) {
        this.uuid = uuid;
        this.lastKnownName = name;
        this.lastKnownType = "player";
    }

    public void updateFrom(Entity entity) {
        this.liveEntity = entity;
        this.lastSeen = System.currentTimeMillis();
        this.lastKnownEntityId = entity.getId();
        this.lastKnownX = entity.getX();
        this.lastKnownY = entity.getY();
        this.lastKnownZ = entity.getZ();
        this.lastKnownVx = entity.getDeltaMovement().x;
        this.lastKnownVy = entity.getDeltaMovement().y;
        this.lastKnownVz = entity.getDeltaMovement().z;
        this.lastKnownYaw = entity.getYRot();
        this.lastKnownPitch = entity.getXRot();
        if (entity.hasCustomName() && entity.getCustomName() != null) {
            lastKnownName = entity.getCustomName().getString();
        } else if (entity instanceof Player p) {
            lastKnownName = p.getDisplayName().getString();
        } else {
            lastKnownName = entity.getType().getDescription().getString();
        }
        lastKnownType = entity.getType().getDescription().getString();
        if (entity instanceof LivingEntity le) {
            lastKnownHp = le.getHealth();
            lastKnownMaxHp = le.getMaxHealth();
            lastKnownDead = !le.isAlive();
            hpTracker.update(le);
        } else {
            lastKnownDead = false;
        }
    }

    public boolean isOnline() {
        return liveEntity != null && !liveEntity.isRemoved();
    }

    public String getDisplayName() {
        return lastKnownName != null ? lastKnownName : uuid.toString().substring(0, 8);
    }
}
