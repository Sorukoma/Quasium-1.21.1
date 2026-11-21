package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.ecology.SmogCleansing;
import net.sorukoma.quasium.ecology.SmokeDensity;

public class VentilationBlockEntity extends BlockEntity implements DraftNode {
    private static final int RUNNING_TICKS = 20;
    private static final float TICKS_PER_MINUTE = 1200f;

    private float cleared;
    private float smogWork;
    private int runTicks;

    public VentilationBlockEntity(BlockPos pos, BlockState state) {
        super(MachineBlocks.VENTILATION_BE.get(), pos, state);
    }

    @Override
    public int draftDemand(Direction side) {
        return Config.VENT_DEMAND.getAsInt();
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        if (level == null || efficiency <= 0f) return;
        runTicks = RUNNING_TICKS;
        cleared += Config.VENT_DENSITY_PER_MINUTE.getAsInt() * efficiency / TICKS_PER_MINUTE;
        int points = (int) cleared;
        if (points > 0) {
            cleared -= points;
            SmokeDensity.addAround(level, worldPosition, 1, -points);
        }
        smogWork += efficiency / (EcologyConfig.VENT_SMOG_MINUTES.getAsInt() * TICKS_PER_MINUTE);
        if (smogWork >= 1f && level instanceof ServerLevel server) {
            smogWork -= 1f;
            SmogCleansing.cleanse(server, worldPosition, 1);
        }
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VentilationBlockEntity vent) {
        if (vent.runTicks > 0) vent.runTicks--;
        boolean running = vent.runTicks > 0;
        if (running != state.getValue(VentilationBlock.RUNNING)) {
            level.setBlock(pos, state.setValue(VentilationBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Cleared", cleared);
        tag.putFloat("SmogWork", smogWork);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cleared = tag.getFloat("Cleared");
        smogWork = tag.getFloat("SmogWork");
    }
}
