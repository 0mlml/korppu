package dev.mlml.korppu.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class KorppuMath {
    public static boolean hasLOS(Vec3d start, Vec3d end, World world, Entity requester) {
        RaycastContext r = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, requester);
        BlockHitResult hit = world.raycast(r);
        return hit.getPos().equals(end);
    }
}
