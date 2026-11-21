package net.sorukoma.quasium.smokelands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.magic.PlayerZones;
import net.sorukoma.quasium.network.OreGlowPacket;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WispService {
    private static final int ORE_GLOW_TICKS = 50;
    private static final int ORE_SCAN_EVERY = 40;

    private WispService() {
    }

    static void serve(ServerLevel level, DraftWisp wisp) {
        Player owner = wisp.owner();
        if (owner == null || owner.level() != level || owner.distanceToSqr(wisp) > 48 * 48) {
            wisp.discard();
            return;
        }
        glow(level, wisp);
        shelter(level, wisp);
        if (wisp.tickCount % ORE_SCAN_EVERY == 0 && owner instanceof ServerPlayer player) showOres(level, wisp.blockPosition(), player);
    }

    public static void glow(ServerLevel level, DraftWisp wisp) {
        BlockPos at = wisp.blockPosition();
        BlockState there = level.getBlockState(at);
        if (there.isAir()) {
            level.setBlockAndUpdate(at, SmokelandsEntities.WISP_LIGHT.get().defaultBlockState().setValue(LightBlock.LEVEL, SmokelandsConfig.WISP_LIGHT.get()));
        } else if (!there.is(SmokelandsEntities.WISP_LIGHT.get())) {
            return;
        }
        if (!at.equals(wisp.light())) darken(level, wisp.light());
        wisp.light(at);
    }

    static void darken(ServerLevel level, @Nullable BlockPos light) {
        if (light != null && level.getBlockState(light).is(SmokelandsEntities.WISP_LIGHT.get())) level.removeBlock(light, false);
    }

    static void shelter(ServerLevel level, DraftWisp wisp) {
        level.players().forEach(player -> shelter(player, wisp));
    }

    public static void shelter(Player player, DraftWisp wisp) {
        int radius = SmokelandsConfig.WISP_ZONE_RADIUS.get();
        if (player.distanceToSqr(wisp) <= (double) radius * radius) PlayerZones.offer(player, 1, 1, "", DraftWisp.GLOW, wisp.level().getGameTime());
    }

    public static List<BlockPos> ores(ServerLevel level, BlockPos center) {
        int reach = SmokelandsConfig.WISP_ORE_GLOW_RADIUS.get();
        BoundingBox around = BoundingBox.fromCorners(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach));
        return SectionPos.betweenClosedStream(SectionPos.blockToSectionCoord(around.minX()), SectionPos.blockToSectionCoord(around.minY()),
                        SectionPos.blockToSectionCoord(around.minZ()), SectionPos.blockToSectionCoord(around.maxX()),
                        SectionPos.blockToSectionCoord(around.maxY()), SectionPos.blockToSectionCoord(around.maxZ()))
                .filter(section -> mayHoldOre(level, section))
                .flatMap(section -> BlockPos.betweenClosedStream(overlap(around, section)))
                .filter(pos -> pos.distSqr(center) <= (double) reach * reach && level.getBlockState(pos).is(ModBlocks.DEEPSLATE_QUASIUM_ORE.get()))
                .map(BlockPos::immutable)
                .limit(OreGlowPacket.MAX_POSITIONS)
                .toList();
    }

    private static boolean mayHoldOre(ServerLevel level, SectionPos section) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(section.x(), section.z());
        if (chunk == null) return false;
        int index = chunk.getSectionIndexFromSectionY(section.y());
        if (index < 0 || index >= chunk.getSectionsCount()) return false;
        LevelChunkSection blocks = chunk.getSection(index);
        return !blocks.hasOnlyAir() && blocks.maybeHas(state -> state.is(ModBlocks.DEEPSLATE_QUASIUM_ORE.get()));
    }

    private static BoundingBox overlap(BoundingBox around, SectionPos section) {
        return new BoundingBox(Math.max(around.minX(), section.minBlockX()), Math.max(around.minY(), section.minBlockY()),
                Math.max(around.minZ(), section.minBlockZ()), Math.min(around.maxX(), section.maxBlockX()),
                Math.min(around.maxY(), section.maxBlockY()), Math.min(around.maxZ(), section.maxBlockZ()));
    }

    private static void showOres(ServerLevel level, BlockPos center, ServerPlayer owner) {
        List<BlockPos> found = ores(level, center);
        if (!found.isEmpty()) PacketDistributor.sendToPlayer(owner, new OreGlowPacket(found, ORE_GLOW_TICKS));
    }
}
