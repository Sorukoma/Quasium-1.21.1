package net.sorukoma.quasium.loot.modifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.selection.genetics.Genetics;

import java.util.List;

public class WildSeedsLootModifier extends LootModifier {
    public static final MapCodec<WildSeedsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst)
                    .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.seeds))
                    .apply(inst, WildSeedsLootModifier::new)
    );

    private final Item seeds;

    public WildSeedsLootModifier(LootItemCondition[] conditions, Item seeds) {
        super(conditions);
        this.seeds = seeds;
    }

    public static ItemStack rollStars(ItemStack seeds, RandomSource random) {
        List<? extends Integer> weights = SelectionConfig.WILD_SEED_STAR_WEIGHTS.get();
        seeds.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), Genetics.wildStars(random, weights));
        seeds.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), Genetics.wildStars(random, weights));
        seeds.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), Genetics.wildStars(random, weights));
        return seeds;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(rollStars(new ItemStack(seeds), context.getRandom()));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends WildSeedsLootModifier> codec() {
        return CODEC;
    }
}
