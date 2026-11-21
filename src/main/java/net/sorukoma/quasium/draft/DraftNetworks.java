package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.network.PipeFlowPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class DraftNetworks {
    private static final int FLOW_SYNC_TICKS = 20;
    private static final double FLOW_VIEW_DISTANCE = 48.0;
    private static final Map<ResourceKey<Level>, LevelNetworks> LEVELS = new HashMap<>();

    private record Orphan(Set<BlockPos> pipes, DraftNetwork.Carry carry) {
    }

    private static final class LevelNetworks {
        final Map<BlockPos, DraftNetwork> byPipe = new HashMap<>();
        final Set<DraftNetwork> networks = new LinkedHashSet<>();
        final Set<BlockPos> pending = new HashSet<>();
        final List<Orphan> orphans = new ArrayList<>();
    }

    private DraftNetworks() {
    }

    private static LevelNetworks of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelNetworks());
    }

    public static void invalidate(ServerLevel level, BlockPos pos) {
        LevelNetworks state = of(level);
        BlockPos origin = pos.immutable();
        state.pending.add(origin);
        dissolve(state, state.byPipe.get(origin));
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = origin.relative(direction);
            state.pending.add(neighbor);
            dissolve(state, state.byPipe.get(neighbor));
        }
    }

    static boolean endpointChanged(ServerLevel level, BlockPos pipe, BlockPos neighbor) {
        LevelNetworks state = LEVELS.get(level.dimension());
        DraftNetwork network = state == null ? null : state.byPipe.get(pipe);
        if (network == null) return true;
        if (level.getBlockState(neighbor).getBlock() instanceof SmokePipeBlock) return false;
        BlockEntity entity = level.getBlockEntity(neighbor);
        boolean node = entity instanceof DraftNode || entity instanceof NoteNode;
        boolean known = network.endpoints().stream().anyMatch(endpoint -> endpoint.pos().equals(neighbor));
        return node != known;
    }

    @Nullable
    public static DraftNetwork networkAt(ServerLevel level, BlockPos pos) {
        LevelNetworks state = LEVELS.get(level.dimension());
        if (state == null) return null;
        if (!state.pending.isEmpty()) rebuild(level, state);
        return state.byPipe.get(pos);
    }

    private static void dissolve(LevelNetworks state, @Nullable DraftNetwork network) {
        if (network == null || !state.networks.remove(network)) return;
        for (BlockPos pipe : network.pipes()) {
            state.byPipe.remove(pipe);
            state.pending.add(pipe);
        }
        if (network.buffer() > 0f || !network.cargo().isEmpty()) state.orphans.add(new Orphan(network.pipes(), network.carry()));
    }

    private static void rebuild(ServerLevel level, LevelNetworks state) {
        List<BlockPos> seeds = new ArrayList<>(state.pending);
        state.pending.clear();
        for (BlockPos seed : seeds) {
            if (state.byPipe.containsKey(seed) || !level.isLoaded(seed)) continue;
            BlockState block = level.getBlockState(seed);
            if (!(block.getBlock() instanceof SmokePipeBlock pipe) || !pipe.conducts(block)) continue;
            DraftNetwork network = DraftNetwork.build(level, seed);
            for (BlockPos pipePos : network.pipes()) state.byPipe.put(pipePos, network);
            for (Iterator<Orphan> it = state.orphans.iterator(); it.hasNext(); ) {
                Orphan orphan = it.next();
                if (orphan.pipes().stream().anyMatch(network.pipes()::contains)) {
                    network.absorb(orphan.carry());
                    it.remove();
                }
            }
            state.networks.add(network);
        }
        state.orphans.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LevelNetworks state = LEVELS.get(level.dimension());
        if (state == null) return;
        if (!state.pending.isEmpty()) rebuild(level, state);
        Set<BlockPos> served = new HashSet<>();
        boolean syncFlow = level.getGameTime() % FLOW_SYNC_TICKS == 0;
        for (DraftNetwork network : state.networks) {
            network.tick(level, served);
            if (syncFlow && network.flowLoad() > 0f && !network.glassPipes().isEmpty()) sendFlow(level, network);
        }
    }

    private static void sendFlow(ServerLevel level, DraftNetwork network) {
        BlockPos anchor = network.glassPipes().get(0);
        PipeFlowPacket packet = new PipeFlowPacket(network.glassPipes(), network.flowColor(), network.flowLoad());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(anchor.getX(), anchor.getY(), anchor.getZ()) <= FLOW_VIEW_DISTANCE * FLOW_VIEW_DISTANCE) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LevelNetworks state = LEVELS.get(level.dimension());
        if (state == null) return;
        ChunkPos chunk = event.getChunk().getPos();
        List<DraftNetwork> touched = new ArrayList<>();
        for (DraftNetwork network : state.networks) {
            if (network.pipes().stream().anyMatch(pipe -> chunk.equals(new ChunkPos(pipe)))) touched.add(network);
        }
        touched.forEach(network -> dissolve(state, network));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        LevelNetworks state = of(level);
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(block -> block.getBlock() instanceof SmokePipeBlock)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).getBlock() instanceof SmokePipeBlock) {
                            state.pending.add(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LEVELS.remove(level.dimension());
    }
}
