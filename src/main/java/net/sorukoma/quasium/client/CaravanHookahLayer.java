package net.sorukoma.quasium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.selection.trade.CaravanLlamas;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class CaravanHookahLayer extends RenderLayer<Llama, LlamaModel<Llama>> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "caravan_hookahs"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/caravan_hookahs.png");
    private static final float HANG_X = 8f;
    private static final float SWAY = 0.12f;

    private final ModelPart root;
    private final ModelPart rightHookah;
    private final ModelPart leftHookah;

    private CaravanHookahLayer(RenderLayerParent<Llama, LlamaModel<Llama>> parent, ModelPart root) {
        super(parent);
        this.root = root;
        rightHookah = root.getChild("right_hookah");
        leftHookah = root.getChild("left_hookah");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("strap", CubeListBuilder.create().texOffs(0, 0).addBox(-7f, 0.5f, 1.5f, 14f, 1f, 1f), PartPose.ZERO);
        root.addOrReplaceChild("right_hookah", hookah(false), PartPose.offset(-HANG_X, 1f, 2f));
        root.addOrReplaceChild("left_hookah", hookah(true), PartPose.offset(HANG_X, 1f, 2f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static CubeListBuilder hookah(boolean mirrored) {
        return CubeListBuilder.create().mirror(mirrored)
                .texOffs(0, 4).addBox(-1f, 0f, -1f, 2f, 1f, 2f)
                .texOffs(10, 4).addBox(-0.5f, 1f, -0.5f, 1f, 5f, 1f)
                .texOffs(0, 12).addBox(-1.5f, 6f, -1.5f, 3f, 4f, 3f);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Llama llama, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (llama.isBaby() || !CaravanLlamas.carriesHookahs(llama)) return;
        float sway = Mth.cos(limbSwing * 0.6662f) * SWAY * limbSwingAmount;
        rightHookah.zRot = sway;
        leftHookah.zRot = sway;
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, LivingEntityRenderer.getOverlayCoords(llama, 0f));
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, CaravanHookahLayer::createLayer);
    }

    @SubscribeEvent
    public static void addToLlamas(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Llama, LlamaModel<Llama>> llamas = event.getRenderer(EntityType.TRADER_LLAMA);
        if (llamas == null) return;
        llamas.addLayer(new CaravanHookahLayer(llamas, event.getEntityModels().bakeLayer(LAYER)));
    }
}
