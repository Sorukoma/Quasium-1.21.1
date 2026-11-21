package net.sorukoma.quasium.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.SelectionRegistry;
import net.sorukoma.quasium.sound.ModSoundEvents;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmogVillagers {
    public static final String THANKED_DAY = QuasiumMod.MOD_ID + ":tea_party_thanked_day";
    private static final int COUGH_ONE_IN = 3;
    private static final int HEAD_SHAKE_TICKS = 40;

    private SmogVillagers() {
    }

    public static void cough(ServerLevel level) {
        int from = EcologyConfig.VILLAGER_COUGH_SMOG.getAsInt();
        for (long key : EcologyIndex.snapshot(level)) {
            LevelChunk chunk = EcologyTicker.loaded(level, key);
            if (chunk == null || SmokeDensity.smog(chunk).level() < from) continue;
            for (Villager villager : level.getEntitiesOfClass(Villager.class, SmogSpawner.column(level, chunk.getPos()), SmogVillagers::chokes)) {
                if (level.random.nextInt(COUGH_ONE_IN) == 0) puff(level, villager);
            }
        }
    }

    @SubscribeEvent
    public static void onTalk(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) return;
        ItemStack held = event.getItemStack();
        boolean naming = held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME);
        if (naming || held.is(Items.VILLAGER_SPAWN_EGG) || villager.isBaby() || villager.isSleeping() || villager.isTrading()) return;
        if (!chokes(villager) || SmokeDensity.smog(level.getChunkAt(villager.blockPosition())).level() < EcologyConfig.VILLAGER_COUGH_SMOG.getAsInt()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        villager.setUnhappyCounter(HEAD_SHAKE_TICKS);
        villager.playSound(SoundEvents.VILLAGER_NO, 1f, villager.getVoicePitch());
        event.getEntity().displayClientMessage(Component.translatable("quasium.smog.villager_refuses"), true);
    }

    public static boolean thankTeaParty(ServerLevel level, BlockPos samovar, List<ServerPlayer> guests, int ticks) {
        List<Villager> village = level.getEntitiesOfClass(Villager.class, SmogSpawner.column(level, new ChunkPos(samovar)).inflate(16, 0, 16), Entity::isAlive);
        long today = SmogSpread.day(level);
        if (village.isEmpty() || village.stream().anyMatch(villager -> thankedOn(villager, today))) return false;
        village.forEach(villager -> villager.getPersistentData().putLong(THANKED_DAY, today));
        guests.forEach(guest -> crown(guest, ticks));
        return true;
    }

    public static void welcomeLateGuests(List<ServerPlayer> guests, int ticksLeft) {
        for (ServerPlayer guest : guests) {
            if (!guest.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) crown(guest, ticksLeft);
        }
    }

    private static void crown(ServerPlayer guest, int ticks) {
        guest.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, ticks, 0, false, false, true));
    }

    private static boolean thankedOn(Villager villager, long day) {
        CompoundTag data = villager.getPersistentData();
        return data.contains(THANKED_DAY) && data.getLong(THANKED_DAY) == day;
    }

    private static boolean chokes(Villager villager) {
        return villager.isAlive() && villager.getVillagerData().getProfession() != SelectionRegistry.HOOKAH_MASTER.get();
    }

    private static void puff(ServerLevel level, Villager villager) {
        Vec3 mouth = villager.getEyePosition().add(villager.getLookAngle().scale(0.4));
        level.sendParticles(ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, 4, 0.1, 0.05, 0.1, 0.01);
        level.playSound(null, villager, ModSoundEvents.COUGH.get(), SoundSource.NEUTRAL, 0.8f, villager.getVoicePitch());
    }
}
