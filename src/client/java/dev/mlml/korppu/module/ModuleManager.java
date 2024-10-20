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

public class ModuleManager {
    @Getter
    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.add(new HeadsUpDisplay());
        modules.add(new Flight());
        modules.add(new Speed());
//        modules.add(new AntiAim());
        modules.add(new OnlineProtections());
        modules.add(new NoFall());
        modules.add(new EdgeJump());
        modules.add(new Backtrack());
        modules.add(new Freecam());
        modules.add(new FastMine());
        modules.add(new PingSpoof());
        modules.add(new LongJump());
        modules.add(new Passives());
        modules.add(new Meta());
        modules.add(new WallHack());
        modules.add(new KillAura());
        modules.add(new AttackManip());
        modules.add(new TPRange());
//        modules.add(new Replanter());
        modules.add(new Logger());
        modules.add(new AtoB());

        for (Module m : modules) {
            KeyBindingHelper.registerKeyBinding(m.getKeybind());
            ClientTickEvents.END_CLIENT_TICK.register(m::update);
            HudRenderCallback.EVENT.register(m::renderDraw);
            WorldRenderEvents.LAST.register(m::worldDraw);
        }

        KorppuMod.LOGGER.info("Initialized " + modules.size() + " modules");
    }

    public static boolean doNotSendPackets() {
        Meta meta = (Meta) getModule(Meta.class);
        return meta == null || !meta.getSendPackets().getValue();
    }

    public static String getCommandPrefix() {
        Meta meta = (Meta) getModule(Meta.class);
        return meta != null ? meta.getPrefix().getValue() : ";";
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (module.getClass().equals(moduleClass)) {
                return (T) module;
            }
        }
        return null;
    }

    public static Module getModuleByStringIgnoreCase(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static Module getModuleByString(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }
}
