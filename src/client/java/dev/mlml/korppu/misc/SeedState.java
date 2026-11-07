package dev.mlml.korppu.misc;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.SeedChangedEvent;

public final class SeedState {
    private static Long seed;

    private SeedState() {
    }

    public static Long getSeed() {
        return seed;
    }

    public static void setSeed(Long newSeed, String source) {
        if (seed == null ? newSeed == null : seed.equals(newSeed)) {
            return;
        }

        Long previous = seed;
        seed = newSeed;
        KorppuMod.eventManager.trigger(new SeedChangedEvent(previous, newSeed, source));
    }
}
