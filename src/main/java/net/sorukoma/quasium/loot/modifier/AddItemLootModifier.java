package net.sorukoma.quasium.loot.modifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddItemLootModifier extends LootModifier {
    private final ItemStack item;

    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> codecStart(inst)
                    .and(ItemStack.CODEC.fieldOf("item").forGetter(m -> m.item))
                    .apply(inst, AddItemLootModifier::new)
    );

    public AddItemLootModifier(LootItemCondition[] conditions, ItemStack item) {
        super(conditions);
        this.item = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(item.copy());
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends AddItemLootModifier> codec() {
        return CODEC;
    }
}