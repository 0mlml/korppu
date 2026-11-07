package dev.mlml.korppu.imgui;

import net.minecraft.world.entity.LivingEntity;

public class HpTracker {
    private float lastHp = -1f;
    private float hpDelta = 0f;
    private long snapshotTime = 0;

    public void reset() {
        lastHp = -1f;
        hpDelta = 0f;
        snapshotTime = System.currentTimeMillis();
    }

    public void update(LivingEntity le) {
        float hp = le.getHealth();
        long now = System.currentTimeMillis();
        if (lastHp < 0) {
            lastHp = hp;
            snapshotTime = now;
        } else if (now - snapshotTime >= 1000) {
            hpDelta = hp - lastHp;
            lastHp = hp;
            snapshotTime = now;
        }
    }

    public float getDelta() { return hpDelta; }
}
