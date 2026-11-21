package net.sorukoma.quasium.cart;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.data.BackpackHookahState;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.hookah.HookahSignals;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.magic.SmokeCasting;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class HookahMinecart extends AbstractMinecart {
    private static final EntityDataAccessor<Optional<UUID>> HOSE_HOLDER =
            SynchedEntityData.defineId(HookahMinecart.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final String HOOKAH_TAG = "Hookah";
    private static final double WALK_SPEED = 0.216;
    private static final int RATTLE_INTERVAL = 20;
    private static final double PIPE_SEARCH_RADIUS = 64.0;

    private final Hookah hookah = new Hookah();
    @Nullable
    private ServerPlayer holder;

    public HookahMinecart(EntityType<? extends HookahMinecart> type, Level level) {
        super(type, level);
    }

    public Hookah hookah() {
        return hookah;
    }

    @Nullable
    public UUID hoseHolderId() {
        return entityData.get(HOSE_HOLDER).orElse(null);
    }

    @Nullable
    public Player hoseHolder() {
        UUID id = hoseHolderId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public boolean isHeldBy(Player player) {
        return player.getUUID().equals(hoseHolderId());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSE_HOLDER, Optional.empty());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = super.interact(player, hand);
        if (result.consumesAction()) return result;
        ItemStack main = player.getMainHandItem();
        if (player instanceof ServerPlayer smoker) {
            if (main.isEmpty() && player.isSecondaryUseActive() && isHeldBy(player)) unhook();
            else if (main.is(ModItems.HOOKAH_PIPE.get()) || (main.isEmpty() && player.getOffhandItem().is(ModItems.HOOKAH_PIPE.get()))) hook(smoker);
            else {
                QuasiumModAdvancements.HOOKAH_OPENED.get().trigger(smoker);
                smoker.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new CartHookahMenu(id, inventory, this), getDisplayName()));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void startPuffing(ServerPlayer player) {
        if (player != holder || !HookahPipeItem.holdsPipe(player) || HookahPipeItem.coolingDown(player) || hookah.isPuffing()) return;
        String cold = hookah.coldReason();
        if (cold != null) HookahSignals.refuse(player, cold);
        else hookah.startPuffing();
    }

    public void stopPuffing(ServerPlayer player) {
        if (player != holder || !hookah.isPuffing()) return;
        SmokeCasting.release(player, hookah);
        hookah.stopPuffing();
    }

    private void hook(ServerPlayer player) {
        UUID current = hoseHolderId();
        if (current != null && !current.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("quasium.message.cart_pipe_busy"), true);
            return;
        }
        if (current == null) {
            unhookAll(player);
            dropHookahPipes(player);
            entityData.set(HOSE_HOLDER, Optional.of(player.getUUID()));
        }
        holder = player;
        hookah.setPlayer(player);
        PacketDistributor.sendToPlayer(player, new CartHosePacket(getId()));
        player.sendSystemMessage(Component.translatable("quasium.message.pipe_attached_cart"));
    }

    private void unhook() {
        ServerPlayer player = holder;
        if (player == null) return;
        if (hookah.isPuffing()) hookah.stopPuffing();
        holder = null;
        hookah.setPlayer(null);
        entityData.set(HOSE_HOLDER, Optional.empty());
        PacketDistributor.sendToPlayer(player, new CartHosePacket(-1));
        player.sendSystemMessage(Component.translatable("quasium.message.pipe_detached_cart"));
    }

    public static void unhookAll(Player player) {
        if (player.level() instanceof ServerLevel level) {
            level.getEntities(HookahCarts.HOOKAH_MINECART.get(), cart -> cart.isHeldBy(player)).forEach(HookahMinecart::unhook);
        }
    }

    private static void dropHookahPipes(ServerPlayer player) {
        Level level = player.level();
        AABB area = player.getBoundingBox().inflate(PIPE_SEARCH_RADIUS);
        for (HookahConnectionEntity pipe : level.getEntitiesOfClass(HookahConnectionEntity.class, area,
                entity -> player.getUUID().equals(entity.getAttachedPlayerUUID()))) {
            BlockPos pos = pipe.getHookahBlockPos();
            if (pos != null && level.getBlockEntity(pos) instanceof HookahBlockEntity placed) pipe.stopPuffing(placed, player);
            pipe.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        keepHose();
        hookah.tick();
        if (tickCount % RATTLE_INTERVAL == 0 && getDeltaMovement().horizontalDistance() > WALK_SPEED) {
            playSound(ModSoundEvents.MINECART_HOOKAH_RATTLE.get(), 0.7f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    private void keepHose() {
        if (holder == null) return;
        ServerPlayer online = holder.server.getPlayerList().getPlayer(holder.getUUID());
        if (online != null && online != holder) {
            holder = online;
            hookah.setPlayer(online);
        }
        if (holder.isRemoved() || holder.level() != level()) unhook();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            ItemStackHandler items = hookah.getItemHandler();
            for (int slot = 0; slot < items.getSlots(); slot++) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), items.getStackInSlot(slot));
            }
        }
        super.remove(reason);
    }

    @Override
    public void onRemovedFromLevel() {
        if (!level().isClientSide) unhook();
        super.onRemovedFromLevel();
    }

    @Override
    protected void destroy(DamageSource source) {
        super.destroy(source);
        if (level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) spawnAtLocation(ModBlocks.HOOKAH_BLOCK_ITEM.get());
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HookahCarts.HOOKAH_MINECART_ITEM.get());
    }

    @Override
    public Type getMinecartType() {
        return Type.CHEST;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.HOOKAH_BLOCK.get().defaultBlockState();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(HOOKAH_TAG, hookah.save(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hookah.load(tag.getCompound(HOOKAH_TAG), registryAccess());
    }

    public static final class Hookah extends BackpackHookahState {
        public Hookah() {
            super(null);
        }

        @Override
        public void syncToClient() {
        }

        @Override
        public boolean burnForCast(ServerPlayer caster, int units) {
            if (!super.burnForCast(caster, units)) return false;
            SmokecraftTrigger.fire(caster, "cart_cast");
            return true;
        }
    }
}
