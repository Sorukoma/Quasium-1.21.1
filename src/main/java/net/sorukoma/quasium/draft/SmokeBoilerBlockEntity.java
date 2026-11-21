package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.ecology.SmokeDensity;
import net.sorukoma.quasium.screen.SmokeBoilerMenu;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.NotNull;

public class SmokeBoilerBlockEntity extends BlockEntity implements DraftNode, MenuProvider {
    public static final int FUEL_SLOT = 0;
    public static final int DATA_WATER = 0;
    public static final int DATA_WATER_CAPACITY = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_MAX = 3;
    public static final int DATA_COUNT = 4;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = 1200;
    private static final int LOOP_TICKS = 40;

    private int water;
    private int fuel;
    private int fuelMax;
    private int steamCarry;
    private int smokeCarry;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getBurnTime(RecipeType.SMELTING) > 0;
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

    public SmokeBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorBlocks.SMOKE_BOILER_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int water() {
        return water;
    }

    public static int waterCapacity() {
        return Config.BOILER_WATER_CAPACITY.getAsInt();
    }

    public boolean addWater(int ml) {
        if (water + ml > waterCapacity()) return false;
        water += ml;
        setChanged();
        return true;
    }

    @Override
    public int draftOutput(Direction side) {
        return getBlockState().getValue(SmokeBoilerBlock.LIT) ? Config.BOILER_DRAFT.getAsInt() : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmokeBoilerBlockEntity boiler) {
        if (boiler.fuel <= 0 && boiler.water > 0) boiler.consumeFuel();
        boolean lit = boiler.fuel > 0 && boiler.water > 0;
        if (lit) boiler.boil(level, pos);
        if (lit != state.getValue(SmokeBoilerBlock.LIT)) {
            level.setBlock(pos, state.setValue(SmokeBoilerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
        if (lit && level.getGameTime() % LOOP_TICKS == 0) {
            level.playSound(null, pos, ModSoundEvents.BOILER_LOOP.get(), SoundSource.BLOCKS, 0.7f, 1f);
        }
    }

    private void boil(Level level, BlockPos pos) {
        fuel--;
        steamCarry += Config.BOILER_WATER_ML_PER_SECOND.getAsInt();
        water = Math.max(0, water - steamCarry / TICKS_PER_SECOND);
        steamCarry %= TICKS_PER_SECOND;
        smokeCarry += Config.BOILER_DENSITY_PER_MINUTE.getAsInt();
        if (smokeCarry >= TICKS_PER_MINUTE) {
            smokeCarry -= TICKS_PER_MINUTE;
            SmokeDensity.add(level, pos, 1);
        }
        setChanged();
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

    public void drops() {
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(FUEL_SLOT));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("Water", water);
        tag.putInt("Fuel", fuel);
        tag.putInt("FuelMax", fuelMax);
        tag.putInt("SteamCarry", steamCarry);
        tag.putInt("SmokeCarry", smokeCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        water = tag.getInt("Water");
        fuel = tag.getInt("Fuel");
        fuelMax = tag.getInt("FuelMax");
        steamCarry = tag.getInt("SteamCarry");
        smokeCarry = tag.getInt("SmokeCarry");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.smoke_boiler");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SmokeBoilerMenu(containerId, inventory, this, data);
    }
}
