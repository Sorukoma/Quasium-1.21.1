package net.sorukoma.quasium.mind;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

final class MindAdvancements implements AdvancementProvider.AdvancementGenerator {
    private static final TagKey<Biome> ARID =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "hookah_tourist/arid"));
    private static final int PEAK_Y = 200;
    private static final int TOURIST_XP = 100;

    private Consumer<AdvancementHolder> saver;
    private ExistingFileHelper files;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper files) {
        this.saver = saver;
        this.files = files;
        ResourceLocation treatise = save("treatise", "smokecraft/first_scan", new ItemStack(MindRegistry.TREATISE.get()), AdvancementType.TASK,
                "treatise");
        ResourceLocation research = save("first_research", treatise, new ItemStack(MindRegistry.RESEARCH_TABLE_ITEM.get()), AdvancementType.TASK,
                "research");
        save("perk", research, new ItemStack(Items.EXPERIENCE_BOTTLE), AdvancementType.GOAL, "perk");
        save("hidden_spell", research, new ItemStack(ModItems.BURNED_TOBACCO.get()), AdvancementType.GOAL, "cast:fume", "cast:delirium_breath");
        save("bestiary", "smokecraft/first_scan", EssenceBottleItem.of("menthol", 2), AdvancementType.TASK, "bestiary_drop");
        ResourceLocation dream = save("dream", "root", new ItemStack(Items.RED_BED), AdvancementType.TASK, "dream");
        save("lucid", dream, new ItemStack(Items.ENDER_EYE), AdvancementType.GOAL, "lucid_choice");
        save("nightmare", dream, new ItemStack(Items.WITHER_ROSE), AdvancementType.TASK, "nightmare");
        save("enlightenment", "kaif/kaif_level_30", new ItemStack(Items.GLOWSTONE_DUST), AdvancementType.CHALLENGE, "enlightenment");
        ResourceLocation coma = save("kaif_coma", "smokecraft/overdose", new ItemStack(MindRegistry.SMOKELAND_HOOK.get()), AdvancementType.GOAL,
                "kaif_coma");
        save("smoke_phantom", "smokecraft/overdose", new ItemStack(Items.PHANTOM_MEMBRANE), AdvancementType.TASK, "smoke_phantom");
        save("ghost_deal", coma, new ItemStack(Items.EMERALD), AdvancementType.GOAL, "ghost_deal");
        tourist(registries.lookupOrThrow(Registries.BIOME));
    }

    private void tourist(HolderGetter<Biome> biomes) {
        Map<String, LocationPredicate.Builder> places = new LinkedHashMap<>();
        places.put("arid", in(biomes.getOrThrow(ARID)));
        places.put("snowy", in(biomes.getOrThrow(Tags.Biomes.IS_SNOWY)));
        places.put("jungle", in(biomes.getOrThrow(Tags.Biomes.IS_JUNGLE)));
        places.put("swamp", in(biomes.getOrThrow(Tags.Biomes.IS_SWAMP)));
        places.put("mushroom_fields", in(biomes.getOrThrow(Tags.Biomes.IS_MUSHROOM)));
        places.put("cherry_grove", LocationPredicate.Builder.inBiome(biomes.getOrThrow(Biomes.CHERRY_GROVE)));
        places.put("deep_dark", LocationPredicate.Builder.inBiome(biomes.getOrThrow(Biomes.DEEP_DARK)));
        places.put("ocean", in(biomes.getOrThrow(Tags.Biomes.IS_OCEAN)));
        places.put("peak", LocationPredicate.Builder.atYLocation(MinMaxBounds.Doubles.atLeast(PEAK_Y)).setDimension(Level.OVERWORLD));
        places.put("nether", LocationPredicate.Builder.inDimension(Level.NETHER));
        places.put("the_end", LocationPredicate.Builder.inDimension(Level.END));
        places.put("smokelands", LocationPredicate.Builder.inDimension(SmokelandsDimension.LEVEL));
        Advancement.Builder tourist = Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder(QuasiumMod.MOD_ID + ":progression/obtained_backpack_hookah"))
                .display(new ItemStack(ModItems.BACKPACK_HOOKAH.get()), Component.translatable("advancements.quasium.mind.hookah_tourist.title"),
                        Component.translatable("advancements.quasium.mind.hookah_tourist.description"), null, AdvancementType.CHALLENGE,
                        true, true, false)
                .rewards(AdvancementRewards.Builder.experience(TOURIST_XP));
        places.forEach((place, where) ->
                tourist.addCriterion(place, SmokecraftTrigger.on(MindStats.PUFF, EntityPredicate.Builder.entity().located(where))));
        tourist.save(saver, Titles.TOURIST, files);
    }

    private static LocationPredicate.Builder in(HolderSet<Biome> biomes) {
        return LocationPredicate.Builder.location().setBiomes(biomes);
    }

    private ResourceLocation save(String name, String parent, ItemStack icon, AdvancementType type, String... events) {
        return save(name, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, parent), icon, type, events);
    }

    private ResourceLocation save(String name, ResourceLocation parent, ItemStack icon, AdvancementType type, String... events) {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .parent(AdvancementSubProvider.createPlaceholder(parent.toString()))
                .display(icon, Component.translatable("advancements.quasium.mind." + name + ".title"),
                        Component.translatable("advancements.quasium.mind." + name + ".description"), null, type, true, true, false)
                .requirements(AdvancementRequirements.Strategy.OR);
        for (String event : events) builder.addCriterion(event.replace(':', '_'), SmokecraftTrigger.on(event));
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mind/" + name);
        builder.save(saver, id, files);
        return id;
    }
}
