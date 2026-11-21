package net.sorukoma.quasium.mind.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.mind.MindIcons;
import net.sorukoma.quasium.mind.MindPackets;
import net.sorukoma.quasium.mind.dream.DreamOutcome;

import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LucidDreamScreen extends MindDialog {
    private static final int MOONLIGHT = 0xD8CEF5;

    public LucidDreamScreen() {
        super(Component.translatable("quasium.dream.lucid.title"));
    }

    @Override
    Component body() {
        return Component.translatable("quasium.dream.lucid.body");
    }

    @Override
    List<Choice> choices() {
        return Arrays.stream(DreamOutcome.values()).filter(outcome -> outcome != DreamOutcome.LUCID)
                .map(outcome -> new Choice(outcome.title(), () -> MindPackets.act("lucid", outcome.id()), MindIcons.dream(outcome)))
                .toList();
    }

    @Override
    ResourceLocation panel() {
        return MindIcons.DREAM_PANEL;
    }

    @Override
    int ink() {
        return MOONLIGHT;
    }
}
