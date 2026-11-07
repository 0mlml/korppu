package dev.mlml.korppu.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CommandSuggestions.class)
public interface ICommandSuggestorMixin {
    @Invoker("formatChat")
    FormattedCharSequence invokeProvideRenderText(String original, int index);
}
