package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.page.PageRegistry;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SultanRewards {
    public static final ResourceLocation BOTTOMLESS_FLASK = SmokelandsDimension.id("bottomless_flask");
    public static final ResourceLocation DREAM_QUARTZ = SmokelandsDimension.id("dream_quartz");
    public static final ResourceLocation ETERNAL_COAL = SmokelandsDimension.id("eternal_coal");

    private SultanRewards() {
    }

    public static List<ItemStack> loot(List<ServerPlayer> survivors, Set<SultanVows.Vow> vows) {
        List<ItemStack> loot = new ArrayList<>();
        item(BOTTOMLESS_FLASK).filter(flask -> survivors.stream().noneMatch(player -> player.getInventory().hasAnyMatching(stack -> stack.is(flask))))
                .ifPresent(flask -> loot.add(new ItemStack(flask)));
        survivors.stream().filter(player -> !player.getInventory().hasAnyMatching(stack -> stack.is(PageRegistry.LARGE_SPELL_TETRAD.get())))
                .forEach(player -> loot.add(new ItemStack(PageRegistry.LARGE_SPELL_TETRAD.get())));
        item(DREAM_QUARTZ).ifPresent(quartz -> loot.add(new ItemStack(quartz, SmokelandsConfig.REWARD_DREAM_QUARTZ.get())));
        item(ETERNAL_COAL).ifPresent(coal -> loot.add(new ItemStack(coal)));
        loot.add(new ItemStack(SmokelandsItems.MUSIC_DISC_ASH_LOFI.get()));
        if (vows.isEmpty()) return loot;
        item(DREAM_QUARTZ).ifPresent(quartz -> loot.add(new ItemStack(quartz, vows.size() * SmokelandsConfig.VOW_DREAM_QUARTZ.get())));
        loot.add(new ItemStack(SmokelandsItems.ASH_OF_TIME.get(), vows.size()));
        return loot;
    }

    private static Optional<Item> item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id);
    }

    static void drop(SultanOfSmoke sultan, ServerLevel level) {
        loot(sultan.arenaPlayers(), sultan.vows()).forEach(stack -> sultan.spawnAtLocation(stack, 1f));
    }

    static void defeated(SultanOfSmoke sultan, ServerLevel level) {
        SultanArena.of(level).defeated(sultan.home(), level.getGameTime());
        Component news = Component.translatable("quasium.sultan.defeated", SmokelandsConfig.RESPAWN_DAYS.get());
        boolean outsmoked = SultanVows.all(sultan.vows());
        for (ServerPlayer player : sultan.arenaPlayers()) {
            SmokecraftTrigger.fire(player, "sultan_defeated");
            player.displayClientMessage(news, false);
            if (outsmoked) MindData.raise(player, SultanVows.OUTSMOKED);
        }
    }
}
