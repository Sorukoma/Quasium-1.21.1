package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sorukoma.quasium.block.TobaccoCropBlock;
import net.sorukoma.quasium.item.TobaccoVarieties;

import java.util.ArrayList;
import java.util.List;

public class TobaccoCropBlockEntity extends BlockEntity {
    private static final long UNKNOWN = -1;

    private int strengthStars;
    private int yieldStars;
    private int qualityStars;
    private boolean initialized;
    private long matureSince = UNKNOWN;
    private long lastAphidDay = UNKNOWN;
    private List<String> lineage = List.of();

    public TobaccoCropBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOBACCO_CROP_BE.get(), pos, state);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setStars(int strength, int yieldStars, int quality) {
        this.strengthStars = clamp(strength);
        this.yieldStars = clamp(yieldStars);
        this.qualityStars = clamp(quality);
        this.initialized = true;
        setChanged();
    }

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

    public List<String> lineage() {
        return lineage.isEmpty() ? List.of(varietyOf()) : lineage;
    }

    public void setLineage(List<String> lineage) {
        this.lineage = List.copyOf(lineage);
        setChanged();
    }

    public void markMature(long gameTime) {
        matureSince = gameTime;
        setChanged();
    }

    public void forgetMaturity() {
        matureSince = UNKNOWN;
        setChanged();
    }

    public boolean ripeWithin(long gameTime, long windowTicks) {
        return matureSince != UNKNOWN && gameTime - matureSince < windowTicks;
    }

    public boolean startAphidDay(long day) {
        if (day == lastAphidDay) return false;
        lastAphidDay = day;
        setChanged();
        return true;
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
        tag.putLong("matureSince", matureSince);
        tag.putLong("lastAphidDay", lastAphidDay);
        ListTag names = new ListTag();
        lineage.forEach(variety -> names.add(StringTag.valueOf(variety)));
        tag.put("lineage", names);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        strengthStars = tag.getInt("strength");
        yieldStars = tag.getInt("yield");
        qualityStars = tag.getInt("quality");
        initialized = tag.getBoolean("initialized");
        matureSince = tag.contains("matureSince") ? tag.getLong("matureSince") : UNKNOWN;
        lastAphidDay = tag.contains("lastAphidDay") ? tag.getLong("lastAphidDay") : UNKNOWN;
        List<String> names = new ArrayList<>();
        ListTag stored = tag.getList("lineage", Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) names.add(stored.getString(i));
        lineage = List.copyOf(names);
    }
}
