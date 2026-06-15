package net.sorukoma.quasium.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.component.ModDataComponents;
import net.sorukoma.quasium.item.MixedTobaccoItem;
import net.sorukoma.quasium.item.ModItems;
import net.sorukoma.quasium.item.SelfMixedTobaccoItem;
import net.sorukoma.quasium.item.TobaccoItem;
import net.sorukoma.quasium.screen.MixingBowlMenu;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PacketMixTobacco(int percentage1, int percentage2) implements CustomPacketPayload {
    public static final Type<PacketMixTobacco> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, "mix_tobacco"));
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_MIX_WEIGHT_GRAMS = 50;

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketMixTobacco> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.percentage1);
                buf.writeInt(payload.percentage2);
            },
            buf -> new PacketMixTobacco(
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<PacketMixTobacco> type() {
        return TYPE;
    }

    private static boolean isLoadableTobacco(ItemStack stack) {
        return (stack.getItem() instanceof TobaccoItem || stack.getItem() instanceof MixedTobaccoItem || stack.getItem() instanceof SelfMixedTobaccoItem)
                && stack.has(ModDataComponents.TOBACCO_WEIGHT.get())
                && stack.get(ModDataComponents.TOBACCO_WEIGHT.get()) > 0;
    }

    private static Map<String, Integer> getFlavorMap(ItemStack stack) {
        if (stack.getItem() instanceof MixedTobaccoItem) {
            ModDataComponents.TobaccoMixData mixData = stack.get(ModDataComponents.TOBACCO_MIX.get());
            if (mixData != null) {
                return new LinkedHashMap<>(mixData.flavors());
            }
            return new LinkedHashMap<>();
        } else if (stack.getItem() instanceof TobaccoItem tobaccoItem) {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put(tobaccoItem.getFlavor(), 100);
            return map;
        }
        return new LinkedHashMap<>();
    }

    private static List<MobEffectInstance> getEffectsList(ItemStack stack) {
        if (stack.getItem() instanceof SelfMixedTobaccoItem) {
            List<MobEffectInstance> effectsList = stack.get(ModDataComponents.SELF_MIXED_EFFECTS.get());
            if (effectsList != null) {
                return new ArrayList<>(effectsList);
            }
            MobEffectInstance singleEffect = stack.get(ModDataComponents.POTION_EFFECT.get());
            if (singleEffect != null) {
                List<MobEffectInstance> list = new ArrayList<>();
                list.add(singleEffect);
                return list;
            }
        }
        return new ArrayList<>();
    }

    public static void handle(PacketMixTobacco payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() == null) return;
            if (context.player().containerMenu instanceof MixingBowlMenu menu) {
                if (payload.percentage1 < 0 || payload.percentage1 > 100 || payload.percentage2 < 0 || payload.percentage2 > 100) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.percentage_range_error"));
                    return;
                }
                if (payload.percentage1 + payload.percentage2 != 100) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.percentage_sum_error"));
                    return;
                }

                ItemStack stack1 = menu.getItemHandler().getStackInSlot(0);
                ItemStack stack2 = menu.getItemHandler().getStackInSlot(1);

                if (stack1.isEmpty() || stack2.isEmpty() || !isLoadableTobacco(stack1) || !isLoadableTobacco(stack2)) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.invalid_tobacco"));
                    return;
                }

                // Валидация: нельзя смешивать самозамес с обычным табаком
                boolean isSelfMix1 = stack1.getItem() instanceof SelfMixedTobaccoItem;
                boolean isSelfMix2 = stack2.getItem() instanceof SelfMixedTobaccoItem;
                if (isSelfMix1 != isSelfMix2) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.mix_incompatible_types"));
                    return;
                }

                // Валидация: самозамесы можно смешивать только с одинаковой основой
                if (isSelfMix1 && isSelfMix2) {
                    String base1 = stack1.getOrDefault(ModDataComponents.TOBACCO_BASE.get(), "tobacco");
                    String base2 = stack2.getOrDefault(ModDataComponents.TOBACCO_BASE.get(), "tobacco");
                    if (!base1.equals(base2)) {
                        context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.mix_incompatible_base"));
                        return;
                    }
                }

                int weight1 = stack1.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);
                int weight2 = stack2.getOrDefault(ModDataComponents.TOBACCO_WEIGHT.get(), 0);

                if (weight1 == 0 || weight2 == 0) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.empty_tobacco"));
                    return;
                }

                int gramsToTake1 = (int) Math.ceil(weight1 * (payload.percentage1 / 100.0));
                int gramsToTake2 = (int) Math.ceil(weight2 * (payload.percentage2 / 100.0));

                if (gramsToTake1 > weight1 || gramsToTake2 > weight2) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.not_enough_tobacco"));
                    return;
                }

                int totalMixedWeight = gramsToTake1 + gramsToTake2;
                if (totalMixedWeight > MAX_MIX_WEIGHT_GRAMS) {
                    context.player().sendSystemMessage(net.minecraft.network.chat.Component.translatable("quasium.message.mix_weight_limit"));
                    return;
                }

                // === Смешивание самозамесов ===
                if (isSelfMix1 && isSelfMix2) {
                    float strength1 = stack1.getOrDefault(ModDataComponents.TOBACCO_DAMAGE.get(), 0f);
                    float strength2 = stack2.getOrDefault(ModDataComponents.TOBACCO_DAMAGE.get(), 0f);
                    float combinedStrength = (float)(strength1 * payload.percentage1 / 100.0 + strength2 * payload.percentage2 / 100.0);

                    String base = stack1.get(ModDataComponents.TOBACCO_BASE.get());

                    List<MobEffectInstance> combinedEffects = new ArrayList<>();
                    List<MobEffectInstance> effects1 = getEffectsList(stack1);
                    List<MobEffectInstance> effects2 = getEffectsList(stack2);
                    combinedEffects.addAll(effects1);
                    for (MobEffectInstance effect : effects2) {
                        if (combinedEffects.stream().noneMatch(e -> e.getEffect().equals(effect.getEffect()))) {
                            combinedEffects.add(effect);
                        }
                    }

                    ItemStack mixedTobaccoStack = new ItemStack(ModItems.SELF_MIXED_TOBACCO.get());
                    mixedTobaccoStack.set(ModDataComponents.TOBACCO_WEIGHT.get(), totalMixedWeight);
                    mixedTobaccoStack.set(ModDataComponents.TOBACCO_DAMAGE.get(), combinedStrength);
                    mixedTobaccoStack.set(ModDataComponents.TOBACCO_BASE.get(), base);
                    if (combinedEffects.size() == 1) {
                        mixedTobaccoStack.set(ModDataComponents.POTION_EFFECT.get(), combinedEffects.get(0));
                    } else if (combinedEffects.size() > 1) {
                        mixedTobaccoStack.set(ModDataComponents.SELF_MIXED_EFFECTS.get(), combinedEffects);
                    }

                    stack1.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight1 - gramsToTake1);
                    stack2.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight2 - gramsToTake2);

                    if (stack1.get(ModDataComponents.TOBACCO_WEIGHT.get()) <= 0) {
                        menu.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
                    }
                    if (stack2.get(ModDataComponents.TOBACCO_WEIGHT.get()) <= 0) {
                        menu.getItemHandler().setStackInSlot(1, ItemStack.EMPTY);
                    }

                    menu.getItemHandler().setStackInSlot(2, mixedTobaccoStack);
                    menu.broadcastChanges();

                    LOGGER.debug("Mixed self-mixed tobacco: combined strength {}, base {}, {} effects with total weight {}g", combinedStrength, base, combinedEffects.size(), totalMixedWeight);
                }
                // === Смешивание обычного / смешанного табака ===
                else {
                    Map<String, Integer> combinedFlavors = new LinkedHashMap<>();
                    Map<String, Integer> flavors1 = getFlavorMap(stack1);
                    Map<String, Integer> flavors2 = getFlavorMap(stack2);

                    for (Map.Entry<String, Integer> entry : flavors1.entrySet()) {
                        int scaled = (int) Math.round(entry.getValue() * payload.percentage1 / 100.0);
                        combinedFlavors.merge(entry.getKey(), scaled, Integer::sum);
                    }
                    for (Map.Entry<String, Integer> entry : flavors2.entrySet()) {
                        int scaled = (int) Math.round(entry.getValue() * payload.percentage2 / 100.0);
                        combinedFlavors.merge(entry.getKey(), scaled, Integer::sum);
                    }

                    // Ensure percentages sum to exactly 100
                    int totalPercent = combinedFlavors.values().stream().mapToInt(Integer::intValue).sum();
                    if (totalPercent != 100) {
                        String largestKey = combinedFlavors.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(combinedFlavors.keySet().iterator().next());
                        combinedFlavors.put(largestKey, combinedFlavors.get(largestKey) + (100 - totalPercent));
                    }

                    ItemStack mixedTobaccoStack = new ItemStack(ModItems.MIXED_TOBACCO.get());
                    mixedTobaccoStack.set(ModDataComponents.TOBACCO_WEIGHT.get(), totalMixedWeight);
                    mixedTobaccoStack.set(ModDataComponents.TOBACCO_MIX.get(), new ModDataComponents.TobaccoMixData(combinedFlavors));

                    stack1.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight1 - gramsToTake1);
                    stack2.set(ModDataComponents.TOBACCO_WEIGHT.get(), weight2 - gramsToTake2);

                    if (stack1.get(ModDataComponents.TOBACCO_WEIGHT.get()) <= 0) {
                        menu.getItemHandler().setStackInSlot(0, ItemStack.EMPTY);
                    }
                    if (stack2.get(ModDataComponents.TOBACCO_WEIGHT.get()) <= 0) {
                        menu.getItemHandler().setStackInSlot(1, ItemStack.EMPTY);
                    }

                    menu.getItemHandler().setStackInSlot(2, mixedTobaccoStack);
                    menu.broadcastChanges();

                    LOGGER.debug("Mixed tobacco: combined flavors {} with total weight {}g", combinedFlavors, totalMixedWeight);
                }
            } else {
                LOGGER.warn("Player {} tried to mix tobacco without MixingBowlMenu open.", context.player().getName().getString());
            }
        });
    }
}