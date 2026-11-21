package net.sorukoma.quasium.ecology.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.ecology.DymnikEntity;
import net.sorukoma.quasium.ecology.GarevshchikEntity;
import net.sorukoma.quasium.ecology.SmogMobs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmogMobsClient {
    private static final ResourceLocation DYMNIK = texture("dymnik");
    private static final ResourceLocation DYMNIK_EYES = texture("dymnik_eyes");
    private static final ResourceLocation GAREVSHCHIK = texture("garevshchik");
    private static final ResourceLocation GAREVSHCHIK_EYES = texture("garevshchik_eyes");

    private SmogMobsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SmogMobs.DYMNIK.get(), DymnikRenderer::new);
        event.registerEntityRenderer(SmogMobs.GAREVSHCHIK.get(), GarevshchikRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DymnikModel.LAYER, DymnikModel::createBodyLayer);
        event.registerLayerDefinition(GarevshchikModel.LAYER, GarevshchikModel::createBodyLayer);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static final class DymnikRenderer extends MobRenderer<DymnikEntity, DymnikModel> {
        private DymnikRenderer(EntityRendererProvider.Context context) {
            super(context, new DymnikModel(context.bakeLayer(DymnikModel.LAYER)), 0.3f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(DYMNIK_EYES);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(DymnikEntity dymnik) {
            return DYMNIK;
        }

        @Nullable
        @Override
        protected RenderType getRenderType(DymnikEntity dymnik, boolean bodyVisible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(DYMNIK);
        }
    }

    private static final class GarevshchikRenderer extends MobRenderer<GarevshchikEntity, GarevshchikModel> {
        private GarevshchikRenderer(EntityRendererProvider.Context context) {
            super(context, new GarevshchikModel(context.bakeLayer(GarevshchikModel.LAYER)), 0.45f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GAREVSHCHIK_EYES);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(GarevshchikEntity garevshchik) {
            return GAREVSHCHIK;
        }
    }
}
