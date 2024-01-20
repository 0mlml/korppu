package dev.mlml.korppu.event.events;

import dev.mlml.korppu.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

@AllArgsConstructor
public abstract class RenderEvent extends Event
{
    @Getter
    MatrixStack contextStack;

    public static class Entity extends RenderEvent
    {
        @Getter
        net.minecraft.entity.Entity entity;

        public Entity(MatrixStack stack, net.minecraft.entity.Entity e)
        {
            super(stack);
            this.entity = e;
        }
    }

    public static class BlockEntity extends RenderEvent
    {
        @Getter
        net.minecraft.block.entity.BlockEntity entity;

        public BlockEntity(MatrixStack stack, net.minecraft.block.entity.BlockEntity e)
        {
            super(stack);
            this.entity = e;
        }
    }

    @Getter
    public static class Block extends RenderEvent
    {
        BlockPos pos;
        BlockState state;

        public Block(MatrixStack stack, BlockPos bp, BlockState bs)
        {
            super(stack);
            this.pos = bp;
            this.state = bs;
        }
    }

    public static class World extends RenderEvent
    {

        public World(MatrixStack stack)
        {
            super(stack);
        }
    }
}