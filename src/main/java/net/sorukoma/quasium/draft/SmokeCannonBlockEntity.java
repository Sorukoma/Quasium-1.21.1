package net.sorukoma.quasium.draft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.ecology.SmokeDensity;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class SmokeCannonBlockEntity extends BlockEntity implements DraftNode, NoteNode {
    private static final int AIM_TICKS = 10;
    private static final float SHOT_SPEED = 1.5f;
    private static final double BARREL_LIFT = 0.19;
    private static final double MUZZLE = 0.9;

    @Nullable
    private UUID owner;
    private float charge;
    private int ash;

    public SmokeCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ForgeBlocks.SMOKE_CANNON_BE.get(), pos, state);
    }

    public static int tankCapacity() {
        return Config.CANNON_TANK_ML.getAsInt();
    }

    private static int shotDraft() {
        return Config.CANNON_DRAFT_PER_SHOT.getAsInt();
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int ashMl() {
        return ash;
    }

    public boolean charged() {
        return charge >= shotDraft();
    }

    public int chargePercent() {
        return shotDraft() <= 0 ? 100 : Math.round(charge * 100f / shotDraft());
    }

    public int fillAsh(int ml) {
        int accepted = Math.min(ml, tankCapacity() - ash);
        if (accepted <= 0) return 0;
        ash += accepted;
        setChanged();
        return accepted;
    }

    @Override
    public int acceptNote(NoteType note, int ml, boolean simulate) {
        if (note != NoteType.ASH) return 0;
        return simulate ? Math.max(0, Math.min(ml, tankCapacity() - ash)) : fillAsh(ml);
    }

    @Override
    public int draftDemand(Direction side) {
        return charged() ? 0 : shotDraft();
    }

    @Override
    public void draftSupplied(Direction side, float efficiency) {
        charge = Math.min(shotDraft(), charge + efficiency * shotDraft() / 20f);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmokeCannonBlockEntity cannon) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % AIM_TICKS != 0) return;
        if (!cannon.charged() || cannon.ash < Config.CANNON_ASH_PER_SHOT.getAsInt()) return;
        Vec3 barrel = Vec3.atCenterOf(pos).add(0, BARREL_LIFT, 0);
        Monster target = cannon.nearestVisibleMonster(serverLevel, barrel);
        if (target != null) cannon.fire(serverLevel, state, barrel, target.getBoundingBox().getCenter());
    }

    @Nullable
    private Monster nearestVisibleMonster(ServerLevel level, Vec3 barrel) {
        double range = Config.CANNON_RANGE.getAsInt();
        return level.getEntitiesOfClass(Monster.class, new AABB(worldPosition).inflate(range),
                        monster -> monster.isAlive() && aim(monster).distanceToSqr(barrel) <= range * range)
                .stream()
                .sorted(Comparator.comparingDouble(monster -> aim(monster).distanceToSqr(barrel)))
                .filter(monster -> clearShot(level, barrel, aim(monster)))
                .findFirst()
                .orElse(null);
    }

    private static Vec3 aim(Monster monster) {
        return monster.getBoundingBox().getCenter();
    }

    private static Vec3 muzzle(Vec3 barrel, Vec3 aim) {
        return barrel.add(aim.subtract(barrel).normalize().scale(MUZZLE));
    }

    private static boolean clearShot(ServerLevel level, Vec3 barrel, Vec3 aim) {
        ClipContext ray = new ClipContext(muzzle(barrel, aim), aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        return level.clip(ray).getType() == HitResult.Type.MISS;
    }

    private void fire(ServerLevel level, BlockState state, Vec3 barrel, Vec3 aim) {
        Vec3 heading = aim.subtract(barrel);
        Vec3 muzzle = muzzle(barrel, aim);
        SmokeShotEntity shot = new SmokeShotEntity(level, muzzle, owner);
        shot.shoot(heading.x, heading.y, heading.z, SHOT_SPEED, 0f);
        level.addFreshEntity(shot);
        charge = 0f;
        ash -= Config.CANNON_ASH_PER_SHOT.getAsInt();
        SmokeDensity.add(level, worldPosition, Config.CANNON_DENSITY_PER_SHOT.getAsInt());
        setChanged();
        level.playSound(null, worldPosition, ModSoundEvents.CANNON_SHOT.get(), SoundSource.BLOCKS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
        level.sendParticles(ModParticles.noteSmoke(NoteType.ASH.color()), muzzle.x, muzzle.y, muzzle.z, 6, 0.1, 0.1, 0.1, 0.03);
        Direction facing = Direction.getNearest(heading.x, 0, heading.z);
        if (facing != state.getValue(SmokeCannonBlock.FACING)) {
            level.setBlock(worldPosition, state.setValue(SmokeCannonBlock.FACING, facing), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putFloat("Charge", charge);
        tag.putInt("Ash", ash);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        charge = tag.getFloat("Charge");
        ash = tag.getInt("Ash");
    }
}
