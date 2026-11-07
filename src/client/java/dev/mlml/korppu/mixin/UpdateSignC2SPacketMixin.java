package dev.mlml.korppu.mixin;

import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.ColorSigns;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

@Mixin(ServerboundSignUpdatePacket.class)
public class UpdateSignC2SPacketMixin {
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    @Inject(method = "write", at = @At("HEAD"), require = 0)
    private void korppu$rewriteBeforeWrite(FriendlyByteBuf buf, CallbackInfo ci) {
        ColorSigns colorSigns = ModuleManager.getModule(ColorSigns.class);
        if (colorSigns == null || !colorSigns.isEnabled()) return;

        Object self = this;
        for (Field field : self.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(self);
                if (value == null) continue;

                if (value instanceof String s) {
                    field.set(self, rewrite(s));
                } else if (value.getClass().isArray() && value.getClass().getComponentType() == String.class) {
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        String line = (String) Array.get(value, i);
                        if (line != null) Array.set(value, i, rewrite(line));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static String rewrite(String in) {
        return AMP_CODE.matcher(in).replaceAll("\u00A7$1");
    }
}
