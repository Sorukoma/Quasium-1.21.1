package net.sorukoma.quasium.golem.program;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum BlockGroup {
    ACTIONS(null),
    LOOPS("research_golems_counting"),
    CONDITIONS("research_golems_forks"),
    REDSTONE("research_golems_signal");

    @Nullable
    private final ResourceLocation research;

    BlockGroup(@Nullable String research) {
        this.research = research == null ? null : ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, research);
    }

    @Nullable
    public ResourceLocation research() {
        return research;
    }

    public String key() {
        return "quasium.program.group." + name().toLowerCase(Locale.ROOT);
    }
}
