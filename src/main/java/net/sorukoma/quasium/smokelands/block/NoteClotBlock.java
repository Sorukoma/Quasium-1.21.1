package net.sorukoma.quasium.smokelands.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

public class NoteClotBlock extends Block {
    public static final int FULL = 4;
    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 1, FULL);
    public static final MapCodec<NoteClotBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoteType.CODEC.fieldOf("note").forGetter(NoteClotBlock::note), propertiesCodec()
    ).apply(instance, NoteClotBlock::new));
    private static final VoxelShape[] SHAPES = {box(5, 0, 5, 11, 6, 11), box(4, 0, 4, 12, 8, 12), box(3, 0, 3, 13, 10, 13),
            box(2, 0, 2, 14, 12, 14)};

    private final NoteType note;

    public NoteClotBlock(NoteType note, Properties properties) {
        super(properties);
        this.note = note;
        registerDefaultState(stateDefinition.any().setValue(CHARGES, FULL));
    }

    public NoteType note() {
        return note;
    }

    public static int mlPerBottle() {
        return SmokelandsConfig.CLOT_ML.get() / FULL;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CHARGES) - 1];
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) bottle(state, level, pos, player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public void bottle(BlockState state, Level level, BlockPos pos, Player player, ItemStack emptyBottle) {
        emptyBottle.consume(1, player);
        ItemStack essence = EssenceBottleItem.of(note.id(), mlPerBottle());
        if (!player.getInventory().add(essence)) player.drop(essence, false);
        int left = state.getValue(CHARGES) - 1;
        if (left == 0) {
            level.destroyBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, state.setValue(CHARGES, left));
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 0.8f + left * 0.1f);
        if (player instanceof ServerPlayer serverPlayer) SmokecraftTrigger.fire(serverPlayer, "clot_bottled");
    }
}
