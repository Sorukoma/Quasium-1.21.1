package net.sorukoma.quasium.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.sorukoma.quasium.QuasiumMod;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =DeferredRegister.createItems(QuasiumMod.MOD_ID);

    public static final DeferredItem<Item> QUASIUMINGOT = ITEMS.register("quasiumingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAWQUASIUM = ITEMS.register("rawquasium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> KAIFOMETER = ITEMS.register("kaifometer",
            () -> new KaifometerItem(new Item.Properties()));
    public static final DeferredItem<Item> PROGRESSION_BOOK = ITEMS.register("progression_book",
            () -> new ProgressionBookItem(new Item.Properties().stacksTo(1))); // Книга должна быть не стакаемой

    // Новый предмет: трубка кальяна
    public static final DeferredItem<Item> HOOKAH_PIPE = ITEMS.register("hookah_pipe",
            () -> new HookahPipeItem(new Item.Properties().stacksTo(1))); // Трубка должна быть не стакаемой

    public static final DeferredItem<Item> QUASIUM_HELMET = ITEMS.register("quasium_helmet",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.HELMET, new Item.Properties().durability(330)));
    public static final DeferredItem<Item> QUASIUM_CHESTPLATE = ITEMS.register("quasium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(480)));
    public static final DeferredItem<Item> QUASIUM_LEGGINGS = ITEMS.register("quasium_leggings",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(450)));
    public static final DeferredItem<Item> QUASIUM_BOOTS = ITEMS.register("quasium_boots",
            () -> new ArmorItem(ModArmorMaterials.QUASIUM, ArmorItem.Type.BOOTS, new Item.Properties().durability(390)));


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
