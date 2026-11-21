package net.sorukoma.quasium.magic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.mind.research.Perk;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record CastContext(ServerLevel level, @Nullable ServerPlayer caster, ResourceLocation spellId, Spell spell,
                          float power, @Nullable Entity carrier, @Nullable FocusUpgrade upgrade, Vec3 direction) {

    public static final ResourceKey<DamageType> SMOKE_MAGIC =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smoke_magic"));
    private static final Set<String> REACH_KEYS = Set.of("range", "length", "throw");
    private static final Set<String> WIDTH_KEYS = Set.of("width");

    public CastContext(ServerLevel level, @Nullable ServerPlayer caster, ResourceLocation spellId, Spell spell,
                       float power, @Nullable Entity carrier) {
        this(level, caster, spellId, spell, power, carrier, null, caster == null ? Vec3.ZERO : caster.getLookAngle());
    }

    public CastContext withPower(float newPower) {
        return new CastContext(level, caster, spellId, spell, newPower, carrier, upgrade, direction);
    }

    public float param(String key, float fallback) {
        float value = spell.param(key, fallback);
        if (upgrade != FocusUpgrade.COLLIMATOR) return value;
        if (REACH_KEYS.contains(key)) return value * (100 + Perk.focusBonus(caster, Config.UPGRADE_COLLIMATOR_RANGE_PCT.getAsInt() - 100)) / 100f;
        if (WIDTH_KEYS.contains(key)) return value * Config.UPGRADE_COLLIMATOR_WIDTH_PCT.getAsInt() / 100f;
        return value;
    }

    public int scaledTicks(String key, float fallbackSeconds) {
        return Math.round(spell.ticks(key, fallbackSeconds) * power);
    }

    public boolean isCaster(Entity entity) {
        return caster != null && entity.getUUID().equals(caster.getUUID());
    }

    public DamageSource damageSource() {
        return damageSource(SMOKE_MAGIC);
    }

    public DamageSource damageSource(ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
                carrier == null ? caster : carrier, caster);
    }

    public void damage(LivingEntity target, float amount) {
        damage(target, amount, SMOKE_MAGIC);
    }

    public void damage(LivingEntity target, float amount, ResourceKey<DamageType> type) {
        boolean wasAlive = target.isAlive();
        if (target.hurt(damageSource(type), amount * power) && wasAlive && !target.isAlive() && caster != null) {
            PlayerKaifData.addKaif(caster, Config.KAIF_PER_CAST_KILL.getAsInt());
            SmokecraftTrigger.fire(caster, "kill:" + spellId.getPath() + ":"
                    + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath());
        }
    }
}
