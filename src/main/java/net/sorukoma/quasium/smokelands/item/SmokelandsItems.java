package net.sorukoma.quasium.smokelands.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

public final class SmokelandsItems {
    public static final ResourceKey<JukeboxSong> ASH_LOFI_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, SmokelandsDimension.id("ash_lofi"));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LORE_PAGE_NUMBER =
            SmokelandsRegistry.DATA_COMPONENTS.registerComponentType("lore_page",
                    builder -> builder.persistent(Codec.intRange(1, LorePageItem.PAGES)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<UnreadPageFunction>> UNREAD_PAGE =
            SmokelandsRegistry.LOOT_FUNCTIONS.register("unread_lore_page", () -> new LootItemFunctionType<>(UnreadPageFunction.CODEC));

    public static final DeferredItem<Item> ASH_OF_TIME = SmokelandsRegistry.ITEMS.registerSimpleItem("ash_of_time",
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ASH_SACHET = SmokelandsRegistry.ITEMS.register("ash_sachet",
            () -> new AshSachetItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> NICOTINE_FRUIT = SmokelandsRegistry.ITEMS.registerSimpleItem("nicotine_fruit",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f)
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 60), 1f).build()));
    public static final DeferredItem<Item> LORE_PAGE = SmokelandsRegistry.ITEMS.register("lore_page",
            () -> new LorePageItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MUSIC_DISC_ASH_LOFI = SmokelandsRegistry.ITEMS.register("music_disc_ash_lofi",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ASH_LOFI_SONG)));

    private SmokelandsItems() {
    }

    public static void load() {
    }
}
