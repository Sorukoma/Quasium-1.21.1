package net.sorukoma.quasium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Supplier;

public class TobaccoSeedsItem extends Item {

    private final Supplier<net.minecraft.world.level.block.Block> cropBlock;

    public TobaccoSeedsItem(Supplier<net.minecraft.world.level.block.Block> cropBlock, Properties properties) {
        super(properties);
        this.cropBlock = cropBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.FARMLAND)) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.isAir() || aboveState.canBeReplaced()) {
                level.setBlockAndUpdate(abovePos, cropBlock.get().defaultBlockState());
                if (level instanceof ServerLevel) {
                    level.gameEvent(GameEvent.BLOCK_CHANGE, abovePos,
                            net.minecraft.world.level.gameevent.GameEvent.Context.of(context.getPlayer(), cropBlock.get().defaultBlockState()));
                }
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
}