package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.EntityHandsInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.screen.GolemMenu;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class WorkerGolem extends AbstractGolem {
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(WorkerGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LYING = SynchedEntityData.defineId(WorkerGolem.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FALLEN = SynchedEntityData.defineId(WorkerGolem.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> VIBING = SynchedEntityData.defineId(WorkerGolem.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation MOOD_SPEED = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "golem_mood_speed");
    private static final int MOOD_TICKS = 20;
    private static final int SHAKY_PCT = 10;
    private static final int STUMBLE_ODDS = 10;
    private static final int STUMBLE_TICKS = 20;
    private static final int COLLAPSE_TICKS = 40;
    private static final float DOWN_STEP = 0.2f;
    private static final int VIBE_TICKS = 25;
    private static final int NOTE_TICKS = 10;
    private static final float WATER_EDGE_COST = 16f;

    private final GolemMood mood = new GolemMood();
    private final GolemJournal journal = new GolemJournal();
    private final ItemStackHandler feeder = newFeeder();
    final BoundMarks boundMarks = new BoundMarks();
    @Nullable
    private UUID owner;
    private long petReadyAt;
    private int fallTicks;
    private long vibeUntil;
    private float down;
    private float downO;

    protected WorkerGolem(EntityType<? extends WorkerGolem> type, Level level) {
        super(type, level);
        setDropChance(EquipmentSlot.MAINHAND, 2f);
        setPathfindingMalus(PathType.WATER, -1f);
        setPathfindingMalus(PathType.WATER_BORDER, WATER_EDGE_COST);
    }

    public static ItemStackHandler newFeeder() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return GolemMood.Food.of(stack) != null;
            }
        };
    }

    public static Component moodName(int state) {
        return Component.translatable("quasium.golem.mood." + state);
    }

    protected abstract List<Component> statusLines();

    protected abstract InteractionResult interactWith(Player player, InteractionHand hand, ItemStack held);

    abstract boolean worksWith(MarkType type);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOOD, GolemMood.BEST);
        builder.define(LYING, false);
        builder.define(FALLEN, false);
        builder.define(VIBING, false);
    }

    public GolemMood mood() {
        return mood;
    }

    public int moodState() {
        return entityData.get(MOOD);
    }

    public boolean isLying() {
        return entityData.get(LYING);
    }

    public boolean isFallen() {
        return entityData.get(FALLEN);
    }

    public boolean isVibing() {
        return entityData.get(VIBING);
    }

    public void vibe(long now) {
        if (!isVibing()) note("dance");
        vibeUntil = now + VIBE_TICKS;
        entityData.set(VIBING, true);
    }

    public float down(float partialTick) {
        return Mth.lerp(partialTick, downO, down);
    }

    public ItemStackHandler feeder() {
        return feeder;
    }

    public ItemStack tool() {
        return getMainHandItem();
    }

    public GolemJob job() {
        return GolemJob.of(tool());
    }

    public IItemHandler hand() {
        return new RangedWrapper(new EntityHandsInvWrapper(this), 0, 1);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void claim(@Nullable Player player) {
        owner = player == null ? null : player.getUUID();
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    @Nullable
    UUID ownerId() {
        return owner;
    }

    @Nullable
    ServerPlayer onlineOwner() {
        if (owner == null || !(level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    List<BlockPos> marks(MarkType type) {
        return boundMarks.of(this, type);
    }

    public GolemJournal journal() {
        return journal;
    }

    void note(String event, Object... args) {
        journal.add(level().getGameTime(), "quasium.golem.log." + event, args);
    }

    void waitFor(String event) {
        journal.waitFor(level().getGameTime(), "quasium.golem.log." + event);
    }

    public void setMood(int state, float meter) {
        mood.set(state, meter);
        syncMood();
    }

    boolean mayWork() {
        if (mood.canWork()) return true;
        waitFor("starving");
        return false;
    }

    int paced(int ticks) {
        return ticks * 100 / mood.productivity();
    }

    void jobDone() {
        if (isVibing()) return;
        int before = mood.state();
        mood.spend((float) Config.GOLEM_JOB_COST_PCT.getAsDouble());
        afterMood(before);
    }

    @Override
    public void aiStep() {
        updateSwingTime();
        if (level().isClientSide) {
            downO = down;
            down = Mth.approach(down, isFallen() || isLying() ? 1f : 0f, DOWN_STEP);
            if (isVibing() && tickCount % NOTE_TICKS == 0) {
                level().addParticle(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.4, getZ(), random.nextInt(25) / 24.0, 0.0, 0.0);
            }
        } else if (fallTicks > 0 && --fallTicks == 0) {
            entityData.set(FALLEN, false);
        }
        super.aiStep();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isFallen();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().getGameTime() >= vibeUntil) entityData.set(VIBING, false);
        if (tickCount % MOOD_TICKS != 0) return;
        int before = mood.state();
        mood.tick(MOOD_TICKS, isVibing());
        eatFromFeeder();
        afterMood(before);
        stumble();
    }

    private void stumble() {
        int productivity = mood.productivity();
        if (isFallen() || productivity > SHAKY_PCT || productivity == 1 || !getNavigation().isInProgress()) return;
        if (random.nextInt(STUMBLE_ODDS) == 0) fall(STUMBLE_TICKS, SoundEvents.GENERIC_SMALL_FALL);
    }

    private void fall(int ticks, SoundEvent sound) {
        fallTicks = Math.max(fallTicks, ticks);
        entityData.set(FALLEN, true);
        playSound(sound, 0.8f, 0.8f + random.nextFloat() * 0.3f);
    }

    private void eatFromFeeder() {
        GolemMood.Food food = GolemMood.Food.of(feeder.getStackInSlot(0));
        if (food == null || !mood.wantsFromFeeder(food)) return;
        ItemStack meal = feeder.extractItem(0, 1, false);
        eat(food, meal);
        note("ate", meal.getDescriptionId());
    }

    void afterMood(int before) {
        syncMood();
        if (mood.state() < before) {
            note("better", "quasium.golem.mood." + mood.state());
            burst(ParticleTypes.HAPPY_VILLAGER, 8);
            playSound(ModSoundEvents.GOLEM_READY.get(), 0.6f, 1.2f);
        } else if (mood.state() > before) {
            note("worse", "quasium.golem.mood." + mood.state());
            burst(ParticleTypes.SMOKE, 10);
            fall(COLLAPSE_TICKS, SoundEvents.GENERIC_BIG_FALL);
        }
    }

    private void syncMood() {
        boolean lying = mood.productivity() == 1;
        if (lying != isLying() && !firstTick) note(lying ? "lay_down" : "got_up");
        entityData.set(MOOD, mood.state());
        entityData.set(LYING, lying);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        double amount = Math.max(mood.productivity(), SHAKY_PCT) / 100.0 - 1.0;
        AttributeModifier current = speed.getModifier(MOOD_SPEED);
        if (amount == 0.0) {
            speed.removeModifier(MOOD_SPEED);
        } else if (current == null || current.amount() != amount) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(MOOD_SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void burst(ParticleOptions particle, int count) {
        ((ServerLevel) level()).sendParticles(particle, getX(), getY() + getBbHeight() + 0.2, getZ(), count, 0.3, 0.2, 0.3, 0.0);
    }

    @Override
    protected final InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (owner == null && !level().isClientSide) claim(player);
        ItemStack held = player.getItemInHand(hand);
        InteractionResult own = interactWith(player, hand, held);
        if (own != InteractionResult.PASS) return own;
        if (hand == InteractionHand.MAIN_HAND && GolemJob.isTool(held)) return GolemTools.swap(this, player, held);
        GolemMood.Food food = GolemMood.Food.of(held);
        if (hand != InteractionHand.MAIN_HAND || (!held.isEmpty() && food == null)) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!player.isSecondaryUseActive()) openScreen(player);
        else if (food == null) pet(player);
        else feed(player, hand, false);
        return InteractionResult.CONSUME;
    }

    private void openScreen(Player player) {
        if (!isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("quasium.golem.not_owner"), true);
            return;
        }
        List<Component> status = statusLines();
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new GolemMenu(id, inventory, this), getDisplayName()),
                buf -> GolemMenu.writeOpening(buf, this, status));
    }

    public boolean feed(Player player, InteractionHand hand, boolean force) {
        ItemStack held = player.getItemInHand(hand);
        GolemMood.Food food = GolemMood.Food.of(held);
        if (food == null) return false;
        if (!force && mood.overfeeds(food)) {
            player.displayClientMessage(Component.translatable("quasium.golem.overfeed", Math.round(mood.wasted(food) * 100)), true);
            return false;
        }
        int before = mood.state();
        eat(food, held.copyWithCount(1));
        note("fed", held.getDescriptionId());
        held.consume(1, player);
        afterMood(before);
        player.displayClientMessage(Component.translatable("quasium.golem.fed", moodName(mood.state()), Math.round(mood.meter())), true);
        return true;
    }

    void eat(GolemMood.Food food, ItemStack stack) {
        mood.feed(food);
        playSound(SoundEvents.GENERIC_EAT, 0.8f, 0.9f + random.nextFloat() * 0.2f);
        ((ServerLevel) level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), getX(), getEyeY(), getZ(),
                6, 0.15, 0.1, 0.15, 0.05);
    }

    public boolean pet(Player player) {
        int wait = petCooldown();
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("quasium.golem.pet_wait", wait), true);
            return false;
        }
        petReadyAt = level().getGameTime() + Config.GOLEM_PET_COOLDOWN_SECONDS.getAsInt() * 20L;
        mood.pet((float) Config.GOLEM_PET_PCT.getAsDouble());
        syncMood();
        note("petted");
        burst(ParticleTypes.HEART, 3);
        return true;
    }

    public int petCooldown() {
        return (int) Math.max(0L, (petReadyAt - level().getGameTime() + 19) / 20);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        spawnAtLocation(feeder.getStackInSlot(0));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && isDeadOrDying()) GolemBindings.forgetAll(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Mood", mood.save());
        tag.put("Feeder", feeder.serializeNBT(registryAccess()));
        journal.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        mood.load(tag.getCompound("Mood"));
        feeder.deserializeNBT(registryAccess(), tag.getCompound("Feeder"));
        journal.load(tag);
        syncMood();
    }
}
