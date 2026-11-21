package net.sorukoma.quasium.smokelands.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.block.SmokelandsBlocks;
import net.sorukoma.quasium.smokelands.item.LorePageItem;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.smokelands.mob.SmokelandsEntities;
import net.sorukoma.quasium.smokelands.sultan.Sultans;

import java.util.function.Consumer;

public class SmokelandsAdvancements implements AdvancementProvider.AdvancementGenerator {
    private static final ResourceLocation BACKGROUND = SmokelandsDimension.id("textures/gui/advancements/backgrounds/smokelands.png");

    private Consumer<AdvancementHolder> saver;
    private ExistingFileHelper files;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper files) {
        this.saver = saver;
        this.files = files;
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(new ItemStack(SmokelandsBlocks.PETRIFIED_SMOKE.get()), title("root"), description("root"), BACKGROUND,
                        AdvancementType.TASK, true, true, false)
                .addCriterion("portal_lit", SmokecraftTrigger.on("smoke_portal_lit"))
                .addCriterion("entered", SmokecraftTrigger.on("smokelands_enter"))
                .addCriterion("fell_through", SmokecraftTrigger.on("smokelands_coma"))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(saver, id("root"), files);
        single(root, "enter", SmokelandsBlocks.ASH_SAND.get(), AdvancementType.TASK, "smokelands_enter");
        single(root, "fell_through", Items.PHANTOM_MEMBRANE, AdvancementType.GOAL, "smokelands_coma");
        single(root, "sea_walk", Items.FEATHER, AdvancementType.TASK, "smoke_sea_walk");
        single(root, "storm", SmokelandsBlocks.ASH_CACTUS.get(), AdvancementType.TASK, "storm_survived");
        single(root, "wisp", SmokelandsEntities.WISP_FLUTE.get(), AdvancementType.GOAL, "wisp_tamed");
        Advancement.Builder lore = display(root, "lore", SmokelandsItems.LORE_PAGE.get(), AdvancementType.GOAL, false);
        AdvancementHolder book = AdvancementSubProvider.createPlaceholder(QuasiumMod.MOD_ID + ":root");
        for (int page = 1; page <= LorePageItem.PAGES; page++) {
            Advancement.Builder single = display(book, "lore_" + page, SmokelandsItems.LORE_PAGE.get(), AdvancementType.TASK, true);
            pages(single, page, page).save(saver, id("lore_" + page), files);
        }
        pages(lore, 1, LorePageItem.PAGES).save(saver, id("lore"), files);
        AdvancementHolder doused = single(root, "sultan_doused", ModItems.ESSENCE_BOTTLE.get(), AdvancementType.GOAL, "sultan_doused");
        AdvancementHolder sultan = single(doused, "sultan", Sultans.THRONE_ITEM.get(), AdvancementType.CHALLENGE, "sultan_defeated");
        Advancement.Builder epilogue = display(sultan, "epilogue", ModItems.PROGRESSION_BOOK.get(), AdvancementType.CHALLENGE, false)
                .addCriterion("sultan_defeated", SmokecraftTrigger.on("sultan_defeated"));
        pages(epilogue, 1, LorePageItem.PAGES).save(saver, id("epilogue"), files);
    }

    private static Advancement.Builder pages(Advancement.Builder builder, int first, int last) {
        for (int page = first; page <= last; page++) builder.addCriterion("lore_page_" + page, SmokecraftTrigger.on("lore_page_" + page));
        return builder;
    }

    private AdvancementHolder single(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, String event) {
        return display(parent, name, icon, type, false).addCriterion(event, SmokecraftTrigger.on(event)).save(saver, id(name), files);
    }

    private static Advancement.Builder display(AdvancementHolder parent, String name, ItemLike icon, AdvancementType type, boolean quiet) {
        return Advancement.Builder.advancement().parent(parent)
                .display(new ItemStack(icon), title(name), description(name), null, type, !quiet, !quiet, quiet);
    }

    private static Component title(String name) {
        return Component.translatable("advancements.quasium.smokelands." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.quasium.smokelands." + name + ".description");
    }

    private static ResourceLocation id(String name) {
        return SmokelandsDimension.id("smokelands/" + name);
    }
}
