package net.sorukoma.quasium.selection.trade;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.SelectionConfig;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class CartographerMaps {
    public static final TagKey<Structure> ON_ABANDONED_LOUNGE_MAPS = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "on_abandoned_lounge_maps"));
    public static final String LOUNGE_MAP_NAME = "item.quasium.abandoned_lounge_map";
    public static final int LOUNGE_MAP_LEVEL = 2;

    private CartographerMaps() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER) return;
        event.getTrades().get(LOUNGE_MAP_LEVEL).add(new VillagerTrades.TreasureMapForEmeralds(SelectionConfig.LOUNGE_MAP_PRICE.get(),
                ON_ABANDONED_LOUNGE_MAPS, LOUNGE_MAP_NAME, MapDecorationTypes.RED_X, 12, 5));
    }
}
