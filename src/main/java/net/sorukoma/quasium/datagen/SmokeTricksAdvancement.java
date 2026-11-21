package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.SmokeTricks;

import java.util.function.Consumer;

public class SmokeTricksAdvancement implements AdvancementProvider.AdvancementGenerator {
    private static final String KEY = "advancements.quasium.smokecraft.smoke_tricks";

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder(QuasiumMod.MOD_ID + ":smokecraft/ring_combo_5"))
                .display(new ItemStack(Items.WIND_CHARGE), Component.translatable(KEY + ".title"), Component.translatable(KEY + ".description"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .rewards(AdvancementRewards.Builder.experience(50));
        for (SmokeTricks.Trick trick : SmokeTricks.Trick.values()) builder.addCriterion(trick.key(), SmokecraftTrigger.on(trick.id()));
        builder.save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/smoke_tricks"), existingFileHelper);
    }
}
