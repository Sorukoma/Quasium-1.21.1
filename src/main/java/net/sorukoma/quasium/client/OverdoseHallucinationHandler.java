package net.sorukoma.quasium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class OverdoseHallucinationHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final int MIN_HALLUCINATION_INTERVAL_TICKS = 5 * 20;
    private static final int MAX_HALLUCINATION_INTERVAL_TICKS = 25 * 20;
    private static final int MIN_PHANTOM_LIFETIME_TICKS = 4 * 20;
    private static final int MAX_PHANTOM_LIFETIME_TICKS = 12 * 20;
    private static final int MAX_CONCURRENT_PHANTOMS = 4;
    private static final int MIN_SOUND_INTERVAL_TICKS = 4 * 20;
    private static final int MAX_SOUND_INTERVAL_TICKS = 18 * 20;
    private static final int PHANTOM_MIN_DISTANCE = 4;
    private static final int PHANTOM_MAX_DISTANCE = 15;
    private static final float PHANTOM_BASE_ALPHA = 0.55f;
    private static final int FADE_TICKS = 20;

    // Entity types that cannot be used as phantom hallucinations (crash, invisible, or problematic)
    private static final Set<ResourceLocation> EXCLUDED_ENTITY_TYPES = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "player"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "lightning_bolt"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "experience_orb"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "wither"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "item"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "tnt"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "falling_block"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "area_effect_cloud"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "evoker_fangs"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "marker"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "display"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "interaction"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "fishing_bobber")
    );

    private static final SoundEvent[] SOUND_POOL = {
            SoundEvents.ZOMBIE_AMBIENT, SoundEvents.ZOMBIE_HURT, SoundEvents.ZOMBIE_STEP,
            SoundEvents.SKELETON_AMBIENT, SoundEvents.SKELETON_STEP,
            SoundEvents.CREEPER_HURT, SoundEvents.CREEPER_DEATH,
            SoundEvents.ENDERMAN_AMBIENT, SoundEvents.ENDERMAN_STARE, SoundEvents.ENDERMAN_HURT,
            SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_STEP,
            SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_HURT,
            SoundEvents.PIG_AMBIENT, SoundEvents.PIG_HURT, SoundEvents.PIG_DEATH,
            SoundEvents.SHEEP_AMBIENT, SoundEvents.SHEEP_HURT,
            SoundEvents.GHAST_AMBIENT, SoundEvents.GHAST_HURT,
            SoundEvents.BLAZE_BURN, SoundEvents.BLAZE_DEATH,
            SoundEvents.WOLF_HOWL, SoundEvents.WOLF_HURT,
            SoundEvents.COW_AMBIENT, SoundEvents.COW_HURT, SoundEvents.COW_DEATH,
            SoundEvents.CHICKEN_AMBIENT, SoundEvents.CHICKEN_HURT,
            SoundEvents.BAT_AMBIENT, SoundEvents.BAT_HURT,
            SoundEvents.FIRE_AMBIENT, SoundEvents.LAVA_AMBIENT,
            SoundEvents.WATER_AMBIENT, SoundEvents.AMBIENT_CAVE.value(),
            SoundEvents.PLAYER_BURP, SoundEvents.PLAYER_HURT,
            SoundEvents.ENDER_DRAGON_AMBIENT, SoundEvents.ENDER_DRAGON_HURT,
            SoundEvents.WITHER_AMBIENT, SoundEvents.WITHER_HURT,
            SoundEvents.WARDEN_AMBIENT, SoundEvents.WARDEN_HURT,
            SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.ALLAY_DEATH,
            SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT,
            SoundEvents.HOGLIN_AMBIENT, SoundEvents.HOGLIN_HURT,
            SoundEvents.PILLAGER_AMBIENT, SoundEvents.PILLAGER_HURT,
            SoundEvents.RAVAGER_ROAR, SoundEvents.RAVAGER_HURT,
            SoundEvents.BELL_RESONATE, SoundEvents.ANVIL_LAND,
            SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.CHEST_CLOSE, SoundEvents.CHEST_OPEN,
            SoundEvents.PORTAL_TRIGGER, SoundEvents.PORTAL_AMBIENT
    };

    private static final List<PhantomEntity> phantomEntities = new ArrayList<>();
    private static long nextPhantomSpawnTick = 0;
    private static long hauntedUntil = 0;
    private static long nextSoundSpawnTick = 0;
    private static List<ResourceLocation> cachedAvailableSounds = null;
    // Dynamically populated from all registered EntityTypes (vanilla + mods)
    private static List<EntityType<?>> cachedEntityTypes = null;

    private static class PhantomEntity {
        final Entity entity;
        final double x, y, z;
        final float yaw;
        final long spawnTime;
        final int lifetimeTicks;

        PhantomEntity(Entity entity, double x, double y, double z, float yaw, long spawnTime, int lifetimeTicks) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.spawnTime = spawnTime;
            this.lifetimeTicks = lifetimeTicks;
        }
    }

    /**
     * Lazily populates the entity type pool from BuiltInRegistries.ENTITY_TYPE,
     * which includes ALL entity types from ALL mods in the modpack.
     * Excludes types that crash or are invisible when created without world context.
     */
    private static List<EntityType<?>> getEntityTypes(Minecraft mc) {
        if (cachedEntityTypes == null) {
            cachedEntityTypes = new ArrayList<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (EXCLUDED_ENTITY_TYPES.contains(key)) continue;
                // Try to create the entity — if it fails, skip this type
                try {
                    Entity testEntity = type.create(mc.level);
                    // Only living entities make sense as hallucinations. This also defensively
                    // excludes non-living modded entities that crash or render incorrectly when
                    // spawned outside their normal lifecycle — e.g. Create / Create Aeronautics
                    // contraption & Sable sub-level entities, whose renderers NPE without an
                    // attached contraption.
                    if (testEntity instanceof net.minecraft.world.entity.LivingEntity) {
                        testEntity.discard();
                        cachedEntityTypes.add(type);
                    } else if (testEntity != null) {
                        testEntity.discard();
                    }
                } catch (Exception e) {
                    // Some mod entity types crash on create() — skip them
                }
            }
        }
        return cachedEntityTypes;
    }

    public static void haunt(int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) hauntedUntil = mc.level.getGameTime() + ticks;
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        boolean haunted = mc.level != null && mc.level.getGameTime() < hauntedUntil;
        return (ClientOverdoseData.isOverdosed() || haunted) && ConfigArgs.synced(MindConfig.HALLUCINATION_INTENSITY_PCT) > 0;
    }

    private static int maxPhantoms() {
        return Math.max(1, Math.round(MAX_CONCURRENT_PHANTOMS * ConfigArgs.synced(MindConfig.HALLUCINATION_INTENSITY_PCT) / 100f));
    }

    public static void clientTick() {
        if (!active()) {
            if (!phantomEntities.isEmpty() || nextPhantomSpawnTick != 0) clearHallucinations();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long currentTick = mc.level.getGameTime();
        RandomSource random = mc.level.random;

        // Spawn new phantom entities on timer
        if (currentTick >= nextPhantomSpawnTick && phantomEntities.size() < maxPhantoms()) {
            spawnPhantomEntity(mc, currentTick, random);
            nextPhantomSpawnTick = currentTick + random.nextIntBetweenInclusive(
                    MIN_HALLUCINATION_INTERVAL_TICKS, MAX_HALLUCINATION_INTERVAL_TICKS);
        }

        // Remove expired phantoms
        phantomEntities.removeIf(p -> currentTick - p.spawnTime >= p.lifetimeTicks);

        // Spawn phantom sounds on timer
        if (currentTick >= nextSoundSpawnTick) {
            spawnPhantomSound(mc, random);
            nextSoundSpawnTick = currentTick + random.nextIntBetweenInclusive(
                    MIN_SOUND_INTERVAL_TICKS, MAX_SOUND_INTERVAL_TICKS);
        }
    }

    public static void renderPhantoms(RenderLevelStageEvent event) {
        if (!active() || phantomEntities.isEmpty()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long currentTick = mc.level.getGameTime();
        RandomSource random = mc.level.random;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = event.getCamera().getPosition();

        for (PhantomEntity phantom : phantomEntities) {
            long age = currentTick - phantom.spawnTime;
            float alpha = calculateAlpha(age, phantom.lifetimeTicks, random);
            if (alpha <= 0.01f) continue;

            double relX = phantom.x - camPos.x;
            double relY = phantom.y - camPos.y;
            double relZ = phantom.z - camPos.z;

            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

            try {
                dispatcher.render(
                        phantom.entity,
                        relX, relY, relZ,
                        phantom.yaw,
                        0.0f,
                        event.getPoseStack(),
                        bufferSource,
                        15728880
                );
            } catch (Exception e) {
                // Рендерер чужого мода может бросить исключение на сущности вне нормального
                // жизненного цикла — не роняем рендер-тред, просто пропускаем этот фантом.
                LOGGER.warn("Failed to render hallucination phantom {}: {}",
                        phantom.entity.getType(), e.toString());
            }

            bufferSource.endBatch();

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    private static float calculateAlpha(long ageTicks, int lifetimeTicks, RandomSource random) {
        if (ageTicks < FADE_TICKS) {
            return PHANTOM_BASE_ALPHA * ((float) ageTicks / FADE_TICKS);
        }
        if (ageTicks > lifetimeTicks - FADE_TICKS) {
            return PHANTOM_BASE_ALPHA * ((float) (lifetimeTicks - ageTicks) / FADE_TICKS);
        }
        float flicker = 0.05f * (random.nextFloat() - 0.5f);
        return Mth.clamp(PHANTOM_BASE_ALPHA + flicker, 0.3f, 0.7f);
    }

    private static void spawnPhantomEntity(Minecraft mc, long currentTick, RandomSource random) {
        List<EntityType<?>> types = getEntityTypes(mc);
        if (types.isEmpty()) return;

        EntityType<?> type = types.get(random.nextInt(types.size()));
        Entity entity;
        try {
            entity = type.create(mc.level);
        } catch (Exception e) {
            return; // If creation fails, skip this spawn
        }
        if (entity == null) return;

        Player player = mc.player;
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = PHANTOM_MIN_DISTANCE + random.nextDouble() * (PHANTOM_MAX_DISTANCE - PHANTOM_MIN_DISTANCE);
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = player.getY() + random.nextDouble() * 3 - 1;

        float yaw = random.nextFloat() * 360;
        int lifetime = random.nextIntBetweenInclusive(MIN_PHANTOM_LIFETIME_TICKS, MAX_PHANTOM_LIFETIME_TICKS);

        entity.setPos(x, y, z);
        entity.setYRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setXRot(random.nextFloat() * 30 - 15);
        entity.tickCount = random.nextInt(1000);

        phantomEntities.add(new PhantomEntity(entity, x, y, z, yaw, currentTick, lifetime));
    }

    private static void spawnPhantomSound(Minecraft mc, RandomSource random) {
        SoundEvent sound;

        if (random.nextFloat() < 0.7f) {
            sound = SOUND_POOL[random.nextInt(SOUND_POOL.length)];
        } else {
            sound = pickRandomFromAllSounds(mc, random);
            if (sound == null) {
                sound = SOUND_POOL[random.nextInt(SOUND_POOL.length)];
            }
        }

        Player player = mc.player;
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 3 + random.nextDouble() * 12;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        double y = player.getY() + random.nextDouble() * 5 - 2;

        float pitch = 0.5f + random.nextFloat() * 1.5f;
        float volume = 0.3f + random.nextFloat() * 0.5f;

        SoundSource source = SoundSource.HOSTILE;
        if (random.nextFloat() < 0.3f) {
            source = random.nextFloat() < 0.5f ? SoundSource.AMBIENT : SoundSource.NEUTRAL;
        }

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound, source, volume, pitch,
                random, x, y, z
        );

        mc.getSoundManager().play(soundInstance);
    }

    private static SoundEvent pickRandomFromAllSounds(Minecraft mc, RandomSource random) {
        if (cachedAvailableSounds == null) {
            Collection<ResourceLocation> available = mc.getSoundManager().getAvailableSounds();
            cachedAvailableSounds = new ArrayList<>(available);
        }

        if (cachedAvailableSounds.isEmpty()) return null;

        ResourceLocation loc = cachedAvailableSounds.get(random.nextInt(cachedAvailableSounds.size()));
        return BuiltInRegistries.SOUND_EVENT.get(loc);
    }

    public static void clearHallucinations() {
        for (PhantomEntity phantom : phantomEntities) {
            phantom.entity.discard();
        }
        phantomEntities.clear();
        nextPhantomSpawnTick = 0;
        nextSoundSpawnTick = 0;
        cachedAvailableSounds = null;
        cachedEntityTypes = null;
    }
}
