package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.KaifZones;
import net.sorukoma.quasium.util.SmokeColorUtil;

public class SmokeJarBlockEntity extends BlockEntity {
    private String flavor = "";
    private int ticksLeft = -1;

    public SmokeJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMOKE_JAR_BE.get(), pos, state);
    }

    public int color() {
        return SmokeColorUtil.colorForFlavor(flavor);
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmokeJarBlockEntity jar) {
        if (jar.ticksLeft < 0) jar.ticksLeft = Config.SMOKE_JAR_MINUTES.getAsInt() * 1200;
        if (--jar.ticksLeft <= 0) {
            level.destroyBlock(pos, true);
            return;
        }
        if (level.getGameTime() % KaifZones.TICK_INTERVAL != 0 || !(level instanceof ServerLevel serverLevel)) return;
        jar.setChanged();
        int radius = KaifZones.radiusFor(1);
        if (KaifZones.lofiPlayingNear(serverLevel, pos, radius)) radius += Config.ZONE_JUKEBOX_BONUS_RADIUS.getAsInt();
        KaifZones.spread(serverLevel, pos, 1, 1, radius, jar.flavor, jar.color());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        flavor = input.getOrDefault(ModDataComponents.TOBACCO_FLAVOR.get(), "");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("flavor", flavor);
        tag.putInt("ticks_left", ticksLeft);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flavor = tag.getString("flavor");
        if (tag.contains("ticks_left")) ticksLeft = tag.getInt("ticks_left");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("flavor", flavor);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
