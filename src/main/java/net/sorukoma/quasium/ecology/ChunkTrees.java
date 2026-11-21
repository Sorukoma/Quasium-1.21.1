package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ChunkTrees {
    private ChunkTrees() {
    }

    public static int count(LevelChunk chunk) {
        ChunkPos origin = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int trunks = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int woodTop = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= woodTop) continue;
                pos.set(origin.getMinBlockX() + x, woodTop, origin.getMinBlockZ() + z);
                if (chunk.getBlockState(pos).is(BlockTags.LOGS)) trunks++;
            }
        }
        return trunks;
    }
}
