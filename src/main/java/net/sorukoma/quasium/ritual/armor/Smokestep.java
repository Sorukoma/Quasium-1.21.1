package net.sorukoma.quasium.ritual.armor;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class Smokestep {
    private static final int RING_PUFFS = 16;
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static volatile long clientReadyAt;

    public record DashPacket() implements CustomPacketPayload {
        public static final Type<DashPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokestep_dash"));
        public static final StreamCodec<ByteBuf, DashPacket> STREAM_CODEC = StreamCodec.unit(new DashPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(DashPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) dash(player);
            });
        }
    }

    public record ReadyPacket() implements CustomPacketPayload {
        public static final Type<ReadyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokestep_ready"));
        public static final StreamCodec<ByteBuf, ReadyPacket> STREAM_CODEC = StreamCodec.unit(new ReadyPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ReadyPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> clientReadyAt = 0);
        }
    }

    private Smokestep() {
    }

    public static boolean canDash(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).has(RitualRegistry.SMOKESTEP.get()) && !player.onGround() && !player.isInWater()
                && !player.isInLava() && !player.isPassenger() && !player.getAbilities().flying && !player.onClimbable() && !player.isFallFlying();
    }

    public static Vec3 impulse(Player player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        Vec3 forward = look.lengthSqr() < 1e-6 ? Vec3.ZERO : look.normalize().scale(ConfigArgs.synced(RitualConfig.SMOKESTEP_DASH_SPEED));
        return new Vec3(forward.x, ConfigArgs.synced(RitualConfig.SMOKESTEP_DASH_LIFT), forward.z);
    }

    public static int cooldownTicks() {
        return ConfigArgs.synced(RitualConfig.SMOKESTEP_DASH_COOLDOWN_SEC) * 20;
    }

    public static float cooldownLeft(Player player) {
        if (!player.getItemBySlot(EquipmentSlot.FEET).has(RitualRegistry.SMOKESTEP.get())) return 0f;
        long readyAt = player.level().isClientSide ? clientReadyAt : READY_AT.getOrDefault(player.getUUID(), 0L);
        return ticksLeft(readyAt, player.level().getGameTime()) / (float) cooldownTicks();
    }

    public static void resetCooldown(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new ReadyPacket());
    }

    public static boolean clientDash(long now) {
        if (ticksLeft(clientReadyAt, now) > 0) return false;
        clientReadyAt = now + cooldownTicks();
        return true;
    }

    private static long ticksLeft(long readyAt, long now) {
        long left = readyAt - now;
        return left > cooldownTicks() ? 0 : Math.max(0, left);
    }

    public static boolean dash(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (!canDash(player) || now < READY_AT.getOrDefault(player.getUUID(), Long.MIN_VALUE)) return false;
        READY_AT.put(player.getUUID(), now + cooldownTicks());
        player.setDeltaMovement(impulse(player));
        player.hurtMarked = true;
        player.resetFallDistance();
        ServerLevel level = player.serverLevel();
        Vec3 feet = player.position();
        for (int puff = 0; puff < RING_PUFFS; puff++) {
            double angle = Math.PI * 2 * puff / RING_PUFFS;
            level.sendParticles(ParticleTypes.CLOUD, feet.x + Math.cos(angle) * 0.7, feet.y, feet.z + Math.sin(angle) * 0.7, 1, 0, 0, 0, 0.01);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
        SmokecraftTrigger.fire(player, "smokestep_dash");
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        READY_AT.remove(event.getEntity().getUUID());
    }
}
