package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.ritual.RitualMath;
import net.sorukoma.quasium.ritual.RitualRecipe;
import net.sorukoma.quasium.ritual.RitualRecipes;

import java.util.ArrayList;
import java.util.List;

public final class RitualStart {
    private RitualStart() {
    }

    public static boolean ignite(ServerLevel level, RitualBrazierBlockEntity brazier, ServerPlayer player) {
        BlockPos pos = brazier.getBlockPos();
        RitualChecklist.Result checklist = RitualChecklist.of(level, brazier, player);
        if (!checklist.ready()) {
            RitualReport.checklist(level, pos, checklist).forEach(player::sendSystemMessage);
            return refuse(player, checklist.refusal().orElse(Component.translatable("quasium.ritual.no_ritual")));
        }
        RitualRecipes.Match match = checklist.match().orElseThrow();
        List<RitualPedestalBlockEntity> pedestals = RitualSite.pedestals(level, pos);
        List<BlockPos> reserved = new ArrayList<>();
        for (int index : match.pedestals()) reserved.add(pedestals.get(index).getBlockPos());
        RitualRecipe recipe = match.recipe();
        brazier.begin(new RitualRun(match.id(), recipe, reserved));
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 0.8f);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.35, 0.1, 0.35, 0.02);
        SmokecraftTrigger.fire(player, "ritual_start");
        player.displayClientMessage(Component.translatable("quasium.ritual.started", RitualRun.displayName(match.id()),
                RitualMath.seconds(recipe.pulseMs())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    private static boolean refuse(ServerPlayer player, Component reason) {
        player.displayClientMessage(reason.copy().withStyle(ChatFormatting.GOLD), true);
        return false;
    }
}
