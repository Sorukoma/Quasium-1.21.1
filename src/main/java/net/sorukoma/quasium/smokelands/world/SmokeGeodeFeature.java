package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;

import java.util.List;

public class SmokeGeodeFeature extends Feature<NoneFeatureConfiguration> {
    private static final List<NoteType> DUNE_NOTES = List.of(NoteType.ASH, NoteType.RESIN);
    private static final float CLOT_CHANCE = 0.3f;

    public SmokeGeodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos centre = context.origin();
        int radius = Mth.nextInt(random, 3, 5);
        BlockState shell = SmokelandsBlocks.PETRIFIED_SMOKE.get().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            double distance = Math.sqrt(pos.distSqr(centre));
            if (distance > radius + 0.3 || level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) continue;
            level.setBlock(pos, distance > radius - 1.2 ? shell : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, 0, radius))) {
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).is(shell.getBlock())) continue;
            if (random.nextFloat() >= CLOT_CHANCE) continue;
            NoteType note = DUNE_NOTES.get(random.nextInt(DUNE_NOTES.size()));
            level.setBlock(pos, SmokelandsBlocks.CLOTS.get(note).get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
