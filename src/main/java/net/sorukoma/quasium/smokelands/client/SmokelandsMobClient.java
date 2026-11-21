package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.mob.SmokelandsEntities;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmokelandsMobClient {
    private static final ResourceLocation TAR_SLIME = SmokelandsDimension.id("textures/entity/tar_slime.png");

    private SmokelandsMobClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SmokelandsEntities.MIST_PHANTOM.get(), MistPhantomRenderer::new);
        event.registerEntityRenderer(SmokelandsEntities.TAR_SLIME.get(), context -> new SlimeRenderer(context) {
            @Override
            public ResourceLocation getTextureLocation(Slime slime) {
                return TAR_SLIME;
            }
        });
        event.registerEntityRenderer(SmokelandsEntities.DRAFT_WISP.get(), WispRenderer::new);
    }
}
