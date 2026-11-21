package net.sorukoma.quasium.mind.ghost;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.mind.KaifDecay;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.Tolerance;

public final class GhostDeal {
    public static final String EMERALDS = "emeralds";
    public static final String LEVELS = "levels";
    public static final String LEAVE = "leave";
    public static final String SEALED = "ghost_deal";
    private static final double REACH = 8;

    private GhostDeal() {
    }

    public static boolean answer(ServerPlayer player, String choice) {
        HookahGhost ghost = player.serverLevel().getEntitiesOfClass(HookahGhost.class, player.getBoundingBox().inflate(REACH),
                candidate -> candidate.visits(player)).stream().findFirst().orElse(null);
        if (ghost == null) return false;
        if (LEAVE.equals(choice)) {
            ghost.vanish();
            return false;
        }
        if (!pay(player, choice)) return false;
        PlayerKaifData.addKaif(player, MindConfig.GHOST_KAIF_GAIN.getAsInt());
        Tolerance.addAll(player, MindConfig.GHOST_TOLERANCE.getAsInt());
        MindData.setTime(player, KaifDecay.CURSED_UNTIL, player.level().getGameTime() + MindConfig.GHOST_CURSE_TICKS.getAsInt());
        MindData.raise(player, SEALED);
        player.level().playSound(null, ghost.blockPosition(), MindRegistry.GHOST_DEAL.get(), SoundSource.HOSTILE, 1f, 1f);
        player.displayClientMessage(Component.translatable("quasium.ghost.dealt").withStyle(ChatFormatting.DARK_AQUA), false);
        SmokecraftTrigger.fire(player, "ghost_deal");
        ghost.vanish();
        return true;
    }

    private static boolean pay(ServerPlayer player, String choice) {
        if (EMERALDS.equals(choice)) {
            int price = MindConfig.GHOST_PRICE_EMERALDS.getAsInt();
            if (player.getInventory().countItem(Items.EMERALD) < price) {
                player.displayClientMessage(Component.translatable("quasium.ghost.no_emeralds", price), true);
                return false;
            }
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(Items.EMERALD), price, player.inventoryMenu.getCraftSlots());
            return true;
        }
        if (LEVELS.equals(choice)) {
            int price = MindConfig.GHOST_PRICE_LEVELS.getAsInt();
            if (player.experienceLevel < price) {
                player.displayClientMessage(Component.translatable("quasium.ghost.no_levels", price), true);
                return false;
            }
            player.giveExperienceLevels(-price);
            return true;
        }
        return false;
    }
}
