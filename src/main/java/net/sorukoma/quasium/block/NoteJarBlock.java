package net.sorukoma.quasium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

public class NoteJarBlock extends BaseEntityBlock {
    public static final MapCodec<NoteJarBlock> CODEC = simpleCodec(NoteJarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 13, 12), Block.box(5, 13, 5, 11, 15, 11));

    public NoteJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NoteJarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof NoteJarBlockEntity jar)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        NoteType bottleNote = NoteType.byId(EssenceBottleItem.typeOf(stack));
        if (bottleNote != null) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            int poured = jar.pour(bottleNote, EssenceBottleItem.amountOf(stack));
            if (poured <= 0) return ItemInteractionResult.CONSUME;
            int left = EssenceBottleItem.amountOf(stack) - poured;
            if (left > 0) {
                stack.set(ModDataComponents.ESSENCE_AMOUNT.get(), left);
            } else {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            }
            clink(level, pos, 0.9f);
            return ItemInteractionResult.CONSUME;
        }
        if (stack.is(Items.GLASS_BOTTLE) && jar.note() != null) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            NoteType note = jar.note();
            int taken = jar.take(Config.NOTE_BOTTLE_CAPACITY.getAsInt());
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, EssenceBottleItem.of(note.id(), taken)));
            clink(level, pos, 1.2f);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NoteJarBlockEntity jar) {
            player.displayClientMessage(describe(jar), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component describe(NoteJarBlockEntity jar) {
        if (jar.note() == null) return Component.translatable("block.quasium.note_jar.empty");
        return Component.translatable("block.quasium.note_jar.contents", jar.note().displayName(), jar.amount(),
                NoteJarBlockEntity.capacity());
    }

    private static void clink(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, ModSoundEvents.NOTE_JAR_CLINK.get(), SoundSource.BLOCKS, 0.8f, pitch);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof NoteJarBlockEntity jar ? Math.round(jar.fill() * 15f) : 0;
    }
}
