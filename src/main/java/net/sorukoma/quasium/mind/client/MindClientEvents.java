package net.sorukoma.quasium.mind.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.TobaccoItem;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.mind.research.Perk;
import net.sorukoma.quasium.progression.ConfigArgs;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class MindClientEvents {
    private static final float MAX_SHAKE_DEGREES = 1.5f;

    private MindClientEvents() {
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        int threshold = ConfigArgs.synced(MindConfig.WITHDRAWAL_SHAKE_KAIF);
        int kaif = ClientKaifData.getKaifValue();
        if (player == null || kaif >= threshold || !player.isUsingItem()) return;
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof BowItem) && !(using.getItem() instanceof CrossbowItem)) return;
        float depth = Mth.clamp((threshold - kaif) / (float) (threshold + 100), 0f, 1f);
        float shake = MAX_SHAKE_DEGREES * depth * ConfigArgs.synced(MindConfig.SCREEN_SHAKE_PCT) / 100f;
        float time = (float) (player.tickCount + event.getPartialTick());
        event.setYaw(event.getYaw() + shake * (Mth.sin(time * 1.7f) + 0.5f * Mth.sin(time * 4.3f)));
        event.setPitch(event.getPitch() + shake * (Mth.cos(time * 2.1f) + 0.5f * Mth.sin(time * 3.7f)));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || !Perk.SOMMELIER.ownedBy(event.getEntity())) return;
        String flavor = event.getItemStack().getItem() instanceof TobaccoItem tobacco ? tobacco.getFlavor()
                : event.getItemStack().get(ModDataComponents.TOBACCO_FLAVOR.get());
        if (flavor == null || !ClientMind.data().getCompound("Preferences").contains(flavor)) return;
        int score = ClientMind.data().getCompound("Preferences").getInt(flavor);
        String verdict = score > 0 ? "liked" : score < 0 ? "disliked" : "neutral";
        event.getToolTip().add(Component.translatable("quasium.sommelier." + verdict).withStyle(ChatFormatting.DARK_AQUA));
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.KAIFOMETER.get())) return;
        InteractionHand other = event.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (event.getEntity().getItemInHand(other).isEmpty()) KaifometerScreen.open();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMind.reset();
    }
}
