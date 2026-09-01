package net.sorukoma.quasium.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.entity.ModularHookahBlockEntity;
import net.sorukoma.quasium.hookah.HookahShaftVariant;

/**
 * Рендерит модульные части поверх базовой колбы (сам блок колбы рисуется JSON-моделью):
 *  - шахту (part-модель по варианту),
 *  - диффузор (part-модель), если накручен,
 *  - чашу (item HOOKAH_BOWL) на верхушке шахты,
 *  - каллауд (item CALLOUD) над чашей.
 *
 * Все части используют baked-модели из ModelManager (part-модели зарегистрированы
 * как отдельные ресурсы) или рендер предметов.
 */
public class ModularHookahRenderer implements BlockEntityRenderer<ModularHookahBlockEntity> {

    // Геометрия сборки (в долях блока). Полная высота ~1.2 блока.
    // Шахта заходит нижним концом внутрь колбы, поэтому старт ниже верхушки тела колбы.
    private static final float SHAFT_BASE = 5f / 16f;   // низ шахты — внутри колбы
    private static final float SHAFT_MODEL_H = 9f / 16f; // высота part-модели шахты
    private static final float SHAFT_TOP = SHAFT_BASE + SHAFT_MODEL_H; // ~0.875
    // Чаша ставится на верхний конец шахты; каллауд — сверху чаши.
    private static final float BOWL_Y = SHAFT_TOP;             // ~0.875
    private static final float CALLOUD_Y = BOWL_Y + 3f / 16f;  // над обедом чаши (~1.06)

    private final BlockEntityRendererProvider.Context context;

    public ModularHookahRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(ModularHookahBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light, int overlay) {
        // Вода видна в колбе даже до установки шахты.
        renderWater(entity, poseStack, bufferSource, light, overlay);

        if (!entity.hasShaft()) return;

        var modelManager = Minecraft.getInstance().getModelManager();

        // ─── Шахта (нижним концом внутри колбы) ───
        HookahShaftVariant variant = entity.getShaftVariant();
        if (variant == null) variant = HookahShaftVariant.WHITE;
        renderPart(modelManager, "modular_shaft_part_" + variant.getName(),
                poseStack, bufferSource, light, overlay, SHAFT_BASE);

        // ─── Диффузор: муфта, накрученная на нижний (погружной) конец шахты, тех же размеров ───
        if (entity.hasScrewedDiffuser()) {
            renderPart(modelManager, "modular_diffuser_part",
                    poseStack, bufferSource, light, overlay, SHAFT_BASE);
        }

        // ─── Чаша (на верхнем конце шахты) ───
        ItemStack bowl = entity.getItemHandler().getStackInSlot(1);
        if (!bowl.isEmpty()) {
            renderPart(modelManager, "modular_bowl_part",
                    poseStack, bufferSource, light, overlay, BOWL_Y);

            // ─── Каллауд (коробка с выемкой над чашей) ───
            ItemStack calloud = entity.getItemHandler().getStackInSlot(0);
            if (!calloud.isEmpty()) {
                renderPart(modelManager, "modular_calloud_part",
                        poseStack, bufferSource, light, overlay, CALLOUD_Y);
                // Угли внутри каллауда — число кубиков = число углей (1..8).
                int coals = calloudCharcoalCount(calloud);
                if (coals > 0) {
                    int n = Math.max(1, Math.min(coals, 8));
                    renderPart(modelManager, "modular_calloud_coals_" + n,
                            poseStack, bufferSource, light, overlay, CALLOUD_Y);
                }
            }
        }
    }

    /** Число углей в каллауде (базовые + уложенные сверху). */
    private int calloudCharcoalCount(ItemStack calloud) {
        var data = calloud.get(net.sorukoma.quasium.component.ModDataComponents.CALLOUD_CONTENTS.get());
        if (data == null) return 0;
        return data.charcoals().size() + data.stackedCharcoals().size();
    }

