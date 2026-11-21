package net.sorukoma.quasium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.sorukoma.quasium.Config;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;

import java.util.Map;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmokecraftClientEvents {
    private SmokecraftClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "zone_tint"), (graphics, deltaTracker) -> {
            int tier = ClientZoneData.tier();
            int strength = Config.ZONE_TINT_PCT.getAsInt();
            if (tier <= 0 || strength <= 0 || Minecraft.getInstance().options.hideGui) return;
            int alpha = Math.round(tier * 20 * strength / 100f);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | (ClientZoneData.color() & 0xFFFFFF));
        });
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "ring_timing"), RingTiming::render);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) return;
        Map.Entry<ResourceLocation, NoteProfile> profile = NoteProfiles.forStack(event.getItemStack());
        if (profile == null) return;
        if (ClientMagicData.isScanned(profile.getKey().toString())) {
            event.getToolTip().add(Component.translatable("quasium.tooltip.notes", profile.getValue().describe()).withStyle(ChatFormatting.GRAY));
        } else if (ClientMagicData.scannedCount() > 0 || NoteProfiles.explicitFor(event.getItemStack()) != null) {
            event.getToolTip().add(Component.translatable("quasium.tooltip.notes_unknown").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientMagicData.glowingOres().isEmpty()) return;
        long left = ClientMagicData.glowUntil() - minecraft.level.getGameTime();
        if (left <= 0) return;
        float alpha = Math.min(1f, left / 40f);
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(GlowRenderType.LINES_THROUGH_WALLS);
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : ClientMagicData.glowingOres()) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(pos).deflate(0.002), 1f, 0.55f, 0.2f, alpha);
        }
        poseStack.popPose();
        buffers.endBatch(GlowRenderType.LINES_THROUGH_WALLS);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMagicData.reset();
        ClientZoneData.reset();
    }
}
