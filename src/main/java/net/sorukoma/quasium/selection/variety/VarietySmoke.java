package net.sorukoma.quasium.selection.variety;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.component.ModDataComponents.BowlTobaccoGroup;
import net.sorukoma.quasium.component.ModDataComponents.HookahBowlContents;
import net.sorukoma.quasium.effect.ModEffects;
import net.sorukoma.quasium.entity.ToxicSmokeEntity;
import net.sorukoma.quasium.item.TobaccoVarieties;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.sound.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class VarietySmoke {
    private static final String CHORUS_READY_AT = "QuasiumChorusReadyAt";
    private static final double FLOOR_EPSILON = 1.0E-4;

    private VarietySmoke() {
    }

    public static void onExhale(ServerPlayer player, @Nullable HookahBowlContents contents) {
        if (contents == null) return;
        List<BowlTobaccoGroup> groups = contents.groups();
        Set<String> burning = contents.slotAssignments().values().stream()
                .filter(index -> index >= 0 && index < groups.size()).map(groups::get)
                .filter(BowlTobaccoGroup::isSelfMixed).map(BowlTobaccoGroup::base).collect(Collectors.toSet());
        if (burning.contains(TobaccoVarieties.ECHO)) muffle(player);
        if (burning.contains(TobaccoVarieties.ASHEN)) breatheFire(player);
        if (burning.contains(TobaccoVarieties.CHORUS)) blink(player);
    }

    public static void muffle(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.ECHO_MUFFLE, SelectionConfig.ECHO_MUFFLE_SECONDS.get() * 20));
        player.playNotifySound(ModSoundEvents.ECHO_LEAF_MUFFLE.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    public static void breatheFire(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, SelectionConfig.ASHEN_FIRE_RESISTANCE_SECONDS.get() * 20));
        Vec3 look = player.getLookAngle();
        ToxicSmokeEntity flame = new ToxicSmokeEntity(player.level(), player, player.getEyePosition().add(look.scale(0.35)),
                look.scale(0.45), SelectionConfig.ASHEN_BREATH_DAMAGE.get(), false);
        flame.setBurning(true);
        player.level().addFreshEntity(flame);
    }

    public static boolean blink(ServerPlayer player) {
        long now = player.level().getGameTime();
        long readyAt = player.getPersistentData().getLong(CHORUS_READY_AT);
        if (now < readyAt) {
            player.displayClientMessage(Component.translatable("quasium.message.chorus.cooldown", (readyAt - now + 19) / 20), true);
            return false;
        }
        Vec3 target = blinkTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("quasium.message.chorus.nowhere"), true);
            return false;
        }
        Vec3 from = player.position();
        player.stopRiding();
        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();
        player.level().gameEvent(GameEvent.TELEPORT, from, GameEvent.Context.of(player));
        player.level().playSound(null, target.x, target.y, target.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getPersistentData().putLong(CHORUS_READY_AT, now + SelectionConfig.CHORUS_TELEPORT_COOLDOWN_SECONDS.get() * 20L);
        return true;
    }

    @Nullable
    public static Vec3 blinkTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = SelectionConfig.CHORUS_TELEPORT_RANGE.get();
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double reach = hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(eye);
        for (double distance = reach; distance >= 1; distance -= 0.5) {
            Vec3 feet = landingAt(player, eye.add(look.scale(distance)));
            if (feet != null) return feet;
        }
        return null;
    }

    @Nullable
    public static Vec3 landingAt(ServerPlayer player, Vec3 spot) {
        Level level = player.level();
        if (!level.hasChunkAt(BlockPos.containing(spot))) return null;
        AABB footprint = player.getBoundingBox().move(spot.x - player.getX(), 0, spot.z - player.getZ());
        for (BlockPos pos = BlockPos.containing(spot); pos.getY() >= level.getMinBuildHeight(); pos = pos.below()) {
            double floor = Double.NEGATIVE_INFINITY;
            for (AABB part : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                AABB placed = part.move(pos);
                if (placed.maxX <= footprint.minX || placed.minX >= footprint.maxX
                        || placed.maxZ <= footprint.minZ || placed.minZ >= footprint.maxZ) continue;
                if (placed.maxY > spot.y + FLOOR_EPSILON) {
                    if (placed.minY <= spot.y) return null;
                    continue;
                }
                floor = Math.max(floor, placed.maxY);
            }
            if (floor == Double.NEGATIVE_INFINITY) continue;
            Vec3 feet = new Vec3(spot.x, floor, spot.z);
            AABB box = player.getBoundingBox().move(feet.subtract(player.position()));
            return level.noCollision(player, box) && !level.containsAnyLiquid(box) ? feet : null;
        }
        return null;
    }
}
