package net.sorukoma.quasium.smokelands.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmokeBlock extends LiquidBlock {
    public SmokeBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean surface = level.getFluidState(pos.above()).isEmpty();
        if (surface && context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
                && SmokeSea.holds(player) && !context.isDescending() && context.isAbove(Shapes.block(), pos, true)) {
            return Shapes.block();
        }
        return Shapes.empty();
    }
}
