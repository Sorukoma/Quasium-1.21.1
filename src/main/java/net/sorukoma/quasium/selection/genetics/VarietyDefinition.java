package net.sorukoma.quasium.selection.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.QuasiumMod;

import java.util.Optional;

public record VarietyDefinition(int strength, int yield, int quality, boolean wild, TagKey<Block> soil,
                                Optional<ResourceKey<Level>> nativeDimension, int foreignGrowthPct) {
    public static final TagKey<Block> DEFAULT_SOIL =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "tobacco_soil"));

    private static final Codec<Integer> STARS = Codec.intRange(Genetics.MIN_STARS, Genetics.MAX_STARS);

    public static final Codec<VarietyDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STARS.fieldOf("strength").forGetter(VarietyDefinition::strength),
            STARS.fieldOf("yield").forGetter(VarietyDefinition::yield),
            STARS.fieldOf("quality").forGetter(VarietyDefinition::quality),
            Codec.BOOL.optionalFieldOf("wild", false).forGetter(VarietyDefinition::wild),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("soil", DEFAULT_SOIL).forGetter(VarietyDefinition::soil),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("native_dimension").forGetter(VarietyDefinition::nativeDimension),
            Codec.intRange(1, 100).optionalFieldOf("foreign_growth_pct", 100).forGetter(VarietyDefinition::foreignGrowthPct)
    ).apply(instance, VarietyDefinition::new));

    public int growthPct(Level level) {
        return nativeDimension.isEmpty() || nativeDimension.get().equals(level.dimension()) ? 100 : foreignGrowthPct;
    }
}
