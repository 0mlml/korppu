package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"), require = 0)
    boolean hasStatusEffect_(LivingEntity ent, RegistryEntry<StatusEffect> effect) {
        if (ent.equals(KorppuMod.mc.player) && ModuleManager.getModule(Passives.class).getNoLevitation()
                .getValue() && effect.matches(StatusEffects.LEVITATION)) {
            return false;
        } else {
            return ent.hasStatusEffect(effect);
        }
    }
}