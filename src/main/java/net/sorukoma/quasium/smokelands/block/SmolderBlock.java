package net.sorukoma.quasium.smokelands.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

public class SmolderBlock extends Block {
    private static final VoxelShape SINKING = box(0, 0, 0, 16, 14, 16);
    private static final String LAST_DRAIN = "QuasiumSmolderDrain";
    private static final int SMOKE_CHANCE = 8;

    public SmolderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SINKING;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ServerPlayer player) drain(player);
    }

    public static void drain(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now - player.getPersistentData().getLong(LAST_DRAIN) < 20) return;
        player.getPersistentData().putLong(LAST_DRAIN, now);
        if (PlayerKaifData.getKaifValue(player) > PlayerKaifData.MIN_KAIF) {
            PlayerKaifData.subtractKaif(player, SmokelandsConfig.SMOLDER_KAIF_PER_SECOND.get());
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SMOKE_CHANCE) != 0 || !level.getBlockState(pos.above()).isAir()) return;
        double x = pos.getX() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.02, z, 0, 0.02, 0);
        if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 1.02, z, 0, 0.01, 0);
    }
}
