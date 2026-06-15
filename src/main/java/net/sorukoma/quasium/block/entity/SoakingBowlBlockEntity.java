package net.sorukoma.quasium.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.block.SoakingBowlBlock;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
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

    private float tobaccoStrength = 0.0f; // Базовая крепость
    private ItemStack resultItem = ItemStack.EMPTY;

    private int molassesMass = 0; // Масса мелассы: 0–25 (добавляется только после смешивания)
    private boolean soaking = false;
    private int soakingProgress = 0;
    private int soakingTotalTime = 200; // 10 секунд при 20 тиков в секунду

    public void updateTooltip() {
        if (!resultItem.isEmpty()) {
            // Устанавливаем компонент для отображения количества мелассы
            resultItem.set(ModDataComponents.MOLASSES_AMOUNT.get(), molassesMass);
        }
    }

    public SoakingBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOAKING_BOWL_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ItemStack getResultItem() {
        return resultItem;
    }

    public void clearResult() {
        this.resultItem = ItemStack.EMPTY;
        this.molassesMass = 0; // Обнуляем мелассу при извлечении
        setChanged();
    }

    public int getMolassesAmount() {
        return molassesMass;
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

    public Component getSoakingStatusMessage() {
        int remaining = soakingTotalTime - soakingProgress;
        int seconds = remaining / 20; // 20 тиков = 1 секунда
        return Component.translatable("quasium.message.soaking_bowl.soaking_progress", seconds);
    }

    public void addMolasses(int amount) {
        // Меласса добавляется когда загружены листья и зелье, но табак еще не создан и не в процессе замеса
        boolean hasLeavesAndPotion = !itemHandler.getStackInSlot(0).isEmpty() &&
                                 !itemHandler.getStackInSlot(1).isEmpty();
        boolean noResult = resultItem.isEmpty();
        boolean notSoaking = !soaking;

        if (hasLeavesAndPotion && noResult && notSoaking) {
            this.molassesMass = Math.min(molassesMass + amount, 5); // Максимум 5 мл
            setChanged();
        }
    }

    public void startSoaking(boolean isMakhorka, Player player) {
        if (level != null && !level.isClientSide) {
            this.soaking = true;
            this.soakingProgress = 0;
            this.tobaccoStrength = isMakhorka ? 1.0f : 0.2f;
            setChanged();
            
            // Отправляем сообщение клиенту
            player.displayClientMessage(Component.translatable("quasium.message.soaking_bowl.start_soaking"), true);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoakingBowlBlockEntity entity) {
        if (level.isClientSide) return;

        // Обработка процесса смешивания
        if (entity.soaking) {
            entity.soakingProgress++;

            if (entity.soakingProgress >= entity.soakingTotalTime) {
                // Процесс смешивания завершен
                ItemStack leavesSlot = entity.itemHandler.getStackInSlot(0);
                ItemStack potionSlot = entity.itemHandler.getStackInSlot(1);

                entity.resultItem = ModItems.SELF_MIXED_TOBACCO.get().getDefaultInstance();

                // Применяем эффект зелья
                PotionContents potionContents = potionSlot.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                if (potionContents.getAllEffects().iterator().hasNext()) {
                    MobEffectInstance effect = potionContents.getAllEffects().iterator().next();
                    entity.resultItem.set(ModDataComponents.POTION_EFFECT.get(), effect);
                }

                // Устанавливаем базовую крепость: махорка = 1.0, обычный табак = 0.2
                boolean isMakhorka = leavesSlot.getItem() == ModItems.MAKHORKA_LEAVES.get();
                float baseStrength = isMakhorka ? 1.0f : 0.2f;

                // Крепость уменьшается в зависимости от количества мелассы
                // 0 мл мелассы = максимальная крепость, 5 мл мелассы = 0 крепости
                float strength = baseStrength * (1.0f - (entity.molassesMass / 5.0f));
                entity.resultItem.set(ModDataComponents.TOBACCO_DAMAGE.get(), strength);

                // Устанавливаем вес 30г
                entity.resultItem.set(ModDataComponents.TOBACCO_WEIGHT.get(), 30);
                entity.resultItem.set(ModDataComponents.MOLASSES_AMOUNT.get(), entity.molassesMass);

                // Устанавливаем основу (тип листьев)
                String tobaccoBase = isMakhorka ? "makhorka" : "tobacco";
                entity.resultItem.set(ModDataComponents.TOBACCO_BASE.get(), tobaccoBase);

                // Очищаем входные слоты
                entity.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                entity.itemHandler.setStackInSlot(1, ItemStack.EMPTY);

                // Останавливаем процесс смешивания
                entity.soaking = false;
                entity.soakingProgress = 0;

                entity.setChanged();
                // Синхронизируем результат на клиент (он создаётся не через itemHandler)
                level.sendBlockUpdated(pos, state, state, 3);
            } else {
                // Помечаем dirty только пока идёт замес, а не каждый тик в простое
                entity.setChanged();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putFloat("tobaccoStrength", tobaccoStrength);
        tag.put("resultItem", resultItem.saveOptional(registries));
        tag.putInt("molassesMass", molassesMass);
        tag.putBoolean("soaking", soaking);
        tag.putInt("soakingProgress", soakingProgress);
        tag.putInt("soakingTotalTime", soakingTotalTime);
        return tag;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        tobaccoStrength = tag.getFloat("tobaccoStrength");
        resultItem = ItemStack.parse(registries, tag.getCompound("resultItem")).orElse(ItemStack.EMPTY);
        molassesMass = tag.getInt("molassesMass");
        soaking = tag.getBoolean("soaking");
        soakingProgress = tag.getInt("soakingProgress");
        soakingTotalTime = tag.contains("soakingTotalTime") ? tag.getInt("soakingTotalTime") : 200;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putFloat("tobaccoStrength", tobaccoStrength);
        tag.put("resultItem", resultItem.saveOptional(registries));
        tag.putInt("molassesMass", molassesMass);
        tag.putBoolean("soaking", soaking);
        tag.putInt("soakingProgress", soakingProgress);
        tag.putInt("soakingTotalTime", soakingTotalTime);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}