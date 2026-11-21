package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.entity.DryingShelfBlockEntity;
import net.sorukoma.quasium.block.entity.FermentationBarrelBlockEntity;
import net.sorukoma.quasium.block.entity.SoakingBowlBlockEntity;
import net.sorukoma.quasium.block.entity.TobaccoAutomation;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.TobaccoVarieties;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

final class CuringGoal extends ClayChoreGoal {
    private static final int DRIED = 2;

    private final MixingChores mixing;
    private boolean starved;
    private boolean bowlStarved;
    private boolean shelfSeen;

    CuringGoal(ClayGolemEntity golem) {
        super(golem, GolemJob.TOBACCO);
        this.mixing = new MixingChores(golem);
    }

    @Override
    protected void noZone() {
        if (hasFreshLeaves()) golem.waitFor("leaves_no_shelf");
    }

    @Nullable
    @Override
    protected Chore next(List<BlockPos> zones) {
        Level level = golem.level();
        starved = false;
        bowlStarved = false;
        shelfSeen = false;
        for (BlockPos zone : zones) {
            BoundingBox box = ZoneBox.of(level, zone);
            if (!ZoneBox.loaded(level, box)) continue;
            List<BlockEntity> racks = ZoneBox.blockEntities(level, box).stream().filter(rack -> !golem.isUnreachable(rack.getBlockPos()))
                    .sorted(Comparator.comparingDouble(rack -> golem.distanceToSqr(rack.getBlockPos().getCenter()))).toList();
            shelfSeen |= racks.stream().anyMatch(DryingShelfBlockEntity.class::isInstance);
            Chore chore = golem.hasRoom() ? collect(zone, racks) : null;
            if (chore == null) chore = load(zone, racks);
            if (chore != null) return chore;
        }
        if (!shelfSeen && hasFreshLeaves()) golem.waitFor("leaves_no_shelf");
        else if (starved) golem.waitFor("no_fresh_leaves");
        else if (bowlStarved) golem.waitFor("bowl_no_supplies");
        return null;
    }

    static Predicate<ItemStack> kept(ClayGolemEntity golem) {
        Predicate<ItemStack> kept = stack -> false;
        for (BlockPos zone : golem.marks(MarkType.ZONE)) {
            BoundingBox box = ZoneBox.of(golem.level(), zone);
            if (!ZoneBox.loaded(golem.level(), box)) continue;
            List<BlockEntity> racks = ZoneBox.blockEntities(golem.level(), box);
            if (racks.stream().anyMatch(DryingShelfBlockEntity.class::isInstance)) kept = kept.or(TobaccoAutomation::freshLeaf);
            if (racks.stream().anyMatch(SoakingBowlBlockEntity.class::isInstance)) kept = kept.or(MixingChores::kept);
        }
        return kept;
    }

    private boolean hasFreshLeaves() {
        return ClayStores.slotOf(golem.cargo(), TobaccoAutomation::freshLeaf) >= 0 || ClayStores.holding(golem, TobaccoAutomation::freshLeaf) != null;
    }