    /** Рендер standalone block-модели детали со смещением по Y (в блочных координатах 0..1). */
    private void renderPart(net.minecraft.client.resources.model.ModelManager modelManager, String path,
                            PoseStack poseStack, MultiBufferSource bufferSource,
                            int light, int overlay, float yOffset) {
        var modelId = net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "block/" + path));
        var model = modelManager.getModel(modelId);
        poseStack.pushPose();
        poseStack.translate(0, yOffset, 0);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.cutout()),
                null,
                model,
                1f, 1f, 1f,
                light, overlay);
        poseStack.popPose();
    }

    /**
     * Слой воды внутри колбы — заполненный объём (все боковые грани + верх + низ),
     * высота пропорциональна waterMb. Грани намотаны наружу (CCW), чтобы не отсекались
     * back-face culling'ом полупрозрачного слоя.
     */
    private void renderWater(ModularHookahBlockEntity entity, PoseStack poseStack,
                             MultiBufferSource bufferSource, int light, int overlay) {
        int waterMb = entity.getWaterMb();
        if (waterMb <= 0) return;

        float fill = Math.min(1f, waterMb / (float) ModularHookahBlockEntity.MAX_WATER_MB);
        // Заполняем почти всё тело колбы (x/z 4..12 в модели → 0.25..0.75).
        float lo = 4.2f / 16f;
        float hi = 11.8f / 16f;
        float y0 = 0.5f / 16f;
        float y1 = y0 + fill * (5.3f / 16f);

        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite =
                Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                        .apply(ResourceLocation.withDefaultNamespace("block/water_still"));

        var buffer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.translucent());
        var pose = poseStack.last();
        float uu0 = sprite.getU0(), uu1 = sprite.getU1();
        float vv0 = sprite.getV0(), vv1 = sprite.getV1();
        int r = 60, g = 130, b = 235, a = 235;

        // Полностью залитый параллелепипед: используем яркий full-brightness свет,
        // чтобы вода не выглядела тёмной внутри стекла.
        int waterLight = 0x00F000F0;

        // top (+Y)
        face(pose, buffer, lo, y1, lo,  lo, y1, hi,  hi, y1, hi,  hi, y1, lo, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, 0, 1, 0);
        // bottom (-Y)
        face(pose, buffer, lo, y0, hi,  lo, y0, lo,  hi, y0, lo,  hi, y0, hi, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, 0, -1, 0);
        // north (-Z)
        face(pose, buffer, hi, y1, lo,  hi, y0, lo,  lo, y0, lo,  lo, y1, lo, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, 0, 0, -1);
        // south (+Z)
        face(pose, buffer, lo, y1, hi,  lo, y0, hi,  hi, y0, hi,  hi, y1, hi, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, 0, 0, 1);
        // west (-X)
        face(pose, buffer, lo, y1, lo,  lo, y0, lo,  lo, y0, hi,  lo, y1, hi, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, -1, 0, 0);
        // east (+X)
        face(pose, buffer, hi, y1, hi,  hi, y0, hi,  hi, y0, lo,  hi, y1, lo, uu0, vv0, uu1, vv1, r, g, b, a, waterLight, overlay, 1, 0, 0);
    }

    /** Один прямоугольник по 4 явным вершинам (порядок = наружу, CCW). */
    private void face(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buf,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float u0, float v0, float u1, float v1,
                      int r, int g, int b, int a, int light, int overlay,
                      float nx, float ny, float nz) {
        vertex(pose, buf, ax, ay, az, u0, v0, r, g, b, a, light, overlay, nx, ny, nz);
        vertex(pose, buf, bx, by, bz, u0, v1, r, g, b, a, light, overlay, nx, ny, nz);
        vertex(pose, buf, cx, cy, cz, u1, v1, r, g, b, a, light, overlay, nx, ny, nz);
        vertex(pose, buf, dx, dy, dz, u1, v0, r, g, b, a, light, overlay, nx, ny, nz);
    }

    private void vertex(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buf,
                        float x, float y, float z, float u, float v,
                        int r, int g, int b, int a, int light, int overlay,
                        float nx, float ny, float nz) {
        buf.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
