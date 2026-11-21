package net.sorukoma.quasium.draft;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SmokeSplitterBlock extends SmokePipeBlock {
    public static final MapCodec<SmokeSplitterBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PipeTier.CODEC.fieldOf("tier").forGetter(SmokeSplitterBlock::tier), propertiesCodec()).apply(instance, SmokeSplitterBlock::new));

    public SmokeSplitterBlock(PipeTier tier, Properties properties) {
        super(tier, box(4, 4, 4, 12, 12, 12), properties);
    }

    @Override
    protected MapCodec<? extends SmokeSplitterBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean splits() {
        return true;
    }
}
