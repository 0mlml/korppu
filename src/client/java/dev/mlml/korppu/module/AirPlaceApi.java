package dev.mlml.korppu.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class AirPlaceApi {
    private AirPlaceApi() {}

    public static BlockHitResult findPlacementTarget(Minecraft mc) {
        if (mc.player == null) return null;
        return findPlacementTarget(mc, mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
    }

    public static BlockHitResult findPlacementTarget(Minecraft mc, double reach) {
        if (mc.player == null || mc.level == null) return null;

        HitResult hit = mc.player.pick(reach, 0, false);
        if (!(hit instanceof BlockHitResult bhr)) return null;

        BlockPos pos = bhr.getBlockPos();

        if (bhr.getType() == HitResult.Type.MISS) {
            if (!mc.level.getBlockState(pos).canBeReplaced()) return null;
            Vec3 look = mc.player.getViewVector(1.0f);
            Direction facing = Direction.getApproximateNearest((float) -look.x, (float) -look.y, (float) -look.z);
            return new BlockHitResult(pos.getCenter(), facing, pos, false);
        }

        return bhr;
    }
}
