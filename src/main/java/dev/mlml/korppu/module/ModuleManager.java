package dev.mlml.korppu.module;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.modules.*;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager
{
    @Getter
    private static final List<Module> modules = new ArrayList<>();

    public static void init()
    {
        modules.add(new HeadsUpDisplay());
        modules.add(new Flight());
        modules.add(new OnlineProtections());
        modules.add(new NoFall());
        modules.add(new EdgeJump());
        modules.add(new Backtrack());

        for (Module m : modules)
        {
            KeyBindingHelper.registerKeyBinding(m.getKeybind());
            ClientTickEvents.END_CLIENT_TICK.register(m::update);
            HudRenderCallback.EVENT.register(m::onRender);
            WorldRenderEvents.LAST.register(m::onWorldRender);
        }

        KorppuMod.LOGGER.info("Initialized " + modules.size() + " modules");
    }

    public static Module getModule(Class<? extends Module> moduleClass)
    {
        for (Module module : modules)
        {
            if (module.getClass().equals(moduleClass))
            {
                return module;
            }
        }
        return null;
    }
}
