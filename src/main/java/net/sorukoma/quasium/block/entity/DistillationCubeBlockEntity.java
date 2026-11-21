package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.SmokeAura;
import net.sorukoma.quasium.block.DistillationCubeBlock;
import net.sorukoma.quasium.magic.MagicMath;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.screen.DistillationCubeMenu;
import net.sorukoma.quasium.selection.quality.LeafQuality;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DistillationCubeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROCESS_TICKS = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_MAX = 3;
    public static final int DATA_WATER = 4;
    public static final int DATA_WATER_CAPACITY = 5;
    public static final int DATA_NOTES = 6;
    public static final int DATA_NOTE_CAPACITY = 12;
    public static final int DATA_COUNT = 13;

    private final int[] notes = new int[NoteType.values().length];
    private int water;
    private int progress;
    private float boost = 1f;
    private long boostUntil = -1;
    private float boostCarry;
    private int fuel;
    private int fuelMax;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == INPUT_SLOT) return distillable(stack);
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
            if (index >= DATA_NOTES && index < DATA_NOTE_CAPACITY) return notes[index - DATA_NOTES];
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESS_TICKS -> Config.DISTILL_TICKS_PER_ITEM.getAsInt();
                case DATA_FUEL -> fuel;
                case DATA_FUEL_MAX -> fuelMax;
                case DATA_WATER -> water;
                case DATA_WATER_CAPACITY -> Config.DISTILL_WATER_CAPACITY.getAsInt();
                case DATA_NOTE_CAPACITY -> Config.NOTE_BUFFER_CAPACITY.getAsInt();
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

    public DistillationCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTILLATION_CUBE_BE.get(), pos, state);
    }

    public static boolean distillable(ItemStack stack) {
        Map.Entry<ResourceLocation, NoteProfile> profile = NoteProfiles.forStack(stack);
        return profile != null && !profile.getValue().isEntity();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ContainerData getData() {
        return data;
    }

    public int noteAmount(NoteType note) {
        return notes[note.ordinal()];
    }

    public boolean hasRoomForWater(int ml) {
        return water + ml <= Config.DISTILL_WATER_CAPACITY.getAsInt();
    }

    public void addWater(int ml) {
        water = Math.min(Config.DISTILL_WATER_CAPACITY.getAsInt(), water + ml);
        setChanged();
    }

    public void boost(float multiplier, long until) {
        boost = multiplier;
        boostUntil = until;
    }

    public int extractNote(NoteType note, int maxMl) {
        int taken = Math.min(maxMl, notes[note.ordinal()]);
        if (taken <= 0) return 0;
        notes[note.ordinal()] -= taken;
        setChanged();
        return taken;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DistillationCubeBlockEntity cube) {
        if (level.isClientSide) return;
        int processTicks = Config.DISTILL_TICKS_PER_ITEM.getAsInt();
        Map<NoteType, Integer> yield = cube.currentYield();
        boolean canWork = yield != null && cube.water >= Config.DISTILL_WATER_PER_ITEM.getAsInt()
                && MagicMath.fitsInBuffers(cube.bufferMap(), idMap(yield), Config.NOTE_BUFFER_CAPACITY.getAsInt());

        if (canWork && cube.fuel <= 0) cube.consumeFuel();
        boolean burning = cube.fuel > 0;
        int steps = 1;
        if (level.getGameTime() <= cube.boostUntil) {
            cube.boostCarry += cube.boost - 1f;
            steps += (int) cube.boostCarry;
            cube.boostCarry -= (int) cube.boostCarry;
        }
        if (burning) {
            cube.fuel = Math.max(0, cube.fuel - MagicMath.fuelTicksNeeded(processTicks, Config.DISTILL_FUEL_PER_ITEM.getAsInt()) * steps);
        }
        if (burning && canWork) {
            cube.progress += steps;
            if (cube.progress >= processTicks) cube.finishItem(yield);
            if (level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, ModSoundEvents.CUBE_BUBBLE.get(), SoundSource.BLOCKS, 0.45f,
                        0.9f + level.random.nextFloat() * 0.2f);
            }
        } else if (cube.progress > 0) {
            cube.progress = Math.max(0, cube.progress - 2);
        }
        if (state.getValue(DistillationCubeBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(DistillationCubeBlock.LIT, burning), 3);
        }
        if (burning || canWork) cube.setChanged();
    }

    @Nullable
    private Map<NoteType, Integer> currentYield() {
        ItemStack input = items.getStackInSlot(INPUT_SLOT);
        Map.Entry<ResourceLocation, NoteProfile> entry = NoteProfiles.forStack(input);
        return entry == null || entry.getValue().isEntity() ? null : LeafQuality.noteYield(input, entry.getValue());
    }

    private void consumeFuel() {
        ItemStack fuelStack = items.getStackInSlot(FUEL_SLOT);
        int burnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) return;
        ItemStack remainder = fuelStack.getCraftingRemainingItem();
        fuel = fuelMax = burnTime;
        if (fuelStack.getCount() == 1 && !remainder.isEmpty()) {
            items.setStackInSlot(FUEL_SLOT, remainder);
        } else {
            items.extractItem(FUEL_SLOT, 1, false);
        }
    }

    private void finishItem(Map<NoteType, Integer> yield) {
        yield.forEach((note, ml) -> notes[note.ordinal()] += SmokeAura.boosted(level, worldPosition, ml, EcologyConfig.BONUS_CUBE_PCT));
        water -= Config.DISTILL_WATER_PER_ITEM.getAsInt();
        items.extractItem(INPUT_SLOT, 1, false);
        progress = 0;
    }

    private Map<String, Integer> bufferMap() {
        Map<String, Integer> buffers = new HashMap<>();
        for (NoteType note : NoteType.values()) buffers.put(note.id(), notes[note.ordinal()]);
        return buffers;
    }

    private static Map<String, Integer> idMap(Map<NoteType, Integer> yield) {
        Map<String, Integer> incoming = new HashMap<>();
        yield.forEach((note, ml) -> incoming.put(note.id(), ml));
        return incoming;
    }

    public void drops() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putIntArray("Notes", notes);
        tag.putInt("Water", water);
        tag.putInt("Progress", progress);
        tag.putInt("Fuel", fuel);
        tag.putInt("FuelMax", fuelMax);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        int[] saved = tag.getIntArray("Notes");
        System.arraycopy(saved, 0, notes, 0, Math.min(saved.length, notes.length));
        water = tag.getInt("Water");
        progress = tag.getInt("Progress");
        fuel = tag.getInt("Fuel");
        fuelMax = tag.getInt("FuelMax");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.distillation_cube");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DistillationCubeMenu(containerId, inventory, this, data);
    }
}
