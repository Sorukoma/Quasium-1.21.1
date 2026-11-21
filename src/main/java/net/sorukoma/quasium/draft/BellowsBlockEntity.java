package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;

public class BellowsBlockEntity extends BlockEntity implements DraftNode {
    private int pulseTicks;
    private long readyAt;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        super(DraftBlocks.BELLOWS_BE.get(), pos, state);
    }

    public boolean pump(long now) {
        if (now < readyAt) return false;
        readyAt = now + Config.BELLOWS_COOLDOWN_TICKS.getAsInt();
        pulseTicks = 20;
        return true;
    }

    @Override
    public int draftOutput(Direction side) {
        int output = pulseTicks > 0 ? Config.BELLOWS_PULSE_DRAFT.getAsInt() : 0;
        if (getBlockState().getValue(BellowsBlock.POWERED)) output += Config.BELLOWS_REDSTONE_DRAFT.getAsInt();
        return output;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BellowsBlockEntity bellows) {
        if (bellows.pulseTicks > 0) bellows.pulseTicks--;
        boolean pumping = bellows.pulseTicks > 0 || (state.getValue(BellowsBlock.POWERED) && level.getGameTime() % 40 < 20);
        if (pumping != state.getValue(BellowsBlock.PUMPING)) {
            level.setBlock(pos, state.setValue(BellowsBlock.PUMPING, pumping), Block.UPDATE_CLIENTS);
        }
    }
}
