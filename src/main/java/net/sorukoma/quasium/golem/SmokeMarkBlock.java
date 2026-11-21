package net.sorukoma.quasium.golem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class SmokeMarkBlock extends Block implements EntityBlock {
    public static final MapCodec<SmokeMarkBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MarkType.CODEC.fieldOf("mark").forGetter(SmokeMarkBlock::type), propertiesCodec()).apply(instance, SmokeMarkBlock::new));
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    private final MarkType type;

    public SmokeMarkBlock(MarkType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public MarkType type() {
        return type;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (SmokeMarks.count(context.getLevel(), context.getClickedPos()) < Config.MARKS_PER_CHUNK.getAsInt()) return defaultBlockState();
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("quasium.mark.chunk_full", Config.MARKS_PER_CHUNK.getAsInt()), true);
        }
        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(this)) return;
        SmokeMarks.add(level, pos);
        level.playSound(null, pos, ModSoundEvents.SEAL_PLACE.get(), SoundSource.BLOCKS, 0.7f, 1f);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokeMarkBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(placer instanceof ServerPlayer player)) return;
        if (type != MarkType.PROGRAM && type != MarkType.FEED) SmokecraftTrigger.fire(player, "mark_place");
        if (!(level.getBlockEntity(pos) instanceof SmokeMarkBlockEntity mark)) return;
        mark.claim(player);
        GolemStaffItem.bindOnPlacement(player, mark);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) SmokeMarks.remove(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        Vector3f color = rgb(type.color());
        level.addParticle(new DustParticleOptions(color, 0.6f), pos.getX() + random.nextDouble(), pos.getY() + 0.1,
                pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
    }

    private static Vector3f rgb(int color) {
        return new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f);
    }
}
