package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.SoakingBowlBlock;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoVarieties;
import org.jetbrains.annotations.Nullable;

public class SoakingBowlBlockEntity extends BlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // Каждый слот — один предмет
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false; // Проверка в useItemOn
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // Извлечение только через ванильную миску
        }
    };

    private boolean soaking = false;
    private boolean ready = false;
    private int soakingProgress = 0;
    private int soakingTotalTime = 600;

    // ПКМ-химия
    private int tobaccoGrams = 0;
    private int molassesMb = 0;
    private int essenceMb = 0;
    private String essenceType = "";
    private boolean isMakhorka = false;
    private String variety = TobaccoVarieties.DIKAR;
    private boolean overdried = false;
    // Звёзды конкретного листа: у гибрида крепость индивидуальна и не выводится из названия сорта.
    private int leafStrengthStars = 0;
    private int leafYieldStars = 0;
    private int leafQualityStars = 0;
    // Токсичность листа приходит от мутации "toxic" гибрида; такой замес наносит урон при курении.
    private boolean leafToxic = false;

    private boolean toxic = false;
    private ItemStack resultItem = ItemStack.EMPTY;

    public SoakingBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOAKING_BOWL_BE.get(), pos, state);
    }

    public int getTobaccoGrams() { return tobaccoGrams; }
    public void setTobaccoGrams(int v) {
        tobaccoGrams = v;
        invalidateSoaking();
    }
    public int getMolassesMb() { return molassesMb; }
    public void setMolassesMb(int v) {
        molassesMb = v;
        invalidateSoaking();
    }
    public int getEssenceMb() { return essenceMb; }
    public void setEssenceMb(int v) { essenceMb = v; }
    public String getEssenceType() { return essenceType; }
    public void setEssenceType(String v) { essenceType = v; }
    public boolean isMakhorka() { return isMakhorka; }
    public void setMakhorka(boolean v) { isMakhorka = v; }
    public void setVariety(String v) { variety = v; }
    public void setLeafStars(int strength, int yield, int quality) {
        leafStrengthStars = strength;
        leafYieldStars = yield;
        leafQualityStars = quality;
    }
    public void setLeafToxic(boolean v) { leafToxic = v; }
    public boolean isOverdried() { return overdried; }
    public void setOverdried(boolean value) { overdried = value; }
    public boolean isReady() { return ready; }
    public boolean isToxic() { return toxic || leafToxic; }
    public ItemStack getResultItem() { return resultItem; }

    public ItemStack takeResult() {
        if (!ready || resultItem.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = resultItem;
        resultItem = ItemStack.EMPTY;
        resetMixture();
        return result;
    }

    private void invalidateSoaking() {
        soaking = false;
        ready = false;
        toxic = false;
        soakingProgress = 0;
        setChangedAndSync();
    }

    public void addEssence(String type, int amount) {
        if (!essenceType.isEmpty() && !java.util.Arrays.asList(essenceType.split("\\+")).contains(type)) {
            essenceType += "+" + type;
        } else if (essenceType.isEmpty()) {
            essenceType = type;
        }
        essenceMb += amount;
        ready = false;
        toxic = false;
        soaking = false;
        soakingProgress = 0;
        setChangedAndSync();
    }

    public boolean canStartSoaking() {
        return tobaccoGrams == 1 && molassesMb + essenceMb > 0;
    }

    public void stir() {
        if (ready) return;
        if (!soaking) {
            if (!canStartSoaking()) return;
            soaking = true;
            soakingProgress = 0;
        } else {
            soakingProgress = Math.min(soakingTotalTime, soakingProgress + 100);
        }
        setChangedAndSync();
    }

    public void resetMixture() {
        tobaccoGrams = 0;
        molassesMb = 0;
        essenceMb = 0;
        essenceType = "";
        variety = TobaccoVarieties.DIKAR;
        overdried = false;
        leafStrengthStars = 0;
        leafYieldStars = 0;
        leafQualityStars = 0;
        leafToxic = false;
        soaking = false;
        ready = false;
        toxic = false;
        soakingProgress = 0;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isSoaking() {
        return soaking;
    }

    public int getSoakingProgress() {
        return soakingProgress;
    }

    public int getSoakingTotalTime() {
        return soakingTotalTime;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoakingBowlBlockEntity entity) {
        if (level.isClientSide) return;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && level.getGameTime() % 20 == 0 && entity.tobaccoGrams > 0) {
            net.minecraft.core.particles.ParticleOptions particle;
            if (entity.isToxic()) particle = net.minecraft.core.particles.ParticleTypes.WITCH;
            else if (entity.ready) particle = net.minecraft.core.particles.ParticleTypes.ENCHANT;
            else if (entity.molassesMb / (float)entity.tobaccoGrams < 0.15f) particle = net.minecraft.core.particles.ParticleTypes.SMOKE;
            else particle = net.minecraft.core.particles.ParticleTypes.SPLASH;
            serverLevel.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5,
                    2, 0.2, 0.03, 0.2, 0.01);
        }
        if (entity.soaking) {
            entity.soakingProgress++;
            if (entity.soakingProgress >= entity.soakingTotalTime) {
                entity.soaking = false;
                entity.ready = true;
                long essenceTypes = java.util.Arrays.stream(entity.essenceType.split("\\+"))
                        .filter(type -> !type.isBlank()).distinct().count();
                // Токсичность: либо лист имеет мутацию "toxic", либо очень много разных экстрактов (перебор).
                entity.toxic = entity.leafToxic || (essenceTypes >= 3 && level.random.nextBoolean());

                String base = entity.variety;
                // Крепость смеси учитывает индивидуальную крепость листа (звёзды), а не только сорт.
                // У гибрида звёзды варьируются от родителей, поэтому дефолт по сорту тут неверен.
                int strengthStars = entity.leafStrengthStars > 0
                        ? entity.leafStrengthStars : TobaccoVarieties.strength(entity.variety);
                int yieldStars = entity.leafYieldStars > 0
                        ? entity.leafYieldStars : TobaccoVarieties.yield(entity.variety);
                int qualityStars = entity.leafQualityStars > 0
                        ? entity.leafQualityStars : TobaccoVarieties.quality(entity.variety);
                float baseStrength = strengthStars / 5.0f;
                float dryingMultiplier = entity.overdried ? 1.2f : 1.0f;
                float liquidMultiplier = Math.max(0.25f,
                        1.0f - entity.molassesMb / 40.0f - entity.essenceMb / 200.0f);
                float strength = Math.max(0.1f, Math.min(1.0f,
                        baseStrength * dryingMultiplier * 0.9f * liquidMultiplier));
                ItemStack result = new ItemStack(ModItems.SELF_MIXED_TOBACCO.get());
                result.set(ModDataComponents.TOBACCO_WEIGHT.get(), 30);
                result.set(ModDataComponents.TOBACCO_BASE.get(), base);
                result.set(ModDataComponents.TOBACCO_VARIETY.get(), entity.variety);
                result.set(ModDataComponents.TOBACCO_STRENGTH_STARS.get(), strengthStars);
                result.set(ModDataComponents.TOBACCO_YIELD_STARS.get(), yieldStars);
                result.set(ModDataComponents.TOBACCO_QUALITY_STARS.get(), qualityStars);
                result.set(ModDataComponents.TOBACCO_DAMAGE.get(), strength);
                result.set(ModDataComponents.ESSENCE_TYPE.get(), entity.essenceType);
                result.set(ModDataComponents.ESSENCE_AMOUNT.get(), entity.essenceMb);
                result.set(ModDataComponents.MOLASSES_AMOUNT.get(), entity.molassesMb);
                result.set(ModDataComponents.TOXIC.get(), entity.toxic);
                result.set(ModDataComponents.SELF_MIXED_EFFECTS.get(),
                        SoakingBowlBlock.createEssenceEffects(entity.essenceType));
                entity.resultItem = result;
                entity.tobaccoGrams = 0;
                entity.molassesMb = 0;
                entity.essenceMb = 0;
                entity.essenceType = "";
                entity.isMakhorka = false;
                entity.overdried = false;
                entity.leafStrengthStars = 0;
                entity.leafYieldStars = 0;
                entity.leafQualityStars = 0;
                entity.leafToxic = false;
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.2f);
                entity.setChangedAndSync();
            } else if (entity.soakingProgress % 20 == 0) {
                entity.setChangedAndSync();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("soaking", soaking);
        tag.putBoolean("ready", ready);
        tag.putInt("soakingProgress", soakingProgress);
        tag.putInt("soakingTotalTime", soakingTotalTime);
        tag.putInt("tobaccoGrams", tobaccoGrams);
        tag.putInt("molassesMb", molassesMb);
        tag.putInt("essenceMb", essenceMb);
        tag.putString("essenceType", essenceType);
        tag.putString("variety", variety);
        tag.putBoolean("isMakhorka", isMakhorka);
        tag.putBoolean("overdried", overdried);
        tag.putInt("leafStrengthStars", leafStrengthStars);
        tag.putInt("leafYieldStars", leafYieldStars);
        tag.putInt("leafQualityStars", leafQualityStars);
        tag.putBoolean("leafToxic", leafToxic);
        tag.putBoolean("toxic", toxic);
        tag.put("resultItem", resultItem.saveOptional(registries));
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        soaking = tag.getBoolean("soaking");
        ready = tag.getBoolean("ready");
        soakingProgress = tag.getInt("soakingProgress");
        soakingTotalTime = tag.contains("soakingTotalTime") ? tag.getInt("soakingTotalTime") : 600;
        tobaccoGrams = tag.contains("tobaccoGrams") ? tag.getInt("tobaccoGrams") : 0;
        molassesMb = tag.contains("molassesMb") ? tag.getInt("molassesMb") : 0;
        essenceMb = tag.contains("essenceMb") ? tag.getInt("essenceMb") : 0;
        essenceType = tag.contains("essenceType") ? tag.getString("essenceType") : "";
        variety = tag.contains("variety") ? tag.getString("variety") : TobaccoVarieties.DIKAR;
        isMakhorka = tag.contains("isMakhorka") ? tag.getBoolean("isMakhorka") : false;
        overdried = tag.getBoolean("overdried");
        leafStrengthStars = tag.getInt("leafStrengthStars");
        leafYieldStars = tag.getInt("leafYieldStars");
        leafQualityStars = tag.getInt("leafQualityStars");
        leafToxic = tag.getBoolean("leafToxic");
        toxic = tag.getBoolean("toxic");
        resultItem = tag.contains("resultItem")
                ? ItemStack.parse(registries, tag.getCompound("resultItem")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putBoolean("soaking", soaking);
        tag.putBoolean("ready", ready);
        tag.putInt("soakingProgress", soakingProgress);
        tag.putInt("soakingTotalTime", soakingTotalTime);
        tag.putInt("tobaccoGrams", tobaccoGrams);
        tag.putInt("molassesMb", molassesMb);
        tag.putInt("essenceMb", essenceMb);
        tag.putString("essenceType", essenceType);
        tag.putString("variety", variety);
        tag.putBoolean("isMakhorka", isMakhorka);
        tag.putBoolean("overdried", overdried);
        tag.putInt("leafStrengthStars", leafStrengthStars);
        tag.putInt("leafYieldStars", leafYieldStars);
        tag.putInt("leafQualityStars", leafQualityStars);
        tag.putBoolean("leafToxic", leafToxic);
        tag.putBoolean("toxic", toxic);
        tag.put("resultItem", resultItem.saveOptional(registries));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
