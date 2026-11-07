package dev.mlml.korppu.module;

import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.Config;
import dev.mlml.korppu.player.PlayerManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Settings shared by combat modules for entities that should never be targeted, regardless of the target filter.
 */
public class TargetProtection {
    private final BooleanSetting ignoreFriends;
    private final BooleanSetting ignoreNamed;
    private final BooleanSetting ignoreTamed;
    private final BooleanSetting ignoreNeutral;

    public TargetProtection(Config config) {
        ignoreFriends = config.add(new BooleanSetting("Ignore Friends", "Never target players marked as friends", true));
        ignoreNamed = config.add(new BooleanSetting("Ignore Named", "Never target mobs with a custom name", true));
        ignoreTamed = config.add(new BooleanSetting("Ignore Tamed", "Never target tamed animals (pets, horses)", true));
        ignoreNeutral = config.add(new BooleanSetting("Ignore Neutral", "Don't target neutral mobs (zombified piglins, endermen, ...) unless they are aggressive", true));
    }

    public boolean isProtected(Entity e) {
        if (e instanceof Player p) {
            return ignoreFriends.getValue() && PlayerManager.getInstance().isFriend(p.getUUID());
        }
        if (ignoreNamed.getValue() && e.hasCustomName()) return true;
        if (ignoreTamed.getValue() && (e instanceof TamableAnimal t && t.isTame() || e instanceof AbstractHorse h && h.isTamed())) {
            return true;
        }
        return ignoreNeutral.getValue() && e instanceof NeutralMob && e instanceof Mob m && !m.isAggressive();
    }
}
