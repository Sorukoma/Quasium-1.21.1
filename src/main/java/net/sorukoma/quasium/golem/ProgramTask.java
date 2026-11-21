package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.golem.program.BlockKind;
import net.sorukoma.quasium.golem.program.ProgramBlock;

record ProgramTask(ProgramBlock block, BlockPos place, Direction face, boolean inArea) {
    BlockKind type() {
        return block.kind();
    }

    Vec3 spot() {
        return type() == BlockKind.GO ? Vec3.atBottomCenterOf(place.relative(face)) : Vec3.atCenterOf(place);
    }
}
