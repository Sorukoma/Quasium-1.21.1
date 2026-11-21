package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import org.jetbrains.annotations.Nullable;

public class FlaskChargerBlockEntity extends BlockEntity implements DraftNode {
    private float seconds;

    public FlaskChargerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlocks.FLASK_CHARGER_BE.get(), pos, state);
    }

    @Nullable
    public HookahBlockEntity hookah() {
        if (level == null) return null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof HookahBlockEntity hookah) return hookah;
        }
        return null;
    }

    @Override
    public int draftDemand(Direction side) {
        HookahBlockEntity hookah = hookah();
        return hookah != null && hookah.flaskCharges() < Config.FLASK_MAX_CHARGES.getAsInt() ? Config.CHARGER_DEMAND.getAsInt() : 0;
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        HookahBlockEntity hookah = hookah();
        if (hookah == null) return;
        int period = Config.CHARGER_SECONDS_PER_CHARGE.getAsInt();
        seconds += efficiency / 20f;
        setChanged();
        if (seconds < period || !hookah.addFlaskCharge()) return;
        seconds -= period;
        if (level instanceof ServerLevel serverLevel) {
            BlockPos top = hookah.getBlockPos();
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.getX() + 0.5, top.getY() + 1.1, top.getZ() + 0.5,
                    12, 0.2, 0.15, 0.2, 0.2);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Seconds", seconds);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seconds = tag.getFloat("Seconds");
    }
}
