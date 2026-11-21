package net.sorukoma.quasium.smokelands.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.client.ClientKaifData;
import net.sorukoma.quasium.smokelands.DraftCycle;
import net.sorukoma.quasium.smokelands.SmokelandsDimension;
import net.sorukoma.quasium.smokelands.SmokelandsKaif;
import net.sorukoma.quasium.smokelands.fluid.SmokeFluids;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class SmokelandsClient {
    private static final ResourceLocation CLOCK_TIME = ResourceLocation.withDefaultNamespace("time");

    private SmokelandsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        SmokelandsKaif.readClientFrom(ClientKaifData::getKaifValue);
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(SmokeFluids.SOURCE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SmokeFluids.FLOWING.get(), RenderType.translucent());
            draftClock();
        });
    }

    private static void draftClock() {
        ItemPropertyFunction sunClock = ItemProperties.getProperty(new ItemStack(Items.CLOCK), CLOCK_TIME);
        if (sunClock == null) return;
        ItemProperties.register(Items.CLOCK, CLOCK_TIME, (ClampedItemPropertyFunction) (stack, level, entity, seed) -> {
            Level at = level != null ? level : entity != null ? entity.level() : null;
            if (at != null && SmokelandsDimension.contains(at)) return DraftCycle.phase(at, 0f);
            return sunClock.call(stack, level, entity, seed);
        });
    }

    @SubscribeEvent
    public static void registerSky(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SmokelandsDimension.EFFECTS, new SmokelandsSky());
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new SmokeFluidClient(), SmokeFluids.TYPE.get());
    }
}
