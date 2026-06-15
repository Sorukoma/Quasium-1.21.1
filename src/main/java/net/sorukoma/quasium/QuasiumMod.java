package net.sorukoma.quasium;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
import net.sorukoma.quasium.event.ModEvents;
import net.sorukoma.quasium.item.ModArmorMaterials;
import net.sorukoma.quasium.item.ModCreativeModeTabs;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.progression.manager.ProgressionArticleManager;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.curios.ModCurios;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.screen.ModMenuTypes;
import net.sorukoma.quasium.screen.HookahScreen;
import net.sorukoma.quasium.screen.MixingBowlScreen;
import net.sorukoma.quasium.screen.BackpackHookahScreen;
import net.sorukoma.quasium.screen.HookahBowlScreen;
import net.sorukoma.quasium.screen.HeaterScreen;
import net.sorukoma.quasium.screen.CalloudScreen;
import net.sorukoma.quasium.client.ClientHookahInputHandler;
import net.sorukoma.quasium.entity.ModEntityTypes;
import net.sorukoma.quasium.client.renderer.entity.HookahConnectionRenderer;
import net.sorukoma.quasium.client.renderer.entity.ToxicSmokeRenderer;
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
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@Mod(QuasiumMod.MOD_ID)
public class QuasiumMod {
    public static final String MOD_ID = "quasium";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ProgressionArticleManager progressionArticleManager;

    public QuasiumMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        QuasiumModAdvancements.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        modEventBus.addListener(ModPackets::register);
        // ModEvents is auto-registered via @EventBusSubscriber annotation — no manual register needed
        NeoForge.EVENT_BUS.register(NeoForgeClientEvents.class);
        ModCurios.register(modEventBus);
        net.sorukoma.quasium.loot.ModLootModifiers.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            net.sorukoma.quasium.recipe.DryingRecipes.init();
            net.sorukoma.quasium.recipe.AnvilRecipes.init();
        });

        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        progressionArticleManager = new ProgressionArticleManager();
        event.addListener(progressionArticleManager);
    }

    public static ProgressionArticleManager getProgressionArticleManager() {
        return progressionArticleManager;
    }

    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(
                Ingredient.of(ModItems.BOTTLED_GHAST_ACID.get()),
                Ingredient.of(ModItems.TALLOW.get()),
                new ItemStack(ModItems.GLYCERIN_BOTTLE.get())
        );
    }

    @EventBusSubscriber(modid = QuasiumMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.HOOKAH_MENU.get(), HookahScreen::new);
            event.register(ModMenuTypes.MIXING_BOWL_MENU.get(), MixingBowlScreen::new);
            event.register(ModMenuTypes.BACKPACK_HOOKAH_MENU.get(), BackpackHookahScreen::new);
            event.register(ModMenuTypes.HOOKAH_BOWL_MENU.get(), HookahBowlScreen::new);
            event.register(ModMenuTypes.HEATER_MENU.get(), HeaterScreen::new);
            event.register(ModMenuTypes.CALLOUD_MENU.get(), CalloudScreen::new);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.HOOKAH_CONNECTION.get(), HookahConnectionRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.TOXIC_SMOKE.get(), ToxicSmokeRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DRYING_SHELF_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.DryingShelfRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.QUASIUM_ANVIL_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.QuasiumAnvilRenderer::new);
        }
    }

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

            boolean holdingKaifometer = mainHandItem.getItem() == ModItems.KAIFOMETER.get() ||
                                        offHandItem.getItem() == ModItems.KAIFOMETER.get();

            if (holdingKaifometer) {
                int kaifValue = net.sorukoma.quasium.client.ClientKaifData.getKaifValue();

                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();

                int barVisualWidth = 10;
                int barVisualHeight = 80;
                int padding = 5;

                int barX = screenWidth - barVisualWidth - padding;
                int barY = screenHeight / 2 - (barVisualHeight / 2);

                String kaifText = String.format("%d/%d", kaifValue, PlayerKaifData.MAX_KAIF);
                int textWidth = minecraft.font.width(kaifText);
                int textX = barX + barVisualWidth - textWidth;
                int textY = barY - minecraft.font.lineHeight - padding;

                guiGraphics.drawString(minecraft.font, kaifText, textX, textY, 0xFFFFFFFF, true);

                guiGraphics.fill(barX, barY, barX + barVisualWidth, barY + barVisualHeight, 0xFF000000);

                int filledHeight = (int) ((double) kaifValue / PlayerKaifData.MAX_KAIF * barVisualHeight);
                int filledYStart = barY + barVisualHeight - filledHeight;
                guiGraphics.fill(barX, filledYStart, barX + barVisualWidth, barY + barVisualHeight, 0xFF00FF00);
            }
        }
    }
}