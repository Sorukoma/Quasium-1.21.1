package net.sorukoma.quasium.smokelands.sultan;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.ritual.brazier.RitualPedestalBlockEntity;
import net.sorukoma.quasium.ritual.brazier.RitualSite;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.item.SmokelandsItems;
import net.sorukoma.quasium.tea.SealBlocks;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class SultanVows {
    public static final String OUTSMOKED = "sultan_outsmoked";

    public enum Vow {
        QUARTZ(RitualRegistry.DREAM_QUARTZ),
        COAL(RitualRegistry.ETERNAL_COAL),
        ASH(SmokelandsItems.ASH_OF_TIME),
        SEAL(SealBlocks.SOBRIETY_SEAL_ITEM);

        private final ItemLike item;

        Vow(ItemLike item) {
            this.item = item;
        }

        public Item item() {
            return item.asItem();
        }
    }

    private SultanVows() {
    }

    public static SultanFight.Rules rules(Set<Vow> vows) {
        return SultanOfSmoke.rules().hastened(vows.contains(Vow.QUARTZ) ? SmokelandsConfig.VOW_INHALE_PCT.get() : 100,
                vows.contains(Vow.COAL) ? SmokelandsConfig.VOW_WINDOW_PCT.get() : 100);
    }

    public static boolean all(Set<Vow> vows) {
        return vows.size() == Vow.values().length;
    }

    public static int mask(Set<Vow> vows) {
        int mask = 0;
        for (Vow vow : vows) mask |= 1 << vow.ordinal();
        return mask;
    }

    public static EnumSet<Vow> of(int mask) {
        EnumSet<Vow> vows = EnumSet.noneOf(Vow.class);
        for (Vow vow : Vow.values()) {
            if ((mask & 1 << vow.ordinal()) != 0) vows.add(vow);
        }
        return vows;
    }

    public static Map<Vow, RitualPedestalBlockEntity> placed(Level level, BlockPos brazier) {
        Map<Vow, RitualPedestalBlockEntity> placed = new EnumMap<>(Vow.class);
        for (RitualPedestalBlockEntity pedestal : RitualSite.pedestals(level, brazier)) {
            Arrays.stream(Vow.values()).filter(vow -> pedestal.item().is(vow.item())).findFirst()
                    .ifPresent(vow -> placed.putIfAbsent(vow, pedestal));
        }
        return placed;
    }

    public static void burn(ServerLevel level, Collection<RitualPedestalBlockEntity> pedestals) {
        for (RitualPedestalBlockEntity pedestal : pedestals) {
            pedestal.setItem(ItemStack.EMPTY);
            Vec3 top = Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.7, 0);
            level.sendParticles(ParticleTypes.FLAME, top.x, top.y, top.z, 12, 0.15, 0.2, 0.15, 0.02);
            level.playSound(null, pedestal.getBlockPos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 0.7f);
        }
    }

    public static Component names(Collection<Vow> vows) {
        return ComponentUtils.formatList(vows.stream().map(vow -> vow.item().getDescription()).toList(), Component.literal(", "));
    }

    public static Component report(Level level, BlockPos brazier) {
        Set<Vow> vows = placed(level, brazier).keySet();
        if (vows.isEmpty()) {
            return Component.translatable("quasium.ritual.report.no_vows", names(Arrays.asList(Vow.values()))).withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("quasium.ritual.report.vows", names(vows)).withStyle(all(vows) ? ChatFormatting.RED : ChatFormatting.GOLD);
    }

    public static boolean sealsFlask(ServerPlayer player) {
        double reach = SmokelandsConfig.ARENA_RADIUS.get() * 2.0;
        return !player.serverLevel().getEntitiesOfClass(SultanOfSmoke.class, player.getBoundingBox().inflate(reach),
                sultan -> sultan.isAlive() && sultan.vows().contains(Vow.SEAL) && sultan.arena().contains(player.position())).isEmpty();
    }
}
