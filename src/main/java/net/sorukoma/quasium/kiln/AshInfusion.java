package net.sorukoma.quasium.kiln;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.magic.NoteType;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class AshInfusion {
    private static final Map<LivingEntity, DamageSource> FINISHING_BLOWS = new WeakHashMap<>();

    private AshInfusion() {
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        ItemStack weapon = source.getWeaponItem();
        if (victim instanceof Mob && source.getEntity() instanceof Player && weapon != null
                && NoteInfusion.of(weapon) == NoteType.ASH
                && victim.getHealth() < victim.getMaxHealth() * Config.INFUSION_ASH_FINISH_PCT.getAsInt() / 100f) {
            FINISHING_BLOWS.put(victim, source);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity body = event.getEntity();
        if (FINISHING_BLOWS.remove(body) != event.getSource() || !(event.getSource().getEntity() instanceof Player player)) return;
        event.setCanceled(true);
        ((ServerLevel) body.level()).sendParticles(ParticleTypes.LARGE_SMOKE, body.getX(), body.getY(0.5), body.getZ(),
                24, body.getBbWidth() / 2, body.getBbHeight() / 3, body.getBbWidth() / 2, 0.02);
        body.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7f, 0.8f);
        collectAsh(player);
    }

    private static void collectAsh(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            int ml = EssenceBottleItem.amountOf(stack);
            if (EssenceBottleItem.typeOf(stack).equals(NoteType.ASH.id()) && ml < Config.NOTE_BOTTLE_CAPACITY.getAsInt()) {
                stack.set(ModDataComponents.ESSENCE_AMOUNT.get(), ml + 1);
                return;
            }
        }
        ItemStack bottle = EssenceBottleItem.of(NoteType.ASH.id(), 1);
        if (!player.getInventory().add(bottle)) player.drop(bottle, false);
    }
}
