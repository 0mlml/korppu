package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"),
            require = 0
    )
    private void overridePitchForAntiAim(LivingEntity entity, LivingEntityRenderState renderState, float tickDelta, CallbackInfo ci) {
        if (entity == KorppuMod.mc.player && Rotations.isEnabled()) {
            renderState.xRot = Rotations.getClientPitch();
        }

        Passives passives = ModuleManager.getModule(Passives.class);
        if (passives == null || !passives.isEnabled()) return;

        if (passives.getDinnerboneEntities().getValue()) {
            trySetBooleanField(renderState, true, "isUpsideDown", "upsideDown");
        }

        if (passives.getDeadmau5Ears().getValue()) {
            trySetBooleanField(renderState, true, "showExtraEars", "hasEars");
        }
    }

    private static void trySetBooleanField(Object obj, boolean value, String... names) {
        for (String name : names) {
            try {
                var field = obj.getClass().getField(name);
                if (field.getType() == boolean.class) {
                    field.setBoolean(obj, value);
                    return;
                }
            } catch (Throwable ignored) {
            }

            try {
                var field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                if (field.getType() == boolean.class) {
                    field.setBoolean(obj, value);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
