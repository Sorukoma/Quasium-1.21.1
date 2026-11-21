package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

public class SmokeFilterBlock extends SmokePipeBlock {
    public static final EnumProperty<FilterNote> FILTER = EnumProperty.create("note", FilterNote.class);
    public static final MapCodec<SmokeFilterBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PipeTier.CODEC.fieldOf("tier").forGetter(SmokeFilterBlock::tier), propertiesCodec()).apply(instance, SmokeFilterBlock::new));

    public SmokeFilterBlock(PipeTier tier, Properties properties) {
        super(tier, box(4.5, 6.5, 4.5, 11.5, 9.5, 11.5), properties);
        registerDefaultState(defaultBlockState().setValue(FILTER, FilterNote.NONE));
    }

    @Override
    protected MapCodec<? extends SmokeFilterBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FILTER);
    }

    @Nullable
    @Override
    public NoteType filterNote(BlockState state) {
        return state.getValue(FILTER).note();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        NoteType note = NoteType.byId(EssenceBottleItem.typeOf(stack));
        if (note == null) return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        setFilter(state, level, pos, player, FilterNote.of(note));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        setFilter(state, level, pos, player, state.getValue(FILTER).next());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void setFilter(BlockState state, Level level, BlockPos pos, Player player, FilterNote filter) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        level.setBlock(pos, state.setValue(FILTER, filter), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
        player.displayClientMessage(filter.describe(), true);
        DraftNetworks.invalidate(serverLevel, pos);
    }
}
