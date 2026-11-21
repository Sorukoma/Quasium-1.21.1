package net.sorukoma.quasium.ritual.armor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.item.ModItems;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class QuasiumDodge {
    private static final String CHARGES = "QuasiumHazeCharges";
    private static final String BUILDUP = "QuasiumHazeBuildup";
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final int FRESH_HIT_TICKS = 10;

    private QuasiumDodge() {
    }

    public static boolean wearsFullSet(LivingEntity wearer) {
        return wearer.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.QUASIUM_HELMET.get())
                && wearer.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.QUASIUM_CHESTPLATE.get())
                && wearer.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.QUASIUM_LEGGINGS.get())
                && wearer.getItemBySlot(EquipmentSlot.FEET).is(ModItems.QUASIUM_BOOTS.get());
    }

    public static boolean dodgeable(Player player, DamageSource source) {
        return wearsFullSet(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean heavy(Player player, float damage) {
        return damage >= player.getHealth() || damage >= player.getMaxHealth() * Config.QUASIUM_ARMOR_HAZE_HIT_PCT.get() / 100f;
    }

    public static int charges(Player player) {
        return Math.min(player.getPersistentData().getInt(CHARGES), Config.QUASIUM_ARMOR_HAZE_CHARGES.get());
    }

    public static void buildHazeForASecond(Player player) {
        int charges = charges(player);
        if (charges >= Config.QUASIUM_ARMOR_HAZE_CHARGES.get()
                || PlayerKaifData.getKaifValue(player) < Config.QUASIUM_ARMOR_HAZE_MIN_KAIF.get()) return;
        CompoundTag data = player.getPersistentData();
        int seconds = data.getInt(BUILDUP) + 1;
        boolean built = seconds >= Config.QUASIUM_ARMOR_HAZE_SECONDS.get();
        data.putInt(BUILDUP, built ? 0 : seconds);
        if (built) data.putInt(CHARGES, charges + 1);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0 || !wearsFullSet(player)) return;
        buildHazeForASecond(player);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0f || !dodgeable(player, event.getSource())) return;
        if (player.invulnerableTime > FRESH_HIT_TICKS || player.getRandom().nextInt(100) >= SmokeArmor.dodgePct(player)) return;
        event.setCanceled(true);
        slipIntoSmoke(player, 8);
        player.displayClientMessage(Component.translatable("quasium.message.dodge"), true);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !dodgeable(player, event.getSource())) return;
        int charges = charges(player);
        if (charges == 0 || !heavy(player, event.getNewDamage() - player.getAbsorptionAmount())) return;
        player.getPersistentData().putInt(CHARGES, charges - 1);
        event.setNewDamage(0f);
        slipIntoSmoke(player, 24);
        player.displayClientMessage(Component.translatable("quasium.message.haze", charges - 1, Config.QUASIUM_ARMOR_HAZE_CHARGES.get()), true);
    }

    private static void slipIntoSmoke(ServerPlayer player, int puffs) {
        for (EquipmentSlot slot : ARMOR_SLOTS) player.getItemBySlot(slot).hurtAndBreak(1, player, slot);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), puffs, 0.4, 0.6, 0.4, 0.02);
    }
}
