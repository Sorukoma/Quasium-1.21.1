package net.sorukoma.quasium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.network.PacketStopHookahPuffSoundClient;
import net.sorukoma.quasium.network.PacketSpawnSmoke;
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
    public static final double PIPE_DETACH_DISTANCE_SQ = 2.0 * 2.0; // Квадрат расстояния для отсоединения трубки (2 блока)

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
        if (pCompound.hasUUID("AttachedPlayerUUID")) {
            this.attachedPlayerUUID = pCompound.getUUID("AttachedPlayerUUID");
        }
        if (pCompound.contains("HookahBlockPos")) {
            this.hookahBlockPos = NbtUtils.readBlockPos(pCompound, "HookahBlockPos").orElse(null);
        }
        this.entityData.set(IS_PUFFING, pCompound.getBoolean("IsPuffing"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        if (this.attachedPlayerUUID != null) {
            pCompound.putUUID("AttachedPlayerUUID", this.attachedPlayerUUID);
        }
        if (this.hookahBlockPos != null) {
            pCompound.put("HookahBlockPos", NbtUtils.writeBlockPos(this.hookahBlockPos));
        }
        pCompound.putBoolean("IsPuffing", this.entityData.get(IS_PUFFING));
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
                // Отправляем пакет клиенту, чтобы остановить звук, если он играет
                ModPackets.sendToClientsTrackingBlock(new PacketStopHookahPuffSoundClient(this.hookahBlockPos, false), blockEntity);
                return;
            }

            // Проверяем дистанцию между игроком и кальяном
            if (player.distanceToSqr(Vec3.atCenterOf(this.hookahBlockPos)) > PIPE_DETACH_DISTANCE_SQ) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You walked too far from the hookah!"));
                this.stopPuffing(hookahBlockEntity, player); // Передаем игрока для спавна дыма
                this.discard(); // Отбрасываем сущность, если игрок отошел слишком далеко
                return;
            }

            // Если игрок больше не держит трубку в руке, отсоединяем
            boolean holdingPipe = player.getMainHandItem().getItem() == net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get() ||
                                  player.getOffhandItem().getItem() == net.sorukoma.quasium.item.ModItems.HOOKAH_PIPE.get();
            if (!holdingPipe) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You let go of the hookah pipe!"));
                this.stopPuffing(hookahBlockEntity, player); // Передаем игрока для спавна дыма
                this.discard();
                return;
            }

            // Обновляем позицию сущности, чтобы она всегда была на кальяне
            this.setPos(this.hookahBlockPos.getX() + 0.5, this.hookahBlockPos.getY() + 0.5, this.hookahBlockPos.getZ() + 0.5);

            // Логика затяжки каждый тик, пока игрок держит ПКМ
            if (this.isPuffing()) {
                if (!hookahBlockEntity.tryPuff(player)) { // Затяжка на 1 тик
                    this.stopPuffing(hookahBlockEntity, player); // Если затяжка не удалась, останавливаем
                }
            }
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