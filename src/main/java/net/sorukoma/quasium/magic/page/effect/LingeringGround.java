package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.magic.page.PageTarget;
import net.sorukoma.quasium.magic.page.PageTargets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class LingeringGround {
    private static final int SLICK_EVERY_TICKS = 5;
    private static final int SLIPPERY_TICKS = 10;
    private static final double SLICK_HEIGHT = 1.5;

    private record Placed(ResourceKey<Level> dimension, BlockPos pos, Block block, BlockState restore, long until) {
    }

    private record Slick(ResourceKey<Level> dimension, Vec3 center, float radius, long until, @Nullable UUID caster) {
    }

    private static final List<Placed> PLACED = new ArrayList<>();
    private static final List<Slick> SLICKS = new ArrayList<>();

    private LingeringGround() {
    }

    static void place(ServerLevel level, BlockPos pos, BlockState restore, int ticks) {
        PLACED.add(new Placed(level.dimension(), pos.immutable(), level.getBlockState(pos).getBlock(), restore, level.getGameTime() + ticks));
    }

    static void slick(ServerLevel level, Vec3 center, float radius, int ticks, @Nullable ServerPlayer caster) {
        SLICKS.add(new Slick(level.dimension(), center, radius, level.getGameTime() + ticks, caster == null ? null : caster.getUUID()));
    }

    static void tick(MinecraftServer server) {
        PLACED.removeIf(placed -> restoreIfDue(server, placed));
        if (server.getTickCount() % SLICK_EVERY_TICKS == 0) SLICKS.removeIf(slick -> !holds(server, slick));
    }

    static void clear() {
        PLACED.clear();
        SLICKS.clear();
    }

    private static boolean restoreIfDue(MinecraftServer server, Placed placed) {
        ServerLevel level = server.getLevel(placed.dimension());
        if (level == null) return true;
        if (level.getGameTime() < placed.until()) return false;
        if (level.isLoaded(placed.pos()) && level.getBlockState(placed.pos()).is(placed.block())) {
            level.setBlockAndUpdate(placed.pos(), placed.restore());
        }
        return true;
    }

    private static boolean holds(MinecraftServer server, Slick slick) {
        ServerLevel level = server.getLevel(slick.dimension());
        if (level == null || level.getGameTime() >= slick.until()) return false;
        ServerPlayer caster = slick.caster() == null ? null : server.getPlayerList().getPlayer(slick.caster());
        Vec3 center = slick.center();
        double reach = slick.radius() * slick.radius();
        for (LivingEntity walker : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center)
                .inflate(slick.radius(), SLICK_HEIGHT, slick.radius()))) {
            double dx = walker.getX() - center.x;
            double dz = walker.getZ() - center.z;
            if (dx * dx + dz * dz > reach || !walker.onGround() || !PageTargets.accepts(PageTarget.FOE, caster, walker)) continue;
            walker.addEffect(new MobEffectInstance(HeartRegistry.SLIPPERY, SLIPPERY_TICKS, 0, false, false, true));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.1, center.z, 3, slick.radius() / 2, 0.02, slick.radius() / 2, 0.0);
        return true;
    }
}
