package net.sorukoma.quasium.smokelands.portal;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.sorukoma.quasium.QuasiumMod;
import net.sorukoma.quasium.advancements.triggers.SmokecraftTrigger;
import net.sorukoma.quasium.smokelands.SmokelandsRegistry;

import java.util.List;

@EventBusSubscriber(modid = QuasiumMod.MOD_ID)
public final class SmokePortals {
    public static final TagKey<Block> FRAMES = TagKey.create(Registries.BLOCK, id("smoke_portal_frames"));
    public static final ResourceLocation ETERNAL_COAL = id("eternal_coal");
    public static final ResourceLocation ACTIVATOR = id("smoke_portal_activator");
    public static final DeferredBlock<SmokePortalBlock> PORTAL = SmokelandsRegistry.BLOCKS.register("smoke_portal",
            () -> new SmokePortalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().strength(-1f)
                    .sound(SoundType.GLASS).lightLevel(state -> 11).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final ResourceKey<PoiType> POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("smoke_portal"));
    public static final DeferredHolder<PoiType, PoiType> POI = SmokelandsRegistry.POI_TYPES.register("smoke_portal",
            () -> new PoiType(ImmutableSet.copyOf(PORTAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    private SmokePortals() {
    }

    public static void load() {
    }

    public static boolean isIgniter(ItemStack stack) {
        return List.of(ETERNAL_COAL, ACTIVATOR).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean light(Level level, BlockPos inside) {
        return SmokePortalShape.find(level, inside).map(shape -> {
            shape.fill(level);
            return true;
        }).orElse(false);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getFace() == null || !isIgniter(stack)) return;
        BlockPos inside = event.getPos().relative(event.getFace());
        Level level = event.getLevel();
        if (SmokePortalShape.find(level, inside).isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || !light(level, inside)) return;
        level.playSound(null, inside, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 0.6f);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SmokecraftTrigger.fire(player, "smoke_portal_lit");
        if (ACTIVATOR.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) && !player.getAbilities().instabuild) stack.shrink(1);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(QuasiumMod.MOD_ID, path);
    }
}
