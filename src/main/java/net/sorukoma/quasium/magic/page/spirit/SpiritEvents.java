package net.sorukoma.quasium.magic.page.spirit;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.world.WorldLootProvider;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SpiritEvents {
    private SpiritEvents() {
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!WorldLootProvider.ABANDONED_LOUNGE.location().equals(event.getName())) return;
        event.getTable().addPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(SpiritRegistry.SPIRIT_MIX.get()))
                .build());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SpiritMix.forget(event.getEntity().getUUID());
    }
}
