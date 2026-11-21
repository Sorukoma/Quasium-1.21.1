package net.sorukoma.quasium.ritual.brazier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.api.RitualCompleteEvent;
import net.sorukoma.quasium.block.entity.NoteJarBlockEntity;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.ecology.SmokeDensity;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RitualOutcome {
    public static final ResourceLocation DYMNIK = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "dymnik");
    private static final double BLAST_RADIUS = 4.0;
    private static final double POP_SPEED = 0.25;

    private RitualOutcome() {
    }

    static void complete(ServerLevel level, RitualBrazierBlockEntity brazier, RitualRun run) {
        List<RitualPedestalBlockEntity> pedestals = reserved(level, run);
        if (pedestals.size() < run.pedestals().size() || pedestals.stream().anyMatch(pedestal -> pedestal.item().isEmpty())) {
            fail(level, brazier, run);
            return;
        }
        BlockPos pos = brazier.getBlockPos();
        ItemStack made = run.recipe().craft(brazier.catalyst());
        brazier.setCatalyst(ItemStack.EMPTY);
        pedestals.forEach(pedestal -> pedestal.setItem(ItemStack.EMPTY));
        Vec3 top = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        popOut(level, top, made);
        level.playSound(null, pos, RitualRegistry.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1f, 1f);
        if (run.recipe().result().filter(stack -> stack.is(RitualRegistry.SMOKE_PORTAL_ACTIVATOR.get())).isPresent()) {
            level.playSound(null, pos, RitualRegistry.PORTAL_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
        }
        level.sendParticles(ParticleTypes.END_ROD, top.x, top.y, top.z, 40, 0.5, 0.8, 0.5, 0.06);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.x, top.y, top.z, 12, 0.3, 0.2, 0.3, 0.02);
        List<ServerPlayer> participants = RitualSite.participants(level, pos);
        for (ServerPlayer player : participants) {
            SmokecraftTrigger.fire(player, "ritual");
            SmokecraftTrigger.fire(player, "ritual:" + run.id().getPath());
            if (run.group()) SmokecraftTrigger.fire(player, "ritual_group");
            if (run.recipe().needsNirvana()) SmokecraftTrigger.fire(player, "ritual_nirvana");
            player.displayClientMessage(Component.translatable("quasium.ritual.done", RitualRun.displayName(run.id()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        NeoForge.EVENT_BUS.post(new RitualCompleteEvent(level, pos, run.id(), participants));
    }

    static void fail(ServerLevel level, RitualBrazierBlockEntity brazier, RitualRun run) {
        BlockPos pos = brazier.getBlockPos();
        Block.popResource(level, pos.above(), brazier.catalyst());
        brazier.setCatalyst(ItemStack.EMPTY);
        for (RitualPedestalBlockEntity pedestal : reserved(level, run)) {
            Block.popResource(level, pedestal.getBlockPos().above(), pedestal.takeItem());
        }
        Vec3 center = Vec3.atCenterOf(pos);
        level.playSound(null, pos, RitualRegistry.RITUAL_FAIL.get(), SoundSource.BLOCKS, 1.2f, 1f);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 3, 0.6, 0.3, 0.6, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z, 60, 1.2, 0.6, 1.2, 0.05);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.8, center.z, 20, 1.0, 0.4, 1.0, 0.03);
        for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(BLAST_RADIUS), Entity::isPushable)) {
            Vec3 away = entity.position().subtract(center).multiply(1, 0, 1).normalize().scale(0.6);
            entity.push(away.x, 0.35, away.z);
            entity.hurtMarked = true;
        }
        for (ServerPlayer player : RitualSite.participants(level, pos)) {
            PlayerKaifData.subtractKaif(player, RitualConfig.FAIL_KAIF_LOSS.getAsInt());
            SmokecraftTrigger.fire(player, "ritual_failed");
            player.displayClientMessage(Component.translatable("quasium.ritual.broken", RitualRun.displayName(run.id()),
                    RitualConfig.FAIL_NOTE_RETURN_PCT.getAsInt()).withStyle(ChatFormatting.RED), true);
        }
        returnNotes(level, pos, run.drawn());
        SmokeDensity.add(level, pos, RitualConfig.FAIL_DENSITY.getAsInt());
        if (RitualConfig.FAIL_SPAWNS_DYMNIK.get()) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(DYMNIK).ifPresent(type -> type.spawn(level, pos.above(2), MobSpawnType.EVENT));
        }
        QuasiumMod.LOGGER.info("Ritual {} at {} broke at {}% with instability {}", run.id(), pos, Math.round(run.progress() * 100),
                Math.round(run.instability()));
    }

    private static void popOut(ServerLevel level, Vec3 top, ItemStack made) {
        if (made.isEmpty()) return;
        ItemEntity drop = new ItemEntity(level, top.x, top.y + 0.2, top.z, made, 0.0, POP_SPEED, 0.0);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }

    private static void returnNotes(ServerLevel level, BlockPos pos, Map<NoteType, Integer> drawn) {
        List<NoteJarBlockEntity> jars = RitualSite.jars(level, pos);
        int pct = RitualConfig.FAIL_NOTE_RETURN_PCT.getAsInt();
        drawn.forEach((note, ml) -> {
            int left = ml * pct / 100;
            jars.sort(Comparator.comparing(jar -> jar.note() != note));
            for (NoteJarBlockEntity jar : jars) {
                if (left <= 0) break;
                left -= jar.pour(note, left);
            }
            bottle(level, pos, note, left);
        });
    }

    private static void bottle(ServerLevel level, BlockPos pos, NoteType note, int ml) {
        int capacity = Math.max(1, Config.NOTE_BOTTLE_CAPACITY.getAsInt());
        for (int left = ml; left > 0; left -= capacity) {
            Block.popResource(level, pos.above(), EssenceBottleItem.of(note.id(), Math.min(capacity, left)));
        }
    }

    private static List<RitualPedestalBlockEntity> reserved(ServerLevel level, RitualRun run) {
        List<RitualPedestalBlockEntity> found = new ArrayList<>();
        for (BlockPos pos : run.pedestals()) {
            if (level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) found.add(pedestal);
        }
        return found;
    }
}
