package net.sorukoma.quasium.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.sorukoma.quasium.client.RecipeGate;
import net.sorukoma.quasium.client.gui.book.BookArt;
import net.sorukoma.quasium.mind.client.ResearchLocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class GatedEmiRecipe implements EmiRecipe {
    private static final int OUTPUT = 18;
    private static final List<Component> LOCKED = List.of(Component.translatable("quasium.progression.recipe_locked"),
            Component.translatable("quasium.progression.recipe_locked_hint"));

    private final EmiRecipe inner;
    private final BooleanSupplier locked;
    private final Supplier<List<Component>> reason;
    @Nullable
    private final ResourceLocation node;

    GatedEmiRecipe(EmiRecipe inner, ResourceLocation gate) {
        this(inner, () -> RecipeGate.isLocked(gate), () -> ResearchLocks.withTeacher(LOCKED, gate), null);
    }

    private GatedEmiRecipe(EmiRecipe inner, BooleanSupplier locked, Supplier<List<Component>> reason, @Nullable ResourceLocation node) {
        this.inner = inner;
        this.locked = locked;
        this.reason = reason;
        this.node = node;
    }

    static EmiRecipe research(EmiRecipe inner, @Nullable ResourceLocation node) {
        if (node == null) return inner;
        return new GatedEmiRecipe(inner, () -> ResearchLocks.hides(node), () -> ResearchLocks.tooltip(node), node);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return inner.getCategory();
    }

    @Override
    public ResourceLocation getId() {
        return inner.getId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inner.getInputs();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return inner.getCatalysts();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return inner.getOutputs();
    }

    @Override
    public int getDisplayWidth() {
        return inner.getDisplayWidth();
    }

    @Override
    public int getDisplayHeight() {
        return inner.getDisplayHeight() + (node == null ? 0 : ResearchLocks.RESERVED_HEIGHT);
    }

    @Override
    public boolean supportsRecipeTree() {
        return !locked.getAsBoolean() && inner.supportsRecipeTree();
    }

    @Override
    public boolean hideCraftable() {
        return locked.getAsBoolean() || inner.hideCraftable();
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return locked.getAsBoolean() ? null : inner.getBackingRecipe();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int width = widgets.getWidth();
        int height = widgets.getHeight();
        if (!locked.getAsBoolean()) {
            inner.addWidgets(widgets);
            if (node != null) {
                int top = inner.getDisplayHeight() + 4;
                widgets.addDrawable(0, top, width, height - top, (graphics, mouseX, mouseY, delta) -> ResearchLocks.draw(graphics, node, 0, 0, width));
            }
            return;
        }
        int left = (width - BookArt.LOCK_W - 6 - OUTPUT) / 2;
        widgets.addDrawable(left, (height - BookArt.LOCK_H) / 2, BookArt.LOCK_W, BookArt.LOCK_H,
                (graphics, mouseX, mouseY, delta) -> graphics.blitSprite(BookArt.LOCK, 0, 0, BookArt.LOCK_W, BookArt.LOCK_H));
        if (!inner.getOutputs().isEmpty()) {
            widgets.addSlot(inner.getOutputs().get(0), left + BookArt.LOCK_W + 6, (height - OUTPUT) / 2);
        }
        widgets.addTooltipText(reason.get(), 0, 0, width, height);
    }
}
