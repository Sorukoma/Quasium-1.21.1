package net.sorukoma.quasium.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.item.EssenceBottleItem;

import java.util.Optional;

public record MixingRecipe(String essence, int ml, Item catalyst, Item result, Optional<ResourceLocation> research) {
    public static final Codec<MixingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("essence").forGetter(MixingRecipe::essence),
            ExtraCodecs.POSITIVE_INT.fieldOf("ml").forGetter(MixingRecipe::ml),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("catalyst").forGetter(MixingRecipe::catalyst),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(MixingRecipe::result),
            ResourceLocation.CODEC.optionalFieldOf("research").forGetter(MixingRecipe::research)
    ).apply(instance, MixingRecipe::new));

    public boolean matches(ItemStack bottle, ItemStack catalystStack) {
        return EssenceBottleItem.typeOf(bottle).equals(essence)
                && EssenceBottleItem.amountOf(bottle) >= ml
                && catalystStack.is(catalyst);
    }

    public ItemStack drain(ItemStack bottle) {
        return EssenceBottleItem.drained(bottle, ml);
    }

    public ItemStack bottle() {
        return EssenceBottleItem.of(essence, ml);
    }
}
