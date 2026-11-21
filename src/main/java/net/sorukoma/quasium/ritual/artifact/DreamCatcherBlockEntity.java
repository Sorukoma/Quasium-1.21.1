package net.sorukoma.quasium.ritual.artifact;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;

public class DreamCatcherBlockEntity extends BlockEntity {
    public static final String ESSENCE = "dream";

    private int ml;

    public DreamCatcherBlockEntity(BlockPos pos, BlockState state) {
        super(RitualRegistry.DREAM_CATCHER_BE.get(), pos, state);
    }

    public int ml() {
        return ml;
    }

    public int catchDream(int amount) {
        int caught = Math.max(0, Math.min(amount, RitualConfig.DREAM_CATCHER_CAP_ML.getAsInt() - ml));
        if (caught > 0) {
            ml += caught;
            setChanged();
        }
        return caught;
    }

    public int take(int wanted) {
        int taken = Math.min(wanted, ml);
        if (taken > 0) {
            ml -= taken;
            setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Dream", ml);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ml = tag.getInt("Dream");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ml = input.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (ml > 0) components.set(ModDataComponents.ESSENCE_AMOUNT.get(), ml);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Dream");
    }
}
