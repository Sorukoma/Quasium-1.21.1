package net.sorukoma.quasium;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.event.ModEvents;
import net.sorukoma.quasium.item.ModArmorMaterials;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.progression.manager.ProgressionArticleManager;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.curios.ModCurios; // Оставляем импорт, так как ModCurios теперь регистрирует валидатор
import net.sorukoma.quasium.block.ModBlocks; // Импортируем ModBlocks
import net.sorukoma.quasium.block.entity.ModBlockEntities; // Импортируем ModBlockEntities
import net.sorukoma.quasium.screen.ModMenuTypes; // Импортируем ModMenuTypes
import net.sorukoma.quasium.screen.HookahScreen; // Импортируем HookahScreen
import net.sorukoma.quasium.client.ClientHookahInputHandler; // Импортируем ClientHookahInputHandler
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
// import net.minecraft.client.gui.screens.MenuScreens; // Удаляем импорт MenuScreens
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent; // Добавляем импорт RegisterMenuScreensEvent

import net.minecraft.core.BlockPos; // Добавляем импорт BlockPos
import java.util.Map; // Добавляем импорт Map
import java.util.UUID; // Добавляем импорт UUID
import java.util.concurrent.ConcurrentHashMap; // Добавляем импорт ConcurrentHashMap
import org.jetbrains.annotations.Nullable; // Добавляем импорт Nullable


// Удаляем импорты для валидатора Curios, так как они теперь в ModCurios
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.world.item.PotionItem;
// import net.minecraft.world.item.Items;
// import top.theillusivec4.curios.api.CuriosApi;
// import top.theillusivec4.curios.api.SlotResult;
// import java.util.function.Predicate;


