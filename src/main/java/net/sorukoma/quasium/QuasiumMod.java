package net.sorukoma.quasium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.data.PlayerKaifData;
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
import net.sorukoma.quasium.screen.BackpackHookahScreen;
import net.sorukoma.quasium.screen.HookahBowlScreen;
import net.sorukoma.quasium.screen.HeaterScreen;
import net.sorukoma.quasium.screen.CalloudScreen;
import net.sorukoma.quasium.entity.ModEntityTypes;
import net.sorukoma.quasium.client.renderer.entity.HookahConnectionRenderer;
import net.sorukoma.quasium.client.renderer.entity.ToxicSmokeRenderer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


@Mod(QuasiumMod.MOD_ID)
public class QuasiumMod {
    public static final String MOD_ID = "quasium";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ProgressionArticleManager progressionArticleManager;

    public QuasiumMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterCapabilities);

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
        // NeoForgeClientEvents is auto-registered (client-only) via @EventBusSubscriber — must NOT be
        // registered here, since touching the class on a dedicated server loads client-only classes.
        ModCurios.register(modEventBus);
        net.sorukoma.quasium.loot.ModLootModifiers.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            net.sorukoma.quasium.recipe.DryingRecipes.init();
            net.sorukoma.quasium.recipe.AnvilRecipes.init();
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(ModItems.DIKAR_LEAVES.get(), 0.5f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(ModItems.PESCHANIK_LEAVES.get(), 0.5f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(ModItems.BOLOTNIK_LEAVES.get(), 0.5f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(ModItems.LEDYANOY_LEAVES.get(), 0.5f);
        });
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        progressionArticleManager = new ProgressionArticleManager();
        event.addListener(progressionArticleManager);
    }

    public static ProgressionArticleManager getProgressionArticleManager() {
        return progressionArticleManager;
    }

    // Mod-bus event: registered explicitly in the constructor, so it must NOT carry @SubscribeEvent
    // (this instance is also registered on the game bus, which rejects mod-bus events).
    public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.BONSAI_POT_BE.get(),
                (entity, side) -> net.sorukoma.quasium.block.entity.BonsaiPotBlockEntity.getCapability(entity, side));
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
            event.register(ModMenuTypes.BACKPACK_HOOKAH_MENU.get(), BackpackHookahScreen::new);
            event.register(ModMenuTypes.HOOKAH_BOWL_MENU.get(), HookahBowlScreen::new);
            event.register(ModMenuTypes.HEATER_MENU.get(), HeaterScreen::new);
            event.register(ModMenuTypes.CALLOUD_MENU.get(), CalloudScreen::new);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.HOOKAH_CONNECTION.get(), HookahConnectionRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.TOXIC_SMOKE.get(), ToxicSmokeRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.BONSAI_POT_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.BonsaiPotRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DRYING_SHELF_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.DryingShelfRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.SOAKING_BOWL_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.SoakingBowlRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.QUASIUM_ANVIL_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.QuasiumAnvilRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.TOBACCO_POLE_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.TobaccoPoleRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.MODULAR_HOOKAH_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.ModularHookahRenderer::new);
        }

        @SubscribeEvent
        public static void registerAdditionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
            // Part-модели модульного кальяна, рендерящиеся через BlockEntityRenderer
            // (не привязаны к blockstate/item, поэтому регистрируем явно, иначе getModel вернёт missing).
            for (String color : new String[]{"white", "black", "red", "blue", "amber"}) {
                event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                QuasiumMod.MOD_ID, "block/modular_shaft_part_" + color)));
            }
            for (String part : new String[]{"modular_diffuser_part", "modular_bowl_part", "modular_calloud_part"}) {
                event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "block/" + part)));
            }
            for (int n = 1; n <= 8; n++) {
                event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "block/modular_calloud_coals_" + n)));
            }
        }

        @SubscribeEvent
        public static void registerItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> {
                if (tintIndex != 0) return -1;
                String type = stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), "");
                return switch (type) {
                    case "mint" -> 0xFF3CD070;
                    case "lemon" -> 0xFFFFF15A;
                    case "lime" -> 0xFF8CD63F;
                    case "orange" -> 0xFFFF9D2E;
                    case "grapefruit" -> 0xFFFF6F61;
                    case "berry" -> 0xFFC2185B;
                    case "ice" -> 0xFF64B5F6;
                    case "honey" -> 0xFFFFB300;
                    case "nether" -> 0xFF880E4F;
                    case "lavender" -> 0xFFB06CCF;
                    case "cocoa" -> 0xFF6D3B25;
                    case "apple" -> 0xFFD84A3A;
                    case "watermelon" -> 0xFFF25C6B;
                    case "grape" -> 0xFF7746B5;
                    case "cherry" -> 0xFFB5163A;
                    case "vanilla" -> 0xFFF1E0A6;
                    case "coffee" -> 0xFF4B2E22;
                    case "cinnamon" -> 0xFFB65A2A;
                    case "coconut" -> 0xFFE8DFC8;
                    case "rose" -> 0xFFF07FA5;
                    case "anise" -> 0xFF9BCB75;
                    default -> type.isEmpty() ? 0xFFFFFFFF : 0xFF9C27B0;
                };
            }, ModItems.ESSENCE_BOTTLE.get());
            event.register((stack, tintIndex) -> {
                int stage = stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0);
                return switch (stage) {
                    case 1 -> 0xFF78934F;
                    case 2 -> 0xFF9A874D;
                    default -> 0xFF4F8A3C;
                };
            }, ModItems.TOBACCO_LEAVES.get(), ModItems.MAKHORKA_LEAVES.get(),
                    ModItems.DIKAR_LEAVES.get(), ModItems.PESCHANIK_LEAVES.get(),
                    ModItems.BOLOTNIK_LEAVES.get(), ModItems.LEDYANOY_LEAVES.get(),
                    ModItems.HYBRID_TOBACCO_LEAVES.get());
        }
    }

    @EventBusSubscriber(modid = QuasiumMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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

                // Display kaif rank name above the bar
                net.sorukoma.quasium.data.KaifAbilitiesHandler.RankInfo rankInfo =
                        net.sorukoma.quasium.data.KaifAbilitiesHandler.getKaifRank(kaifValue);
                String rankText = Component.translatable(rankInfo.translationKey()).getString();
                int rankTextWidth = minecraft.font.width(rankText);
                int rankTextX = barX + barVisualWidth - rankTextWidth;
                int rankTextY = textY - minecraft.font.lineHeight - 2;
                guiGraphics.drawString(minecraft.font, rankText, rankTextX, rankTextY, rankInfo.color(), true);

                guiGraphics.drawString(minecraft.font, kaifText, textX, textY, 0xFFFFFFFF, true);

                guiGraphics.fill(barX, barY, barX + barVisualWidth, barY + barVisualHeight, 0xFF000000);

                int filledHeight = (int) ((double) kaifValue / PlayerKaifData.MAX_KAIF * barVisualHeight);
                int filledYStart = barY + barVisualHeight - filledHeight;
                guiGraphics.fill(barX, filledYStart, barX + barVisualWidth, barY + barVisualHeight, 0xFF00FF00);
            }
        }
    }
}
