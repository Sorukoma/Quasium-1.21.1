package net.sorukoma.quasium.smokelands.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.function.Consumer;

public class OptionalItemEntry extends LootPoolSingletonContainer {
    public static final MapCodec<OptionalItemEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(entry -> entry.item)
    ).and(singletonFields(instance)).apply(instance, OptionalItemEntry::new));

    private final ResourceLocation item;

    private OptionalItemEntry(ResourceLocation item, int weight, int quality, List<LootItemCondition> conditions,
                              List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.item = item;
    }

    public static LootPoolSingletonContainer.Builder<?> of(ResourceLocation item) {
        return simpleBuilder((weight, quality, conditions, functions) -> new OptionalItemEntry(item, weight, quality, conditions, functions));
    }

    @Override
    public LootPoolEntryType getType() {
        return SmokelandsWorldgen.OPTIONAL_ITEM.get();
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
        BuiltInRegistries.ITEM.getOptional(item).ifPresent(found -> output.accept(new ItemStack(found)));
    }
}
