package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DraftNetwork {
    private static final int MAX_PIPES = 4096;

    public record Endpoint(BlockPos pos, Direction side, int distance, @Nullable NoteType filter, @Nullable BlockPos group) {
    }

    record Carry(float buffer, Map<NoteType, Integer> cargo) {
    }

    private record Live(Endpoint endpoint, BlockEntity entity, int demand) {
    }

    private final Set<BlockPos> pipes;
    private final List<BlockPos> glassPipes;
    private final PipeTier bottleneck;
    private final List<Endpoint> endpoints;
    private final EnumMap<NoteType, Integer> cargo = new EnumMap<>(NoteType.class);
    private float buffer;
    private boolean draftedThisSecond;
    private int lastProduction;
    private int lastDemand;
    private float lastEfficiency;
    private int flowColor = 0xDDDDDD;
    private float flowLoad;

    private DraftNetwork(Set<BlockPos> pipes, List<BlockPos> glassPipes, PipeTier bottleneck, List<Endpoint> endpoints) {
        this.pipes = pipes;
        this.glassPipes = glassPipes;
        this.bottleneck = bottleneck;
        this.endpoints = endpoints;
    }

    static DraftNetwork build(ServerLevel level, BlockPos seed) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> glass = new ArrayList<>();
        List<Endpoint> endpoints = new ArrayList<>();
        PipeTier bottleneck = PipeTier.QUASIUM;
        distance.put(seed.immutable(), 0);
        queue.add(seed.immutable());
        while (!queue.isEmpty() && distance.size() <= MAX_PIPES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SmokePipeBlock pipe)) continue;
            bottleneck = PipeTier.weaker(bottleneck, pipe.tier());
            if (pipe.tier() == PipeTier.GLASS) glass.add(pos);
            int depth = distance.get(pos);
            for (Direction direction : Direction.values()) {
                if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction))) continue;
                BlockPos next = pos.relative(direction);
                if (!level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (nextState.getBlock() instanceof SmokePipeBlock nextPipe) {
                    if (nextPipe.conducts(nextState) && !distance.containsKey(next)) {
                        distance.put(next, depth + 1);
                        queue.add(next);
                    }
                    continue;
                }
                BlockEntity entity = level.getBlockEntity(next);
                if (entity instanceof DraftNode || entity instanceof NoteNode) {
                    endpoints.add(new Endpoint(next, direction.getOpposite(), depth, pipe.filterNote(state), pipe.splits() ? pos : null));
                }
            }
        }
        endpoints.sort(Comparator.comparingInt(Endpoint::distance));
        return new DraftNetwork(new HashSet<>(distance.keySet()), glass, bottleneck, endpoints);
    }

    public Set<BlockPos> pipes() {
        return pipes;
    }

    List<BlockPos> glassPipes() {
        return glassPipes;
    }

    public PipeTier bottleneck() {
        return bottleneck;
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public int lastProduction() {
        return lastProduction;
    }

    public int lastDemand() {
        return lastDemand;
    }

    public float lastEfficiency() {
        return lastEfficiency;
    }

    public float buffer() {
        return buffer;
    }

    public Map<NoteType, Integer> cargo() {
        return cargo;
    }

    int flowColor() {
        return flowColor;
    }

    float flowLoad() {
        return flowLoad;
    }

    public int capacity() {
        return Config.DRAFT_BUFFER_PER_PIPE.getAsInt() * pipes.size();
    }

    public int cargoCapacity() {
        return Config.CARGO_ML_PER_PIPE.getAsInt() * pipes.size();
    }

    public int cargoTotal() {
        return cargo.values().stream().mapToInt(Integer::intValue).sum();
    }

    Carry carry() {
        return new Carry(buffer, new EnumMap<>(cargo));
    }

    void absorb(Carry carry) {
        buffer = Math.min(capacity(), buffer + carry.buffer());
        carry.cargo().forEach((note, ml) -> cargo.merge(note, ml, Integer::sum));
    }

    void tick(ServerLevel level, Set<BlockPos> served) {
        List<Live> live = new ArrayList<>();
        int produced = 0;
        int demanded = 0;
        for (Endpoint endpoint : endpoints) {
            if (!level.isLoaded(endpoint.pos()) || !served.add(endpoint.pos())) continue;
            BlockEntity entity = level.getBlockEntity(endpoint.pos());
            if (entity == null || entity.isRemoved()) continue;
            int demand = 0;
            if (entity instanceof DraftNode node) {
                produced += Math.max(0, node.draftOutput(endpoint.side()));
                demand = Math.max(0, node.draftDemand(endpoint.side()));
                demanded += demand;
            }
            live.add(new Live(endpoint, entity, demand));
        }
        int cap = bottleneck.draftPerSecond();
        float inflow = Math.min(produced, cap) / 20f;
        buffer = Math.min(buffer + inflow, capacity());
        float want = demanded / 20f;
        float efficiency = want <= 0f ? 0f : Math.min(1f, Math.min(buffer, cap / 20f) / want);
        for (Live entry : live) {
            if (entry.demand() > 0 && entry.entity() instanceof DraftNode node) node.draftSupplied(entry.endpoint().side(), efficiency);
        }
        buffer = Math.max(0f, buffer - want * efficiency);
        lastProduction = produced;
        lastDemand = demanded;
        lastEfficiency = efficiency;
        if (inflow > 0f) draftedThisSecond = true;
        if (level.getGameTime() % 20 == 0) {
            moveCargo(live);
            draftedThisSecond = false;
        }
    }

    private void moveCargo(List<Live> live) {
        flowLoad = 0f;
        int budget = bottleneck.notesPerSecond();
        if (!draftedThisSecond) {
            feedFromStorage(live, budget);
            return;
        }
        int room = cargoCapacity() - cargoTotal();
        int pulled = 0;
        for (Live entry : live) {
            if (!(entry.entity() instanceof NoteNode node) || node.isStorage()) continue;
            for (Map.Entry<NoteType, Integer> offer : List.copyOf(node.noteSupply().entrySet())) {
                int wanted = Math.min(offer.getValue(), Math.min(room, budget - pulled));
                if (wanted <= 0) break;
                int taken = node.takeNote(offer.getKey(), wanted);
                if (taken <= 0) continue;
                cargo.merge(offer.getKey(), taken, Integer::sum);
                room -= taken;
                pulled += taken;
            }
        }
        int delivered = 0;
        long red = 0;
        long green = 0;
        long blue = 0;
        for (NoteType note : NoteType.values()) {
            int amount = cargo.getOrDefault(note, 0);
            if (amount <= 0 || delivered >= budget) continue;
            int sent = deliver(note, Math.min(amount, budget - delivered), live);
            if (sent <= 0) continue;
            if (sent >= amount) cargo.remove(note);
            else cargo.put(note, amount - sent);
            delivered += sent;
            red += (long) (note.color() >> 16 & 0xFF) * sent;
            green += (long) (note.color() >> 8 & 0xFF) * sent;
            blue += (long) (note.color() & 0xFF) * sent;
        }
        int fed = feedFromStorage(live, budget - delivered);
        int moved = delivered + fed;
        if (moved > 0 && delivered > 0) flowColor = (int) (red / delivered) << 16 | (int) (green / delivered) << 8 | (int) (blue / delivered);
        flowLoad = Math.min(1f, Math.max(pulled, moved) / (float) Math.max(1, budget));
    }

    private int feedFromStorage(List<Live> live, int budget) {
        int fed = 0;
        for (Live consumer : live) {
            if (fed >= budget || !(consumer.entity() instanceof NoteNode sink) || sink.isStorage()
                    || sink.notePriority() < NoteNode.PRIORITY_NORMAL) continue;
            for (NoteType note : NoteType.values()) {
                NoteType filter = consumer.endpoint().filter();
                if (filter != null && filter != note) continue;
                int need = sink.acceptNote(note, budget - fed, true);
                for (Live store : live) {
                    if (need <= 0) break;
                    if (!(store.entity() instanceof NoteNode storage) || !storage.isStorage()) continue;
                    int taken = storage.takeNote(note, need);
                    if (taken <= 0) continue;
                    int accepted = sink.acceptNote(note, taken, false);
                    if (accepted < taken) storage.acceptNote(note, taken - accepted, false);
                    need -= accepted;
                    fed += accepted;
                }
            }
        }
        return fed;
    }

    private int deliver(NoteType note, int amount, List<Live> live) {
        List<Live> sinks = new ArrayList<>();
        for (Live entry : live) {
            NoteType filter = entry.endpoint().filter();
            if (entry.entity() instanceof NoteNode && (filter == null || filter == note)) sinks.add(entry);
        }
        sinks.sort(Comparator.comparingInt((Live entry) -> -priority(entry, note))
                .thenComparingInt(entry -> entry.endpoint().distance())
                .thenComparingLong(entry -> entry.endpoint().group() == null ? Long.MIN_VALUE : entry.endpoint().group().asLong()));
        int left = amount;
        int index = 0;
        while (index < sinks.size() && left > 0) {
            BlockPos group = sinks.get(index).endpoint().group();
            int end = index + 1;
            while (group != null && end < sinks.size() && group.equals(sinks.get(end).endpoint().group())) end++;
            List<Live> members = sinks.subList(index, end);
            int share = Math.max(1, left / members.size());
            for (Live member : members) {
                if (left <= 0) break;
                left -= ((NoteNode) member.entity()).acceptNote(note, Math.min(share, left), false);
            }
            for (Live member : members) {
                if (left <= 0) break;
                left -= ((NoteNode) member.entity()).acceptNote(note, left, false);
            }
            index = end;
        }
        return amount - left;
    }

    private static int priority(Live entry, NoteType note) {
        if (entry.endpoint().filter() == note) return NoteNode.PRIORITY_NORMAL + 1;
        return ((NoteNode) entry.entity()).notePriority();
    }
}
