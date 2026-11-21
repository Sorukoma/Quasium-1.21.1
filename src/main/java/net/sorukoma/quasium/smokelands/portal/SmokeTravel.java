package net.sorukoma.quasium.smokelands.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.api.KaifComaEvent;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokeTravel {
    public static final ResourceLocation HOOK = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "smokeland_hook");
    public static final String NOT_READY = "quasium.message.quasium_ore.low_kaif";
    private static final String RETURN = "QuasiumSmokelandsReturn";
    private static final String COMA_UNTIL = "QuasiumComaUntil";
    private static final double STEP_BACK = 1.5;

    private SmokeTravel() {
    }

    public static Optional<Component> refusal(ServerPlayer player) {
        if (SmokelandsDimension.contains(player.level())) return Optional.empty();
        if (PlayerKaifData.getKaifValue(player) >= SmokelandsConfig.PORTAL_KAIF.get() || fallsThrough(player)) return Optional.empty();
        return Optional.of(Component.translatable(NOT_READY));
    }

    public static boolean fallsThrough(ServerPlayer player) {
        return inComa(player) && hasHook(player);
    }

    public static boolean inComa(ServerPlayer player) {
        return player.level().getGameTime() < player.getPersistentData().getLong(COMA_UNTIL);
    }

    public static boolean hasHook(Player player) {
        Item hook = BuiltInRegistries.ITEM.get(HOOK);
        return hook != Items.AIR && player.getInventory().hasAnyMatching(stack -> stack.is(hook));
    }

    @SubscribeEvent
    public static void onComa(KaifComaEvent event) {
        ServerPlayer player = event.player();
        player.getPersistentData().putLong(COMA_UNTIL, player.level().getGameTime() + event.durationTicks());
        if (!fallsThrough(player) || SmokelandsDimension.contains(player.level())) return;
        DimensionTransition transition = transition(player.serverLevel(), player);
        if (transition == null) return;
        SmokecraftTrigger.fire(player, "smokelands_coma");
        player.displayClientMessage(Component.translatable("quasium.smokelands.fell_through"), true);
        player.changeDimension(transition);
    }

    @Nullable
    public static DimensionTransition transition(ServerLevel from, ServerPlayer player) {
        if (SmokelandsDimension.contains(from)) return home(player);
        ServerLevel smokelands = player.server.getLevel(SmokelandsDimension.LEVEL);
        if (smokelands == null) return null;
        remember(player);
        BlockPos arrival = SmokeArrival.arrive(smokelands, player.blockPosition());
        return new DimensionTransition(smokelands, Vec3.atBottomCenterOf(arrival), Vec3.ZERO, player.getYRot(), player.getXRot(),
                SmokeTravel::arrived);
    }

    private static DimensionTransition home(ServerPlayer player) {
        CompoundTag back = player.getPersistentData().getCompound(RETURN);
        ResourceLocation dimension = ResourceLocation.tryParse(back.getString("dimension"));
        ServerLevel level = dimension == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null || SmokelandsDimension.contains(level)) {
            return new DimensionTransition(player.server.overworld(), player, SmokeTravel::arrived);
        }
        return new DimensionTransition(level, new Vec3(back.getDouble("x"), back.getDouble("y"), back.getDouble("z")), Vec3.ZERO,
                back.getFloat("yaw"), 0f, SmokeTravel::arrived);
    }

    private static void remember(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        Vec3 back = look.lengthSqr() < 1e-4 ? player.position() : player.position().subtract(look.normalize().scale(STEP_BACK));
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", player.level().dimension().location().toString());
        tag.putDouble("x", back.x);
        tag.putDouble("y", back.y);
        tag.putDouble("z", back.z);
        tag.putFloat("yaw", player.getYRot() + 180f);
        player.getPersistentData().put(RETURN, tag);
    }

    private static void arrived(Entity entity) {
        DimensionTransition.PLACE_PORTAL_TICKET.onTransition(entity);
        entity.level().playSound(null, entity.blockPosition(), SmokelandsSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() == SmokelandsDimension.LEVEL && event.getEntity() instanceof ServerPlayer player) {
            SmokecraftTrigger.fire(player, "smokelands_enter");
        }
    }
}
