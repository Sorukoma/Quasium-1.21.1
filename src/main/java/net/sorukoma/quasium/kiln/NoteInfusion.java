package net.sorukoma.quasium.kiln;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.GearDurability;
import net.sorukoma.quasium.magic.NoteType;
import net.sorukoma.quasium.progression.ConfigArgs;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class NoteInfusion {
    private NoteInfusion() {
    }

    @Nullable
    public static NoteType of(ItemStack stack) {
        String id = stack.get(ModDataComponents.NOTE_INFUSION.get());
        return id == null ? null : NoteType.byId(id);
    }

    public static boolean infusable(ItemStack stack) {
        return stack.getMaxDamage() > 0;
    }

    public static ItemStack infused(ItemStack tool, NoteType note) {
        ItemStack result = tool.copy();
        result.set(ModDataComponents.NOTE_INFUSION.get(), note.id());
        GearDurability.refresh(result);
        return result;
    }

    public static MutableComponent effect(NoteType note) {
        Object[] numbers = switch (note) {
            case EMBER -> new Object[]{ConfigArgs.synced(Config.INFUSION_EMBER_FIRE_SECONDS), ConfigArgs.synced(Config.INFUSION_EMBER_DROP_PCT)};
            case RESIN -> new Object[]{ConfigArgs.synced(Config.INFUSION_RESIN_DURABILITY_PCT)};
            case DEW -> new Object[]{ConfigArgs.synced(Config.INFUSION_DEW_EXTRA_DROPS)};
            case ASH -> new Object[]{ConfigArgs.synced(Config.INFUSION_ASH_FINISH_PCT)};
            case MENTHOL, NECTAR -> new Object[0];
        };
        return Component.translatable("quasium.infusion." + note.id(), numbers);
    }

    public static ItemStack exampleTool(NoteType note) {
        return new ItemStack(switch (note) {
            case EMBER, ASH -> Items.IRON_SWORD;
            case MENTHOL, RESIN -> Items.IRON_PICKAXE;
            case NECTAR -> Items.IRON_HOE;
            case DEW -> Items.SHEARS;
        });
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        NoteType note = of(event.getItemStack());
        if (note == null) return;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(Math.min(1, tooltip.size()), Component.translatable("quasium.infusion.tooltip", note.displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Math.min(2, tooltip.size()), effect(note).withStyle(ChatFormatting.GRAY));
    }
}
