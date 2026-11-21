package net.sorukoma.quasium.ritual.artifact;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.ritual.RitualConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DreamCatcherBlock extends BaseEntityBlock {
    public static final MapCodec<DreamCatcherBlock> CODEC = simpleCodec(DreamCatcherBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

    public DreamCatcherBlock(Properties properties) {
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
        return new DreamCatcherBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.GLASS_BOTTLE) || !(level.getBlockEntity(pos) instanceof DreamCatcherBlockEntity catcher) || catcher.ml() <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        int taken = catcher.take(Config.NOTE_BOTTLE_CAPACITY.getAsInt());
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, EssenceBottleItem.of(DreamCatcherBlockEntity.ESSENCE, taken)));
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.4f);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DreamCatcherBlockEntity catcher) {
            player.displayClientMessage(Component.translatable("block.quasium.dream_catcher.contents", catcher.ml(),
                    RitualConfig.DREAM_CATCHER_CAP_ML.getAsInt()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int catchNight(ServerLevel level, List<? extends ServerPlayer> sleepers) {
        int caught = 0;
        int radius = RitualConfig.DREAM_CATCHER_RADIUS.getAsInt();
        for (ServerPlayer sleeper : sleepers) {
            if (!sleeper.isSleeping()) continue;
            BlockPos head = sleeper.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(head.offset(-radius, -radius, -radius), head.offset(radius, radius, radius))) {
                if (!(level.getBlockEntity(pos) instanceof DreamCatcherBlockEntity catcher)
                        || pos.distSqr(head) > (double) radius * radius) continue;
                int gained = catcher.catchDream(RitualConfig.DREAM_CATCHER_ML_PER_NIGHT.getAsInt());
                if (gained <= 0) continue;
                caught += gained;
                level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.2);
                SmokecraftTrigger.fire(sleeper, "dream_caught");
            }
        }
        return caught;
    }
}
