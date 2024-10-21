package dev.mlml.korppu.module.modules;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.module.Module;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class WallHack extends Module {
    private final DoubleSetting range = config.add(new DoubleSetting("Range", "How far to see", 64d, 1d, 128d, 1));
    private final BooleanSetting players = config.add(new BooleanSetting("Players", "See players", true));
    private final BooleanSetting mobs = config.add(new BooleanSetting("Mobs", "See mobs", true));
    private final BooleanSetting animals = config.add(new BooleanSetting("Animals", "See animals", true));
    private final BooleanSetting items = config.add(new BooleanSetting("Items", "See items", true));
    private final BooleanSetting others = config.add(new BooleanSetting("Others", "See others", true));
    @Getter
    private final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "How to render entities", Mode.Glowing));

    public WallHack() {
        super("WallHack", "See through walls", GLFW.GLFW_KEY_U);
    }

    public boolean checkShouldRender(Entity ent) {
        if (KorppuMod.mc.player == null) {
            return false;
        }

        if (KorppuMod.mc.player.getPos().distanceTo(ent.getPos()) > range.getValue()) {
            return false;
        }

        if (players.getValue() && ent instanceof PlayerEntity) {
            return true;
        }

        if (mobs.getValue() && ent instanceof Monster) {
            return true;
        }

        if (animals.getValue() && ent instanceof PassiveEntity) {
            return true;
        }

        if (items.getValue() && ent instanceof ItemEntity) {
            return true;
        }

        return others.getValue() && !(ent instanceof PlayerEntity) && !(ent instanceof Monster) && !(ent instanceof PassiveEntity) && !(ent instanceof ItemEntity);
    }

    @Override
    public void onWorldRender(WorldRenderContext wrc) {

    }

    public enum Mode {
        Glowing
    }
}
