package net.sorukoma.quasium.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuasiumClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue HIDE_LOCKED_RECIPES = BUILDER
            .comment("Hide how to craft mod items in the progression book, JEI and EMI until the recipe is unlocked")
            .define("hideLockedRecipes", true);

    public static final ModConfigSpec.BooleanValue DARK_BOOK = BUILDER
            .comment("Dark pages with light text in the progression book; the moon and sun button in the book flips it")
            .define("darkBook", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private QuasiumClientConfig() {
    }

    public static void flipDarkBook() {
        DARK_BOOK.set(!DARK_BOOK.get());
        DARK_BOOK.save();
    }
}
