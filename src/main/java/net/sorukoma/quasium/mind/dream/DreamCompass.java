package net.sorukoma.quasium.mind.dream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.BlockFinder;
import net.sorukoma.quasium.mind.MindConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DreamCompass {
    public static final TagKey<Block> TARGETS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "dream_compass"));
    private static final Map<UUID, Course> COURSES = new HashMap<>();

    private record Course(BlockPos target, long until) {
    }

    private DreamCompass() {
    }

    static void start(ServerPlayer player) {
        List<BlockPos> found = BlockFinder.near(player.serverLevel(), player.blockPosition(),
                MindConfig.COMPASS_SEARCH_CHUNKS.getAsInt() * 16, state -> state.is(TARGETS), 1);
        if (found.isEmpty()) {
            player.displayClientMessage(Component.translatable("quasium.dream.compass.nothing"), false);
            return;
        }
        COURSES.put(player.getUUID(), new Course(found.get(0), player.level().getGameTime() + MindConfig.COMPASS_SECONDS.getAsInt() * 20L));
    }

    public static void tick(ServerPlayer player) {
        Course course = COURSES.get(player.getUUID());
        if (course == null) return;
        if (player.level().getGameTime() > course.until()) {
            COURSES.remove(player.getUUID());
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 way = Vec3.atCenterOf(course.target()).subtract(eye).normalize();
        for (int step = 1; step <= 6; step++) {
            Vec3 at = eye.add(way.scale(step * 0.8));
            player.serverLevel().sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, false, at.x, at.y - 0.3, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public static void forget(UUID player) {
        COURSES.remove(player);
    }
}
