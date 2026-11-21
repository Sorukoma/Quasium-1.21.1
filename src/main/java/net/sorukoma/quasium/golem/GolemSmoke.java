package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.LoungeCushionBlock;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.entity.SeatEntity;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.ritual.brazier.RitualBrazierBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class GolemSmoke {
    private static final int LOOK_TICKS = 20;

    @Nullable
    private BlockPos zone;
    @Nullable
    private BlockPos hookah;
    private long lookAt;

    static boolean withOwner(ClayGolemEntity golem) {
        return hookah(golem) != null;
    }

    @Nullable
    static HookahBlockEntity hookah(ClayGolemEntity golem) {
        UUID owner = golem.ownerId();
        if (owner == null || !golem.tool().is(ModItems.HOOKAH_PIPE.get())) return null;
        GolemSmoke smoke = golem.getData(SmokeMarks.SMOKE);
        long now = golem.level().getGameTime();
        if (now >= smoke.lookAt) {
            smoke.lookAt = now + LOOK_TICKS;
            smoke.look(golem, owner);
        }
        return smoke.hookah == null ? null : smokedAt(golem.level(), smoke.hookah, owner);
    }

    @Nullable
    static BlockPos freeCushion(ClayGolemEntity golem, HookahBlockEntity hookah) {
        Level level = golem.level();
        BlockPos center = hookah.getBlockPos();
        long now = level.getGameTime();
        BlockPos best = null;
        for (BlockPos pos : around(center)) {
            if (!cushion(level, pos) || SeatEntity.isOccupied(level, pos) || golem.isUnreachable(pos)
                    || hookah.zone().takenByOther(pos, golem.getUUID(), now)) continue;
            if (best == null || golem.distanceToSqr(pos.getCenter()) < golem.distanceToSqr(best.getCenter())) best = pos.immutable();
        }
        return best;
    }

    static boolean anyCushion(Level level, BlockPos hookah) {
        for (BlockPos pos : around(hookah)) {
            if (cushion(level, pos)) return true;
        }
        return false;
    }

    private static Iterable<BlockPos> around(BlockPos hookah) {
        int reach = Config.GOLEM_SMOKE_CUSHION_REACH.getAsInt();
        return BlockPos.betweenClosed(hookah.offset(-reach, -1, -reach), hookah.offset(reach, 1, reach));
    }

    static boolean cushion(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LoungeCushionBlock;
    }

    private void look(ClayGolemEntity golem, UUID owner) {
        if (zone != null && !inReach(golem, zone)) zone = null;
        hookah = zone == null ? null : smokedIn(golem.level(), zone, owner);
        if (hookah != null) return;
        for (BlockPos mark : golem.marks(MarkType.ZONE)) {
            hookah = smokedIn(golem.level(), mark, owner);
            if (hookah == null) continue;
            zone = mark;
            return;
        }
    }

    private static boolean inReach(ClayGolemEntity golem, BlockPos zone) {
        Level level = golem.level();
        return level.isLoaded(zone) && level.getBlockEntity(zone) instanceof SmokeMarkBlockEntity mark && mark.type() == MarkType.ZONE
                && mark.golems().contains(golem.getUUID())
                && ZoneBox.of(level, zone).inflatedBy(GolemBindings.markRadius()).isInside(golem.blockPosition());
    }

    @Nullable
    private static BlockPos smokedIn(Level level, BlockPos zone, UUID owner) {
        BoundingBox box = ZoneBox.of(level, zone);
        if (!ZoneBox.loaded(level, box)) return null;
        for (BlockEntity entity : ZoneBox.blockEntities(level, box)) {
            if (entity instanceof HookahBlockEntity hookah && smokedBy(hookah, owner)) return hookah.getBlockPos();
        }
        return null;
    }

    @Nullable
    private static HookahBlockEntity smokedAt(Level level, BlockPos pos, UUID owner) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof HookahBlockEntity hookah && smokedBy(hookah, owner) ? hookah : null;
    }

    private static boolean smokedBy(HookahBlockEntity hookah, UUID owner) {
        return !(hookah instanceof RitualBrazierBlockEntity) && hookah.isSmoldering() && hookah.zone().puffedBy(owner, hookah.getLevel().getGameTime());
    }
}
