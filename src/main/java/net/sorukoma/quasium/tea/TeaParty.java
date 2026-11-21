package net.sorukoma.quasium.tea;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import net.sorukoma.quasium.ecology.SmogCleansing;
import net.sorukoma.quasium.ecology.SmogMath;
import net.sorukoma.quasium.ecology.SmogVillagers;
import net.sorukoma.quasium.particle.ModParticles;

import java.util.List;

public final class TeaParty {
    private static final int TEA_COLOR = 0xC89A58;
    public static final int CALM_EVERY_TICKS = 20;
    private static final int RING_EVERY_TICKS = 10;
    private static final int RING_PARTICLES = 6;
    private static final int CUSHION_HEIGHT = 2;

    private TeaParty() {
    }

    private static int cushions(Level level, BlockPos samovar) {
        int radius = EcologyConfig.TEA_PARTY_RADIUS.getAsInt();
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(samovar.offset(-radius, -CUSHION_HEIGHT, -radius),
                samovar.offset(radius, CUSHION_HEIGHT, radius))) {
            if (pos.distSqr(samovar) <= (double) radius * radius && level.getBlockState(pos).is(LoungeBlocks.LOUNGE_CUSHION.get())) found++;
        }
        return found;
    }

    public static void tryStart(ServerPlayer host, SamovarBlockEntity samovar, ItemStack teapot) {
        if (samovar.partyTicks() > 0) {
            host.displayClientMessage(Component.translatable("quasium.tea_party.running", samovar.partyTicks() / SmogMath.TICKS_PER_MINUTE + 1), true);
            return;
        }
        if (!TeapotItem.isFull(teapot)) {
            host.displayClientMessage(Component.translatable("quasium.tea_party.need_full_teapot", EcologyConfig.TEAPOT_CUPS.getAsInt()), true);
            return;
        }
        int missing = EcologyConfig.TEA_PARTY_CUSHIONS.getAsInt() - cushions(host.serverLevel(), samovar.getBlockPos());
        if (missing > 0) {
            host.displayClientMessage(Component.translatable("quasium.tea_party.need_cushions", missing, EcologyConfig.TEA_PARTY_RADIUS.getAsInt()), true);
            return;
        }
        TeapotItem.setCups(teapot, 0);
        start(host.serverLevel(), samovar);
    }

    private static void start(ServerLevel level, SamovarBlockEntity samovar) {
        BlockPos pos = samovar.getBlockPos();
        int ticks = EcologyConfig.TEA_PARTY_MINUTES.getAsInt() * SmogMath.TICKS_PER_MINUTE;
        samovar.setPartyTicks(ticks);
        level.playSound(null, pos, EcologyRegistry.TEA_PARTY_CHIME.get(), SoundSource.BLOCKS, 1f, 1f);
        List<ServerPlayer> guests = guests(level, pos);
        samovar.setVillageThanked(SmogCleansing.cleanse(level, pos, EcologyConfig.TEA_PARTY_SMOG_CLEANSE.getAsInt()) > 0
                && SmogVillagers.thankTeaParty(level, pos, guests, ticks));
        for (ServerPlayer guest : guests) {
            SmokecraftTrigger.fire(guest, "tea_party");
            guest.displayClientMessage(Component.translatable("quasium.tea_party.started", EcologyConfig.TEA_PARTY_MINUTES.getAsInt()), true);
        }
    }

    public static void tick(ServerLevel level, SamovarBlockEntity samovar) {
        int left = samovar.partyTicks() - 1;
        samovar.setPartyTicks(left);
        BlockPos pos = samovar.getBlockPos();
        if (left % RING_EVERY_TICKS == 0) ring(level, pos);
        if (left % CALM_EVERY_TICKS != 0) return;
        List<ServerPlayer> guests = guests(level, pos);
        serve(left, guests);
        if (samovar.villageThanked() && left > 0) SmogVillagers.welcomeLateGuests(guests, left);
    }

    public static void serve(int ticksLeft, List<? extends ServerPlayer> guests) {
        int perMinute = EcologyConfig.TEA_PARTY_KAIF_PER_MINUTE.getAsInt();
        boolean soothe = perMinute > 0 && ticksLeft % SmogMath.ticksBetween(perMinute) < CALM_EVERY_TICKS;
        for (ServerPlayer guest : guests) {
            guest.removeEffect(MobEffects.CONFUSION);
            if (soothe && PlayerKaifData.getKaifValue(guest) < 0) PlayerKaifData.addKaif(guest, 1);
        }
    }

    private static List<ServerPlayer> guests(ServerLevel level, BlockPos samovar) {
        double radius = EcologyConfig.TEA_PARTY_RADIUS.getAsInt();
        Vec3 center = Vec3.atCenterOf(samovar);
        return level.players().stream().filter(player -> !player.isSpectator() && player.distanceToSqr(center) <= radius * radius).toList();
    }

    private static void ring(ServerLevel level, BlockPos samovar) {
        double radius = EcologyConfig.TEA_PARTY_RADIUS.getAsInt();
        Vec3 center = Vec3.atCenterOf(samovar);
        for (int i = 0; i < RING_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            level.sendParticles(ModParticles.noteSmoke(TEA_COLOR), center.x + Math.cos(angle) * radius, center.y + level.random.nextDouble() - 0.3,
                    center.z + Math.sin(angle) * radius, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
