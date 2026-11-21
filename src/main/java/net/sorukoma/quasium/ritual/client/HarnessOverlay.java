package net.sorukoma.quasium.ritual.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ritual.RitualRegistry;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class HarnessOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/quasium_harness.png");
    private static final float OVER_ARMOR = 1.15f;

    @Nullable
    private static HumanoidModel<LivingEntity> straps;

    private HarnessOverlay() {
    }

    public static void render(ItemStack backpack, LivingEntity wearer, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!backpack.has(RitualRegistry.QUASIUM_HARNESS.get())) return;
        HumanoidModel<LivingEntity> model = straps();
        ICurioRenderer.followBodyRotations(wearer, model);
        model.setAllVisible(false);
        model.body.visible = true;
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
    }

    private static HumanoidModel<LivingEntity> straps() {
        if (straps == null) {
            straps = new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(OVER_ARMOR), 0f), 64, 32).bakeRoot());
        }
        return straps;
    }
}
