package net.sorukoma.quasium.mind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class MindStats {
    public static final String PUFF = "puff";
    public static final List<DeferredHolder<ResourceLocation, ResourceLocation>> ALL = List.of(MindRegistry.GRAMS_SMOKED,
            MindRegistry.PUFFS, MindRegistry.CASTS, MindRegistry.ZONE_MINUTES, MindRegistry.OVERDOSES, MindRegistry.NOTES_OPENED,
            MindRegistry.HYBRIDS_FOUND);

    private MindStats() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ALL.forEach(stat ->
                Stats.CUSTOM.get(stat.get(), stat == MindRegistry.GRAMS_SMOKED ? StatFormatter.DIVIDE_BY_TEN : StatFormatter.DEFAULT)));
    }

    public static void award(ServerPlayer player, DeferredHolder<ResourceLocation, ResourceLocation> stat, int amount) {
        if (amount > 0) player.awardStat(stat.get(), amount);
    }

    public static void puff(ServerPlayer player) {
        award(player, MindRegistry.PUFFS, 1);
        SmokecraftTrigger.fire(player, PUFF);
    }

    public static int get(ServerPlayer player, DeferredHolder<ResourceLocation, ResourceLocation> stat) {
        return player.getStats().getValue(Stats.CUSTOM.get(stat.get()));
    }

    public static CompoundTag snapshot(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        ALL.forEach(stat -> tag.putInt(stat.getId().getPath(), get(player, stat)));
        return tag;
    }
}
