package net.sorukoma.quasium.mind;

import net.minecraft.resources.ResourceLocation;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.mind.dream.DreamOutcome;
import net.sorukoma.quasium.mind.research.Perk;

public final class MindIcons {
    public static final ResourceLocation DREAM_PANEL = sprite("dream/panel");
    public static final ResourceLocation NIGHTMARE = sprite("dream/nightmare");

    private MindIcons() {
    }

    public static ResourceLocation perk(Perk perk) {
        return sprite("perk/" + perk.id());
    }

    public static ResourceLocation title(Titles title) {
        return sprite("title/" + title.id());
    }

    public static ResourceLocation dream(DreamOutcome outcome) {
        return sprite("dream/" + outcome.id());
    }

    private static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mind/" + path);
    }
}
