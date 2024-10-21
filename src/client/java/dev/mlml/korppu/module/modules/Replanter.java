package dev.mlml.korppu.module.modules;

import dev.mlml.korppu.KorppuMod;
import dev.mlml.korppu.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import org.lwjgl.glfw.GLFW;

public class Replanter extends Module {
    public Replanter() {
        super("Replanter", "Automatically replants crops", GLFW.GLFW_KEY_KP_2);
    }

    private record NeedToReplant(BlockState bs) {}

    @Override
    public void onTick() {
        if (KorppuMod.mc.world == null || KorppuMod.mc.player == null) {
            return;
        }

        BlockState bs = KorppuMod.mc.world.getBlockState(KorppuMod.mc.player.getBlockPos());
//        bs.get
        Block block = bs.getBlock();
        if (block == null) {
            return;
        }

        if (block instanceof CropBlock) {
            CropBlock crop = (CropBlock) block;
            if (crop.isMature(KorppuMod.mc.world.getBlockState(KorppuMod.mc.player.getBlockPos()))) {
            }
        }
    }
}
