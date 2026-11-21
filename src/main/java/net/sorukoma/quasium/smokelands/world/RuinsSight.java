package net.sorukoma.quasium.smokelands.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.MindTicks;
import net.sorukoma.quasium.mind.Perception;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class RuinsSight {
    private static final int LOOK_EVERY = 20;
    private static final int CHUNK_REACH = 2;
    private static final int SURVEY_TICKS = 200;
    private static final Map<ServerPlayer, Survey> SURVEYS = new WeakHashMap<>();

    private record Survey(ServerLevel level, ChunkPos chunk, long takenAt, Set<StructureStart> ruins) {
        boolean fits(ServerPlayer player) {
            return level == player.serverLevel() && chunk.equals(player.chunkPosition()) && level.getGameTime() - takenAt < SURVEY_TICKS;
        }
    }

    private RuinsSight() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % LOOK_EVERY != 0) return;
        if (!SmokelandsDimension.contains(player.level()) || !sees(player)) return;
        for (StructureStart ruins : survey(player).ruins()) {
            if (near(player, ruins.getBoundingBox())) Perception.sawRuins(player);
            reveal(player, ruins);
        }
    }

    private static Survey survey(ServerPlayer player) {
        Survey last = SURVEYS.get(player);
        if (last != null && last.fits(player)) return last;
        Survey fresh = new Survey(player.serverLevel(), player.chunkPosition(), player.serverLevel().getGameTime(), ruinsAround(player));
        SURVEYS.put(player, fresh);
        return fresh;
    }

    public static boolean sees(Player player) {
        return !player.isSpectator() && player.getTags().contains(MindTicks.SEES_RUINS);
    }

    public static boolean veiled(StructureStart ruins) {
        return ruins.getPieces().stream().anyMatch(piece -> piece instanceof RuinsPiece ruin && ruin.veiled());
    }

    public static boolean reveal(ServerPlayer seer, StructureStart ruins) {
        ServerLevel level = seer.serverLevel();
        BoundingBox box = ruins.getBoundingBox();
        if (!sees(seer) || !veiled(ruins) || !near(seer, box) || !loaded(level, box)) return false;
        if (!level.getEntitiesOfClass(Player.class, AABB.of(box).inflate(1)).isEmpty()) return false;
        for (StructurePiece piece : ruins.getPieces()) {
            if (piece instanceof RuinsPiece ruin && ruin.veiled()) ruin.reveal(level, level.getRandom());
        }
        level.getChunk(ruins.getChunkPos().x, ruins.getChunkPos().z).setUnsaved(true);
        BlockPos middle = box.getCenter();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, middle.getX() + 0.5, box.minY() + 2, middle.getZ() + 0.5, 80,
                box.getXSpan() / 3.0, 1.5, box.getZSpan() / 3.0, 0.02);
        level.playSound(null, middle, SmokelandsSounds.PORTAL_TRAVEL.get(), SoundSource.AMBIENT, 1.5f, 0.6f);
        seer.displayClientMessage(Component.translatable("quasium.smokelands.ruins_revealed"), true);
        Perception.sawRuins(seer);
        return true;
    }

    private static Set<StructureStart> ruinsAround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ChunkPos centre = player.chunkPosition();
        Set<StructureStart> found = new LinkedHashSet<>();
        for (int x = centre.x - CHUNK_REACH; x <= centre.x + CHUNK_REACH; x++) {
            for (int z = centre.z - CHUNK_REACH; z <= centre.z + CHUNK_REACH; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(x, z),
                        structure -> structure.type() == SmokelandsWorldgen.RUINS.get())) {
                    found.add(start);
                }
            }
        }
        return found;
    }

    private static boolean near(Player player, BoundingBox box) {
        BlockPos middle = box.getCenter();
        double dx = player.getX() - middle.getX() - 0.5;
        double dz = player.getZ() - middle.getZ() - 0.5;
        int reach = SmokelandsConfig.RUINS_SIGHT_RADIUS.get();
        return dx * dx + dz * dz <= (double) reach * reach;
    }

    private static boolean loaded(ServerLevel level, BoundingBox box) {
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) return false;
            }
        }
        return true;
    }
}
