package net.sorukoma.quasium.block.entity;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.AromaLampBlock;
import net.sorukoma.quasium.block.LoungeBlocks;
import net.sorukoma.quasium.compat.SableCompat;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.ecology.SmogCleansing;
import net.sorukoma.quasium.magic.AromaEffects;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.util.SmokeColorUtil;
import org.jetbrains.annotations.Nullable;

public class AromaLampBlockEntity extends BlockEntity {
    private static final int PUFF_EVERY_TICKS = SharedConstants.TICKS_PER_SECOND;
    private static final int PUFF_PARTICLES = 2;
    private static final double BOWL_TOP = 8.5 / 16.0;
    private static final String LAVENDER = "lavender";

    private String essence = "";
    private int amount;
    private int burnTicks;

    public AromaLampBlockEntity(BlockPos pos, BlockState state) {
        super(LoungeBlocks.AROMA_LAMP_BE.get(), pos, state);
    }

    public String essence() {
        return essence;
    }

    public int amount() {
        return amount;
    }

    public static int capacity() {
        return Config.AROMA_LAMP_CAPACITY.getAsInt();
    }

    public static int ticksPerMl() {
        return Config.AROMA_LAMP_SECONDS_PER_ML.getAsInt() * SharedConstants.TICKS_PER_SECOND;
    }

    public boolean isLit() {
        return getBlockState().getValue(AromaLampBlock.LIT);
    }

    public int pour(String incoming, int ml) {
        if (incoming.isEmpty() || (!essence.isEmpty() && !essence.equals(incoming))) return 0;
        int accepted = Math.min(ml, capacity() - amount);
        if (accepted <= 0) return 0;
        essence = incoming;
        amount += accepted;
        changed();
        return accepted;
    }

    public boolean canLight() {
        return amount > 0 && !isLit();
    }

    public void setLit(boolean lit) {
        if (level != null && isLit() != lit) level.setBlock(worldPosition, getBlockState().setValue(AromaLampBlock.LIT, lit), 3);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AromaLampBlockEntity lamp) {
        if (!(level instanceof ServerLevel server) || !state.getValue(AromaLampBlock.LIT)) return;
        long time = level.getGameTime();
        if (time % PUFF_EVERY_TICKS == 0) lamp.puff(server);
        if (time % AromaEffects.REFRESH_TICKS == 0) lamp.scentNearbyPlayers(server);
        if (LAVENDER.equals(AromaEffects.primaryFlavor(lamp.essence))) SmogCleansing.lavender(server, pos, time);
        if (++lamp.burnTicks >= ticksPerMl()) lamp.burnOneMl();
    }

    private void puff(ServerLevel level) {
        int color = SmokeColorUtil.colorForFlavor(AromaEffects.primaryFlavor(essence));
        level.sendParticles(ModParticles.noteSmoke(color), worldPosition.getX() + 0.5, worldPosition.getY() + BOWL_TOP,
                worldPosition.getZ() + 0.5, PUFF_PARTICLES, 0.06, 0.02, 0.06, 0.004);
    }

    private void scentNearbyPlayers(ServerLevel level) {
        double radius = Config.ZONE_RADIUS_T1.getAsInt();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && SableCompat.distanceSqrToBlockCenter(player, worldPosition) <= radius * radius) {
                AromaEffects.apply(player, essence);
            }
        }
    }

    private void burnOneMl() {
        burnTicks = 0;
        if (--amount <= 0) {
            amount = 0;
            essence = "";
            setLit(false);
        }
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (amount > 0) {
            tag.putString("Essence", essence);
            tag.putInt("Amount", amount);
            tag.putInt("BurnTicks", burnTicks);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essence = tag.getString("Essence");
        amount = essence.isEmpty() ? 0 : tag.getInt("Amount");
        burnTicks = tag.getInt("BurnTicks");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        essence = input.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
        amount = essence.isEmpty() ? 0 : input.getOrDefault(ModDataComponents.ESSENCE_AMOUNT.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (amount > 0) {
            components.set(ModDataComponents.ESSENCE_TYPE.get(), essence);
            components.set(ModDataComponents.ESSENCE_AMOUNT.get(), amount);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Essence");
        tag.remove("Amount");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
