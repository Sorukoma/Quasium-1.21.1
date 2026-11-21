package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.NoteType;

public class SmokeSorterBlockEntity extends BlockEntity implements NoteNode {
    private int essenceMl;
    private int credit;

    public SmokeSorterBlockEntity(BlockPos pos, BlockState state) {
        super(DraftBlocks.SMOKE_SORTER_BE.get(), pos, state);
    }

    public int essenceMl() {
        return essenceMl;
    }

    public static int tankCapacity() {
        return Config.SORTER_TANK_ML.getAsInt();
    }

    public int fill(int ml) {
        int accepted = Math.min(ml, tankCapacity() - essenceMl);
        if (accepted <= 0) return 0;
        essenceMl += accepted;
        setChanged();
        return accepted;
    }

    @Override
    public int acceptNote(NoteType note, int ml, boolean simulate) {
        int perEssence = Config.SORTER_NOTES_PER_ESSENCE.getAsInt();
        int burnable = Math.min(ml, credit + essenceMl * perEssence);
        if (simulate || burnable <= 0) return Math.max(0, burnable);
        int unpaid = burnable - credit;
        if (unpaid > 0) {
            int essence = (unpaid + perEssence - 1) / perEssence;
            essenceMl -= essence;
            credit += essence * perEssence;
        }
        credit -= burnable;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.95,
                    worldPosition.getZ() + 0.5, 1, 0.05, 0.02, 0.05, 0.01);
        }
        return burnable;
    }

    @Override
    public int notePriority() {
        return PRIORITY_LAST;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Essence", essenceMl);
        tag.putInt("Credit", credit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essenceMl = tag.getInt("Essence");
        credit = tag.getInt("Credit");
    }
}
