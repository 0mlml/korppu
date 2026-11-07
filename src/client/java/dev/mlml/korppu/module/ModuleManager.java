package dev.mlml.korppu.module;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.event.GamemodeTracker;
import dev.mlml.korppu.event.Listener;
import dev.mlml.korppu.event.events.HudRenderEvent;
import dev.mlml.korppu.event.events.WorldRenderEvent;
import dev.mlml.korppu.imgui.ImGuiManager;
import dev.mlml.korppu.imgui.KorppuImGui;
import dev.mlml.korppu.module.modules.*;
import lombok.Getter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    @Getter
    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        KorppuImGui.init();

        add(Category.RENDER, new HeadsUpDisplay());
        add(Category.MOVEMENT, new Flight());
        add(Category.MOVEMENT, new EntityFly());
        add(Category.MOVEMENT, new Speed());
        add(Category.MOVEMENT, new NoFall());
        add(Category.PLAYER, new AntiHunger());
        add(Category.MOVEMENT, new EdgeJump());
        add(Category.COMBAT, new Backtrack());
        add(Category.RENDER, new Freecam());
        add(Category.PLAYER, new FastMine());
        add(Category.PLAYER, new PingSpoof());
        add(Category.COMBAT, new AttackManip());
        add(Category.COMBAT, new AttributeSwapper());
        add(Category.COMBAT, new KillAura());
        add(Category.RENDER, new WallHack());
        add(Category.RENDER, new BlockESP());
        add(Category.PLAYER, new Passives());
        add(Category.CLIENT, new Meta());
        add(Category.CLIENT, new Notifier());
        add(Category.CLIENT, new AntiCrash());
        add(Category.COMBAT, new AntiBot());
        add(Category.CLIENT, new SilentDisconnect());
        add(Category.CLIENT, new GhostMode());
        add(Category.MOVEMENT, new ExtraElytra());
        add(Category.PLAYER, new AutoExtinguish());
        add(Category.PLAYER, new ChestAura());
        add(Category.COMBAT, new VehicleOneHit());
        add(Category.RENDER, new SoundLocator());
        add(Category.RENDER, new ChorusExploit());
        add(Category.PLAYER, new ColorSigns());
        add(Category.PLAYER, new AirPlace());
        add(Category.PLAYER, new AntiAim());
        add(Category.COMBAT, new Aimbot());
        add(Category.COMBAT, new VelocitySpoof());
        add(Category.RENDER, new EntityInfo());
        add(Category.MOVEMENT, new Squake());

        add(Category.PLAYER, new AutoDrop());
        add(Category.PLAYER, new SurvivalQoL());
        add(Category.CLIENT, new PacketDelay());
        add(Category.MOVEMENT, new InfiniteElytra());
        add(Category.PLAYER, new InfiniteTools());
        add(Category.RENDER, new MobGearESP());
        add(Category.RENDER, new AdvancedItemESP());
        add(Category.COMBAT, new KnockBackModifier());
        add(Category.COMBAT, new KnockbackPlus());
        add(Category.MOVEMENT, new PortalGodMode());
        add(Category.CLIENT, new AutoSpam());
        add(Category.EXPLOIT, new BungeeSpoofer());
        add(Category.MOVEMENT, new BoatNoclip());
        add(Category.EXPLOIT, new SuperInstaMine());
        add(Category.EXPLOIT, new LecternCrash());
        add(Category.PLAYER, new MultiUse());
        add(Category.MOVEMENT, new TPFly());
        add(Category.MOVEMENT, new Glide());
        add(Category.PLAYER, new InfiniteReach());
        add(Category.RENDER, new BaseHunter());
        add(Category.PLAYER, new TreeAura());
        add(Category.PLAYER, new Painter());
        add(Category.PLAYER, new AutoTnt());
        add(Category.PLAYER, new AutoWither());
        add(Category.PLAYER, new Lavacast());
        add(Category.COMBAT, new LavaAura());
        add(Category.PLAYER, new ObsidianFarm());
        add(Category.RENDER, new HoleAndTunnelESP());
        add(Category.RENDER, new WaypointCoordExploit());
        add(Category.RENDER, new OreSim());
        add(Category.EXPLOIT, new ShulkerDupe());
        add(Category.EXPLOIT, new InvDupe());
        add(Category.EXPLOIT, new InvDupeModule());
        add(Category.EXPLOIT, new OPServerKill());
        add(Category.EXPLOIT, new OPServerKillModule());
        add(Category.EXPLOIT, new ForceOPBook());
        add(Category.EXPLOIT, new ForceOPSign());
        add(Category.COMBAT, new HandOfGod());
        add(Category.CLIENT, new InteractionMenu());
        add(Category.PLAYER, new RemoteEnderChest());

        for (Module m : modules) {
            KeyMappingHelper.registerKeyMapping(m.getKeybind());
            ClientTickEvents.START_CLIENT_TICK.register(m::update);
            ClientTickEvents.END_CLIENT_TICK.register(m::afterUpdate);
        }

        ClientTickEvents.START_CLIENT_TICK.register(mc -> KorppuImGui.tick());
        ClientTickEvents.START_CLIENT_TICK.register(mc -> GamemodeTracker.tick());

        KorppuMod.eventManager.register(ModuleManager.class);

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ImGuiManager.dispose());

        KorppuMod.LOGGER.info("Initialized {} modules", modules.size());
    }

    @Listener
    public static void onWorldRender(WorldRenderEvent event) {
        EspManager.beginFrame();
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onWorldRender(event.getMatrixStack(), event.getTickDelta());
            }
        }
        EspManager.render(event.getMatrixStack());
    }

    public static void renderWorld(PoseStack matrices, float tickDelta) {
        onWorldRender(new WorldRenderEvent(matrices, tickDelta));
    }

    public static void renderHud(GuiGraphicsExtractor context, float tickDelta) {
        KorppuMod.eventManager.trigger(new HudRenderEvent(context, tickDelta));
    }

    private static void add(Category category, Module module) {
        module.setCategory(category);
        modules.add(module);
    }

    public static boolean isSendPackets() {
        Passives passives = getModule(Passives.class);
        return passives != null && passives.getSendPackets().getValue();
    }

    public static String getCommandPrefix() {
        Meta meta = getModule(Meta.class);
        return meta != null ? meta.getPrefix().getValue() : ";";
    }

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
