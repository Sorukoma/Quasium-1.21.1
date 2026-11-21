package net.sorukoma.quasium.magic.page;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.research.Research;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class PageUnlocks {
    public static final String START = "start";
    public static final String FOUND = "found";
    public static final String NOTE_ADVANCEMENTS = "notes/";
    private static final String RESEARCH = "research:";
    private static final String NOTE = "note:";

    private PageUnlocks() {
    }

    public static boolean open(Player player, String unlock) {
        if (START.equals(unlock)) return true;
        if (FOUND.equals(unlock)) return false;
        if (unlock.startsWith(RESEARCH)) {
            ResourceLocation node = ResourceLocation.tryParse(unlock.substring(RESEARCH.length()));
            if (node == null) return false;
            return player.level().isClientSide ? ClientMind.researched(node) : Research.studied(player, node);
        }
        if (unlock.startsWith(NOTE)) return player.level().isClientSide ? ClientMind.flag(unlock) : MindData.flag(player, unlock);
        return false;
    }

    public static String noteUnlock(NoteType note) {
        return NOTE + note.id();
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementProgressEvent event) {
        ResourceLocation id = event.getAdvancement().id();
        if (!id.getNamespace().equals(QuasiumMod.MOD_ID) || !id.getPath().startsWith(NOTE_ADVANCEMENTS)) return;
        if (!event.getAdvancementProgress().isDone()) return;
        NoteType note = NoteType.byId(id.getPath().substring(NOTE_ADVANCEMENTS.length()));
        if (note != null) MindData.raise(event.getEntity(), noteUnlock(note));
    }
}
