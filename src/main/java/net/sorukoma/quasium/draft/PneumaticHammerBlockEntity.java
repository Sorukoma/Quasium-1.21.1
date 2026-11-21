package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.entity.QuasiumAnvilBlockEntity;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PneumaticHammerBlockEntity extends BlockEntity implements DraftNode {
    private static final int LOWERED_TICKS = 4;

    @Nullable
    private UUID owner;
    @Nullable
    private ServerPlayer ownerOnline;
    private float progress;

    public PneumaticHammerBlockEntity(BlockPos pos, BlockState state) {
        super(ForgeBlocks.PNEUMATIC_HAMMER_BE.get(), pos, state);
    }

    public void setOwner(ServerPlayer player) {
        owner = player.getUUID();
        ownerOnline = player;
        setChanged();
    }

    @Nullable
    private ServerPlayer smith() {
        if (ownerOnline == null || ownerOnline.isRemoved()) {
            ownerOnline = owner != null && level instanceof ServerLevel serverLevel ? serverLevel.getServer().getPlayerList().getPlayer(owner) : null;
        }
        return ownerOnline;
    }

    @Nullable
    private QuasiumAnvilBlockEntity anvil() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof QuasiumAnvilBlockEntity anvil ? anvil : null;
    }

    @Override
    public int draftDemand(Direction side) {
        QuasiumAnvilBlockEntity anvil = anvil();
        ServerPlayer smith = smith();
        boolean ready = anvil != null && smith != null && anvil.readiness(smith) == QuasiumAnvilBlockEntity.Strike.READY;
        return ready ? Config.HAMMER_DEMAND.getAsInt() : 0;
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        progress += efficiency;
        setChanged();
        int ticksPerHit = Config.HAMMER_TICKS_PER_HIT.getAsInt();
        if (progress < ticksPerHit) return;
        QuasiumAnvilBlockEntity anvil = anvil();
        ServerPlayer smith = smith();
        if (anvil == null || smith == null) return;
        progress -= ticksPerHit;
        if (anvil.strike(smith, this::pound, this::deliver) == QuasiumAnvilBlockEntity.Strike.CRAFTED) {
            SmokecraftTrigger.fire(smith, "hammer_craft");
        }
    }

    private void pound() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, worldPosition, ModSoundEvents.PNEUMATIC_HIT.get(), SoundSource.BLOCKS, 1f,
                0.9f + serverLevel.random.nextFloat() * 0.2f);
        serverLevel.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7,
                worldPosition.getZ() + 0.5, 4, 0.45, 0.05, 0.45, 0.01);
        BlockState state = getBlockState();
        serverLevel.setBlock(worldPosition, state.setValue(PneumaticHammerBlock.STRIKING, true), Block.UPDATE_CLIENTS);
        serverLevel.scheduleTick(worldPosition, state.getBlock(), LOWERED_TICKS);
    }

    private void deliver(ItemStack result) {
        if (level == null) return;
        BlockPos anvilPos = worldPosition.below();
        IItemHandler container = level.getCapability(Capabilities.ItemHandler.BLOCK, anvilPos.below(), Direction.UP);
        ItemStack rest = container == null ? result : ItemHandlerHelper.insertItemStacked(container, result, false);
        if (rest.isEmpty()) return;
        level.addFreshEntity(new ItemEntity(level, anvilPos.getX() + 0.5, anvilPos.getY() + 1.02, anvilPos.getZ() + 0.5, rest, 0, 0, 0));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putFloat("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        progress = tag.getFloat("Progress");
    }
}
