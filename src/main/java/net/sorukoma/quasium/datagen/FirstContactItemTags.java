package net.sorukoma.quasium.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.ModItemTags;
import net.sorukoma.quasium.item.ModItems;

import java.util.concurrent.CompletableFuture;

public class FirstContactItemTags extends IntrinsicHolderTagsProvider<Item> {
    public FirstContactItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, registries, item -> item.builtInRegistryHolder().key(), QuasiumMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        IntrinsicTagAppender<Item> contact = tag(ModItemTags.FIRST_CONTACT);
        BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(QuasiumMod.MOD_ID)).sorted()
                .map(BuiltInRegistries.ITEM::get).filter(item -> item != ModItems.PROGRESSION_BOOK.get()).forEach(contact::add);
    }
}
