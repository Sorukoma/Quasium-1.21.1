package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.hookah.HookahBulbVariant;
import net.sorukoma.quasium.hookah.HookahShaftVariant;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity модульного кальяна. Наследует всю механику курения от HookahBlockEntity
 * (чаша, каллауд, жар, кайф) и добавляет:
 *  - вариант колбы/шахты (для рендера),
 *  - состояние сборки (шахта установлена, диффузор накручен),
 *  - уровень воды в колбе (0..MAX_WATER_MB), влияющий на качество покура.
 *
 * Правило воды: оптимум {@link #OPTIMAL_WATER_MB}. Отклонение в любую сторону
 * (мало/много/нет) снижает множитель качества затяжки, но не блокирует покур.
 */
public class ModularHookahBlockEntity extends HookahBlockEntity {

    public static final int MAX_WATER_MB = 1000;      // одно ведро
    public static final int OPTIMAL_WATER_MB = 750;   // оптимальный уровень

    private HookahBulbVariant bulbVariant = HookahBulbVariant.WHITE;
    @Nullable
    private HookahShaftVariant shaftVariant = null; // null → шахта не установлена
    private boolean hasShaft = false;
    private boolean hasScrewedDiffuser = false;      // диффузор накручен на шахту
    private int waterMb = 0;

    public ModularHookahBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MODULAR_HOOKAH_BE.get(), pPos, pBlockState);
    }

    // ─── Accessors ───
    public HookahBulbVariant getBulbVariant() { return bulbVariant; }
    @Nullable public HookahShaftVariant getShaftVariant() { return shaftVariant; }
    public boolean hasShaft() { return hasShaft; }
    public boolean hasScrewedDiffuser() { return hasScrewedDiffuser; }
    public int getWaterMb() { return waterMb; }

    public void setBulbVariant(HookahBulbVariant variant) {
        this.bulbVariant = variant;
        markDirtyAndSync();
    }

    public void setShaft(HookahShaftVariant variant) {
        this.shaftVariant = variant;
        this.hasShaft = true;
        markDirtyAndSync();
    }

    public void removeShaft() {
        this.shaftVariant = null;
        this.hasShaft = false;
        this.hasScrewedDiffuser = false;
        markDirtyAndSync();
    }

    public void setScrewedDiffuser(boolean value) {
        this.hasScrewedDiffuser = value;
        markDirtyAndSync();
    }

    public void setWaterMb(int mb) {
        this.waterMb = Math.max(0, Math.min(MAX_WATER_MB, mb));
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Диффузор для механики курения активен, если он накручен на шахту (модульный
     * вариант), либо вставлен в слот аксессуаров (унаследованное поведение).
     */
    @Override
    public boolean hasDiffuser() {
        return hasScrewedDiffuser || super.hasDiffuser();
    }

    /**
     * Качество покура зависит от уровня воды. Оптимум {@link #OPTIMAL_WATER_MB} → 1.0.
     * Отклонение снижает множитель линейно, минимум 0.4 (покур всегда возможен).
     */
    @Override
    protected float getPuffQualityMultiplier() {
        int deviation = Math.abs(waterMb - OPTIMAL_WATER_MB);
        // Нормируем на максимально возможное отклонение (в большую сторону — до пустого/полного).
        int maxDeviation = Math.max(OPTIMAL_WATER_MB, MAX_WATER_MB - OPTIMAL_WATER_MB);
        float penalty = maxDeviation > 0 ? (float) deviation / maxDeviation : 0f;
        return Math.max(0.4f, 1.0f - penalty * 0.6f);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.modular_hookah");
    }

    // ─── NBT ───
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("BulbVariant", bulbVariant.getName());
        tag.putBoolean("HasShaft", hasShaft);
        if (shaftVariant != null) tag.putString("ShaftVariant", shaftVariant.getName());
        tag.putBoolean("HasScrewedDiffuser", hasScrewedDiffuser);
        tag.putInt("WaterMb", waterMb);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bulbVariant = HookahBulbVariant.byName(tag.getString("BulbVariant"));
        hasShaft = tag.getBoolean("HasShaft");
        shaftVariant = tag.contains("ShaftVariant") ? HookahShaftVariant.byName(tag.getString("ShaftVariant")) : null;
        hasScrewedDiffuser = tag.getBoolean("HasScrewedDiffuser");
        waterMb = tag.getInt("WaterMb");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putString("BulbVariant", bulbVariant.getName());
        tag.putBoolean("HasShaft", hasShaft);
        if (shaftVariant != null) tag.putString("ShaftVariant", shaftVariant.getName());
        tag.putBoolean("HasScrewedDiffuser", hasScrewedDiffuser);
        tag.putInt("WaterMb", waterMb);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
