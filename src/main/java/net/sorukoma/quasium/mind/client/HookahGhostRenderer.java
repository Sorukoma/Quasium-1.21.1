package net.sorukoma.quasium.mind.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.ghost.HookahGhost;

@OnlyIn(Dist.CLIENT)
public class HookahGhostRenderer extends MobRenderer<HookahGhost, VillagerModel<HookahGhost>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/hookah_ghost.png");

    public HookahGhostRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(HookahGhost ghost) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(HookahGhost ghost, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected void scale(HookahGhost ghost, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375f, 0.9375f, 0.9375f);
        poseStack.translate(0f, -0.1f - Mth.sin((ghost.tickCount + partialTick) * 0.08f) * 0.06f, 0f);
    }
}
