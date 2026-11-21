package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmokePipeBlock extends PipeBlock {
    public static final TagKey<Block> CONNECTABLE =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "draft_connectable"));
    public static final MapCodec<SmokePipeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PipeTier.CODEC.fieldOf("tier").forGetter(SmokePipeBlock::tier), propertiesCodec()).apply(instance, SmokePipeBlock::new));

    private final PipeTier tier;

    public SmokePipeBlock(PipeTier tier, Properties properties) {
        this(tier, Shapes.empty(), properties);
    }

    protected SmokePipeBlock(PipeTier tier, VoxelShape body, Properties properties) {
        super(0.1875f, properties);
        this.tier = tier;
        for (int index = 0; index < shapeByIndex.length; index++) shapeByIndex[index] = Shapes.or(shapeByIndex[index], body);
        BlockState state = stateDefinition.any();
        for (Direction direction : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends SmokePipeBlock> codec() {
        return CODEC;
    }

    public PipeTier tier() {
        return tier;
    }

    public boolean conducts(BlockState state) {
        return true;
    }

    @Nullable
    public NoteType filterNote(BlockState state) {
        return null;
    }

    public boolean splits() {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.getBlock() instanceof SmokePipeBlock || neighbor.is(CONNECTABLE);
    }

    public BlockState connectedState(BlockGetter level, BlockPos pos) {
        return withConnections(level, pos, defaultBlockState());
    }

    protected BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        boolean connected = connectsTo(level, pos, direction);
        if (connected != state.getValue(PROPERTY_BY_DIRECTION.get(direction)) && level instanceof ServerLevel serverLevel) {
            DraftNetworks.invalidate(serverLevel, pos);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.getBlock() instanceof SmokePipeBlock || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level instanceof ServerLevel serverLevel && DraftNetworks.endpointChanged(serverLevel, pos, neighborPos)) {
            DraftNetworks.invalidate(serverLevel, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) DraftNetworks.invalidate(serverLevel, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel && !state.is(newState.getBlock())) DraftNetworks.invalidate(serverLevel, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static List<Component> report(ServerLevel level, BlockPos pos) {
        DraftNetwork network = DraftNetworks.networkAt(level, pos);
        if (network == null) return List.of(Component.translatable("quasium.draft.pipe.closed"));
        PipeTier tier = network.bottleneck();
        int produced = network.lastProduction();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("quasium.draft.scan.network", network.pipes().size(),
                Component.translatable("block.quasium." + tier.getSerializedName() + "_pipe"), tier.draftPerSecond(), tier.notesPerSecond()));
        lines.add(network.lastDemand() == 0
                ? Component.translatable("quasium.draft.scan.idle", produced)
                : Component.translatable("quasium.draft.scan.flow", produced, network.lastDemand(), Math.round(network.lastEfficiency() * 100)));
        if (produced > tier.draftPerSecond()) lines.add(Component.translatable("quasium.draft.scan.capped", tier.draftPerSecond()));
        lines.add(Component.translatable("quasium.draft.scan.store", Math.round(network.buffer()), network.capacity(),
                network.cargoTotal(), network.cargoCapacity()));
        if (!network.cargo().isEmpty()) {
            lines.add(Component.translatable("quasium.draft.scan.cargo", ComponentUtils.formatList(network.cargo().entrySet(),
                    note -> Component.empty().append(note.getKey().displayName()).append(" ")
                            .append(Component.translatable("gui.quasium.ml", note.getValue())))));
        }
        lines.add(nodes(level, network));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SmokeFilterBlock) lines.add(state.getValue(SmokeFilterBlock.FILTER).describe());
        return lines;
    }

    private static Component nodes(ServerLevel level, DraftNetwork network) {
        Set<BlockPos> seen = new HashSet<>();
        int generators = 0;
        int pulling = 0;
        int machines = 0;
        int asking = 0;
        int jars = 0;
        for (DraftNetwork.Endpoint endpoint : network.endpoints()) {
            if (!level.isLoaded(endpoint.pos()) || !seen.add(endpoint.pos())) continue;
            BlockEntity entity = level.getBlockEntity(endpoint.pos());
            if (entity instanceof NoteNode jar && jar.isStorage()) {
                jars++;
            } else if (entity instanceof DraftNode node) {
                if (DraftCatalog.roleOf(entity.getBlockState().getBlock()) == DraftCatalog.Role.PRODUCER) {
                    generators++;
                    if (node.draftOutput(endpoint.side()) > 0) pulling++;
                } else {
                    machines++;
                    if (node.draftDemand(endpoint.side()) > 0) asking++;
                }
            }
        }
        return Component.translatable("quasium.draft.scan.nodes", generators, pulling, machines, asking, jars);
    }
}
