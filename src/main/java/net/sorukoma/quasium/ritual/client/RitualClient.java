package net.sorukoma.quasium.ritual.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.progression.ConfigArgs;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.ritual.RitualRegistry;
import net.sorukoma.quasium.ritual.armor.Smokestep;
import net.sorukoma.quasium.ritual.artifact.QuasiumHarness;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class RitualClient {
    private static final float USE_SLOWDOWN = 0.2f;
    private static final ResourceLocation ARTIFACT = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "artifact");

    private static boolean jumpWasDown;

    private RitualClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RitualRegistry.RITUAL_BRAZIER_BE.get(), BrazierRenderer::new);
        event.registerBlockEntityRenderer(RitualRegistry.RITUAL_PEDESTAL_BE.get(), FloatingItemRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ritual_pulse"), PulseIndicator::render);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            artifactLook(ModItems.BACKPACK_HOOKAH.get(), RitualRegistry.QUASIUM_HARNESS.get());
            artifactLook(ModItems.QUASIUM_CHESTPLATE.get(), RitualRegistry.SMOKE_CARAPACE.get());
            artifactLook(Items.DIAMOND_BOOTS, RitualRegistry.SMOKESTEP.get());
        });
    }

    private static void artifactLook(Item item, DataComponentType<Unit> marker) {
        ItemProperties.register(item, ARTIFACT, (ClampedItemPropertyFunction) (stack, level, entity, seed) -> stack.has(marker) ? 1f : 0f);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !player.isUsingItem() || player.isPassenger()) return;
        if (!player.getUseItem().is(ModItems.HOOKAH_PIPE.get()) || !ClientHookahInputHandler.isConnectedToBackpack()
                || !QuasiumHarness.worn(player)) return;
        event.getInput().leftImpulse /= USE_SLOWDOWN;
        event.getInput().forwardImpulse /= USE_SLOWDOWN;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        if (stack.has(RitualRegistry.QUASIUM_HARNESS.get())) {
            int lungs = ConfigArgs.synced(RitualConfig.HARNESS_LUNG_PCT);
            lines.add(line("harness", lungs % 100 == 0 ? lungs / 100 : lungs / 100f, ConfigArgs.synced(RitualConfig.HARNESS_RING_COOLDOWN_PCT),
                    ConfigArgs.synced(RitualConfig.HARNESS_LINK_RADIUS)));
        }
        if (stack.has(RitualRegistry.SMOKE_CARAPACE.get())) {
            lines.add(line("carapace", ConfigArgs.synced(RitualConfig.CARAPACE_CHARGE_CAP), ConfigArgs.synced(RitualConfig.CARAPACE_NOVA_PCT),
                    ConfigArgs.synced(RitualConfig.CARAPACE_NOVA_RADIUS), Math.round(stack.getOrDefault(RitualRegistry.CARAPACE_CHARGE.get(), 0f))));
        }
        if (stack.has(RitualRegistry.SMOKESTEP.get())) {
            lines.add(line("smokestep", ConfigArgs.synced(RitualConfig.SMOKESTEP_DASH_COOLDOWN_SEC),
                    ConfigArgs.synced(RitualConfig.SMOKESTEP_FALL_DAMAGE_PCT)));
        }
        if (stack.has(RitualRegistry.SMOKE_TEMPERED.get())) {
            lines.add(line("temper", ConfigArgs.synced(RitualConfig.SMOKE_TEMPER_DURABILITY_PCT)));
        }
        if (stack.has(RitualRegistry.SMOKE_THREAD.get())) {
            lines.add(line("thread", ConfigArgs.synced(RitualConfig.SMOKE_THREAD_DODGE_PCT),
                    ConfigArgs.synced(RitualConfig.SMOKE_THREAD_DODGE_CAP_PCT)));
        }
    }

    private static Component line(String key, Object... args) {
        return Component.translatable("quasium.ritual.tooltip." + key, args).withStyle(ChatFormatting.DARK_PURPLE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        boolean down = minecraft.options.keyJump.isDown();
        if (down && !jumpWasDown && minecraft.screen == null && Smokestep.canDash(player) && Smokestep.clientDash(player.level().getGameTime())) {
            player.setDeltaMovement(Smokestep.impulse(player));
            player.level().addParticle(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 0, 0, 0);
            PacketDistributor.sendToServer(new Smokestep.DashPacket());
        }
        jumpWasDown = down;
    }
}
