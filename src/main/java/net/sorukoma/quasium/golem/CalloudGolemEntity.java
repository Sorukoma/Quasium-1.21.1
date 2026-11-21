package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.item.CharcoalItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;

public class CalloudGolemEntity extends WorkerGolem implements InventoryCarrier {
    private static final int IDLE_RADIUS = 8;
    private static final int ANCHOR_CHECK_TICKS = 100;

    private final SimpleContainer coal = new SimpleContainer(Config.COAL_GOLEM_INVENTORY.get());
    private boolean refillPaused;
    private boolean refillRequested;
    private ItemStack tobacco = ItemStack.EMPTY;
    private int ash;

    public CalloudGolemEntity(EntityType<? extends CalloudGolemEntity> type, Level level) {
        super(type, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.COAL_GOLEM_HEALTH.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.COAL_GOLEM_SPEED.get());
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(1, new GolemMealGoal(this));
        goalSelector.addGoal(2, new HeatHookahGoal(this));
        goalSelector.addGoal(2, new RefillBowlGoal(this));
        goalSelector.addGoal(3, new FetchCoalGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, true, this::threatens));
    }

    private boolean threatens(LivingEntity entity) {
        if (!(entity instanceof Enemy) || !(entity instanceof Mob mob) || mob.getTarget() == null) return false;
        return mob.getTarget() == this || mob.getTarget().getUUID().equals(ownerId());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % ANCHOR_CHECK_TICKS != 0) return;
        List<BlockPos> heatMarks = marks(MarkType.HEAT);
        if (heatMarks.isEmpty()) clearRestriction();
        else restrictTo(heatMarks.get(0), IDLE_RADIUS);
    }

    @Override
    protected List<Component> statusLines() {
        return List.of(Component.translatable("quasium.calloud_golem.coal", coalCount(), coal.getContainerSize()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.igniteForSeconds(Config.COAL_GOLEM_FIRE_SECONDS.get());
        return hit;
    }

    @Override
    boolean worksWith(MarkType type) {
        return type == MarkType.HEAT || type == MarkType.FILL || type == MarkType.FEED;
    }

    @Override
    protected InteractionResult interactWith(Player player, InteractionHand hand, ItemStack held) {
        if (!(held.getItem() instanceof CharcoalItem)) return InteractionResult.PASS;
        if (!level().isClientSide) {
            addCoal(held);
            playSound(SoundEvents.ITEM_PICKUP, 0.4f, 1f);
            player.displayClientMessage(Component.translatable("quasium.calloud_golem.coal", coalCount(), coal.getContainerSize()), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSoundEvents.GOLEM_COAL_STEP.get(), 0.4f, 1f);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(Items.CLAY_BALL);
        spawnAtLocation(ModItems.CALLOUD.get());
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        coal.removeAllItems().forEach(this::spawnAtLocation);
        spawnAtLocation(tobacco);
        spawnAtLocation(new ItemStack(ModItems.BURNED_TOBACCO.get(), ash));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        writeInventoryToTag(tag, registryAccess());
        tag.putBoolean("RefillPaused", refillPaused);
        if (!tobacco.isEmpty()) tag.put("Tobacco", tobacco.save(registryAccess()));
        tag.putInt("Ash", ash);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readInventoryFromTag(tag, registryAccess());
        refillPaused = tag.getBoolean("RefillPaused");
        tobacco = ItemStack.parseOptional(registryAccess(), tag.getCompound("Tobacco"));
        ash = Math.max(0, tag.getInt("Ash"));
    }

    @Override
    public SimpleContainer getInventory() {
        return coal;
    }

    public boolean refillPaused() {
        return refillPaused;
    }

    public boolean toggleRefill() {
        refillPaused = !refillPaused;
        note(refillPaused ? "refill_paused" : "refill_resumed");
        return true;
    }

    public boolean requestRefill() {
        refillRequested = true;
        note("refill_requested");
        return true;
    }

    boolean takeRefillRequest() {
        boolean requested = refillRequested;
        refillRequested = false;
        return requested;
    }

    ItemStack tobacco() {
        return tobacco;
    }

    void carry(ItemStack grams) {
        tobacco = grams;
    }

    void addAsh(int grams) {
        ash += grams;
    }

    boolean carriesSatchel() {
        return !tobacco.isEmpty() || ash > 0;
    }

    int unloadSatchel(IItemHandler store) {
        int stored = ash;
        spawnAtLocation(ItemHandlerHelper.insertItemStacked(store, tobacco, false));
        spawnAtLocation(ItemHandlerHelper.insertItemStacked(store, new ItemStack(ModItems.BURNED_TOBACCO.get(), ash), false));
        tobacco = ItemStack.EMPTY;
        ash = 0;
        return stored;
    }

    public int coalCount() {
        int count = 0;
        for (ItemStack stack : coal.getItems()) count += stack.getCount();
        return count;
    }

    int coalRoom() {
        return coal.getContainerSize() - coalCount();
    }

    boolean hasCoal() {
        return !coal.isEmpty();
    }

    ItemStack nextCoal() {
        for (ItemStack stack : coal.getItems()) {
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    public void addCoal(ItemStack stack) {
        int taken = Math.min(stack.getCount(), coalRoom());
        if (stack.getItem() instanceof CharcoalItem && taken > 0) coal.addItem(stack.split(taken));
    }
}
