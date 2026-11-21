package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.network.PacketSyncHookahConnection;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class HookahConnectionEntity extends Entity {

    // Synched data for client-side rendering and logic
    private static final EntityDataAccessor<Optional<UUID>> ATTACHED_PLAYER_UUID =
            SynchedEntityData.defineId(HookahConnectionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<BlockPos>> HOOKAH_BLOCK_POS =
            SynchedEntityData.defineId(HookahConnectionEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> IS_PUFFING =
            SynchedEntityData.defineId(HookahConnectionEntity.class, EntityDataSerializers.BOOLEAN);

    // Server-side only fields for convenience
    @Nullable
    private UUID attachedPlayerUUID;
    @Nullable
    private BlockPos hookahBlockPos;

    // Constants
    public static final double PIPE_REACH = 3.5;
    public static final double PIPE_DETACH_DISTANCE_SQ = PIPE_REACH * PIPE_REACH;

    public HookahConnectionEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noCulling = true; // Всегда рендерить, даже если вне поля зрения
    }

    public HookahConnectionEntity(Level pLevel, UUID playerUUID, BlockPos hookahPos) {
        this(ModEntityTypes.HOOKAH_CONNECTION.get(), pLevel);
        this.attachedPlayerUUID = playerUUID;
        this.hookahBlockPos = hookahPos;
        this.setPos(hookahPos.getX() + 0.5, hookahPos.getY() + 0.5, hookahPos.getZ() + 0.5); // Устанавливаем позицию сущности на кальян

        // Устанавливаем синхронизируемые данные сразу при создании сущности на сервере
        // Этот конструктор вызывается только на сервере, поэтому проверка isClientSide не нужна.
        this.entityData.set(ATTACHED_PLAYER_UUID, Optional.ofNullable(this.attachedPlayerUUID));
        this.entityData.set(HOOKAH_BLOCK_POS, Optional.ofNullable(this.hookahBlockPos));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        pBuilder.define(ATTACHED_PLAYER_UUID, Optional.empty());
        pBuilder.define(HOOKAH_BLOCK_POS, Optional.empty());
        pBuilder.define(IS_PUFFING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (pKey.equals(ATTACHED_PLAYER_UUID)) {
            this.attachedPlayerUUID = this.entityData.get(ATTACHED_PLAYER_UUID).orElse(null);
        }
        if (pKey.equals(HOOKAH_BLOCK_POS)) {
            this.hookahBlockPos = this.entityData.get(HOOKAH_BLOCK_POS).orElse(null);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        UUID smoker = getAttachedPlayerUUID();
        if (!level().isClientSide && smoker != null && level().getPlayerByUUID(smoker) instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new PacketSyncHookahConnection(Optional.empty()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) { // Server-side logic
            if (this.attachedPlayerUUID == null || this.hookahBlockPos == null) {
                this.discard(); // Отбрасываем сущность, если нет данных
                return;
            }

            // Проверяем существование игрока
            Player player = this.level().getPlayerByUUID(this.attachedPlayerUUID);
            if (player == null) {
                this.discard(); // Отбрасываем сущность, если игрок не найден
                return;
            }

            // Проверяем существование блока кальяна
            BlockEntity blockEntity = this.level().getBlockEntity(this.hookahBlockPos);
            if (!(blockEntity instanceof HookahBlockEntity hookahBlockEntity)) {
                this.discard(); // Отбрасываем сущность, если блок кальяна не найден или не того типа
                return;
            }

            // Проверяем дистанцию между игроком и кальяном (sub-level aware для Sable:
            // координаты кальяна на контрапшене лежат в plot-grid, обычная distanceToSqr дала бы мусор)
            if (net.sorukoma.quasium.compat.SableCompat.distanceSqr(player, Vec3.atCenterOf(this.hookahBlockPos)) > PIPE_DETACH_DISTANCE_SQ) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("quasium.message.walked_away"), true);
                this.stopPuffing(hookahBlockEntity, player); // Передаем игрока для спавна дыма
                this.discard(); // Отбрасываем сущность, если игрок отошел слишком далеко
                return;
            }

            if (!HookahPipeItem.carriesPipe(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("quasium.message.let_go"), true);
                this.stopPuffing(hookahBlockEntity, player);
                this.discard();
                return;
            }
            if (!HookahPipeItem.holdsPipe(player)) this.stopPuffing(hookahBlockEntity, player);

            // Обновляем позицию сущности, чтобы она всегда была на кальяне
            this.setPos(this.hookahBlockPos.getX() + 0.5, this.hookahBlockPos.getY() + 0.5, this.hookahBlockPos.getZ() + 0.5);

            // Затяжку тикает HookahBlockEntity, чтобы общий ресурс обрабатывался один раз.
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        // Рендерить всегда, если игрок находится в пределах дальности отслеживания
        return pDistance < 4096.0D; // 64 * 64
    }

    // Getters for client-side rendering
    public UUID getAttachedPlayerUUID() {
        return this.entityData.get(ATTACHED_PLAYER_UUID).orElse(null);
    }

    public BlockPos getHookahBlockPos() {
        return this.entityData.get(HOOKAH_BLOCK_POS).orElse(null);
    }

    public boolean isPuffing() {
        return this.entityData.get(IS_PUFFING);
    }

    // Методы для управления состоянием затяжки
    public void startPuffing(HookahBlockEntity hookahBlockEntity) {
        if (!this.level().isClientSide && !this.isPuffing()) {
            this.entityData.set(IS_PUFFING, true);
            hookahBlockEntity.startPuffing(this);
        }
    }

    public void stopPuffing(HookahBlockEntity hookahBlockEntity, Player player) {
        if (!this.level().isClientSide && this.isPuffing()) {
            this.entityData.set(IS_PUFFING, false);
            hookahBlockEntity.stopPuffing(this, player); // Передаем игрока
        }
    }

    public void setPuffing(boolean puffing) {
        if (!this.level().isClientSide) {
            this.entityData.set(IS_PUFFING, puffing);
        }
    }
}
