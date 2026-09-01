package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.item.TobaccoVarieties;

/**
 * Хранит индивидуальные параметры конкретного куста (крепость/урожай/качество в звёздах).
 * Дикие кусты из генерации инициализируются лениво в {@link TobaccoCropBlock#randomTick}
 * с пониженным качеством; кусты, посаженные игроком из семени, получают звёзды семени.
 */
public class TobaccoCropBlockEntity extends BlockEntity {
    private int strengthStars;
    private int yieldStars;
    private int qualityStars;
    private boolean initialized;

    public TobaccoCropBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOBACCO_CROP_BE.get(), pos, state);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Задаёт звёзды явно (посадка игроком из семени с компонентами). */
    public void setStars(int strength, int yieldStars, int quality) {
        this.strengthStars = clamp(strength);
        this.yieldStars = clamp(yieldStars);
        this.qualityStars = clamp(quality);
        this.initialized = true;
        setChanged();
    }

    /**
     * Ленивая инициализация дикого куста: крепость/урожай берутся из базовых
     * значений сорта, качество занижено — 1 звезда, изредка (15%) 2.
     */
    public void initWild(RandomSource random) {
        if (initialized) return;
        String variety = varietyOf();
        this.strengthStars = TobaccoVarieties.strength(variety);
        this.yieldStars = TobaccoVarieties.yield(variety);
        this.qualityStars = random.nextFloat() < 0.15f ? 2 : 1;
        this.initialized = true;
        setChanged();
    }

    public int getStrengthStars() {
        return initialized ? strengthStars : TobaccoVarieties.strength(varietyOf());
    }

    public int getYieldStars() {
        return initialized ? yieldStars : TobaccoVarieties.yield(varietyOf());
    }

    public int getQualityStars() {
        return initialized ? qualityStars : 1;
    }

    private String varietyOf() {
        return getBlockState().getBlock() instanceof TobaccoCropBlock crop ? crop.getVariety() : TobaccoVarieties.DIKAR;
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(5, value));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("strength", strengthStars);
        tag.putInt("yield", yieldStars);
        tag.putInt("quality", qualityStars);
        tag.putBoolean("initialized", initialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        strengthStars = tag.getInt("strength");
        yieldStars = tag.getInt("yield");
        qualityStars = tag.getInt("quality");
        initialized = tag.getBoolean("initialized");
    }
}
