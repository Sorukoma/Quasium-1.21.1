package net.sorukoma.quasium.item;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.magic.FocusEssence;
import net.sorukoma.quasium.magic.Spell;
import net.sorukoma.quasium.magic.Spells;
import net.sorukoma.quasium.mind.ClientMind;
import net.sorukoma.quasium.screen.HookahBowlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.world.SimpleMenuProvider;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class HookahBowlItem extends Item {

    public HookahBowlItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        if (!pLevel.isClientSide()) {
            ModDataComponents.HookahBowlContents contents = itemstack.getOrDefault(
                    ModDataComponents.HOOKAH_BOWL_CONTENTS.get(),
                    ModDataComponents.HookahBowlContents.cleared()
            );

            pPlayer.openMenu(new SimpleMenuProvider(
                    (pContainerId, pPlayerInventory, pPlayerEntity) ->
                            new HookahBowlMenu(pContainerId, pPlayerInventory, pUsedHand, contents),
                    Component.translatable("container.quasium.hookah_bowl")
            ), (RegistryFriendlyByteBuf buf) -> {
                buf.writeVarInt(pUsedHand.ordinal());
                ModDataComponents.HookahBowlContents.STREAM_CODEC.encode(buf, contents);
            });
        }
        return InteractionResultHolder.success(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        ModDataComponents.HookahBowlContents contents = pStack.get(ModDataComponents.HOOKAH_BOWL_CONTENTS.get());
        if (contents != null && !contents.slotAssignments().isEmpty()) {
            // Show heated indicator BEFORE weight info
            if (contents.heated()) {
                pTooltipComponents.add(Component.translatable("quasium.tooltip.heated_tobacco").withStyle(ChatFormatting.RED));
            }

            int grams = contents.filledSlotCount();
            pTooltipComponents.add(Component.translatable("quasium.tooltip.tobacco_weight", grams).withStyle(ChatFormatting.GRAY));

            // Show compatibility status
            if (!contents.isCompatible()) {
                pTooltipComponents.add(Component.translatable("quasium.tooltip.incompatible_mix").withStyle(ChatFormatting.RED));
            }

            // Show tobacco groups info
            Map<Integer, Integer> groupWeights = contents.getGroupWeights();
            Map<Integer, Integer> groupPercentages = contents.getGroupPercentages();

            for (int i = 0; i < contents.groups().size(); i++) {
                ModDataComponents.BowlTobaccoGroup group = contents.groups().get(i);
                Integer weight = groupWeights.get(i);
                Integer pct = groupPercentages.get(i);
                if (weight == null || pct == null) continue;

                if (group.isRegular()) {
                    pTooltipComponents.add(Component.literal(pct + "% ")
                            .append(Component.translatable("quasium.flavor." + group.flavor()))
                            .withStyle(ChatFormatting.AQUA));
                } else {
                    pTooltipComponents.add(Component.literal(pct + "% ")
                            .append(Component.translatable("quasium.base." + group.base()))
                            .append(Component.literal(" ("))
                            .append(Component.translatable("quasium.tooltip.tobacco_strength_short", String.format("%.1f", group.strength())))
                            .append(Component.literal(")"))
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        } else {
            pTooltipComponents.add(Component.translatable("gui.quasium.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        appendFocus(pStack, pTooltipComponents);
    }

    private static void appendFocus(ItemStack bowl, List<Component> tooltip) {
        List<FocusEssence> focus = Spells.focusOf(bowl);
        if (focus.stream().allMatch(FocusEssence::isEmpty)) return;
        for (FocusEssence entry : focus) {
            if (entry.isEmpty()) continue;
            tooltip.add(Component.translatable("quasium.tooltip.focus_essence",
                    Component.translatable("item.quasium.essence_bottle." + entry.essence()), entry.ml()).withStyle(ChatFormatting.DARK_PURPLE));
        }
        ResourceLocation spell = Spells.forBowl(bowl, ClientMind.knownSpells());
        tooltip.add(spell == null
                ? Component.translatable("quasium.tooltip.focus_no_spell").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("quasium.tooltip.focus_spell", Spell.displayName(spell)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
