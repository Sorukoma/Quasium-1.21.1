package net.sorukoma.quasium.curios;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.item.BackpackHookahItem;
import net.sorukoma.quasium.item.ModItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ModCurios {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation BACKPACK_HOOKAH_VALIDATOR_ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "backpack_hookah_validator");

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModCurios::enqueueIMC);
        // Регистрируем клиентский setup только на клиенте: на выделенном сервере
        // обращение к CuriosRendererRegistry/рендереру могло бы уронить загрузку класса.
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            eventBus.addListener(ModCurios::onClientSetup);
        }
    }

    private static void enqueueIMC(final InterModEnqueueEvent event) {
        LOGGER.info("Registering Curios Predicate for validator: {}", BACKPACK_HOOKAH_VALIDATOR_ID);
        event.enqueueWork(() -> CuriosApi.registerCurioPredicate(BACKPACK_HOOKAH_VALIDATOR_ID, ModCurios::isBackpackHookahItem));
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(ModItems.BACKPACK_HOOKAH.get(), net.sorukoma.quasium.client.renderer.curios.BackpackHookahCurioRenderer::new);
        });
    }

    private static boolean isBackpackHookahItem(SlotResult slotResult) {
        ItemStack stack = slotResult.stack();
        return stack.getItem() instanceof BackpackHookahItem;
    }
}