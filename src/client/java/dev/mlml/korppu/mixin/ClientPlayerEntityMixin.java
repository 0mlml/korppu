package dev.mlml.korppu.mixin;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.misc.Rotations;
import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Aimbot;
import dev.mlml.korppu.module.modules.Flight;
import dev.mlml.korppu.module.modules.Freecam;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = LocalPlayer.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void rotations$overrideBodyYaw(CallbackInfo ci) {
        if (!Rotations.isEnabled()) return;
        float yaw = Rotations.getClientYaw();
        ILivingEntityMixin acc = (ILivingEntityMixin)(Object)this;
        acc.setBodyYaw(yaw);
        acc.setLastBodyYaw(yaw);
        acc.setHeadYaw(yaw);
        acc.setLastHeadYaw(yaw);
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    public void preventPush(double x, double z, CallbackInfo ci) {
        Freecam fcm = ModuleManager.getModule(Freecam.class);
        Flight fm = ModuleManager.getModule(Flight.class);
        if ((!Objects.isNull(fcm) && fcm.isEnabled()) || (!Objects.isNull(fm) && fm.isEnabled() && fm.getMode().equals(Flight.Mode.Ability) && fm.getPhase().getValue())) {
            ci.cancel();
        }
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    float replaceMovementPacketYaw(LocalPlayer instance) {
        if (Rotations.isEnabled()) {
            return Rotations.getClientYaw();
        } else {
            return instance.getYRot();
        }
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    float replaceMovementPacketPitch(LocalPlayer instance) {
        if (Rotations.isEnabled()) {
            return Rotations.getClientPitch();
        } else {
            return instance.getXRot();
        }
    }

    @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean noSlow$suppressItemSlow(LocalPlayer instance) {
        Aimbot aimbot = ModuleManager.getModule(Aimbot.class);
        if (aimbot != null && aimbot.isNoSlow()) return false;
        return instance.isUsingItem();
    }
}