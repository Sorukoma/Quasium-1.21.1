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
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.PureTobaccoSmokedTrigger;
import net.sorukoma.quasium.item.ModItems;

import java.util.List;
import java.util.function.Consumer;

public class AllTobaccosAdvancement implements AdvancementProvider.AdvancementGenerator {
    private static final String KEY = "advancements.quasium.smokecraft.all_tobaccos";

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
        List<String> kinds = PureTobaccoSmokedTrigger.kinds();
        Advancement.Builder builder = Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder(QuasiumMod.MOD_ID + ":progression/first_tobacco"))
                .display(new ItemStack(ModItems.HOOKAH_BOWL.get()),
                        Component.translatable(KEY + ".title"),
                        Component.translatable(KEY + ".description", kinds.size()),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .rewards(AdvancementRewards.Builder.experience(100));
        kinds.forEach(kind -> builder.addCriterion(kind, PureTobaccoSmokedTrigger.smoked(kind)));
        builder.save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/all_tobaccos"), existingFileHelper);
    }
}
