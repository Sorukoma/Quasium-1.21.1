package net.sorukoma.quasium.smokelands.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

import java.util.Optional;

public class RuinedPortalStructure extends Structure {
    public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool)
    ).apply(instance, RuinedPortalStructure::new));
    private static final int REACH = 8;
    private static final int MAX_SLOPE = 6;
    private static final int JIGSAW_DEPTH = 1;

    private final Holder<StructureTemplatePool> startPool;

    public RuinedPortalStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool) {
        super(settings);
        this.startPool = startPool;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (context.random().nextInt(100) >= SmokelandsConfig.RUINED_PORTAL_CHANCE_PCT.getAsInt()) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        return SmokelandsWorldgen.dryGround(context, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), REACH, MAX_SLOPE)
                .flatMap(ground -> JigsawPlacement.addPieces(context, startPool, Optional.empty(), JIGSAW_DEPTH,
                        new BlockPos(chunk.getMiddleBlockX(), ground, chunk.getMiddleBlockZ()), false, Optional.empty(), REACH,
                        PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING));
    }

    @Override
    public StructureType<?> type() {
        return SmokelandsWorldgen.RUINED_PORTAL.get();
    }
}
