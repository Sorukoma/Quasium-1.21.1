package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.screen.HookahBowlMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

final class RefillBowlGoal extends GolemErrandGoal {
    private static final int FULL_GRAMS = 15;

    @Nullable
    private BlockPos hookah;
    @Nullable
    private BlockPos store;
    private boolean topUp;

    RefillBowlGoal(CalloudGolemEntity golem) {
        super(golem, 60, "refill");
    }

    @Override
    protected boolean wanted() {
        return !golem.refillPaused();
    }

    @Nullable
    @Override
    protected BlockPos find() {
        if (golem.takeRefillRequest()) topUp = true;
        HookahBlockEntity found = HeatHookahGoal.nearestHookah(golem, candidate -> wantsTobacco(candidate, topUp));
        if (found == null) {
            topUp = false;
            return null;
        }
        hookah = found.getBlockPos();
        store = storeFor(BowlTobacco.contents(bowl(found)));
        if (store == null) golem.waitFor("no_tobacco");
        return store;
    }

    @Override
    protected void arrive(BlockPos pos) {
        if (pos.equals(hookah)) fill(pos);
        else restock(pos);
    }

    private void restock(BlockPos pos) {
        IItemHandler items = store(pos);
        if (items == null) return;
        int ash = golem.unloadSatchel(items);
        if (ash > 0) golem.note("ash_stored", ash, pos);
        if (hookah == null || !golem.level().isLoaded(hookah) || !(golem.level().getBlockEntity(hookah) instanceof HookahBlockEntity target)) return;
        ItemStack bowl = bowl(target);
        if (bowl.isEmpty()) return;
        HookahBowlContents contents = BowlTobacco.contents(bowl);
        ItemStack grams = BowlTobacco.take(golem, items, contents, FULL_GRAMS - contents.filledSlotCount());
        if (grams.isEmpty()) return;
        golem.carry(grams);
        golem.note("took_tobacco", grams.getCount(), pos);
        then(hookah);
    }

    private void fill(BlockPos pos) {
        hookah = null;
        if (!(golem.level() instanceof ServerLevel level) || !(level.getBlockEntity(pos) instanceof HookahBlockEntity target)) return;
        ItemStack bowl = bowl(target);
        if (bowl.isEmpty()) return;
        HookahBowlContents old = BowlTobacco.contents(bowl);
        HookahBowlContents contents = new HookahBowlContents(old.groups(), old.slotAssignments(), Set.of(), old.heated());
        ItemStack grams = golem.tobacco();
        BowlTobaccoGroup kind = HookahBowlMenu.groupOf(grams);
        int added = 0;
        for (int slot = 0; kind != null && slot < HookahBowlContents.MAX_SLOTS && added < grams.getCount()
                && contents.filledSlotCount() < FULL_GRAMS; slot++) {
            if (contents.slotAssignments().containsKey(slot)) continue;
            HookahBowlContents next = contents.addSlot(slot, kind);
            if (!next.isCompatible()) break;
            contents = next;
            added++;
        }
        int ash = old.burnedSlots().size();
        if (added == 0 && ash == 0) return;
        ItemStack shown = added > 0 ? grams.copyWithCount(1) : new ItemStack(ModItems.BURNED_TOBACCO.get());
        grams.shrink(added);
        golem.addAsh(ash);
        ItemStack refilled = bowl.copy();
        refilled.set(ModDataComponents.HOOKAH_BOWL_CONTENTS.get(), added > 0 ? contents.withHeated(false) : contents);
        target.getItemHandler().setStackInSlot(1, refilled);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.NEUTRAL, 0.8f, 1.1f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, shown), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.2, 0.1, 0.2, 0.05);
        if (added > 0) {
            golem.note("refilled", added, pos);
            golem.jobDone();
        }
        if (golem.carriesSatchel() && store != null) then(store);
    }

    @Nullable
    private BlockPos storeFor(HookahBowlContents contents) {
        for (BlockPos mark : golem.marks(MarkType.FILL)) {
            for (BlockPos pos : SmokeMarks.stores(golem.level(), mark)) {
                IItemHandler items = store(pos);
                if (items != null && (golem.carriesSatchel() || BowlTobacco.holds(items, contents))) return pos;
            }
        }
        return null;
    }

    private static ItemStack bowl(HookahBlockEntity hookah) {
        ItemStack bowl = hookah.getItemHandler().getStackInSlot(1);
        return bowl.is(ModItems.HOOKAH_BOWL.get()) ? bowl : ItemStack.EMPTY;
    }

    private static boolean wantsTobacco(HookahBlockEntity hookah, boolean topUp) {
        ItemStack bowl = bowl(hookah);
        int grams = BowlTobacco.contents(bowl).filledSlotCount();
        return !bowl.isEmpty() && (grams == 0 || topUp && grams < FULL_GRAMS);
    }
}
