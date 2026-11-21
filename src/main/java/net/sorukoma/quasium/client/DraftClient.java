package net.sorukoma.quasium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.draft.DraftBlocks;
import net.sorukoma.quasium.draft.SmokeFilterBlock;
import net.sorukoma.quasium.magic.NoteType;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID, value = Dist.CLIENT)
public final class DraftClient {
    private static final int NO_FILTER_COLOR = 0xE8E8E8;

    private DraftClient() {
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            NoteType note = state.getValue(SmokeFilterBlock.FILTER).note();
            return note == null ? NO_FILTER_COLOR : note.color();
        }, DraftBlocks.NOTE_FILTER.get());
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> NO_FILTER_COLOR, DraftBlocks.NOTE_FILTER_ITEM.get());
    }
}
