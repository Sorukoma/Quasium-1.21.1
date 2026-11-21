package net.sorukoma.quasium.ritual.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RitualAdvancementProvider {
    public static final String BRAZIER = "ritual/brazier";
    public static final String FIRST_RITUAL = "ritual/first_ritual";

    private record Goal(String name, String parent, ItemLike icon, AdvancementType type, String event) {
    }

    private static final List<Goal> GOALS = List.of(
            new Goal("start", BRAZIER, Items.FLINT_AND_STEEL, AdvancementType.TASK, "ritual_start"),
            new Goal("failed", BRAZIER, Items.GUNPOWDER, AdvancementType.TASK, "ritual_failed"),
            new Goal("group", FIRST_RITUAL, Items.CAMPFIRE, AdvancementType.GOAL, "ritual_group"),
            new Goal("nirvana", FIRST_RITUAL, RitualRegistry.BOTTOMLESS_FLASK.get(), AdvancementType.CHALLENGE, "ritual_nirvana"),
            new Goal("eternal_coal", FIRST_RITUAL, RitualRegistry.ETERNAL_COAL.get(), AdvancementType.GOAL, "ritual:eternal_coal"),
            new Goal("carapace_nova", FIRST_RITUAL, Items.CHAINMAIL_CHESTPLATE, AdvancementType.GOAL, "carapace_nova"),
            new Goal("smokestep_dash", FIRST_RITUAL, Items.FEATHER, AdvancementType.TASK, "smokestep_dash"),
            new Goal("dream_caught", FIRST_RITUAL, RitualRegistry.DREAM_CATCHER_ITEM.get(), AdvancementType.TASK, "dream_caught"));

    private RitualAdvancementProvider() {
    }

    public static DataProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper files) {
        AdvancementProvider advancements = new AdvancementProvider(output, registries, files, List.of((lookup, saver, helper) -> {
            for (Goal goal : GOALS) {
                saver.accept(Advancement.Builder.advancement()
                        .parent(id(goal.parent()))
                        .display(new ItemStack(goal.icon()),
                                Component.translatable("advancements.quasium.ritual." + goal.name() + ".title"),
                                Component.translatable("advancements.quasium.ritual." + goal.name() + ".description"),
                                null, goal.type(), true, true, false)
                        .addCriterion(goal.event().replace(':', '_'), SmokecraftTrigger.on(goal.event()))
                        .build(id("ritual/" + goal.name())));
            }
        }));
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return advancements.run(cache);
            }

            @Override
            public String getName() {
                return "Ritual advancements";
            }
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
