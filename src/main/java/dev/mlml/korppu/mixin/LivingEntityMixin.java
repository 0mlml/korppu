package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "net/minecraft/entity/LivingEntity.hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"), require = 0)
    boolean hasStatusEffect_(LivingEntity ent, StatusEffect effect) {
        if (ent.equals(KorppuMod.mc.player) && ((Passives) Objects.requireNonNull(ModuleManager.getModule(Passives.class))).getNoLevitation()
                                                                                                                           .getValue() && effect == StatusEffects.LEVITATION) {
            return false;
        } else {
            return ent.hasStatusEffect(effect);
        }
    }
}
