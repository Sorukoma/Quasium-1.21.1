package net.sorukoma.quasium.tea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.EcologyRegistry;
import org.jetbrains.annotations.NotNull;

public class SamovarBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LEAVES_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int TEAPOT_SLOT = 2;
    public static final int SLOTS = 3;
    public static final int DATA_WATER = 0;
    public static final int DATA_WATER_CAPACITY = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_MAX = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_PROGRESS_MAX = 5;
    public static final int DATA_PARTY_SECONDS = 6;
    public static final int DATA_LACK = 7;
    public static final int DATA_COUNT = 8;
    private static final int BOIL_SOUND_TICKS = 60;

    public enum Lack {
        NOTHING, TEAPOT, FULL_TEAPOT, LEAVES, WATER, FUEL;

        public static Lack byIndex(int index) {
            return index >= 0 && index < values().length ? values()[index] : NOTHING;
        }
    }

    private int water;
    private int fuel;
    private int fuelMax;
    private int progress;
    private int partyTicks;
    private boolean villageThanked;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case LEAVES_SLOT -> stack.is(TeaBlocks.DRIED_TEA.get());
                case FUEL_SLOT -> stack.getBurnTime(RecipeType.SMELTING) > 0;
                default -> stack.is(TeaBlocks.TEAPOT.get());
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == TEAPOT_SLOT ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_WATER -> water;
                case DATA_WATER_CAPACITY -> waterCapacity();
                case DATA_FUEL -> fuel;
                case DATA_FUEL_MAX -> fuelMax;
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> brewTicks();
                case DATA_PARTY_SECONDS -> partyTicks / 20;
                case DATA_LACK -> lack().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SamovarBlockEntity(BlockPos pos, BlockState state) {
        super(TeaBlocks.SAMOVAR_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int water() {
        return water;
    }

    public static int waterCapacity() {
        return EcologyConfig.SAMOVAR_WATER_CAPACITY.getAsInt();
    }

    public static int brewTicks() {
        return EcologyConfig.SAMOVAR_BREW_SECONDS.getAsInt() * 20;
    }

    public boolean addWater(int ml) {
        if (water + ml > waterCapacity()) return false;
        water += ml;
        setChanged();
        return true;
    }

    public int partyTicks() {
        return partyTicks;
    }

    void setPartyTicks(int ticks) {
        partyTicks = Math.max(0, ticks);
        setChanged();
    }

    boolean villageThanked() {
        return villageThanked;
    }

    void setVillageThanked(boolean thanked) {
        villageThanked = thanked;
        setChanged();
    }

    public boolean canBrew() {
        return missingIngredient() == Lack.NOTHING;
    }

    public Lack lack() {
        Lack ingredient = missingIngredient();
        if (ingredient != Lack.NOTHING) return ingredient;
        return fuel > 0 || items.getStackInSlot(FUEL_SLOT).getBurnTime(RecipeType.SMELTING) > 0 ? Lack.NOTHING : Lack.FUEL;
    }

    private Lack missingIngredient() {
        ItemStack teapot = items.getStackInSlot(TEAPOT_SLOT);
        if (!teapot.is(TeaBlocks.TEAPOT.get())) return Lack.TEAPOT;
        if (TeapotItem.cups(teapot) > 0) return Lack.FULL_TEAPOT;
        if (items.getStackInSlot(LEAVES_SLOT).getCount() < EcologyConfig.SAMOVAR_LEAVES_PER_POT.getAsInt()) return Lack.LEAVES;
        return water < EcologyConfig.SAMOVAR_WATER_ML.getAsInt() ? Lack.WATER : Lack.NOTHING;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SamovarBlockEntity samovar) {
        boolean ready = samovar.canBrew();
        if (samovar.fuel <= 0 && ready) samovar.consumeFuel();
        if (samovar.fuel > 0) samovar.fuel--;
        if (samovar.fuel > 0 && ready) {
            if (++samovar.progress >= brewTicks()) samovar.finish(level, pos);
            if (level.getGameTime() % BOIL_SOUND_TICKS == 0) {
                level.playSound(null, pos, EcologyRegistry.SAMOVAR_BOIL.get(), SoundSource.BLOCKS, 0.6f, 1f);
            }
        } else if (!ready) {
            samovar.progress = 0;
        }
        boolean lit = samovar.fuel > 0;
        if (lit != state.getValue(SamovarBlock.LIT)) level.setBlock(pos, state.setValue(SamovarBlock.LIT, lit), Block.UPDATE_CLIENTS);
        if (samovar.partyTicks > 0 && level instanceof ServerLevel server) TeaParty.tick(server, samovar);
        samovar.setChanged();
    }

    private void consumeFuel() {
        ItemStack stack = items.getStackInSlot(FUEL_SLOT);
        int burnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) return;
        ItemStack remainder = stack.getCraftingRemainingItem();
        fuel = fuelMax = burnTime;
        if (stack.getCount() == 1 && !remainder.isEmpty()) {
            items.setStackInSlot(FUEL_SLOT, remainder);
        } else {
            items.extractItem(FUEL_SLOT, 1, false);
        }
    }

    private void finish(Level level, BlockPos pos) {
        water -= EcologyConfig.SAMOVAR_WATER_ML.getAsInt();
        items.extractItem(LEAVES_SLOT, EcologyConfig.SAMOVAR_LEAVES_PER_POT.getAsInt(), false);
        ItemStack teapot = items.getStackInSlot(TEAPOT_SLOT).copy();
        TeapotItem.setCups(teapot, EcologyConfig.TEAPOT_CUPS.getAsInt());
        items.setStackInSlot(TEAPOT_SLOT, teapot);
        progress = 0;
        level.playSound(null, pos, EcologyRegistry.TEA_POUR.get(), SoundSource.BLOCKS, 1f, 1f);
    }

    public void drops() {
        if (level == null) return;
        for (int slot = 0; slot < SLOTS; slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("Water", water);
        tag.putInt("Fuel", fuel);
        tag.putInt("FuelMax", fuelMax);
        tag.putInt("Progress", progress);
        tag.putInt("PartyTicks", partyTicks);
        tag.putBoolean("VillageThanked", villageThanked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        water = tag.getInt("Water");
        fuel = tag.getInt("Fuel");
        fuelMax = tag.getInt("FuelMax");
        progress = tag.getInt("Progress");
        partyTicks = tag.getInt("PartyTicks");
        villageThanked = tag.getBoolean("VillageThanked");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.samovar");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SamovarMenu(containerId, inventory, this, data);
    }
}
