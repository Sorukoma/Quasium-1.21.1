package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.DistillationCubeBlockEntity;
import net.sorukoma.quasium.magic.NoteType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class AutoDistillerBlockEntity extends BlockEntity implements DraftNode, NoteNode {
    public AutoDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(DraftBlocks.AUTO_DISTILLER_BE.get(), pos, state);
    }

    @Nullable
    private DistillationCubeBlockEntity cube() {
        if (level == null) return null;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof DistillationCubeBlockEntity cube) return cube;
        }
        return null;
    }

    @Override
    public int draftDemand(Direction side) {
        return cube() == null ? 0 : Config.DISTILLER_DEMAND.getAsInt();
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        DistillationCubeBlockEntity cube = cube();
        if (cube == null || level == null) return;
        cube.boost(1f + (Config.DISTILLER_SPEED_MULTIPLIER.getAsInt() - 1) * efficiency, level.getGameTime() + 1);
    }

    @Override
    public Map<NoteType, Integer> noteSupply() {
        DistillationCubeBlockEntity cube = cube();
        if (cube == null) return Map.of();
        Map<NoteType, Integer> supply = new EnumMap<>(NoteType.class);
        for (NoteType note : NoteType.values()) {
            int amount = cube.noteAmount(note);
            if (amount > 0) supply.put(note, amount);
        }
        return supply;
    }

    @Override
    public int takeNote(NoteType note, int ml) {
        DistillationCubeBlockEntity cube = cube();
        return cube == null ? 0 : cube.extractNote(note, ml);
    }
}
