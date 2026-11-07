package dev.mlml.korppu.misc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;

public class KorppuMath {
    public static boolean hasLOS(Vec3 start, Vec3 end, Level world, Entity requester) {
        ClipContext r = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, requester);
        BlockHitResult hit = world.clip(r);
        return hit.getLocation().equals(end);
    }
}