    private static boolean dried(ItemStack stack) {
        return TobaccoVarieties.isLeaf(stack) && !stack.has(ModDataComponents.FERMENTED.get())
                && stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0) == DRIED;
    }

    private static boolean takeOff(ItemStack stack) {
        return TobaccoVarieties.isLeaf(stack) && stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0) >= DRIED;
    }

    @Nullable
    private Chore collect(BlockPos zone, List<BlockEntity> racks) {
        for (BlockEntity rack : racks) {
            BlockPos pos = rack.getBlockPos();
            if (rack instanceof FermentationBarrelBlockEntity barrel && barrel.isReady()) {
                return Chore.at(zone, pos, () -> barrel.isReady() && !barrel.isRemoved(), () -> {
                    ItemStack result = barrel.takeResult();
                    if (!result.isEmpty()) golem.note("took_fermented", result.getCount(), result.getDescriptionId());
                    golem.takeIn(List.of(result), pos);
                    return true;
                });
            }
            if (rack instanceof DryingShelfBlockEntity shelf && takeOff(leaf(shelf))) {
                return Chore.at(zone, pos, () -> takeOff(leaf(shelf)) && !shelf.isRemoved(), () -> {
                    ItemStack dried = shelf.getItemHandler().extractItem(0, 1, false);
                    golem.note("took_dried", dried.getDescriptionId());
                    golem.takeIn(List.of(dried), pos);
                    return true;
                });
            }
            Chore mixed = mixing.collect(zone, rack);
            if (mixed != null) return mixed;
        }
        return null;
    }

    @Nullable
    private Chore load(BlockPos zone, List<BlockEntity> racks) {
        IItemHandler cargo = golem.cargo();
        int emptyShelves = 0;
        Predicate<ItemStack> bowlNeeds = null;
        for (BlockEntity rack : racks) {
            if (rack instanceof FermentationBarrelBlockEntity barrel && opens(barrel)) {
                return Chore.at(zone, rack.getBlockPos(), () -> opens(barrel) && !barrel.isRemoved(), () -> ferment(barrel));
            }
            if (rack instanceof DryingShelfBlockEntity shelf && leaf(shelf).isEmpty()) {
                if (ClayStores.slotOf(cargo, TobaccoAutomation::freshLeaf) >= 0) {
                    return Chore.at(zone, rack.getBlockPos(), () -> leaf(shelf).isEmpty() && !shelf.isRemoved(), () -> shelve(shelf));
                }
                emptyShelves++;
            }
            Chore poured = mixing.load(zone, rack);
            if (poured != null) return poured;
            if (bowlNeeds == null) bowlNeeds = mixing.missing(rack);
        }
        if (!golem.hasRoom()) return null;
        Chore chore = emptyShelves > 0 ? fetch(zone, TobaccoAutomation::freshLeaf, emptyShelves, "took_leaves") : null;
        starved |= emptyShelves > 0 && chore == null;
        if (chore != null || bowlNeeds == null) return chore;
        chore = fetch(zone, bowlNeeds, 1, "took_for_bowl");
        bowlStarved |= chore == null;
        return chore;
    }

    private static ItemStack leaf(DryingShelfBlockEntity shelf) {
        return shelf.getItemHandler().getStackInSlot(0);
    }

    private boolean opens(FermentationBarrelBlockEntity barrel) {
        ItemStackHandler slot = barrel.getItemHandler();
        ItemStack inside = slot.getStackInSlot(0);
        return !barrel.isFermenting() && !barrel.isReady() && inside.getCount() < slot.getSlotLimit(0)
                && ClayStores.slotOf(golem.cargo(), stack -> dried(stack) && FermentationBarrelBlockEntity.sameBatch(inside, stack)) >= 0;
    }

    private boolean ferment(FermentationBarrelBlockEntity barrel) {
        IItemHandler cargo = golem.cargo();
        ItemStackHandler slot = barrel.getItemHandler();
        ItemStack inside = slot.getStackInSlot(0).copy();
        for (int index = 0; index < cargo.getSlots() && inside.getCount() < slot.getSlotLimit(0); index++) {
            ItemStack stack = cargo.getStackInSlot(index);
            if (!dried(stack) || !FermentationBarrelBlockEntity.sameBatch(inside, stack)) continue;
            ItemStack taken = cargo.extractItem(index, slot.getSlotLimit(0) - inside.getCount(), false);
            inside = inside.isEmpty() ? taken : inside.copyWithCount(inside.getCount() + taken.getCount());
        }
        if (inside.isEmpty()) return true;
        slot.setStackInSlot(0, inside);
        barrel.startFermentation();
        golem.note("barrel_started", inside.getCount(), inside.getDescriptionId());
        return true;
    }

    private boolean shelve(DryingShelfBlockEntity shelf) {
        int slot = ClayStores.slotOf(golem.cargo(), TobaccoAutomation::freshLeaf);
        if (slot < 0) return true;
        ItemStack leaf = golem.cargo().extractItem(slot, 1, false);
        shelf.getItemHandler().setStackInSlot(0, leaf);
        golem.note("shelved", leaf.getDescriptionId());
        return true;
    }

    @Nullable
    private Chore fetch(BlockPos zone, Predicate<ItemStack> wanted, int count, String event) {
        BlockPos store = ClayStores.holding(golem, wanted);
        if (store == null) return null;
        return Chore.at(zone, store, () -> ClayStores.holds(golem, store, wanted), () -> {
            ItemStack taken = ClayStores.take(golem, store, wanted, count);
            if (!taken.isEmpty()) golem.note(event, taken.getCount(), taken.getDescriptionId());
            return true;
        });
    }
}
