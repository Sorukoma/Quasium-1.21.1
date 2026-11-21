package net.sorukoma.quasium.magic.page.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.magic.page.HeartBehavior;
import net.sorukoma.quasium.magic.page.PageHit;

import java.util.Comparator;
import java.util.Map;
import java.util.function.BiConsumer;

public final class NoteHearts {
    private static final float ABSORPTION_PER_LEVEL = 4f;

    private NoteHearts() {
    }

    public static Map<String, HeartBehavior> all() {
        return Map.ofEntries(
                Map.entry("gust", new WorldHearts.Gust()),
                Map.entry("clarity", onEntity(NoteHearts::clarity)),
                Map.entry("blast", new WorldHearts.Blast()),
                Map.entry("whisper", new WorldHearts.Whisper()),
                Map.entry("sticky", onEntity(NoteHearts::sticky)),
                Map.entry("growth", new WorldHearts.Growth()),
                Map.entry("calm", onEntity(NoteHearts::calm)),
                Map.entry("dissolve", onEntity(NoteHearts::dissolve)),
                Map.entry("shackles", onEntity(NoteHearts::shackles)),
                Map.entry("carapace", onEntity(NoteHearts::carapace)),
                Map.entry("blind", onEntity(NoteHearts::blind)),
                Map.entry("decay", onEntity(NoteHearts::decay)));
    }

    private static HeartBehavior onEntity(BiConsumer<PageHit, LivingEntity> effect) {
        return new HeartBehavior() {
            @Override
            public void hitEntity(PageHit hit, LivingEntity target) {
                effect.accept(hit, target);
            }
        };
    }

    private static void clarity(PageHit hit, LivingEntity target) {
        if (!hit.isAlly(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, hit.ticks("glow_seconds", 10f), 0));
            return;
        }
        int cleansed = Math.max(1, (int) (hit.param("cleanse", 1f) * hit.power()));
        target.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .sorted(Comparator.comparingInt(MobEffectInstance::getDuration).reversed())
                .limit(cleansed)
                .map(MobEffectInstance::getEffect)
                .toList()
                .forEach(target::removeEffect);
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.END_ROD, 6);
    }

    private static void sticky(PageHit hit, LivingEntity target) {
        int ticks = hit.ticks("effect_seconds", 4f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, Math.round(hit.param("slow_level", 2f)) - 1));
        target.addEffect(new MobEffectInstance(HeartRegistry.STICKY, ticks, 0));
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.FALLING_HONEY, 8);
    }

    private static void calm(PageHit hit, LivingEntity target) {
        if (target instanceof Mob mob) Strikes.forgetTarget(mob);
        target.addEffect(new MobEffectInstance(HeartRegistry.CALMED, Strikes.controlTicks(target, hit.ticks("calm_seconds", 6f)), 0));
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.NOTE, 3);
    }

    private static void dissolve(PageHit hit, LivingEntity target) {
        int armor = Math.round(hit.param("armor", 4f) * hit.power());
        if (armor <= 0) return;
        target.addEffect(new MobEffectInstance(HeartRegistry.DISSOLVED, hit.flatTicks("effect_seconds", 8f), armor - 1));
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.FALLING_WATER, 10);
    }

    private static void shackles(PageHit hit, LivingEntity target) {
        int ticks = Strikes.controlTicks(target, hit.ticks("root_seconds", 3f), hit.param("player_multiplier", 0.5f));
        target.addEffect(new MobEffectInstance(HeartRegistry.ROOTED, ticks, 0));
        if (target instanceof Mob mob) mob.getNavigation().stop();
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.CRIT, 10);
    }

    private static void carapace(PageHit hit, LivingEntity target) {
        int level = Math.round(hit.param("absorption", 4f) * hit.power() / ABSORPTION_PER_LEVEL);
        if (level <= 0) return;
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, hit.flatTicks("effect_seconds", 20f), level - 1));
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.WAX_ON, 8);
    }

    private static void blind(PageHit hit, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, hit.ticks("blind_seconds", 3f), 0));
        if (target instanceof Mob mob) Strikes.forgetTarget(mob);
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.SQUID_INK, 6);
    }

    private static void decay(PageHit hit, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, hit.ticks("weakness_seconds", 8f), 0));
        int wear = Math.round(hit.param("armor_wear", 10f) * hit.power());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack armor = target.getItemBySlot(slot);
            if (slot.isArmor() && !armor.isEmpty() && armor.isDamageableItem()) armor.hurtAndBreak(wear, target, slot);
        }
        Strikes.puff(hit.level(), hit.page(), target, ParticleTypes.WHITE_ASH, 12);
    }
}
