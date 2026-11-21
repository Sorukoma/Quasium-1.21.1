package net.sorukoma.quasium.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.draft.BottlePouring;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ClayGolemEntity extends WorkerGolem {
    private static final ResourceLocation MENTHOL_SPEED = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "clay_menthol_speed");
    private static final ResourceLocation RESIN_HEALTH = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "clay_resin_health");
    private static final int CARGO_SLOTS = 54;
    private static final double PATH_RANGE = 48.0;
    private static final int FORGET_UNREACHABLE_TICKS = 1200;
    private static final int HERD_STEP = 2;
    private static final int HERD_MIN = 2;
    private static final int HERD_MAX = 32;

    private final ItemStackHandler cargo = new ItemStackHandler(CARGO_SLOTS);
    private final ClayUpgrades upgrades = new ClayUpgrades();
    private final Set<BlockPos> unreachable = new HashSet<>();
    private final Set<BlockPos> stumps = new HashSet<>();
    private long forgetUnreachableAt;
    private boolean sommelier;
    private boolean toolkeeper;
    private boolean deliveredThisCycle;
    private int herdLimit = 8;

    public ClayGolemEntity(EntityType<? extends ClayGolemEntity> type, Level level) {
        super(type, level);
        applyConfigBase();
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Config.CLAY_GOLEM_HEALTH.getDefault())
                .add(Attributes.MOVEMENT_SPEED, Config.CLAY_GOLEM_SPEED.getDefault())
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, PATH_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    private void applyConfigBase() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.CLAY_GOLEM_HEALTH.getAsInt());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Config.CLAY_GOLEM_SPEED.getAsDouble());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(PATH_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new NapGoal(this));
        goalSelector.addGoal(1, new ClayFightGoal(this));
        goalSelector.addGoal(1, new GolemMealGoal(this));
        goalSelector.addGoal(2, new ProgramGoal(this));
        goalSelector.addGoal(2, new SmokeWithOwnerGoal(this));
        goalSelector.addGoal(2, new ToolkeeperGoal(this));
        goalSelector.addGoal(2, new ClayDeliverGoal(this));
        goalSelector.addGoal(3, new ClayHarvestGoal(this));
        goalSelector.addGoal(3, new CuringGoal(this));
        goalSelector.addGoal(3, new LumberjackGoal(this));
        goalSelector.addGoal(3, new HerderGoal(this));
        goalSelector.addGoal(3, new ShearerGoal(this));
        goalSelector.addGoal(3, new FisherGoal(this));
        goalSelector.addGoal(4, new ClayGuardGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        goalSelector.addGoal(7, new BleatGoal(this));
    }

    public boolean isSommelier() {
        return sommelier;
    }

    public boolean becomeSommelier() {
        if (sommelier) return false;
        sommelier = true;
        return true;
    }

    public boolean becomeToolkeeper() {
        if (toolkeeper) return false;
        toolkeeper = true;
        return true;
    }

    boolean savesTool() {
        return toolkeeper && (job() != GolemJob.TOBACCO || GolemPrograms.running(this)) && GolemTools.worn(this);
    }

    boolean choring() {
        return goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.isRunning() && (goal.getGoal() instanceof ClayChoreGoal || goal.getGoal() instanceof FisherGoal));
    }

    Set<BlockPos> stumps() {
        return stumps;
    }

    public int herdLimit() {
        return herdLimit;
    }

    public boolean adjustHerd(boolean more) {
        int next = Mth.clamp(herdLimit + (more ? HERD_STEP : -HERD_STEP), HERD_MIN, HERD_MAX);
        if (next == herdLimit) return false;
        herdLimit = next;
        note("herd_limit", herdLimit);
        return true;
    }

    public IItemHandler cargo() {
        return cargo;
    }

    @Override
    protected List<Component> statusLines() {
        return upgrades.describe(sommelier, toolkeeper);
    }

    @Override
    boolean worksWith(MarkType type) {
        return type != MarkType.HEAT;
    }

    @Override
    protected InteractionResult interactWith(Player player, InteractionHand hand, ItemStack held) {
        NoteType note = NoteType.byId(EssenceBottleItem.typeOf(held));
        if (note == null) return InteractionResult.PASS;
        if (!level().isClientSide) pourNote(player, hand, held, note);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void pourNote(Player player, InteractionHand hand, ItemStack bottle, NoteType note) {
        int perCharge = Config.CLAY_UPGRADE_ML_PER_CHARGE.getAsInt();
        if (!ClayUpgrades.USEFUL.contains(note)) {
            player.displayClientMessage(Component.translatable("quasium.clay_golem.note_useless", note.displayName()), true);
        } else if (!upgrades.hasRoomFor(note)) {
            player.displayClientMessage(Component.translatable("quasium.clay_golem.slots_full"), true);
        } else if (EssenceBottleItem.amountOf(bottle) < perCharge) {
            player.displayClientMessage(Component.translatable("quasium.clay_golem.bottle_too_small", perCharge), true);
        } else {
            int poured = BottlePouring.pour(bottle, player, hand, ml -> upgrades.pour(note, ml));
            refreshUpgrades();
            level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 0.8f, 1.1f);
            player.displayClientMessage(Component.translatable("quasium.clay_golem.poured", note.displayName(), poured), true);
        }
    }

    private void refreshUpgrades() {
        toggle(Attributes.MOVEMENT_SPEED, new AttributeModifier(MENTHOL_SPEED, Config.CLAY_MENTHOL_SPEED_PCT.getAsInt() / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE), upgrades.active(NoteType.MENTHOL));
        int resin = Config.CLAY_RESIN_EXTRA_HEALTH.getAsInt();
        if (toggle(Attributes.MAX_HEALTH, new AttributeModifier(RESIN_HEALTH, resin, AttributeModifier.Operation.ADD_VALUE),
                upgrades.active(NoteType.RESIN))) {
            heal(resin);
        }
    }

    private boolean toggle(Holder<Attribute> attribute, AttributeModifier modifier, boolean on) {
        AttributeInstance instance = getAttribute(attribute);
        boolean added = on && !instance.hasModifier(modifier.id());
        if (on) instance.addOrUpdateTransientModifier(modifier);
        else instance.removeModifier(modifier.id());
        return added;
    }

    private IItemHandler usableCargo() {
        int stacks = Config.CLAY_GOLEM_STACKS.getAsInt()
                + (upgrades.active(NoteType.NECTAR) ? Config.CLAY_NECTAR_EXTRA_STACKS.getAsInt() : 0);
        return new RangedWrapper(cargo, 0, Math.min(CARGO_SLOTS, stacks));
    }

    boolean hasCargo() {
        return cargoCount() > 0;
    }

    int cargoCount() {
        int count = 0;
        for (int slot = 0; slot < cargo.getSlots(); slot++) count += cargo.getStackInSlot(slot).getCount();
        return count;
    }

    boolean hasRoom() {
        IItemHandler usable = usableCargo();
        for (int slot = 0; slot < usable.getSlots(); slot++) {
            if (usable.getStackInSlot(slot).isEmpty()) return true;
        }
        return false;
    }

    boolean wants(ItemStack stack) {
        return burnsJunk(stack) || stow(stack, true).getCount() < stack.getCount();
    }

    ItemStack stow(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(usableCargo(), stack, simulate);
    }

    private boolean burnsJunk(ItemStack stack) {
        return stack.is(ClayGolems.JUNK) && upgrades.active(NoteType.ASH);
    }

    void pickUp(ItemEntity item) {
        ItemStack stack = item.getItem();
        if (burnsJunk(stack)) {
            burn(item.position());
            item.discard();
            return;
        }
        ItemStack rest = ItemHandlerHelper.insertItemStacked(usableCargo(), stack, false);
        take(item, stack.getCount() - rest.getCount());
        if (rest.isEmpty()) item.discard();
        else item.setItem(rest);
    }

    void takeIn(List<ItemStack> drops, BlockPos at) {
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (burnsJunk(drop)) {
                burn(Vec3.atCenterOf(at));
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItemStacked(usableCargo(), drop, false);
            if (!rest.isEmpty()) Block.popResource(level(), at, rest);
        }
    }

    private void burn(Vec3 at) {
        ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.2, at.z, 8, 0.15, 0.1, 0.15, 0.01);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4f, 1.4f);
    }

    int unloadAt(BlockPos mark, boolean simulate) {
        int moved = 0;
        for (Direction side : Direction.values()) moved += unloadInto(mark.relative(side), side.getOpposite(), simulate);
        return moved;
    }

    int unloadInto(BlockPos pos, @Nullable Direction side, boolean simulate) {
        IItemHandler container = level().isLoaded(pos) ? level().getCapability(Capabilities.ItemHandler.BLOCK, pos, side) : null;
        if (container == null) return 0;
        int moved = 0;
        Predicate<ItemStack> kept = ClayChoreGoal.kept(this);
        for (int slot = 0; slot < cargo.getSlots(); slot++) {
            ItemStack stack = cargo.getStackInSlot(slot);
            if (stack.isEmpty() || kept.test(stack)) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(container, stack, simulate);
            moved += stack.getCount() - rest.getCount();
            if (!simulate) cargo.setStackInSlot(slot, rest);
        }
        return moved;
    }

    void endHarvestTrip() {
        deliveredThisCycle = false;
        finishTask();
    }

    void endDelivery() {
        if (!deliveredThisCycle) rewardOwner();
        deliveredThisCycle = true;
        finishTask();
    }

    private void finishTask() {
        upgrades.spendTask();
        refreshUpgrades();
        jobDone();
    }

    private void rewardOwner() {
        ServerPlayer player = onlineOwner();
        if (player != null) SmokecraftTrigger.fire(player, "golem_harvest");
    }

    boolean isUnreachable(BlockPos pos) {
        if (level().getGameTime() >= forgetUnreachableAt) unreachable.clear();
        return unreachable.contains(pos);
    }

    void markUnreachable(BlockPos pos) {
        note("unreachable", pos);
        if (unreachable.isEmpty()) forgetUnreachableAt = level().getGameTime() + FORGET_UNREACHABLE_TICKS;
        unreachable.add(pos.immutable());
    }

    @Nullable
    BlockPos boundGuardMark() {
        List<BlockPos> posts = marks(MarkType.GUARD);
        return posts.isEmpty() ? null : posts.get(0);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        for (int slot = 0; slot < cargo.getSlots(); slot++) {
            spawnAtLocation(cargo.getStackInSlot(slot));
        }
        if (sommelier) spawnAtLocation(ClayGolems.SOMMELIER_MODULE.get());
        if (toolkeeper) spawnAtLocation(ClayGolems.TOOLKEEPER_MODULE.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Cargo", cargo.serializeNBT(registryAccess()));
        tag.put("Notes", upgrades.save());
        tag.putBoolean("Sommelier", sommelier);
        tag.putBoolean("Toolkeeper", toolkeeper);
        tag.putLongArray("Stumps", stumps.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("HerdLimit", herdLimit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cargo.deserializeNBT(registryAccess(), tag.getCompound("Cargo"));
        upgrades.load(tag.getList("Notes", Tag.TAG_COMPOUND));
        sommelier = tag.getBoolean("Sommelier");
        toolkeeper = tag.getBoolean("Toolkeeper");
        stumps.clear();
        for (long stump : tag.getLongArray("Stumps")) stumps.add(BlockPos.of(stump));
        if (tag.contains("HerdLimit")) herdLimit = Mth.clamp(tag.getInt("HerdLimit"), HERD_MIN, HERD_MAX);
        applyConfigBase();
        refreshUpgrades();
        if (tag.contains("Health")) setHealth(tag.getFloat("Health"));
    }

    @Override
    public void stopSleeping() {
        if (!GolemNap.holdsSleep(this) || isDeadOrDying()) super.stopSleeping();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.GOLEM_CLAY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DECORATED_POT_SHATTER;
    }
}
