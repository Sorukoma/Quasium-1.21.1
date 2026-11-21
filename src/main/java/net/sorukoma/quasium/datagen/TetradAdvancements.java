package net.sorukoma.quasium.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.page.PageRegistry;
import net.sorukoma.quasium.magic.page.PageUnlocks;

import java.util.List;
import java.util.function.Consumer;

public class TetradAdvancements implements AdvancementProvider.AdvancementGenerator {
    public static final List<String> ACCORDS = List.of("spark", "lightness", "rime", "glaze", "oblivion", "caramel", "steam", "magma",
            "smolder", "balm", "amber", "ferment", "quagmire", "fog", "collapse");
    public static final String PAGE_WRITTEN = "page_written";
    public static final String FIRST_ACCORD = "first_accord";
    public static final String ALL_ACCORDS = "all_accords";
    private static final int ALL_ACCORDS_XP = 50;

    private Consumer<AdvancementHolder> saver;
    private ExistingFileHelper files;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper files) {
        this.saver = saver;
        this.files = files;
        for (NoteType note : NoteType.values()) firstBottleOf(note);
        ItemStack tetrad = new ItemStack(PageRegistry.SPELL_TETRAD.get());
        save(PAGE_WRITTEN, display(PAGE_WRITTEN, "smokecraft/first_scan", tetrad, AdvancementType.TASK)
                .addCriterion(PAGE_WRITTEN, SmokecraftTrigger.on(PAGE_WRITTEN)));
        Advancement.Builder firstAccord = display(FIRST_ACCORD, "smokecraft/first_cast", EssenceBottleItem.of(NoteType.DEW.id(), 20),
                AdvancementType.TASK).requirements(AdvancementRequirements.Strategy.OR);
        Advancement.Builder allAccords = display(ALL_ACCORDS, "smokecraft/" + FIRST_ACCORD, new ItemStack(Items.NOTE_BLOCK),
                AdvancementType.CHALLENGE).rewards(AdvancementRewards.Builder.experience(ALL_ACCORDS_XP));
        for (String accord : ACCORDS) {
            firstAccord.addCriterion(accord, SmokecraftTrigger.on("accord:" + accord));
            allAccords.addCriterion(accord, SmokecraftTrigger.on("accord:" + accord));
        }
        save(FIRST_ACCORD, firstAccord);
        save(ALL_ACCORDS, allAccords);
        ItemStack membrane = new ItemStack(Items.PHANTOM_MEMBRANE);
        save("spirit_cast", display("spirit_cast", "smokecraft/" + PAGE_WRITTEN, membrane, AdvancementType.GOAL)
                .addCriterion("cast_form_spirit", SmokecraftTrigger.on("cast_form:spirit")));
        ItemStack large = new ItemStack(PageRegistry.LARGE_SPELL_TETRAD.get());
        save("large_tetrad", display("large_tetrad", "smokecraft/" + PAGE_WRITTEN, large, AdvancementType.GOAL)
                .addCriterion("has_large_tetrad", InventoryChangeTrigger.TriggerInstance.hasItems(large.getItem())));
    }

    private void firstBottleOf(NoteType note) {
        ItemPredicate.Builder bottle = ItemPredicate.Builder.item().of(ModItems.ESSENCE_BOTTLE.get())
                .hasComponents(DataComponentPredicate.builder().expect(ModDataComponents.ESSENCE_TYPE.get(), note.id()).build());
        Advancement.Builder.advancement()
                .addCriterion("has_" + note.id(), InventoryChangeTrigger.TriggerInstance.hasItems(bottle))
                .save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, PageUnlocks.NOTE_ADVANCEMENTS + note.id()), files);
    }

    private static Advancement.Builder display(String name, String parent, ItemStack icon, AdvancementType type) {
        String key = "advancements.quasium.smokecraft." + name;
        return Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder(QuasiumMod.MOD_ID + ":" + parent))
                .display(icon, Component.translatable(key + ".title"), Component.translatable(key + ".description"), null, type,
                        true, true, false);
    }

    private void save(String name, Advancement.Builder builder) {
        builder.save(saver, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/" + name), files);
    }
}
