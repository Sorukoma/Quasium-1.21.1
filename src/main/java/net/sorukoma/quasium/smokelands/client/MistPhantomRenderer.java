package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;

final class MistPhantomRenderer extends PhantomRenderer {
    private static final ResourceLocation TEXTURE = SmokelandsDimension.id("textures/entity/mist_phantom.png");
    private static final RenderType EYES = RenderType.eyes(SmokelandsDimension.id("textures/entity/mist_phantom_eyes.png"));

    MistPhantomRenderer(EntityRendererProvider.Context context) {
        super(context);
        layers.clear();
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Phantom phantom) {
        return TEXTURE;
    }
}
