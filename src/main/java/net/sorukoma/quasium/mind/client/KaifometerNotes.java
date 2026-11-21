package net.sorukoma.quasium.mind.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sorukoma.quasium.client.ClientMagicData;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class KaifometerNotes {
    private static final int ROW = 22;
    private static final int TEXT_X = 20;

    private KaifometerNotes() {
    }

    static void render(KaifometerPages page) {
        page.text(Component.translatable("quasium.kaifometer.notes.count", ClientMagicData.scannedCount()), KaifometerPages.LABEL);
        page.skip(2);
        boolean any = false;
        for (Map.Entry<ResourceLocation, NoteProfile> entry : NoteProfiles.known().entrySet()) {
            if (!ClientMagicData.isScanned(entry.getKey().toString())) continue;
            any = true;
            row(page, entry.getValue());
        }
        if (!any) page.text(Component.translatable("quasium.kaifometer.notes.empty"), 0x808080);
    }

    private static void row(KaifometerPages page, NoteProfile profile) {
        EntityType<?> mob = profile.entity().orElse(null);
        ItemStack icon = mob != null ? spawnEgg(mob) : profile.sourceStacks().stream().findFirst().orElse(ItemStack.EMPTY);
        Component name = mob != null ? mob.getDescription() : icon.isEmpty() ? Component.literal("?") : icon.getHoverName();
        int top = page.y();
        if (!icon.isEmpty()) page.graphics().renderFakeItem(icon, page.left(), top + 2);
        String shown = page.font().plainSubstrByWidth(name.getString(), page.width() - TEXT_X);
        page.graphics().drawString(page.font(), shown, page.left() + TEXT_X, top, KaifometerPages.LABEL, false);
        Component notes = mob != null ? Component.translatable("quasium.kaifometer.notes.drop", profile.describe()) : profile.describe();
        page.font().split(notes, page.width() - TEXT_X).stream().findFirst()
                .ifPresent(line -> page.graphics().drawString(page.font(), line, page.left() + TEXT_X, top + 10, 0x606060, false));
        page.skip(ROW);
    }

    private static ItemStack spawnEgg(EntityType<?> mob) {
        SpawnEggItem egg = SpawnEggItem.byId(mob);
        return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
    }
}
