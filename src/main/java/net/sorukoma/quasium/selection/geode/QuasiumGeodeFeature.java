package net.sorukoma.quasium.selection.geode;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Optional;

public class QuasiumGeodeFeature extends GeodeFeature {
    public static final ResourceKey<LootTable> CHEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "chests/quasium_geode"));

    public QuasiumGeodeFeature(Codec<GeodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GeodeConfiguration> context) {
        if (!super.place(context)) return false;
        GeodeConfiguration config = context.config();
        IntProvider wall = config.outerWallDistance;
        int middle = (wall.getMinValue() + wall.getMaxValue()) / 2;
        WorldGenLevel level = context.level();
        chestSpot(level, context.origin().offset(middle, middle, middle), middle - config.minGenOffset).ifPresent(spot -> {
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
            level.setBlock(spot, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_CLIENTS);
            RandomizableContainer.setBlockEntityLootTable(level, context.random(), spot, CHEST_LOOT);
        });
        return true;
    }

    private static Optional<BlockPos> chestSpot(WorldGenLevel level, BlockPos center, int depth) {
        BlockPos.MutableBlockPos spot = center.mutable();
        for (int step = 0; step < depth; step++, spot.move(Direction.DOWN)) {
            BlockState here = level.getBlockState(spot);
            if (!here.isAir() && !(here.getBlock() instanceof AmethystClusterBlock)) return Optional.empty();
            BlockPos below = spot.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return Optional.of(spot.immutable());
        }
        return Optional.empty();
    }
}
