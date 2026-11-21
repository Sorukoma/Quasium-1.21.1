package net.sorukoma.quasium.integration.jei;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.NoteProfiles;
import net.sorukoma.quasium.selection.world.LootOnlyItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.block.ModBlocks;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.recipe.AnvilRecipes;
import net.sorukoma.quasium.recipe.DryingRecipes;
import net.sorukoma.quasium.recipe.EssenceRecipes;
import net.sorukoma.quasium.recipe.FermentationRecipes;
import net.sorukoma.quasium.recipe.SoakingRecipes;

@JeiPlugin
public class QuasiumJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiRecipeGate.attach(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeGate.detach();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new DryingCategory(guiHelper),
                new FermentationCategory(guiHelper),
                new SoakingCategory(guiHelper),
                new AnvilCategory(guiHelper),
                new EssenceCategory(guiHelper),
                new DistillationCategory(guiHelper)
        );
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.ESSENCE_BOTTLE.get(), byComponent(ModDataComponents.ESSENCE_TYPE.get()));
        registration.registerSubtypeInterpreter(ModItems.AROMA_VIAL.get(), byComponent(ModDataComponents.ESSENCE_TYPE.get()));
        registration.registerSubtypeInterpreter(ModItems.SELF_MIXED_TOBACCO.get(), byComponent(DataComponents.ITEM_NAME));
    }

    private static ISubtypeInterpreter<ItemStack> byComponent(DataComponentType<?> type) {
        return new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(type);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object value = stack.get(type);
                return value == null ? "" : value.toString();
            }
        };
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        DryingRecipes.init();
        registration.addRecipes(DryingCategory.RECIPE_TYPE, DryingRecipes.getRecipes());
        registration.addRecipes(FermentationCategory.RECIPE_TYPE, FermentationRecipes.createRecipes());
        registration.addRecipes(SoakingCategory.RECIPE_TYPE, SoakingRecipes.createRecipes());
        AnvilRecipes.init();
        registration.addRecipes(AnvilCategory.RECIPE_TYPE, AnvilRecipes.getRecipes());
        registration.addRecipes(EssenceCategory.RECIPE_TYPE, EssenceRecipes.createRecipes());
        registration.addRecipes(DistillationCategory.RECIPE_TYPE, NoteProfiles.known().values().stream()
                .filter(profile -> !profile.isEntity()).toList());
        for (LootOnlyItems.Entry entry : LootOnlyItems.entries()) registration.addItemStackInfo(entry.stacks(), entry.text());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DRYING_SHELF_ITEM.get()), DryingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FERMENTATION_BARREL_ITEM.get()), FermentationCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.NETHERITE_HAMMER.get()), FermentationCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOAKING_BOWL_ITEM.get()), SoakingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.STICK), SoakingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.QUASIUM_ANVIL_ITEM.get()), AnvilCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.NETHERITE_HAMMER.get()), AnvilCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ESSENCE_PRESS_ITEM.get()), EssenceCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DISTILLATION_CUBE_ITEM.get()), DistillationCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.KAIFOMETER.get()), DistillationCategory.RECIPE_TYPE);
    }
}