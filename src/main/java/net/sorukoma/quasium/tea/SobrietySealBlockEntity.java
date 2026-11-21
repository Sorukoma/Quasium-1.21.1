package net.sorukoma.quasium.tea;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.particle.ModParticles;

public class SobrietySealBlockEntity extends BlockEntity {
    private static final int HUM_TICKS = 40;
    private static final int RING_TICKS = 20;
    private static final int RING_PARTICLES = 10;
    private static final int RING_COLOR = 0xE8F0FF;

    public SobrietySealBlockEntity(BlockPos pos, BlockState state) {
        super(SealBlocks.SOBRIETY_SEAL_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) SobrietySeals.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) SobrietySeals.remove(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) SobrietySeals.remove(level, worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SobrietySealBlockEntity seal) {
        if (!(level instanceof ServerLevel server)) return;
        long time = level.getGameTime();
        if (time % HUM_TICKS == 0) level.playSound(null, pos, EcologyRegistry.SEAL_HUM.get(), SoundSource.BLOCKS, 0.35f, 1f);
        if (time % RING_TICKS != 0) return;
        double radius = EcologyConfig.SEAL_RADIUS.getAsInt();
        Vec3 center = Vec3.atBottomCenterOf(pos);
        for (int i = 0; i < RING_PARTICLES; i++) {
            double angle = server.random.nextDouble() * Math.PI * 2;
            server.sendParticles(ModParticles.noteSmoke(RING_COLOR), center.x + Math.cos(angle) * radius, center.y + 0.2,
                    center.z + Math.sin(angle) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
