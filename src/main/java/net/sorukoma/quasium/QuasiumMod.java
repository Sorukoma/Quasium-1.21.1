package net.sorukoma.quasium;

import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.ModArmorMaterials;
import net.sorukoma.quasium.item.ModCreativeModeTabs;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.network.ModPackets;
import net.sorukoma.quasium.progression.manager.ProgressionArticleManager;
import net.sorukoma.quasium.advancements.triggers.QuasiumModAdvancements;
import net.sorukoma.quasium.curios.ModCurios;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.block.entity.BowlAutomation;
import net.sorukoma.quasium.block.entity.ModBlockEntities;
import net.sorukoma.quasium.block.entity.TobaccoAutomation;
import net.sorukoma.quasium.screen.ModMenuTypes;
import net.sorukoma.quasium.screen.HookahScreen;
import net.sorukoma.quasium.screen.BackpackHookahScreen;
import net.sorukoma.quasium.screen.HookahBowlScreen;
import net.sorukoma.quasium.screen.HeaterScreen;
import net.sorukoma.quasium.screen.CalloudScreen;
import net.sorukoma.quasium.entity.ModEntityTypes;
import net.sorukoma.quasium.client.renderer.entity.HookahConnectionRenderer;
import net.sorukoma.quasium.client.renderer.entity.ToxicSmokeRenderer;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.magic.SmokecraftEvents;
import net.sorukoma.quasium.particle.ModParticles;
import net.sorukoma.quasium.sound.ModSoundEvents;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Map;

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
        ModSoundEvents.register(modEventBus);
        ModParticles.register(modEventBus);
        net.sorukoma.quasium.effect.ModEffects.register(modEventBus);
        net.sorukoma.quasium.block.LoungeBlocks.register(modEventBus);
        net.sorukoma.quasium.block.MixingBlocks.register(modEventBus);
        net.sorukoma.quasium.draft.DraftBlocks.register(modEventBus);
        net.sorukoma.quasium.draft.GeneratorBlocks.register(modEventBus);
        net.sorukoma.quasium.draft.MachineBlocks.register(modEventBus);
        net.sorukoma.quasium.draft.ForgeBlocks.register(modEventBus);
        net.sorukoma.quasium.cart.HookahCarts.register(modEventBus);
        net.sorukoma.quasium.golem.MarkBlocks.register(modEventBus);
        net.sorukoma.quasium.golem.program.ProgramBlocks.register(modEventBus);
        net.sorukoma.quasium.golem.CalloudGolems.register(modEventBus);
        net.sorukoma.quasium.golem.ClayGolems.register(modEventBus);
        net.sorukoma.quasium.golem.WardenGolems.register(modEventBus);
        net.sorukoma.quasium.kiln.KilnBlocks.register(modEventBus);
        net.sorukoma.quasium.magic.page.PageRegistry.register(modEventBus);
        net.sorukoma.quasium.magic.page.spirit.SpiritRegistry.register(modEventBus);
        net.sorukoma.quasium.magic.page.form.PageFormRegistry.register(modEventBus, modContainer);
        net.sorukoma.quasium.magic.page.effect.HeartRegistry.register(modEventBus);
        net.sorukoma.quasium.selection.SelectionRegistry.register(modEventBus);
        net.sorukoma.quasium.ecology.EcologyRegistry.register(modEventBus);
        net.sorukoma.quasium.mind.MindRegistry.register(modEventBus);
        net.sorukoma.quasium.ritual.RitualRegistry.register(modEventBus);
        net.sorukoma.quasium.smokelands.SmokelandsRegistry.register(modEventBus);
        net.sorukoma.quasium.ecology.SmokeDensity.register(modEventBus);
        net.sorukoma.quasium.progression.BookRewards.register(modEventBus);
        modEventBus.addListener(SmokecraftEvents::onRegisterConfigurationTasks);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, net.sorukoma.quasium.client.QuasiumClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.selection.SelectionConfig.SPEC, "quasium-selection.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.ecology.EcologyConfig.SPEC, "quasium-ecology.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.mind.MindConfig.SPEC, "quasium-mind.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.ritual.RitualConfig.SPEC, "quasium-ritual.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.smokelands.SmokelandsConfig.SPEC, "quasium-smokelands.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, net.sorukoma.quasium.magic.page.PageConfig.SPEC, "quasium-pages.toml");
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DRYING_SHELF_BE.get(),
                (shelf, side) -> TobaccoAutomation.shelf(shelf));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FERMENTATION_BARREL_BE.get(),
                (barrel, side) -> TobaccoAutomation.barrel(barrel));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOAKING_BOWL_BE.get(),
                (bowl, side) -> new BowlAutomation(bowl));
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
            event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(ModItems.ESSENCE_BOTTLE.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "note"),
                    (stack, level, entity, seed) -> {
                        NoteType note = NoteType.byId(stack.getOrDefault(ModDataComponents.ESSENCE_TYPE.get(), ""));
                        return note == null ? 0f : (note.ordinal() + 1) / 10f;
                    }));
        }

        @SubscribeEvent
        public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.NOTE_SMOKE.get(), net.sorukoma.quasium.client.particle.NoteSmokeParticle.Provider::new);
            event.registerSpriteSet(ModParticles.APHID.get(), net.minecraft.client.particle.SuspendedTownParticle.HappyVillagerProvider::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.HOOKAH_MENU.get(), HookahScreen::new);
            event.register(ModMenuTypes.BACKPACK_HOOKAH_MENU.get(), BackpackHookahScreen::new);
            event.register(ModMenuTypes.HOOKAH_BOWL_MENU.get(), HookahBowlScreen::new);
            event.register(ModMenuTypes.HEATER_MENU.get(), HeaterScreen::new);
            event.register(ModMenuTypes.CALLOUD_MENU.get(), CalloudScreen::new);
            event.register(ModMenuTypes.DISTILLATION_CUBE_MENU.get(), net.sorukoma.quasium.screen.DistillationCubeScreen::new);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntityTypes.HOOKAH_CONNECTION.get(), HookahConnectionRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.TOXIC_SMOKE.get(), ToxicSmokeRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SMOKE_CAST.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(net.sorukoma.quasium.magic.page.PageRegistry.SMOKE_BOLT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SMOKE_RING.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SMOKE_TRICK.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.NOTE_JAR_BE.get(), net.sorukoma.quasium.client.renderer.blockentity.NoteJarRenderer::new);
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
                NoteType note = NoteType.byId(type);
                if (note != null) return 0xFF000000 | note.color();
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
            }, ModItems.ESSENCE_BOTTLE.get(), ModItems.AROMA_VIAL.get());
            Map<Item, Integer> freshLeaves = Map.of(
                    ModItems.DIKAR_LEAVES.get(), 0xFF7EDC72,
                    ModItems.PESCHANIK_LEAVES.get(), 0xFFFFD27A,
                    ModItems.BOLOTNIK_LEAVES.get(), 0xFF6CC4A4,
                    ModItems.LEDYANOY_LEAVES.get(), 0xFFA6E2FF,
                    ModItems.HYBRID_TOBACCO_LEAVES.get(), 0xFFDCA2F2,
                    ModItems.ECHO_LEAVES.get(), 0xFF8FE8E2,
                    ModItems.ASHEN_LEAVES.get(), 0xFFD8CEC4,
                    ModItems.CHORUS_LEAVES.get(), 0xFFE6BEF4,
                    ModItems.PRIMORDIAL_LEAVES.get(), 0xFFFFE394);
            event.register((stack, tintIndex) -> {
                int fresh = freshLeaves.get(stack.getItem());
                return switch (stack.getOrDefault(ModDataComponents.DRYING_STAGE.get(), 0)) {
                    case 0 -> fresh;
                    case 1 -> FastColor.ARGB32.lerp(0.4f, fresh, 0xFFC89A58);
                    case 2 -> FastColor.ARGB32.lerp(0.7f, fresh, 0xFFC89A58);
                    default -> FastColor.ARGB32.lerp(0.85f, fresh, 0xFF5A3A22);
                };
            }, freshLeaves.keySet().toArray(Item[]::new));
        }
    }
}
