package net.sorukoma.quasium.mind.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.mind.MindData;
import net.sorukoma.quasium.mind.MindRegistry;
import net.sorukoma.quasium.progression.ProgressionArticle;
import net.sorukoma.quasium.progression.ResearchNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Research {
    private Research() {
    }

    public static Set<ResourceLocation> knownSpells(Player player) {
        if (player.level().isClientSide) return ClientMind.knownSpells();
        Set<ResourceLocation> spells = new LinkedHashSet<>();
        for (String id : MindData.strings(player, MindData.SPELLS)) {
            ResourceLocation spell = ResourceLocation.tryParse(id);
            if (spell != null) spells.add(spell);
        }
        return spells;
    }

    public static boolean studied(Player player, ResourceLocation node) {
        return player.level().isClientSide ? ClientMind.researched(node) : MindData.has(player, MindData.RESEARCH, node.toString());
    }

    public static boolean learnSpell(ServerPlayer player, ResourceLocation spell) {
        return MindData.add(player, MindData.SPELLS, spell.toString());
    }

    public static Component title(String articleId) {
        ResourceLocation id = ResourceLocation.tryParse(articleId);
        ProgressionArticle article = id == null ? null : articles().get(id);
        return article == null ? Component.literal(articleId) : Component.translatable(article.getTitle());
    }

    public static void chooseSchool(ServerPlayer player, String school) {
        if (!MindView.TEA.equals(school) && !MindView.HOOKAH.equals(school)) return;
        if (!MindData.root(player).getString(MindData.SCHOOL).isEmpty()) return;
        MindData.root(player).putString(MindData.SCHOOL, school);
        MindData.changed(player);
        SmokecraftTrigger.fire(player, "school:" + school);
        player.displayClientMessage(Component.translatable("quasium.research.school_chosen",
                Component.translatable("quasium.research.school." + school)).withStyle(ChatFormatting.GOLD), false);
    }

    public static boolean study(ServerPlayer player, String nodeId) {
        ResourceLocation id = ResourceLocation.tryParse(nodeId);
        ProgressionArticle article = id == null ? null : articles().get(id);
        if (article == null || !article.isResearch()) return false;
        MindView view = MindView.of(player);
        ResearchNode node = article.getResearch();
        if (view.researched(nodeId)) return false;
        if (ResearchRules.mirror(article) && !view.flag(ResearchRules.MIRROR_SEEN)) return false;
        if (!ResearchRules.available(node, recipe -> player.server.getRecipeManager().byKey(recipe).isPresent())) return false;
        if (ResearchRules.needsSchool(node, view)) {
            player.displayClientMessage(Component.translatable("quasium.research.pick_school"), true);
            return false;
        }
        Optional<ResearchRules.Requirement> missing = ResearchRules.requirements(article, view, Research::title).stream()
                .filter(requirement -> !requirement.met()).findFirst();
        if (missing.isPresent()) {
            player.displayClientMessage(Component.translatable("quasium.research.missing", missing.get().text()), true);
            return false;
        }
        pay(player, node, ResearchRules.cost(node, view));
        grant(player, id, article);
        return true;
    }

    private static void pay(ServerPlayer player, ResearchNode node, ResearchRules.Cost cost) {
        if (cost.treatises() > 0) {
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(MindRegistry.TREATISE.get()), cost.treatises(),
                    player.inventoryMenu.getCraftSlots());
        }
        if (cost.insight() > 0) PlayerMagicData.addInsight(player, -cost.insight());
        cost.notes().forEach((note, ml) -> ResearchRules.drainNote(player.getInventory(), note, ml));
        for (String spec : node.items()) {
            ResearchRules.stack(spec).ifPresent(wanted -> player.getInventory().clearOrCountMatchingItems(stack -> stack.is(wanted.getItem()),
                    wanted.getCount(), player.inventoryMenu.getCraftSlots()));
        }
    }

    private static void grant(ServerPlayer player, ResourceLocation id, ProgressionArticle article) {
        ResearchNode node = article.getResearch();
        MindData.add(player, MindData.RESEARCH, id.toString());
        for (String spell : node.spells()) {
            ResourceLocation spellId = ResourceLocation.tryParse(spell);
            if (spellId != null && Spells.get(spellId) != null) {
                learnSpell(player, spellId);
                player.displayClientMessage(Component.translatable("quasium.research.spell", Spell.displayName(spellId))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        if (node.perk() != null && Perk.known(node.perk())) {
            MindData.add(player, MindData.PERKS, node.perk());
            SmokecraftTrigger.fire(player, "perk");
        }
        for (String spec : node.gives()) {
            ResearchRules.stack(spec).ifPresent(stack -> {
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            });
        }
        List<ResourceLocation> recipes = node.recipes().stream().map(ResourceLocation::tryParse)
                .filter(recipe -> recipe != null && player.server.getRecipeManager().byKey(recipe).isPresent()).toList();
        if (!recipes.isEmpty()) player.awardRecipesByKey(recipes);
        player.level().playSound(null, player.blockPosition(), MindRegistry.RESEARCH_COMPLETE.get(), SoundSource.PLAYERS, 0.8f, 1f);
        player.displayClientMessage(Component.translatable("quasium.research.done", Component.translatable(article.getTitle()))
                .withStyle(ChatFormatting.GOLD), false);
        SmokecraftTrigger.fire(player, "research");
        SmokecraftTrigger.fire(player, "research:" + id.getPath());
    }

    private static Map<ResourceLocation, ProgressionArticle> articles() {
        return QuasiumMod.getProgressionArticleManager().getArticles();
    }
}
