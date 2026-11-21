package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.ritual.RitualConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RitualSite {
    private RitualSite() {
    }

    public static boolean platformComplete(Level level, BlockPos brazier) {
        return missingPlatformBlocks(level, brazier) == 0;
    }

    public static int missingPlatformBlocks(Level level, BlockPos brazier) {
        int missing = 0;
        for (BlockPos pos : BlockPos.betweenClosed(brazier.offset(-1, -1, -1), brazier.offset(1, -1, 1))) {
            if (!level.getBlockState(pos).is(ModBlocks.QUASIUM_BLOCK.get())) missing++;
        }
        return missing;
    }

    public static boolean pedestalSpot(BlockPos brazier, BlockPos pedestal) {
        int dy = pedestal.getY() - brazier.getY();
        int ring = Math.max(Math.abs(pedestal.getX() - brazier.getX()), Math.abs(pedestal.getZ() - brazier.getZ()));
        return dy >= -1 && dy <= 0 && ring >= RitualConfig.PEDESTAL_MIN_DISTANCE.getAsInt()
                && ring <= RitualConfig.PEDESTAL_MAX_DISTANCE.getAsInt();
    }

    public static List<RitualPedestalBlockEntity> pedestals(Level level, BlockPos brazier) {
        int reach = RitualConfig.PEDESTAL_MAX_DISTANCE.getAsInt();
        List<RitualPedestalBlockEntity> found = new ArrayList<>();
        for (BlockEntity entity : around(level, brazier, reach)) {
            if (entity instanceof RitualPedestalBlockEntity pedestal && pedestalSpot(brazier, pedestal.getBlockPos())) found.add(pedestal);
        }
        found.sort(Comparator.comparingDouble((RitualPedestalBlockEntity pedestal) -> pedestal.getBlockPos().distSqr(brazier))
                .thenComparingLong(pedestal -> pedestal.getBlockPos().asLong()));
        return found.subList(0, Math.min(found.size(), RitualConfig.MAX_PEDESTALS.getAsInt()));
    }

    public static List<ItemStack> offered(List<RitualPedestalBlockEntity> pedestals) {
        return pedestals.stream().map(RitualPedestalBlockEntity::item).toList();
    }

    public static List<NoteJarBlockEntity> jars(Level level, BlockPos brazier) {
        int radius = RitualConfig.JAR_RADIUS.getAsInt();
        double reach = (double) radius * radius;
        List<NoteJarBlockEntity> found = new ArrayList<>();
        for (BlockEntity entity : around(level, brazier, radius)) {
            if (entity instanceof NoteJarBlockEntity jar && jar.getBlockPos().distSqr(brazier) <= reach) found.add(jar);
        }
        found.sort(Comparator.comparingDouble(jar -> jar.getBlockPos().distSqr(brazier)));
        return found;
    }

    public static Map<NoteType, Integer> stock(List<NoteJarBlockEntity> jars) {
        Map<NoteType, Integer> stock = new EnumMap<>(NoteType.class);
        for (NoteJarBlockEntity jar : jars) {
            if (jar.note() != null && jar.amount() > 0) stock.merge(jar.note(), jar.amount(), Integer::sum);
        }
        return stock;
    }

    public static List<RitualBrazierBlockEntity> braziers(Level level, BlockPos center, int radius) {
        return around(level, center, radius).stream().filter(RitualBrazierBlockEntity.class::isInstance)
                .map(RitualBrazierBlockEntity.class::cast).toList();
    }

    public static List<ServerPlayer> participants(ServerLevel level, BlockPos brazier) {
        double reach = Math.pow(RitualConfig.PARTICIPANT_RADIUS.getAsInt(), 2);
        return level.players().stream()
                .filter(player -> !player.isSpectator() && player.isAlive() && player.distanceToSqr(brazier.getCenter()) <= reach)
                .toList();
    }

    public static float averageKaif(List<ServerPlayer> participants) {
        if (participants.isEmpty()) return 0f;
        return (float) participants.stream().mapToInt(PlayerKaifData::getKaifValue).average().orElse(0);
    }

    private static List<BlockEntity> around(Level level, BlockPos center, int radius) {
        List<BlockEntity> found = new ArrayList<>();
        ChunkPos min = new ChunkPos(center.offset(-radius, 0, -radius));
        ChunkPos max = new ChunkPos(center.offset(radius, 0, radius));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (!entity.isRemoved() && Math.abs(pos.getY() - center.getY()) <= radius
                            && Math.abs(pos.getX() - center.getX()) <= radius && Math.abs(pos.getZ() - center.getZ()) <= radius) {
                        found.add(entity);
                    }
                }
            }
        }
        return found;
    }
}
