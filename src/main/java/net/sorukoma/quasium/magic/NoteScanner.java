package net.sorukoma.quasium.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.mind.MindStats;
import net.sorukoma.quasium.mind.Perception;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class NoteScanner {
    public enum Kind { ITEM, BLOCK, MOB }

    public static final String SCHOLAR_EVENT = "notes_100";
    public static final int SCHOLAR_NOTES = 100;

    private NoteScanner() {
    }

    public static void scanItem(ServerPlayer player, ItemStack stack, Kind kind) {
        if (stack.isEmpty()) return;
        scan(player, NoteProfiles.forStack(stack), kind, stack.getHoverName());
    }

    public static void scanEntity(ServerPlayer player, LivingEntity entity) {
        scan(player, NoteProfiles.forEntity(entity.getType()), Kind.MOB, entity.getName());
    }

    private static void scan(ServerPlayer player, @Nullable Map.Entry<ResourceLocation, NoteProfile> profile, Kind kind, Component name) {
        int kaifNeeded = kind == Kind.MOB ? Config.SCAN_KAIF_MOB.getAsInt() : Config.SCAN_KAIF_ITEM.getAsInt();
        if (PlayerKaifData.getKaifValue(player) < kaifNeeded) {
            player.displayClientMessage(Component.translatable("quasium.message.quasium_ore.low_kaif"), true);
            return;
        }
        Perception.scanned(player, kind == Kind.MOB);
        if (profile == null) {
            player.displayClientMessage(Component.translatable("quasium.scan.nothing", name), true);
            return;
        }
        String profileId = profile.getKey().toString();
        boolean first = PlayerMagicData.addScanned(player, profileId);
        player.displayClientMessage(Component.translatable("quasium.scan.result", name, profile.getValue().describe()), false);
        player.level().playSound(null, player.blockPosition(), first ? MindRegistry.SCAN_REVEAL.get() : ModSoundEvents.SCANNER_PING.get(),
                SoundSource.PLAYERS, 0.8f, 1f);
        if (handMadeScanned(player) >= SCHOLAR_NOTES) SmokecraftTrigger.fire(player, SCHOLAR_EVENT);
        if (!first || DerivedNotes.isDerived(profileId)) return;
        MindStats.award(player, MindRegistry.NOTES_OPENED, 1);
        int gained = PlayerMagicData.addInsight(player, insightFor(kind));
        if (gained > 0) {
            player.displayClientMessage(Component.translatable("quasium.scan.insight", gained, PlayerMagicData.getInsight(player))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        SmokecraftTrigger.fire(player, "scan");
        if (kind == Kind.MOB) SmokecraftTrigger.fire(player, "scan_mob");
    }

    public static long handMadeScanned(ServerPlayer player) {
        return PlayerMagicData.getScanned(player).stream().filter(id -> !DerivedNotes.isDerived(id)).count();
    }

    private static int insightFor(Kind kind) {
        return switch (kind) {
            case ITEM -> Config.INSIGHT_PER_ITEM.getAsInt();
            case BLOCK -> Config.INSIGHT_PER_BLOCK.getAsInt();
            case MOB -> Config.INSIGHT_PER_MOB.getAsInt();
        };
    }
}
