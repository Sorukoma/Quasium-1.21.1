package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.sorukoma.quasium.sound.ModSoundEvents;

public class SmokeValveBlock extends SmokePipeBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<SmokeValveBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PipeTier.CODEC.fieldOf("tier").forGetter(SmokeValveBlock::tier), propertiesCodec()).apply(instance, SmokeValveBlock::new));

    public SmokeValveBlock(PipeTier tier, Properties properties) {
        super(tier, box(3, 11, 3, 13, 12, 13), properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends SmokeValveBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public boolean conducts(BlockState state) {
        return !state.getValue(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED) || !(level instanceof ServerLevel serverLevel)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSoundEvents.VALVE_CLICK.get(), SoundSource.BLOCKS, 0.6f, powered ? 0.8f : 1.1f);
        DraftNetworks.invalidate(serverLevel, pos);
    }
}
