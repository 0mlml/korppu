package dev.mlml.korppu.mixin;

import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Notifier;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public class ChatInputSuggestorMixin {
    @Inject(method = "refresh", at = @At("TAIL"), require = 0)
    private void korppu$onRefresh(CallbackInfo ci) {
        Notifier notifier = ModuleManager.getModule(Notifier.class);
        if (notifier == null) return;

        List<String> names = readSuggestionNames((Object) this);
        if (!names.isEmpty()) {
            notifier.ingestCommandSuggestions(names);
        }
    }

    private static List<String> readSuggestionNames(Object suggestor) {
        try {
            Class<?> c = suggestor.getClass();
            while (c != null) {
                for (var field : c.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(suggestor);
                    if (value == null) continue;

                    Object suggestions = null;
                    if (value instanceof CompletableFuture<?> future) {
                        if (!future.isDone()) continue;
                        suggestions = future.getNow(null);
                    } else if (value.getClass().getSimpleName().contains("Suggestions")) {
                        suggestions = value;
                    }

                    if (suggestions == null) continue;

                    List<String> names = extractTextsFromSuggestions(suggestions);
                    if (!names.isEmpty()) return names;
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        return List.of();
    }

    private static List<String> extractTextsFromSuggestions(Object suggestions) {
        try {
            var getList = suggestions.getClass().getMethod("getList");
            Object listObj = getList.invoke(suggestions);
            if (!(listObj instanceof List<?> list)) return List.of();

            List<String> out = new ArrayList<>();
            for (Object suggestion : list) {
                try {
                    var getText = suggestion.getClass().getMethod("getText");
                    Object text = getText.invoke(suggestion);
                    if (text instanceof String s) out.add(s);
                } catch (Throwable ignored) {
                }
            }
            return out;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
