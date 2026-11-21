package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.MixingBlocks;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.item.FocusUpgradeItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.FocusUpgrade;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.screen.MixingTableMenu;

import java.util.ArrayList;
import java.util.List;

public class MixingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ESSENCE_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int BOWL_SLOT = 2;
    public static final int UPGRADE_SLOT = 0;
    public static final int FOCUS_SLOT = 1;
    public static final int BOWL_VIEW_SIZE = FOCUS_SLOT + FocusEssence.MAX_SLOTS;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == BOWL_SLOT) showBowl();
            setChanged();
        }

        @Override
        protected void onLoad() {
            showBowl();
        }
    };

    private final ItemStackHandler bowlView = new ItemStackHandler(BOWL_VIEW_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!showingBowl) writeBowl();
        }
    };

    private boolean showingBowl;

    public MixingTableBlockEntity(BlockPos pos, BlockState state) {
        super(MixingBlocks.MIXING_TABLE_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ItemStackHandler getBowlView() {
        return bowlView;
    }

    private void showBowl() {
        ItemStack bowl = items.getStackInSlot(BOWL_SLOT);
        FocusUpgrade upgrade = FocusUpgrade.of(bowl);
        List<FocusEssence> focus = Spells.focusOf(bowl);
        showingBowl = true;
        bowlView.setStackInSlot(UPGRADE_SLOT, upgrade == null ? ItemStack.EMPTY : new ItemStack(upgradeItem(upgrade)));
        for (int i = 0; i < FocusEssence.MAX_SLOTS; i++) {
            bowlView.setStackInSlot(FOCUS_SLOT + i, i < focus.size() ? bottleOf(focus.get(i)) : ItemStack.EMPTY);
        }
        showingBowl = false;
    }

    private void writeBowl() {
        ItemStack bowl = items.getStackInSlot(BOWL_SLOT);
        if (bowl.isEmpty()) return;
        List<FocusEssence> before = Spells.focusOf(bowl);
        List<FocusEssence> focus = new ArrayList<>(FocusEssence.MAX_SLOTS);
        for (int i = 0; i < FocusEssence.MAX_SLOTS; i++) {
            ItemStack stack = bowlView.getStackInSlot(FOCUS_SLOT + i);
            boolean drained = stack.is(Items.GLASS_BOTTLE) && i < before.size();
            focus.add(drained ? before.get(i) : entryOf(stack));
        }
        bowl.set(ModDataComponents.BOWL_FOCUS.get(), focus);
        if (bowlView.getStackInSlot(UPGRADE_SLOT).getItem() instanceof FocusUpgradeItem upgrade) {
            bowl.set(ModDataComponents.BOWL_UPGRADE.get(), upgrade.upgrade().id());
        } else {
            bowl.remove(ModDataComponents.BOWL_UPGRADE.get());
        }
        setChanged();
    }

    private static ItemStack bottleOf(FocusEssence entry) {
        if (entry.essence().isBlank()) return ItemStack.EMPTY;
        return entry.ml() > 0 ? EssenceBottleItem.of(entry.essence(), entry.ml()) : new ItemStack(Items.GLASS_BOTTLE);
    }

    private static FocusEssence entryOf(ItemStack bottle) {
        String essence = EssenceBottleItem.typeOf(bottle);
        return essence.isBlank() ? new FocusEssence("", 0) : new FocusEssence(essence, EssenceBottleItem.amountOf(bottle));
    }

    private static Item upgradeItem(FocusUpgrade upgrade) {
        return switch (upgrade) {
            case RESONATOR -> ModItems.FOCUS_UPGRADE_RESONATOR.get();
            case COLLIMATOR -> ModItems.FOCUS_UPGRADE_COLLIMATOR.get();
            case COOLER -> ModItems.FOCUS_UPGRADE_COOLER.get();
            case SPLITTER -> ModItems.FOCUS_UPGRADE_SPLITTER.get();
            case CONDENSER -> ModItems.FOCUS_UPGRADE_CONDENSER.get();
            case FILTER -> ModItems.FOCUS_UPGRADE_FILTER.get();
        };
    }

    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.mixing_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MixingTableMenu(containerId, inventory, this);
    }
}
