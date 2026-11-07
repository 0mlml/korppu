package dev.mlml.korppu.mixin;

import dev.mlml.korppu.module.ModuleManager;
import dev.mlml.korppu.module.modules.Passives;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WebBlock.class)
public abstract class StructureVoidBlockMixin extends Block {
    public StructureVoidBlockMixin(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return isStructureVoidEnabled() ? RenderShape.MODEL : super.getRenderShape(state);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
        return !isStructureVoidEnabled();
    }

    private static boolean isStructureVoidEnabled() {
        Passives passives = ModuleManager.getModule(Passives.class);
        return passives != null && passives.isEnabled() && passives.getRenderStructureVoid().getValue();
    }
}
