package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.block.entity.HookahBlockEntity;
import net.sorukoma.quasium.draft.DraftNode;
import net.sorukoma.quasium.entity.HookahConnectionEntity;
import net.sorukoma.quasium.item.HookahPipeItem;
import net.sorukoma.quasium.magic.SmokeCasting;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualMath;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;
import org.jetbrains.annotations.Nullable;

public class RitualBrazierBlockEntity extends HookahBlockEntity implements DraftNode {
    private static final int SECOND = RitualMath.TICKS_PER_SECOND;
    private static final int FLARE_SPARKS = 6;
    private static final int PREVIEW_TICKS = 10;
    private static final long UNHEARD = -1;

    private ItemStack catalyst = ItemStack.EMPTY;
    private ItemStack filter = ItemStack.EMPTY;
    @Nullable
    private RitualRun run;
    private float networkDraft;
    private long syncedOrigin = Long.MIN_VALUE;
    @Nullable
    private RitualPulse pulse;
    private long heardPulse = UNHEARD;
    private RitualPreview preview = RitualPreview.NONE;

    public RitualBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(RitualRegistry.RITUAL_BRAZIER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualBrazierBlockEntity brazier) {
        HookahBlockEntity.tick(level, pos, state, brazier);
        if (level instanceof ServerLevel server) brazier.tickRitual(server);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RitualBrazierBlockEntity brazier) {
        RitualPulse beat = brazier.pulse;
        if (beat == null) return;
        long current = beat.lastPulse(level.getGameTime() + 0.5);
        long heard = brazier.heardPulse;
        brazier.heardPulse = current;
        if (current < 1 || current == heard || heard == UNHEARD) return;
        level.playLocalSound(pos, RitualRegistry.RITUAL_PULSE.get(), SoundSource.BLOCKS, 0.8f, 1f, false);
        for (int spark = 0; spark < FLARE_SPARKS; spark++) {
            double angle = spark * Math.PI * 2 / FLARE_SPARKS;
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5 + Math.cos(angle) * 0.25, pos.getY() + 0.75,
                    pos.getZ() + 0.5 + Math.sin(angle) * 0.25, 0, 0.03, 0);
        }
        level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 0, 0, 0);
    }

    public void tickRitual(ServerLevel level) {
        if (run == null) {
            networkDraft = 0f;
            if (getBlockState().getValue(RitualBrazierBlock.LIT)) setLit(false);
            if (level.getGameTime() % PREVIEW_TICKS == 0) show(RitualPreview.of(level, this));
            return;
        }
        float draft = networkDraft;
        networkDraft = 0f;
        switch (run.tick(level, this, draft)) {
            case DONE -> finish(level, true);
            case BROKEN -> finish(level, false);
            case RUNNING -> keepBeat(level);
        }
    }

    private void keepBeat(ServerLevel level) {
        if (run.ticks() % SECOND != 0) return;
        setChanged();
        if (level.getGameTime() - run.ticks() != syncedOrigin) changed();
    }

    private void show(RitualPreview next) {
        if (next.equals(preview)) return;
        preview = next;
        changed();
    }

    void begin(RitualRun started) {
        run = started;
        preview = RitualPreview.NONE;
        setLit(true);
        changed();
    }

    private void finish(ServerLevel level, boolean success) {
        RitualRun ended = run;
        run = null;
        ended.hideBars();
        if (success) RitualOutcome.complete(level, this, ended);
        else RitualOutcome.fail(level, this, ended);
        setLit(false);
        changed();
    }

    private void setLit(boolean lit) {
        if (level != null && getBlockState().getValue(RitualBrazierBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(RitualBrazierBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Nullable
    public RitualRun run() {
        return run;
    }

    @Nullable
    public RitualPulse pulse() {
        return pulse;
    }

    public RitualPreview preview() {
        return preview;
    }

    public ItemStack catalyst() {
        return catalyst;
    }

    public void setCatalyst(ItemStack stack) {
        catalyst = stack.copy();
        changed();
    }

    public ItemStack filter() {
        return filter;
    }

    public boolean hasFilter() {
        return !filter.isEmpty();
    }

    public void setFilter(ItemStack stack) {
        filter = stack.copy();
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static boolean reservedNear(Level level, BlockPos pedestal) {
        int reach = RitualConfig.PEDESTAL_MAX_DISTANCE.getAsInt();
        for (BlockPos pos : BlockPos.betweenClosed(pedestal.offset(-reach, 0, -reach), pedestal.offset(reach, 1, reach))) {
            if (level.getBlockEntity(pos) instanceof RitualBrazierBlockEntity brazier && brazier.run != null && brazier.run.reserves(pedestal)) {
                return true;
            }
        }
        return false;
    }

    public boolean beatsWith(ServerPlayer player) {
        if (run == null || level == null || !HookahPipeItem.holdsPipe(player)) return false;
        if (pipedTo(player)) return true;
        double reach = RitualConfig.HARNESS_LINK_RADIUS.getAsInt();
        return QuasiumHarness.worn(player) && player.distanceToSqr(worldPosition.getCenter()) <= reach * reach;
    }

    public void pressAt(ServerPlayer player, double gameTime) {
        if (run == null || level == null) return;
        press(player, gameTime - (level.getGameTime() - run.ticks()));
    }

    public RitualRhythm.Beat press(ServerPlayer player, double runTick) {
        if (run == null) return RitualRhythm.Beat.PAUSED;
        RitualRhythm.Beat beat = run.press(player, runTick);
        RitualCues.beat(player, beat);
        return beat;
    }

    private boolean pipedTo(Player player) {
        return !level.getEntitiesOfClass(HookahConnectionEntity.class, new AABB(worldPosition).inflate(1.0),
                connection -> player.getUUID().equals(connection.getAttachedPlayerUUID())).isEmpty();
    }

    @Override
    public boolean tryPuff(Player player) {
        if (level == null || level.isClientSide) return false;
        if (run == null) {
            release(player, "quasium.ritual.pipe_idle");
            return false;
        }
        zone().notePuff(player.getUUID(), level.getGameTime());
        if (player instanceof ServerPlayer puller) SmokeCasting.drew(puller);
        return true;
    }

    private void release(Player player, String reason) {
        for (HookahConnectionEntity connection : level.getEntitiesOfClass(HookahConnectionEntity.class, new AABB(worldPosition).inflate(1.0),
                connection -> player.getUUID().equals(connection.getAttachedPlayerUUID()) && connection.isPuffing())) {
            connection.stopPuffing(this, player);
        }
        player.displayClientMessage(Component.translatable(reason), true);
    }

    @Override
    public boolean isSmoldering() {
        return run != null;
    }

    @Override
    @Nullable
    public String coldReason() {
        return null;
    }

    @Override
    public int flaskCharges() {
        return Config.FLASK_MAX_CHARGES.getAsInt();
    }

    @Override
    public boolean addFlaskCharge() {
        return false;
    }

    @Override
    public boolean useFlaskCharge() {
        return false;
    }

    @Override
    public int draftDemand(Direction side) {
        return run == null ? 0 : run.recipe().rate();
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        if (run != null) networkDraft += run.recipe().rate() * efficiency / SECOND;
    }

    @Override
    public void setRemoved() {
        if (run != null) run.hideBars();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeItems(tag, registries);
        if (run != null) tag.put("Run", run.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        catalyst = tag.contains("Catalyst") ? ItemStack.parseOptional(registries, tag.getCompound("Catalyst")) : ItemStack.EMPTY;
        filter = tag.contains("Filter") ? ItemStack.parseOptional(registries, tag.getCompound("Filter")) : ItemStack.EMPTY;
        if (tag.contains("Run")) run = RitualRun.load(tag.getCompound("Run"));
        RitualPulse synced = tag.contains("Pulse") ? RitualPulse.load(tag.getCompound("Pulse")) : null;
        if (synced == null || pulse == null || synced.origin() != pulse.origin()) heardPulse = UNHEARD;
        pulse = synced;
        preview = tag.contains("Preview") ? RitualPreview.load(tag.getCompound("Preview")) : RitualPreview.NONE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeItems(tag, registries);
        if (run != null && level != null) {
            syncedOrigin = level.getGameTime() - run.ticks();
            tag.put("Pulse", new RitualPulse(syncedOrigin, run.recipe().pulseMs(), run.recipe().windowMs()).save());
        }
        if (!preview.isEmpty()) tag.put("Preview", preview.save());
        return tag;
    }

    private void writeItems(CompoundTag tag, HolderLookup.Provider registries) {
        if (!catalyst.isEmpty()) tag.put("Catalyst", catalyst.save(registries));
        if (!filter.isEmpty()) tag.put("Filter", filter.save(registries));
    }
}
