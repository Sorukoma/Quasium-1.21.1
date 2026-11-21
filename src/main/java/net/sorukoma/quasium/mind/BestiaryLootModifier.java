package net.sorukoma.quasium.mind;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.data.PlayerMagicData;
import net.sorukoma.quasium.magic.NoteProfile;
import net.sorukoma.quasium.magic.NoteProfiles;

import java.util.Map;

public class BestiaryLootModifier extends LootModifier {
    public static final MapCodec<BestiaryLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, BestiaryLootModifier::new));

    public BestiaryLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Entity victim = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(victim instanceof Mob mob) || !(context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer hunter)) return loot;
        ResourceLocation table = mob.getLootTable().location();
        if (!table.equals(context.getQueriedLootTableId())) return loot;
        Map.Entry<ResourceLocation, NoteProfile> profile = NoteProfiles.forEntity(mob.getType());
        if (profile == null || !PlayerMagicData.isScanned(hunter, profile.getKey().toString())) return loot;
        loot.addAll(profile.getValue().essenceStacks());
        SmokecraftTrigger.fire(hunter, "bestiary_drop");
        return loot;
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
