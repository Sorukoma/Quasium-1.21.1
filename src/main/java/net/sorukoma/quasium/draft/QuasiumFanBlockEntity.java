package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.sound.ModSoundEvents;

public class QuasiumFanBlockEntity extends BlockEntity implements DraftNode, NoteNode {
    private static final int TICKS_PER_SECOND = 20;

    private int emberMl;
    private int burnTicks;

    public QuasiumFanBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorBlocks.QUASIUM_FAN_BE.get(), pos, state);
    }

    public int emberMl() {
        return emberMl;
    }

    public static int tankCapacity() {
        return Config.FAN_TANK_ML.getAsInt();
    }

    public int fill(int ml) {
        int accepted = Math.min(ml, tankCapacity() - emberMl);
        if (accepted <= 0) return 0;
        emberMl += accepted;
        setChanged();
        return accepted;
    }

    @Override
    public int acceptNote(NoteType note, int ml, boolean simulate) {
        if (note != NoteType.EMBER) return 0;
        return simulate ? Math.max(0, Math.min(ml, tankCapacity() - emberMl)) : fill(ml);
    }

    @Override
    public int draftOutput(Direction side) {
        return getBlockState().getValue(QuasiumFanBlock.RUNNING) ? Config.FAN_DRAFT.getAsInt() : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuasiumFanBlockEntity fan) {
        if (fan.burnTicks <= 0 && fan.emberMl > 0) {
            fan.emberMl--;
            fan.burnTicks = Config.FAN_SECONDS_PER_EMBER_ML.getAsInt() * TICKS_PER_SECOND;
        }
        boolean running = fan.burnTicks > 0;
        if (running) {
            fan.burnTicks--;
            fan.setChanged();
        }
        if (running != state.getValue(QuasiumFanBlock.RUNNING)) {
            level.setBlock(pos, state.setValue(QuasiumFanBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
        if (running && level.getGameTime() % TICKS_PER_SECOND == 0) {
            level.playSound(null, pos, ModSoundEvents.FAN_HUM.get(), SoundSource.BLOCKS, 0.5f, 1f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Ember", emberMl);
        tag.putInt("BurnTicks", burnTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        emberMl = tag.getInt("Ember");
        burnTicks = tag.getInt("BurnTicks");
    }
}
