package net.sorukoma.quasium.block.entity;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sorukoma.quasium.screen.HookahMenu;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketSpawnSmoke;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import com.google.common.collect.Lists;
import net.sorukoma.quasium.network.PacketHookahPuffData;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.sorukoma.quasium.QuasiumMod; // Импортируем QuasiumMod

public class HookahBlockEntity extends BlockEntity implements MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Константы для расхода
    public static final int TICKS_PER_SECOND = 20;
    public static final int MAX_PUFFS_PER_POTION = 60 * TICKS_PER_SECOND; // 1 зелье = 60 секунд затяжки
    public static final int COAL_BURN_TIME_TICKS = 60 * TICKS_PER_SECOND; // 1 уголь горит 1 минуту
    private static final float EFFECT_DURATION_MULTIPLIER = 1.5f; // 1 секунда затяжки -> 1.5 секунды эффекта
    private static final float SMOKE_DURATION_MULTIPLIER = 2.0f; // 1 секунда затяжки -> 2 секунды пара
    private static final int AIR_CONSUMPTION_PER_TICK_PUFF = 5; // Сколько воздуха (в тиках) расходуется за 1 тик затяжки
    private static final double PIPE_DETACH_DISTANCE_SQ = 2.0 * 2.0; // Квадрат расстояния для отсоединения трубки (2 блока)

    private int puffsRemainingPotion = 0; // Оставшееся время зелья в тиках
    private int currentCoalBurnTime = 0; // Оставшееся время горения текущего угля в тиках
    @Nullable
    private PotionContents activePotionContents = null; // Новое поле для хранения активных эффектов зелья

    private final Set<UUID> puffingPlayers = new HashSet<>(); // Игроки, которые в данный момент "тянут" кальян

    @Nullable
    private UUID attachedPlayerUUID = null; // UUID игрока, держащего трубку
    @Nullable
    private String attachedPlayerName = null; // Имя игрока, держащего трубку

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                // Отправка обновления клиенту, если необходимо
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.getItem() == Items.CHARCOAL; // Слот для углей
                case 1 -> stack.getItem() instanceof PotionItem; // Слот для зелья
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    public HookahBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.HOOKAH_BE.get(), pPos, pBlockState);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getPuffsRemainingPotion() {
        return puffsRemainingPotion;
    }

    public int getCurrentCoalBurnTime() {
        return currentCoalBurnTime;
    }

    public int getMaxPuffsPerPotion() {
        return MAX_PUFFS_PER_POTION;
    }

    public int getCoalBurnTimeTicks() {
        return COAL_BURN_TIME_TICKS;
    }

    @Nullable
    public UUID getAttachedPlayerUUID() {
        return attachedPlayerUUID;
    }

    @Nullable
    public String getAttachedPlayerName() {
        return attachedPlayerName;
    }

    public boolean isPipeAttached() {
        return attachedPlayerUUID != null;
    }

    public void attachPipe(UUID playerUUID, String playerName) {
        this.attachedPlayerUUID = playerUUID;
        this.attachedPlayerName = playerName;
        QuasiumMod.addAttachedPipe(playerUUID, worldPosition); // Добавляем запись в QuasiumMod
        setChanged();
        if (level != null && !level.isClientSide()) {
            // Отправить обновление клиенту, чтобы GUI мог обновиться
            ModPackets.sendToClientsTrackingBlock(new PacketHookahPuffData(worldPosition, puffsRemainingPotion, currentCoalBurnTime, attachedPlayerUUID, attachedPlayerName), this);
        }
    }

    public void detachPipe(@Nullable UUID playerUUID) { // Изменяем сигнатуру для принятия UUID
        if (playerUUID != null) {
            QuasiumMod.removeAttachedPipe(playerUUID); // Удаляем запись из QuasiumMod
        }
        this.attachedPlayerUUID = null;
        this.attachedPlayerName = null;
        setChanged();
        if (level != null && !level.isClientSide()) {
            // Отправить обновление клиенту
            ModPackets.sendToClientsTrackingBlock(new PacketHookahPuffData(worldPosition, puffsRemainingPotion, currentCoalBurnTime, attachedPlayerUUID, attachedPlayerName), this);
        }
    }

    public void manualDetachPipe(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        if (attachedPlayerUUID != null && attachedPlayerUUID.equals(player.getUUID())) {
            detachPipe(player.getUUID()); // Используем обновленный метод detachPipe
            stopPuffing(player.getUUID()); // Убеждаемся, что игрок прекращает затяжку

            // Трубка не выдается, так как она не потреблялась при прикреплении

            player.sendSystemMessage(Component.literal("You detached the hookah pipe."));
        } else {
            player.sendSystemMessage(Component.literal("You are not attached to this hookah."));
        }
    }

    public void startPuffing(UUID playerUUID) {
        puffingPlayers.add(playerUUID);
        LOGGER.debug("Player {} started puffing at {}", playerUUID, worldPosition);
    }

    public void stopPuffing(UUID playerUUID) {
        puffingPlayers.remove(playerUUID);
        LOGGER.debug("Player {} stopped puffing at {}", playerUUID, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putInt("PuffsRemainingPotion", puffsRemainingPotion);
        pTag.putInt("CurrentCoalBurnTime", currentCoalBurnTime);
        if (attachedPlayerUUID != null) {
            pTag.putUUID("AttachedPlayerUUID", attachedPlayerUUID);
            pTag.putString("AttachedPlayerName", attachedPlayerName);
        }
        if (activePotionContents != null) {
            // Сохраняем PotionContents
            PotionContents.CODEC.encodeStart(pRegistries.createSerializationContext(NbtOps.INSTANCE), activePotionContents)
                    .resultOrPartial(s -> System.err.println("Failed to save PotionContents: " + s))
                    .ifPresent(tagElement -> pTag.put("ActivePotionContents", tagElement));
        }
        super.saveAdditional(pTag, pRegistries);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        puffsRemainingPotion = pTag.getInt("PuffsRemainingPotion");
        currentCoalBurnTime = pTag.getInt("CurrentCoalBurnTime");
        if (pTag.hasUUID("AttachedPlayerUUID")) {
            attachedPlayerUUID = pTag.getUUID("AttachedPlayerUUID");
            attachedPlayerName = pTag.getString("AttachedPlayerName");
            // При загрузке мира, если трубка была прикреплена, добавляем ее в карту QuasiumMod
            if (level != null && !level.isClientSide()) {
                QuasiumMod.addAttachedPipe(attachedPlayerUUID, worldPosition);
            }
        }
        if (pTag.contains("ActivePotionContents")) {
            // Загружаем PotionContents
            PotionContents.CODEC.parse(pRegistries.createSerializationContext(NbtOps.INSTANCE), pTag.get("ActivePotionContents"))
                    .resultOrPartial(s -> System.err.println("Failed to load PotionContents: " + s))
                    .ifPresent(contents -> activePotionContents = contents);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.quasium.hookah");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new HookahMenu(pContainerId, pPlayerInventory, this);
    }

    // Новый метод для обработки затяжки с переменной длительностью
    public void processPuff(Player player, int puffDurationTicks) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        // Проверяем, что игрок, который тянет, является привязанным игроком
        if (attachedPlayerUUID == null || !attachedPlayerUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("You need to attach the hookah pipe first!"));
            return;
        }

        // Проверяем, что у игрока достаточно кислорода для затяжки
        if (player.getAirSupply() <= AIR_CONSUMPTION_PER_TICK_PUFF) { // Если кислорода меньше, чем минимальный расход
            player.sendSystemMessage(Component.literal("You are out of breath!"));
            stopPuffing(player.getUUID()); // Останавливаем затяжку
            return;
        }

        LOGGER.debug("Processing puff for player {} at {}. Requested duration: {} ticks", player.getName().getString(), worldPosition, puffDurationTicks);

        ItemStack charcoalStack = itemHandler.getStackInSlot(0);
        ItemStack potionStackInSlot = itemHandler.getStackInSlot(1);

        // --- Handle Charcoal ---
        boolean hasCoal = currentCoalBurnTime > 0;
        if (!hasCoal) {
            if (charcoalStack.getItem() == Items.CHARCOAL && !charcoalStack.isEmpty()) {
                charcoalStack.shrink(1);
                itemHandler.setStackInSlot(0, charcoalStack);
                currentCoalBurnTime = COAL_BURN_TIME_TICKS;
                player.sendSystemMessage(Component.literal("New charcoal loaded!"));
                hasCoal = true;
            } else {
                player.sendSystemMessage(Component.literal("Hookah needs charcoal!"));
                return; // Still need charcoal to puff at all
            }
        }

        // --- Handle Potion ---
        boolean hasPotionEffects = puffsRemainingPotion > 0;
        if (!hasPotionEffects) { // If potion puffs are 0 or less
            if (potionStackInSlot.getItem() instanceof PotionItem && !potionStackInSlot.isEmpty()) {
                PotionContents newPotionContents = potionStackInSlot.getComponents().get(DataComponents.POTION_CONTENTS);
                if (newPotionContents != null && newPotionContents.getAllEffects().iterator().hasNext()) { // Check if new potion has effects
                    activePotionContents = newPotionContents;
                    // Потребляем зелье (или оставляем пустую бутылку, если это обычное зелье)
                    if (potionStackInSlot.getItem() == Items.POTION) {
                        itemHandler.setStackInSlot(1, new ItemStack(Items.GLASS_BOTTLE));
                    } else {
                        potionStackInSlot.shrink(1);
                        itemHandler.setStackInSlot(1, potionStackInSlot);
                    }
                    puffsRemainingPotion = MAX_PUFFS_PER_POTION;
                    player.sendSystemMessage(Component.literal("New potion loaded!"));
                    hasPotionEffects = true;
                } else {
                    player.sendSystemMessage(Component.literal("Potion has no effects! Puffing empty."));
                    activePotionContents = null; // Ensure no old effects are applied
                }
            } else {
                player.sendSystemMessage(Component.literal("Hookah needs a potion! Puffing empty."));
                activePotionContents = null; // Ensure no old effects are applied
            }
        }

        // Определяем, сколько затяжек мы можем сделать
        // Potion duration no longer limits the puff itself, only whether effects are applied.
        int actualPuffDuration = Math.min(puffDurationTicks, currentCoalBurnTime);

        LOGGER.debug("Calculated actual puff duration: {} ticks (Requested: {}, Coal left: {})", actualPuffDuration, puffDurationTicks, currentCoalBurnTime);

        if (actualPuffDuration <= 0) {
            String reason = "";
            if (puffDurationTicks <= 0) reason = "Puff duration was 0.";
            else if (currentCoalBurnTime <= 0) reason = "No coal burn time remaining.";
            player.sendSystemMessage(Component.literal("Not enough resources for a puff! " + reason));
            LOGGER.warn("Puff failed for player {} at {}. Reason: {}", player.getName().getString(), worldPosition, reason);
            return;
        }

        // Потребляем ресурсы
        currentCoalBurnTime -= actualPuffDuration; // Coal burns regardless

        // Потребляем potion puffs, если они есть
        if (hasPotionEffects) {
            puffsRemainingPotion -= actualPuffDuration;
            if (puffsRemainingPotion < 0) puffsRemainingPotion = 0; // Предотвращаем отрицательные значения
        }


        // Применяем эффекты зелья ТОЛЬКО если есть оставшиеся затяжки зелья И activePotionContents не null
        if (activePotionContents != null && hasPotionEffects) {
            List<MobEffectInstance> mobEffects = Lists.newArrayList(activePotionContents.getAllEffects());
            for (MobEffectInstance effect : mobEffects) {
                int effectDuration = (int) (actualPuffDuration * EFFECT_DURATION_MULTIPLIER);
                player.addEffect(new MobEffectInstance(effect.getEffect(), effectDuration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
            }
        } else if (activePotionContents != null && !hasPotionEffects) {
            // Если зелье только что закончилось или уже было пустым, очищаем активные эффекты для следующей затяжки.
            activePotionContents = null;
        }


        setChanged();

        // Отправить пакет на клиент для создания частиц дыма
        ModPackets.sendToClientsTrackingBlock(new PacketSpawnSmoke(worldPosition, player.getLookAngle()), this);
        // Отправить пакет на клиент для обновления данных о затяжках
        ModPackets.sendToClientsTrackingBlock(new PacketHookahPuffData(worldPosition, puffsRemainingPotion, currentCoalBurnTime, attachedPlayerUUID, attachedPlayerName), this);

        String potionStatus = hasPotionEffects ? String.format("Potion left: %.1fs", (float)puffsRemainingPotion / TICKS_PER_SECOND) : "Potion empty";
        player.sendSystemMessage(Component.literal(String.format("You puffed the hookah for %.1fs! %s, Coal burn time left: %.1fs",
                (float)actualPuffDuration / TICKS_PER_SECOND, potionStatus, (float)currentCoalBurnTime / TICKS_PER_SECOND)));
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, HookahBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) {
            return;
        }

        // Уменьшаем время горения угля
        if (pBlockEntity.currentCoalBurnTime > 0) {
            pBlockEntity.currentCoalBurnTime--;
            pBlockEntity.setChanged(); // Важно, чтобы изменения сохранялись
            // Отправляем обновление клиенту, чтобы шкала угля обновлялась
            // Отправляем только если изменилось значение, чтобы не спамить пакетами
            if (pBlockEntity.currentCoalBurnTime % TICKS_PER_SECOND == 0 || pBlockEntity.currentCoalBurnTime == 0) { // Обновляем каждую секунду или когда заканчивается
                ModPackets.sendToClientsTrackingBlock(new PacketHookahPuffData(pBlockEntity.worldPosition, pBlockEntity.puffsRemainingPotion, pBlockEntity.currentCoalBurnTime, pBlockEntity.attachedPlayerUUID, pBlockEntity.attachedPlayerName), pBlockEntity);
            }
        }

        // Расход кислорода для игроков, которые "тянут" кальян
        // Расходуем кислород каждый тик
        for (UUID playerUUID : pBlockEntity.puffingPlayers) {
            Player player = pLevel.getPlayerByUUID(playerUUID);
            if (player instanceof ServerPlayer serverPlayer) {
                // Проверяем, что игрок все еще находится рядом с кальяном и смотрит на него (опционально)
                // Для простоты, пока просто расходуем кислород
                serverPlayer.setAirSupply(Math.max(0, serverPlayer.getAirSupply() - pBlockEntity.AIR_CONSUMPTION_PER_TICK_PUFF)); // Расход каждый тик

                // Если кислорода не осталось, останавливаем затяжку
                if (serverPlayer.getAirSupply() <= 0) {
                    pBlockEntity.stopPuffing(playerUUID);
                    serverPlayer.sendSystemMessage(Component.literal("You ran out of air!"));
                }

                // Проверяем расстояние до кальяна
                if (pBlockEntity.isPipeAttached() && pBlockEntity.getAttachedPlayerUUID().equals(playerUUID)) {
                    if (serverPlayer.distanceToSqr(pBlockEntity.getBlockPos().getCenter()) > PIPE_DETACH_DISTANCE_SQ) {
                        pBlockEntity.detachPipe(playerUUID); // Передаем playerUUID
                        pBlockEntity.stopPuffing(playerUUID);
                        serverPlayer.sendSystemMessage(Component.literal("You walked too far from the hookah!"));
                        // Трубка не выдается, так как она не потреблялась при прикреплении

                        // if (!serverPlayer.getInventory().add(new ItemStack(net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get()))) {
                        //     Containers.dropItemStack(pLevel, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), new ItemStack(net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get()));
                        // }
                    }
                }
            }
        }
    }
}