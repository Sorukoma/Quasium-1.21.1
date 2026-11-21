package net.sorukoma.quasium.mind;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerTobaccoData;
import net.sorukoma.quasium.mind.ghost.GhostDeal;
import net.sorukoma.quasium.smokelands.sultan.SultanVows;

import java.util.Locale;
import java.util.function.Predicate;

public enum Titles {
    SOMMELIER(player -> PlayerTobaccoData.getTriedFlavors(player).size() >= MindConfig.TITLE_SOMMELIER_FLAVORS.getAsInt()),
    GENETICIST(Titles::allHybrids),
    IRON_LUNGS(player -> MindStats.get(player, MindRegistry.PUFFS) >= MindConfig.TITLE_IRON_LUNGS_PUFFS.getAsInt()),
    NIGHT_GUEST(player -> MindData.flag(player, GhostDeal.SEALED)),
    HOOKAH_TOURIST(Titles::touredTheWorld),
    SULTAN_OUTSMOKED(player -> MindData.flag(player, SultanVows.OUTSMOKED));

    public static final ResourceLocation TOURIST = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mind/hookah_tourist");
    private static final ResourceLocation WILD_PAIRS = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokecraft/hybrid_wild_pairs");

    private final Predicate<ServerPlayer> earned;

    Titles(Predicate<ServerPlayer> earned) {
        this.earned = earned;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean earnedBy(ServerPlayer player) {
        return earned.test(player);
    }

    static void tick(ServerPlayer player) {
        for (Titles title : values()) {
            if (!MindData.has(player, MindData.TITLES, title.id()) && title.earnedBy(player)) {
                MindData.add(player, MindData.TITLES, title.id());
                player.displayClientMessage(Component.translatable("quasium.title.earned",
                        Component.translatable("quasium.title." + title.id())).withStyle(ChatFormatting.GOLD), false);
                SmokecraftTrigger.fire(player, "title:" + title.id());
            }
        }
    }

    private static boolean allHybrids(ServerPlayer player) {
        return done(player, WILD_PAIRS);
    }

    private static boolean touredTheWorld(ServerPlayer player) {
        return done(player, TOURIST);
    }

    private static boolean done(ServerPlayer player, ResourceLocation advancement) {
        AdvancementHolder holder = player.server.getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
