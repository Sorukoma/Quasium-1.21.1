package net.sorukoma.quasium.draft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.progression.ConfigArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class DraftCatalog {
    public enum Role { PIPE, FITTING, PRODUCER, CONSUMER, SHOT }

    public record Entry(String id, Role role, IntSupplier draft) {
        public ResourceLocation location() {
            return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, id);
        }

        public Item item() {
            return BuiltInRegistries.ITEM.get(location());
        }

        public List<Component> lines() {
            List<Component> lines = new ArrayList<>();
            int value = draft.getAsInt();
            switch (role) {
                case PIPE -> lines.add(Component.translatable("quasium.draft.info.pipe_rate", value, value / PipeTier.DRAFT_PER_NOTE_ML));
                case PRODUCER -> lines.add(Component.translatable("quasium.draft.info.gives", value));
                case CONSUMER -> lines.add(Component.translatable("quasium.draft.info.asks", value));
                case SHOT -> lines.add(Component.translatable("quasium.draft.info.shot", value));
                case FITTING -> {
                }
            }
            lines.add(Component.translatable("quasium.draft.info." + id));
            return lines;
        }
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("clay_pipe", Role.PIPE, PipeTier.CLAY::draftPerSecond),
            new Entry("copper_pipe", Role.PIPE, PipeTier.COPPER::draftPerSecond),
            new Entry("glass_pipe", Role.PIPE, PipeTier.GLASS::draftPerSecond),
            new Entry("quasium_pipe", Role.PIPE, PipeTier.QUASIUM::draftPerSecond),
            new Entry("smoke_valve", Role.FITTING, () -> 0),
            new Entry("note_filter", Role.FITTING, () -> 0),
            new Entry("smoke_splitter", Role.FITTING, () -> 0),
            new Entry("smoke_sorter", Role.FITTING, () -> 0),
            new Entry("bellows", Role.PRODUCER, () -> ConfigArgs.synced(Config.BELLOWS_PULSE_DRAFT)),
            new Entry("smoke_boiler", Role.PRODUCER, () -> ConfigArgs.synced(Config.BOILER_DRAFT)),
            new Entry("quasium_fan", Role.PRODUCER, () -> ConfigArgs.synced(Config.FAN_DRAFT)),
            new Entry("auto_distiller", Role.CONSUMER, () -> ConfigArgs.synced(Config.DISTILLER_DEMAND)),
            new Entry("essence_pump", Role.CONSUMER, () -> ConfigArgs.synced(Config.PUMP_DEMAND)),
            new Entry("flask_charger", Role.CONSUMER, () -> ConfigArgs.synced(Config.CHARGER_DEMAND)),
            new Entry("ventilation", Role.CONSUMER, () -> ConfigArgs.synced(Config.VENT_DEMAND)),
            new Entry("pneumatic_hammer", Role.CONSUMER, () -> ConfigArgs.synced(Config.HAMMER_DEMAND)),
            new Entry("smoke_cannon", Role.SHOT, () -> ConfigArgs.synced(Config.CANNON_DRAFT_PER_SHOT)));

    private DraftCatalog() {
    }

    public static List<Entry> registered() {
        return ENTRIES.stream().filter(entry -> entry.item() != Items.AIR).toList();
    }

    public static Role roleOf(Block block) {
        Item item = block.asItem();
        return registered().stream().filter(entry -> entry.item() == item).map(Entry::role).findFirst().orElse(Role.FITTING);
    }
}
