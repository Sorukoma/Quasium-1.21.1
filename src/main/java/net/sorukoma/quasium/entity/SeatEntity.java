package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.block.LoungeCushionBlock;
import net.sorukoma.quasium.item.HookahPipeItem;

public class SeatEntity extends Entity {
    private boolean hinted;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static boolean isSeated(Player player) {
        return player.getVehicle() instanceof SeatEntity;
    }

    public static boolean keepsSmoker(Player player) {
        return isSeated(player) && HookahPipeItem.holdsPipe(player);
    }

    public static boolean isOccupied(Level level, BlockPos cushion) {
        return !level.getEntities((Entity) null, new AABB(cushion), entity -> entity instanceof SeatEntity || entity.getPose() == Pose.SITTING).isEmpty();
    }

    public static boolean sit(Player player, BlockPos cushion) {
        SeatEntity seat = new SeatEntity(LoungeBlocks.SEAT.get(), player.level());
        seat.setPos(cushion.getX() + 0.5, cushion.getY() + LoungeCushionBlock.SEAT_HEIGHT, cushion.getZ() + 0.5);
        player.level().addFreshEntity(seat);
        player.setJumping(false);
        if (player.startRiding(seat)) return true;
        seat.discard();
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof LoungeCushionBlock)) {
            ejectPassengers();
            discard();
            return;
        }
        if (!hinted && getFirstPassenger() instanceof ServerPlayer rider && HookahPipeItem.holdsPipe(rider)) {
            hinted = true;
            rider.displayClientMessage(Component.translatable("quasium.lounge.pipe_controls",
                    Component.keybind("key.sneak"), Component.keybind("key.jump")), true);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
