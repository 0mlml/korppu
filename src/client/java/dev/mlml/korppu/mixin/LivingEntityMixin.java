package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.events.EntityDeathEvent;
import dev.mlml.korppu.event.events.ItemPickupEvent;
import dev.mlml.korppu.event.events.KilledEntityEvent;
import dev.mlml.korppu.event.events.StopUsingItemEvent;
import net.minecraft.world.damagesource.DamageSource;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.GhostMode;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(at = @At("HEAD"), method = "getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F", cancellable = true)
    private void onGetEffectFadeFactor(Holder<MobEffect> effect, float tickProgress, CallbackInfoReturnable<Float> cir) {
        Passives pm = ModuleManager.getModule(Passives.class);
        if (pm != null && pm.isEnabled()) {
            if (pm.getNoBlind().getValue() && (effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.DARKNESS))) {
                cir.setReturnValue(0f);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "hasEffect(Lnet/minecraft/core/Holder;)Z", cancellable = true)
    private void onHasStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        Passives pm = ModuleManager.getModule(Passives.class);
        if (pm != null && pm.isEnabled()) {
            if (pm.getNoLevitation().getValue() && effect.is(MobEffects.LEVITATION)) {
                cir.setReturnValue(false);
            }
            if (pm.getNoSlow().getValue() && effect.is(MobEffects.SLOWNESS)) {
                cir.setReturnValue(false);
            }
            if (pm.getNoBlind().getValue() && (effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.DARKNESS))) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDeath_(DamageSource source, CallbackInfo ci) {
        KorppuMod.eventManager.trigger(new EntityDeathEvent((LivingEntity) (Object) this, source));
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == KorppuMod.mc.player) {
            GhostMode ghostMode = ModuleManager.getModule(GhostMode.class);
            if (ghostMode != null && ghostMode.isEnabled()) {
                ghostMode.markGhostActive();
                ci.cancel();
            }
        }
    }

    @Inject(method = "onKilledBy", at = @At("HEAD"), require = 0)
    void onKilledBy_(LivingEntity adversary, CallbackInfo ci) {
        KilledEntityEvent kee = new KilledEntityEvent((LivingEntity) (Object) this, adversary);
        KorppuMod.eventManager.trigger(kee);
    }

    @Inject(method = "onItemPickup", at = @At("HEAD"), cancellable = true)
    void sendPickup_(ItemEntity item, CallbackInfo ci) {
        ItemPickupEvent ipe = new ItemPickupEvent((LivingEntity) (Object) this, item, item.getItem().getCount());
        KorppuMod.eventManager.trigger(ipe);
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"), require = 0)
    private void onStopUsingItem(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == KorppuMod.mc.player) {
            ItemStack stack = self.getActiveItem();
            KorppuMod.eventManager.trigger(new StopUsingItemEvent(self, stack));
        }
    }
}
