package net.sorukoma.quasium.mind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class BlockFinder {
    private BlockFinder() {
    }

    public static Predicate<BlockState> matching(List<? extends String> entries) {
        List<Block> blocks = new ArrayList<>();
        List<TagKey<Block>> tags = new ArrayList<>();
        for (String entry : entries) {
            ResourceLocation id = ResourceLocation.tryParse(entry.startsWith("#") ? entry.substring(1) : entry);
            if (id == null) continue;
            if (entry.startsWith("#")) tags.add(TagKey.create(Registries.BLOCK, id));
            else BuiltInRegistries.BLOCK.getOptional(id).ifPresent(blocks::add);
        }
        return state -> blocks.contains(state.getBlock()) || tags.stream().anyMatch(state::is);
    }

    public static List<BlockPos> near(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> wanted, int limit) {
        List<BlockPos> found = new ArrayList<>();
        long reach = (long) radius * radius;
        for (int chunkX = (center.getX() - radius) >> 4; chunkX <= (center.getX() + radius) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - radius) >> 4; chunkZ <= (center.getZ() + radius) >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk != null) scan(chunk, center, radius, reach, wanted, found);
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    private static void scan(LevelChunk chunk, BlockPos center, int radius, long reach, Predicate<BlockState> wanted, List<BlockPos> found) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            if (bottom + 15 < center.getY() - radius || bottom > center.getY() + radius) continue;
            if (section.hasOnlyAir() || !section.maybeHas(wanted)) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!wanted.test(section.getBlockState(x, y, z))) continue;
                        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, bottom + y, chunk.getPos().getMinBlockZ() + z);
                        if (pos.distSqr(center) <= reach) found.add(pos);
                    }
                }
            }
        }
    }
}
