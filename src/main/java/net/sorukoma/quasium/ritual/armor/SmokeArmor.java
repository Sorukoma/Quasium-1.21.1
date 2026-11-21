package net.sorukoma.quasium.ritual.armor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.magic.CastContext;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualMath;
import net.sorukoma.quasium.ritual.RitualRegistry;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeArmor {
    private static final int RING_PUFFS = 24;

    private SmokeArmor() {
    }

    public static int dodgePct(Player player) {
        int threaded = 0;
        for (ItemStack piece : player.getInventory().armor) {
            if (piece.has(RitualRegistry.SMOKE_THREAD.get())) threaded++;
        }
        return RitualMath.dodgePct(Config.QUASIUM_ARMOR_DODGE_PCT.get(), threaded, RitualConfig.SMOKE_THREAD_DODGE_PCT.getAsInt(),
                RitualConfig.SMOKE_THREAD_DODGE_CAP_PCT.getAsInt());
    }

    public static boolean wearsCarapace(LivingEntity wearer) {
        return QuasiumDodge.wearsFullSet(wearer) && wearer.getItemBySlot(EquipmentSlot.CHEST).has(RitualRegistry.SMOKE_CARAPACE.get());
    }

    public static float charge(LivingEntity wearer) {
        return wearer.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(RitualRegistry.CARAPACE_CHARGE.get(), 0f);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0f && wearsCarapace(player)) soak(player, event.getAmount());
    }

    public static void soak(ServerPlayer player, float damage) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        float charge = charge(player) + damage;
        if (charge < RitualConfig.CARAPACE_CHARGE_CAP.getAsInt()) {
            chest.set(RitualRegistry.CARAPACE_CHARGE.get(), charge);
            return;
        }
        chest.set(RitualRegistry.CARAPACE_CHARGE.get(), 0f);
        nova(player, charge);
    }

    public static int nova(ServerPlayer player, float charge) {
        ServerLevel level = player.serverLevel();
        float damage = RitualMath.novaDamage(charge, RitualConfig.CARAPACE_NOVA_PCT.getAsInt());
        double radius = RitualConfig.CARAPACE_NOVA_RADIUS.getAsInt();
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(CastContext.SMOKE_MAGIC), player, player);
        int hit = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                target -> target != player && !(target instanceof Player) && target.isAlive() && target.distanceToSqr(player) <= radius * radius)) {
            if (target.hurt(source, damage)) hit++;
        }
        Vec3 center = player.position().add(0, 1, 0);
        for (int puff = 0; puff < RING_PUFFS; puff++) {
            double angle = Math.PI * 2 * puff / RING_PUFFS;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x + Math.cos(angle) * radius * 0.6, center.y,
                    center.z + Math.sin(angle) * radius * 0.6, 1, 0, 0.02, 0, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 0.7f);
        SmokecraftTrigger.fire(player, "carapace_nova");
        return hit;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity().getItemBySlot(EquipmentSlot.FEET).has(RitualRegistry.SMOKESTEP.get())) {
            event.setDamageMultiplier(event.getDamageMultiplier() * RitualConfig.SMOKESTEP_FALL_DAMAGE_PCT.getAsInt() / 100f);
        }
    }
}
