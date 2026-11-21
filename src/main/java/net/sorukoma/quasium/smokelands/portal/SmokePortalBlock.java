package net.sorukoma.quasium.smokelands.portal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import org.jetbrains.annotations.Nullable;

public class SmokePortalBlock extends Block implements Portal, LiquidBlockContainer {
    public static final MapCodec<SmokePortalBlock> CODEC = simpleCodec(SmokePortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_AXIS = box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_AXIS = box(6, 0, 0, 10, 16, 16);
    private static final double PUSH = 0.6;

    public SmokePortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS : X_AXIS;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos,
                                     BlockPos facingPos) {
        boolean inPlane = facing.getAxis() == Direction.Axis.Y || facing.getAxis() == state.getValue(AXIS);
        if (inPlane && !facingState.is(this) && !SmokePortalShape.isFrame(facingState)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !player.canUsePortal(false)) return;
        SmokeTravel.refusal(player).ifPresentOrElse(reason -> {
            pushOut(player, state.getValue(AXIS), pos);
            if (player.tickCount % 20 == 0) player.displayClientMessage(reason, true);
        }, () -> player.setAsInsidePortal(this, pos));
    }

    private static void pushOut(ServerPlayer player, Direction.Axis axis, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double away = axis == Direction.Axis.X ? player.getZ() - center.z : player.getX() - center.x;
        double push = away < 0 ? -PUSH : PUSH;
        player.setDeltaMovement(axis == Direction.Axis.X ? new Vec3(0, 0.15, push) : new Vec3(push, 0.15, 0));
        player.hurtMarked = true;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player player && player.getAbilities().invulnerable ? 1 : SmokelandsConfig.PORTAL_SECONDS.get() * 20;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return entity instanceof ServerPlayer player ? SmokeTravel.transition(level, player) : null;
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            int color = random.nextBoolean() ? 0x8F6FCF : NoteType.ASH.color();
            level.addParticle(ModParticles.noteSmoke(color), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
