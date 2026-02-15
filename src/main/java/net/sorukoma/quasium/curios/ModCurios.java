package net.sorukoma.quasium.curios;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.sorukoma.quasium.QuasiumMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Predicate;

public class ModCurios {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation POTION_VALIDATOR_ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "potion_validator");

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModCurios::enqueueIMC);
    }

    private static void enqueueIMC(final InterModEnqueueEvent event) {
        LOGGER.info("Registering Curios Predicate for validator: {}", POTION_VALIDATOR_ID);
        event.enqueueWork(() -> CuriosApi.registerCurioPredicate(POTION_VALIDATOR_ID, ModCurios::isPotionItem));
    }

    private static boolean isPotionItem(SlotResult slotResult) {
        ItemStack stack = slotResult.stack();
        boolean isPotion = stack.getItem() instanceof PotionItem || stack.getItem() == Items.POTION;
        // Удалено: LOGGER.debug("Checking item {} for slot {}. Is potion: {}", stack.getDisplayName().getString(), slotResult.slotContext().identifier(), isPotion);
        return isPotion;
    }
}
