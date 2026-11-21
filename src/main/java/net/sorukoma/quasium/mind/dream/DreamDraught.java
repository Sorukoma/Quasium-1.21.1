package net.sorukoma.quasium.mind.dream;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.item.EssenceBottleItem;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.research.Research;
import net.sorukoma.quasium.ritual.artifact.DreamCatcherBlockEntity;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class DreamDraught {
    public static final ResourceLocation NODE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "research_dreams_draught");
    public static final String PENDING = "DreamDraught";

    private DreamDraught() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!DreamCatcherBlockEntity.ESSENCE.equals(EssenceBottleItem.typeOf(event.getItemStack()))) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (event.getEntity() instanceof ServerPlayer player) drink(player, event.getHand());
    }

    public static boolean drink(ServerPlayer player, InteractionHand hand) {
        ItemStack bottle = player.getItemInHand(hand);
        int sip = MindConfig.DREAM_DRAUGHT_ML.getAsInt();
        if (!Research.studied(player, NODE)) return refuse(player, Component.translatable("quasium.dream.draught.unknown", Research.title(NODE.toString())));
        if (pending(player)) return refuse(player, Component.translatable("quasium.dream.draught.already"));
        if (EssenceBottleItem.amountOf(bottle) < sip) return refuse(player, Component.translatable("quasium.dream.draught.too_little", sip));
        player.setItemInHand(hand, EssenceBottleItem.drained(bottle, sip));
        MindData.root(player).putBoolean(PENDING, true);
        MindData.changed(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.8f, 0.8f);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getEyeY(), player.getZ(), 16, 0.3, 0.3, 0.3, 0.4);
        player.displayClientMessage(Component.translatable("quasium.dream.draught.drunk").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        SmokecraftTrigger.fire(player, "dream_draught");
        return true;
    }

    public static boolean pending(ServerPlayer player) {
        return MindData.root(player).getBoolean(PENDING);
    }

    static boolean take(ServerPlayer player) {
        if (!pending(player)) return false;
        MindData.root(player).remove(PENDING);
        MindData.changed(player);
        return true;
    }

    private static boolean refuse(ServerPlayer player, Component reason) {
        player.displayClientMessage(reason, true);
        return false;
    }
}