// The value here should match an entry in the META-INF/neoforged.mods.toml file
@Mod(QuasiumMod.MOD_ID)
public class QuasiumMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "quasium";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ProgressionArticleManager progressionArticleManager; // Объявляем менеджер статей как статический

    // Статическая карта для отслеживания, к какому кальяну прикреплена трубка игрока
    private static final Map<UUID, BlockPos> attachedPipes = new ConcurrentHashMap<>();

    // Удаляем ID для нашего пользовательского валидатора зелий, так как он теперь в ModCurios
    // public static final ResourceLocation POTION_VALIDATOR_ID = ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "potion_validator");

    public QuasiumMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);


        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus); // Регистрируем ModBlocks
        ModBlockEntities.register(modEventBus); // Регистрируем ModBlockEntities
        ModMenuTypes.register(modEventBus); // Регистрируем ModMenuTypes
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModDataComponents.register(modEventBus); // Регистрация ModDataComponents
        QuasiumModAdvancements.register(modEventBus); // Регистрация QuasiumModAdvancements
        modEventBus.addListener(ModPackets::register); // Правильная регистрация сетевых пакетов
        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.register(NeoForgeClientEvents.class); // Явная регистрация NeoForgeClientEvents на NeoForge Event Bus
        ModCurios.register(modEventBus); // Возвращаем вызов ModCurios.register()

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register ProgressionArticleManager
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));

        // Удаляем регистрацию валидатора отсюда, так как она теперь в ModCurios
        // LOGGER.info("Registering Curios potion validator: {}", POTION_VALIDATOR_ID);
        // CuriosApi.registerPredicate(POTION_VALIDATOR_ID, QuasiumMod::isPotionItem);
    }

    // Удаляем метод валидатора для Curios, так как он теперь в ModCurios
    // private static boolean isPotionItem(SlotResult slotResult) {
    //     ItemStack stack = slotResult.stack();
    //     boolean isPotion = stack.getItem() instanceof PotionItem || stack.getItem() == Items.POTION;
    //     LOGGER.debug("Checking item {} for slot {}. Is potion: {}", stack.getDisplayName().getString(), slotResult.slotContext().identifier(), isPotion);
    //     return isPotion;
    // }

    // Метод для регистрации слушателей перезагрузки ресурсов
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        progressionArticleManager = new ProgressionArticleManager(); // Инициализируем статический менеджер
        event.addListener(progressionArticleManager);
    }

    // Статический геттер для получения ProgressionArticleManager
    public static ProgressionArticleManager getProgressionArticleManager() {
        return progressionArticleManager;
    }

    // Методы для управления прикрепленными трубками
    public static void addAttachedPipe(UUID playerUUID, BlockPos hookahPos) {
        attachedPipes.put(playerUUID, hookahPos);
        LOGGER.debug("Attached pipe for player {} to hookah at {}", playerUUID, hookahPos);
    }

    public static void removeAttachedPipe(UUID playerUUID) {
        attachedPipes.remove(playerUUID);
        LOGGER.debug("Detached pipe for player {}", playerUUID);
    }

    @Nullable
    public static BlockPos getAttachedPipePos(UUID playerUUID) {
        return attachedPipes.get(playerUUID);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS){
            event.accept(ModItems.QUASIUMINGOT);
            event.accept(ModItems.RAWQUASIUM);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.QUASIUM_HELMET);
            event.accept(ModItems.QUASIUM_CHESTPLATE);
            event.accept(ModItems.QUASIUM_LEGGINGS);
            event.accept(ModItems.QUASIUM_BOOTS);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.PROGRESSION_BOOK);
            event.accept(ModBlocks.HOOKAH_BLOCK.get()); // Добавляем блок кальяна в творческую вкладку
            event.accept(ModItems.HOOKAH_PIPE.get()); // Добавляем трубку кальяна
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // Класс для событий Mod Bus
    @EventBusSubscriber(modid = QuasiumMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
             // Some client setup code
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.HOOKAH_MENU.get(), HookahScreen::new);
        }
    }

    // Класс для событий NeoForge Event Bus
    public static class NeoForgeClientEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            GuiGraphics guiGraphics = event.getGuiGraphics();

            if (player == null) {
                return;
            }

            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            // Проверяем, держит ли игрок Кайфометр в любой руке
            boolean holdingKaifometer = mainHandItem.getItem() == ModItems.KAIFOMETER.get() ||
                                        offHandItem.getItem() == ModItems.KAIFOMETER.get();

            if (holdingKaifometer) {
                // Получаем кайф из ClientKaifData, так как это клиентская сторона
                int kaifValue = net.sorukoma.quasium.client.ClientKaifData.getKaifValue();

                // Получаем размеры экрана
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();

                // Размеры вертикальной шкалы
                int barVisualWidth = 10;
                int barVisualHeight = 80;
                int padding = 5; // Отступ

                // Позиция для шкалы (справа, по центру по высоте)
                int barX = screenWidth - barVisualWidth - padding;
                int barY = screenHeight / 2 - (barVisualHeight / 2);

                // Позиция для текста (над шкалой, выровнен по правому краю шкалы)
                String kaifText = String.format("%d/%d", kaifValue, PlayerKaifData.MAX_KAIF); // Используем PlayerKaifData.MAX_KAIF
                int textWidth = minecraft.font.width(kaifText);
                int textX = barX + barVisualWidth - textWidth; // Выравнивание по правому краю шкалы
                int textY = barY - minecraft.font.lineHeight - padding; // Над шкалой

                // Рендеринг числового значения
                guiGraphics.drawString(minecraft.font, kaifText, textX, textY, 0xFFFFFFFF, true); // Белый цвет, с тенью

                // Фон шкалы (черный)
                guiGraphics.fill(barX, barY, barX + barVisualWidth, barY + barVisualHeight, 0xFF000000);

                // Заполненная часть шкалы (зеленый, заполнение снизу вверх)
                int filledHeight = (int) ((double) kaifValue / PlayerKaifData.MAX_KAIF * barVisualHeight);
                int filledYStart = barY + barVisualHeight - filledHeight;
                guiGraphics.fill(barX, filledYStart, barX + barVisualWidth, barY + barVisualHeight, 0xFF00FF00);
            }
        }
    }
}
