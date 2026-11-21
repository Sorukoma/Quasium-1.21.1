package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.component.ModDataComponents.CalloudData;
import net.sorukoma.quasium.component.ModDataComponents.CharcoalData;
import net.sorukoma.quasium.component.ModDataComponents.CharcoalSize;
import net.sorukoma.quasium.item.CalloudItem;
import net.sorukoma.quasium.item.CharcoalItem;
import net.sorukoma.quasium.screen.CalloudMenu;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class HeatHookahGoal extends GolemErrandGoal {
    private static final int CALLOUD_VOLUME = 12;
    private static final float LIT = 100f;

    HeatHookahGoal(CalloudGolemEntity golem) {
        super(golem, 40, "heat");
    }

    @Override
    protected boolean wanted() {
        return golem.hasCoal();
    }

    @Nullable
    @Override
    protected BlockPos find() {
        ItemStack coal = golem.nextCoal();
        HookahBlockEntity hookah = nearestHookah(golem, candidate -> wantsCoal(candidate, coal));
        return hookah == null ? null : hookah.getBlockPos();
    }

    @Nullable
    static HookahBlockEntity nearestHookah(CalloudGolemEntity golem, Predicate<HookahBlockEntity> wanted) {
        Level level = golem.level();
        BlockPos center = golem.blockPosition();
        int range = Config.COAL_GOLEM_RANGE.get();
        ChunkPos from = new ChunkPos(center.offset(-range, 0, -range));
        ChunkPos to = new ChunkPos(center.offset(range, 0, range));
        HookahBlockEntity nearest = null;
        double nearestDistance = (double) range * range;
        for (int x = from.x; x <= to.x; x++) {
            for (int z = from.z; z <= to.z; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (BlockEntity entity : level.getChunk(x, z).getBlockEntities().values()) {
                    double distance = entity.getBlockPos().distSqr(center);
                    if (distance <= nearestDistance && entity instanceof HookahBlockEntity hookah && wanted.test(hookah)) {
                        nearest = hookah;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    @Override
    protected void arrive(BlockPos pos) {
        ItemStack coal = golem.nextCoal();
        if (!(golem.level() instanceof ServerLevel level) || !(level.getBlockEntity(pos) instanceof HookahBlockEntity hookah)
                || !wantsCoal(hookah, coal)) return;
        ItemStack calloud = hookah.getItemHandler().getStackInSlot(0);
        CalloudData data = contents(calloud);
        boolean room = hasRoom(data, ((CharcoalItem) coal.getItem()).getSize());
        if (room) {
            calloud.set(ModDataComponents.CALLOUD_CONTENTS.get(), withLitCoal(data, coal));
            coal.shrink(1);
        } else {
            calloud.set(ModDataComponents.CALLOUD_CONTENTS.get(), withColdestRelit(data));
        }
        golem.note(room ? "heated" : "relit", pos);
        hookah.setChanged();
        level.sendBlockUpdated(pos, hookah.getBlockState(), hookah.getBlockState(), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, ModSoundEvents.COAL_GOLEM_IGNITE.get(), SoundSource.NEUTRAL, 0.8f, 1f);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
        ServerPlayer owner = golem.onlineOwner();
        if (owner != null) SmokecraftTrigger.fire(owner, "golem_heat");
        golem.jobDone();
    }

    private static boolean wantsCoal(HookahBlockEntity hookah, ItemStack coal) {
        ItemStack calloud = hookah.getItemHandler().getStackInSlot(0);
        if (!(calloud.getItem() instanceof CalloudItem) || !(coal.getItem() instanceof CharcoalItem piece)) return false;
        CalloudData data = contents(calloud);
        float max = data.maxPossibleHeat();
        boolean cooling = max <= 0f || data.totalHeat() < max * Config.COAL_GOLEM_HEAT_PCT.get() / 100f;
        return cooling && (hasRoom(data, piece.getSize()) || coldest(data) != null);
    }

    @Nullable
    private static CharcoalData coldest(CalloudData data) {
        CharcoalData coldest = null;
        for (CharcoalData piece : data.charcoals()) {
            if (piece.remainingTicks() > 0 && piece.heatingProgress() < LIT
                    && (coldest == null || piece.heatingProgress() < coldest.heatingProgress())) coldest = piece;
        }
        return coldest;
    }

    private static CalloudData withColdestRelit(CalloudData data) {
        CharcoalData coldest = coldest(data);
        List<CharcoalData> base = new ArrayList<>();
        for (CharcoalData piece : data.charcoals()) {
            base.add(piece == coldest ? new CharcoalData(piece.size(), piece.remainingTicks(), LIT) : piece);
        }
        return new CalloudData(base, data.stackedCharcoals());
    }

    private static boolean hasRoom(CalloudData data, CharcoalSize size) {
        int volume = size.getVolume();
        for (CharcoalData piece : data.charcoals()) volume += piece.size().getVolume();
        for (CharcoalData piece : data.stackedCharcoals()) volume += piece.size().getVolume();
        boolean freeSlot = data.charcoals().size() < CalloudMenu.BASE_SLOTS || data.stackedCharcoals().size() < data.charcoals().size();
        return freeSlot && volume <= CALLOUD_VOLUME;
    }

    private static CalloudData withLitCoal(CalloudData data, ItemStack coal) {
        CharcoalSize size = ((CharcoalItem) coal.getItem()).getSize();
        CharcoalData piece = coal.getOrDefault(ModDataComponents.CHARCOAL_DATA.get(), new CharcoalData(size, size.getMaxTicks(), 0f));
        List<CharcoalData> base = new ArrayList<>(data.charcoals());
        List<CharcoalData> stacked = new ArrayList<>(data.stackedCharcoals());
        (base.size() < CalloudMenu.BASE_SLOTS ? base : stacked).add(new CharcoalData(size, piece.remainingTicks(), LIT));
        return new CalloudData(base, stacked);
    }

    private static CalloudData contents(ItemStack calloud) {
        return calloud.getOrDefault(ModDataComponents.CALLOUD_CONTENTS.get(), new CalloudData(List.of(), List.of()));
    }
}
