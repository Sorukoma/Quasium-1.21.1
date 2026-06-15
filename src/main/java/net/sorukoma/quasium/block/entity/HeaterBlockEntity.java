package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.CharcoalItem;
import net.sorukoma.quasium.screen.HeaterMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeaterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CHARCOAL_SLOTS = 15;
    public static final int FUEL_SLOT = 15;
    public static final int TOTAL_SLOTS = 16;

    // Топливо: 1 ванильный charcoal = 1600 тиков (80 сек), как в vanilla furnace
    public static final int FUEL_BURN_TIME = 1600;

    private int fuelBurnTimeRemaining = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FUEL_SLOT) return stack.getItem() == Items.CHARCOAL;
            return stack.getItem() instanceof CharcoalItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide()) {
                setChanged();
            }
        }
    };

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }
    public int getFuelBurnTimeRemaining() { return fuelBurnTimeRemaining; }
    public int getFuelBurnTimeMax() { return FUEL_BURN_TIME; }

    public static void tick(Level level, BlockPos pos, BlockState state, HeaterBlockEntity be) {
        if (level.isClientSide()) return;

        // Потребление топлива
        if (be.fuelBurnTimeRemaining > 0) {
            be.fuelBurnTimeRemaining -= 1;
        }

        // Есть ли уголь, который ещё нужно греть? (иначе нет смысла жечь топливо)
        boolean hasCoalToHeat = false;
        for (int i = 0; i < CHARCOAL_SLOTS; i++) {
            ItemStack stack = be.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof CharcoalItem ci) {
                ModDataComponents.CharcoalData d = stack.getOrDefault(ModDataComponents.CHARCOAL_DATA.get(),
                        new ModDataComponents.CharcoalData(ci.getSize(), ci.getSize().getMaxTicks(), 0f));
                if (d.heatingProgress() < 100f && d.remainingTicks() > 0) {
                    hasCoalToHeat = true;
                    break;
                }
            }
        }

        // Авто-рефилл топлива из слота — только если есть что греть
        boolean structuralChange = false;
        if (be.fuelBurnTimeRemaining <= 0 && hasCoalToHeat && FUEL_SLOT < be.itemHandler.getSlots()) {
            ItemStack fuelStack = be.itemHandler.getStackInSlot(FUEL_SLOT);
            if (fuelStack.getItem() == Items.CHARCOAL && !fuelStack.isEmpty()) {
                fuelStack.shrink(1);
                be.fuelBurnTimeRemaining += FUEL_BURN_TIME;
                be.setChanged();
                structuralChange = true;
            }
        }

        // Греем угли только если есть топливо
        boolean isHeating = be.fuelBurnTimeRemaining > 0;
        boolean changed = false;

        for (int i = 0; i < CHARCOAL_SLOTS; i++) {
            ItemStack stack = be.itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof CharcoalItem charcoalItem)) {
                continue;
            }

            ModDataComponents.CharcoalData data = stack.getOrDefault(ModDataComponents.CHARCOAL_DATA.get(),
                    new ModDataComponents.CharcoalData(charcoalItem.getSize(), charcoalItem.getSize().getMaxTicks(), 0f));

            float newProgress = data.heatingProgress();
            int newRemaining = data.remainingTicks();

            if (isHeating) {
                // Прогрев в 3 раза быстрее сгорания
                if (newProgress < 100f) {
                    int heatingTime = charcoalItem.getSize().getMaxTicks() / 3;
                    float progressPerTick = 100.0f / heatingTime;
                    newProgress = Math.min(100f, newProgress + progressPerTick);
                }
                // Сгорание — 1 тик за тик (только при наличии топлива)
                if (newRemaining > 0) {
                    newRemaining -= 1;
                }
            } else {
                // Остывание без топлива — в 2 раза медленнее прогрева
                if (newProgress > 0f) {
                    int coolingTime = charcoalItem.getSize().getMaxTicks() / 3 * 2;
                    float coolingPerTick = 100.0f / coolingTime;
                    newProgress = Math.max(0f, newProgress - coolingPerTick);
                }
            }

            if (newProgress != data.heatingProgress() || newRemaining != data.remainingTicks()) {
                ModDataComponents.CharcoalData newData = new ModDataComponents.CharcoalData(data.size(), newRemaining, newProgress);
                stack.set(ModDataComponents.CHARCOAL_DATA.get(), newData);
                changed = true;
            }

            if (newRemaining <= 0) {
                be.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                changed = true;
                structuralChange = true;
            }
        }

        if (changed) {
            be.setChanged();
        }

        // Сетевой sync троттлим: раз в 10 тиков при обычном прогреве,
        // но немедленно при структурных изменениях (рефилл/выгорание слота).
        boolean fuelEdge = (be.fuelBurnTimeRemaining == 0) != (be.getLastSyncedFuelTime() == 0);
        boolean periodic = (level.getGameTime() % 10 == 0) && (changed || isHeating);
        if (structuralChange || fuelEdge || periodic) {
            level.sendBlockUpdated(pos, state, state, 3);
            be.markFuelSynced();
        }
    }

    // ─── Sync helper ───
    private int lastSyncedFuelTime = -1;
    public int getLastSyncedFuelTime() { return lastSyncedFuelTime; }
    public void markFuelSynced() { lastSyncedFuelTime = fuelBurnTimeRemaining; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("FuelBurnTimeRemaining", fuelBurnTimeRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        fuelBurnTimeRemaining = tag.getInt("FuelBurnTimeRemaining");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("FuelBurnTimeRemaining", fuelBurnTimeRemaining);
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.heater");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new HeaterMenu(containerId, playerInventory, this);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}