package dev.mlml.korppu.module.modules;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.config.BooleanSetting;
import dev.mlml.korppu.config.DoubleSetting;
import dev.mlml.korppu.config.ListSetting;
import dev.mlml.korppu.module.Module;
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
    final DoubleSetting range = config.add(new DoubleSetting("Range", "How far to see", 64d, 1d, 128d, 1));
    final BooleanSetting players = config.add(new BooleanSetting("Players", "See players", true));
    final BooleanSetting mobs = config.add(new BooleanSetting("Mobs", "See mobs", true));
    final BooleanSetting animals = config.add(new BooleanSetting("Animals", "See animals", true));
    final BooleanSetting items = config.add(new BooleanSetting("Items", "See items", true));
    final BooleanSetting others = config.add(new BooleanSetting("Others", "See others", true));
    final ListSetting<Mode> mode = config.add(new ListSetting<>("Mode", "How to render entities", Mode.BB));

    public WallHack() {
        super("WallHack", "See through walls", GLFW.GLFW_KEY_U);
    }

    private boolean checkShouldRender(Entity ent) {
        if (ent.getUuid().equals(KorppuMod.mc.player.getUuid())) {
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

        return others.getValue();
    }

    @Override
    public void onWorldRender(WorldRenderContext wrc) {
        if (KorppuMod.mc.world == null || KorppuMod.mc.player == null) {
            return;
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        wrc.matrixStack().push();

        for (Entity ent : KorppuMod.mc.world.getEntities()) {
            if (ent.squaredDistanceTo(KorppuMod.mc.player) > range.getValue() * range.getValue()) {
                continue;
            }

            if (!checkShouldRender(ent)) {
                continue;
            }

            switch (mode.getValue()) {
                case BB:

                    break;
                case Rect:

                    break;
            }
        }

        wrc.matrixStack().pop();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public enum Mode {
        BB, Rect
    }
}
