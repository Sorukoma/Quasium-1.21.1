package net.sorukoma.quasium.magic;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

public enum NoteTrait implements StringRepresentable {
    FOOD("food", item -> item.components().has(DataComponents.FOOD)),
    FIRE_RESISTANT("fire_resistant", item -> item.components().has(DataComponents.FIRE_RESISTANT)),
    MUSIC_DISC("music_disc", item -> item.components().has(DataComponents.JUKEBOX_PLAYABLE)),
    POTION("potion", item -> item.components().has(DataComponents.POTION_CONTENTS)),
    ENCHANTED("enchanted", item -> item.components().has(DataComponents.STORED_ENCHANTMENTS)),
    ARMOR("armor", item -> item instanceof ArmorItem),
    TOOL("tool", item -> item.components().has(DataComponents.TOOL) || item.components().has(DataComponents.MAX_DAMAGE)),
    SOUND_WOOD("sound_wood", sounds(SoundType.WOOD, SoundType.NETHER_WOOD, SoundType.BAMBOO_WOOD, SoundType.CHERRY_WOOD,
            SoundType.LADDER, SoundType.SCAFFOLDING, SoundType.CHISELED_BOOKSHELF, SoundType.HANGING_SIGN,
            SoundType.NETHER_WOOD_HANGING_SIGN, SoundType.BAMBOO_WOOD_HANGING_SIGN, SoundType.CHERRY_WOOD_HANGING_SIGN, SoundType.STEM)),
    SOUND_STONE("sound_stone", sounds(SoundType.STONE, SoundType.DEEPSLATE, SoundType.DEEPSLATE_BRICKS, SoundType.DEEPSLATE_TILES,
            SoundType.POLISHED_DEEPSLATE, SoundType.TUFF, SoundType.TUFF_BRICKS, SoundType.POLISHED_TUFF, SoundType.CALCITE,
            SoundType.DRIPSTONE_BLOCK, SoundType.POINTED_DRIPSTONE, SoundType.DECORATED_POT, SoundType.DECORATED_POT_CRACKED,
            SoundType.MUD_BRICKS, SoundType.PACKED_MUD, SoundType.LODESTONE)),
    SOUND_METAL("sound_metal", sounds(SoundType.METAL, SoundType.COPPER, SoundType.COPPER_BULB, SoundType.COPPER_GRATE,
            SoundType.ANVIL, SoundType.CHAIN, SoundType.LANTERN, SoundType.NETHERITE_BLOCK, SoundType.HEAVY_CORE,
            SoundType.TRIAL_SPAWNER, SoundType.VAULT)),
    SOUND_GLASS("sound_glass", sounds(SoundType.GLASS, SoundType.AMETHYST, SoundType.AMETHYST_CLUSTER, SoundType.SMALL_AMETHYST_BUD,
            SoundType.MEDIUM_AMETHYST_BUD, SoundType.LARGE_AMETHYST_BUD)),
    SOUND_PLANT("sound_plant", sounds(SoundType.GRASS, SoundType.WET_GRASS, SoundType.CROP, SoundType.HARD_CROP, SoundType.VINE,
            SoundType.CAVE_VINES, SoundType.LILY_PAD, SoundType.SWEET_BERRY_BUSH, SoundType.BAMBOO, SoundType.BAMBOO_SAPLING,
            SoundType.AZALEA, SoundType.FLOWERING_AZALEA, SoundType.AZALEA_LEAVES, SoundType.MOSS, SoundType.MOSS_CARPET,
            SoundType.PINK_PETALS, SoundType.BIG_DRIPLEAF, SoundType.SMALL_DRIPLEAF, SoundType.SPORE_BLOSSOM, SoundType.HANGING_ROOTS,
            SoundType.GLOW_LICHEN, SoundType.CHERRY_SAPLING, SoundType.CHERRY_LEAVES, SoundType.COBWEB)),
    SOUND_EARTH("sound_earth", sounds(SoundType.GRAVEL, SoundType.SAND, SoundType.ROOTED_DIRT, SoundType.MUD,
            SoundType.MANGROVE_ROOTS, SoundType.MUDDY_MANGROVE_ROOTS, SoundType.SUSPICIOUS_SAND, SoundType.SUSPICIOUS_GRAVEL)),
    SOUND_SOFT("sound_soft", sounds(SoundType.WOOL, SoundType.SPONGE, SoundType.WET_SPONGE, SoundType.SLIME_BLOCK,
            SoundType.HONEY_BLOCK, SoundType.FROGSPAWN, SoundType.FROGLIGHT, SoundType.CANDLE)),
    SOUND_COLD("sound_cold", sounds(SoundType.SNOW, SoundType.POWDER_SNOW)),
    SOUND_WATER("sound_water", sounds(SoundType.CORAL_BLOCK)),
    SOUND_NETHER("sound_nether", sounds(SoundType.NETHERRACK, SoundType.NETHER_BRICKS, SoundType.NETHER_ORE, SoundType.NETHER_GOLD_ORE,
            SoundType.SOUL_SAND, SoundType.SOUL_SOIL, SoundType.BASALT, SoundType.NYLIUM, SoundType.FUNGUS, SoundType.ROOTS,
            SoundType.WART_BLOCK, SoundType.NETHER_WART, SoundType.NETHER_SPROUTS, SoundType.WEEPING_VINES, SoundType.TWISTING_VINES,
            SoundType.SHROOMLIGHT, SoundType.GILDED_BLACKSTONE, SoundType.ANCIENT_DEBRIS)),
    SOUND_DECAY("sound_decay", sounds(SoundType.BONE_BLOCK, SoundType.SCULK, SoundType.SCULK_SENSOR, SoundType.SCULK_CATALYST,
            SoundType.SCULK_VEIN, SoundType.SCULK_SHRIEKER)),
    BLOCK("block", item -> item instanceof BlockItem),
    ITEM("item", item -> true),
    FIRE_IMMUNE_MOB("fire_immune_mob", item -> false),
    MOB("mob", item -> false);

    public static final Codec<NoteTrait> CODEC = StringRepresentable.fromEnum(NoteTrait::values);

    private final String id;
    private final Predicate<Item> items;

    NoteTrait(String id, Predicate<Item> items) {
        this.id = id;
        this.items = items;
    }

    public boolean matches(Item item) {
        return items.test(item);
    }

    public boolean matches(EntityType<?> type) {
        return this == MOB || this == FIRE_IMMUNE_MOB && type.fireImmune();
    }

    public boolean forMobs() {
        return this == MOB || this == FIRE_IMMUNE_MOB;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    private static Predicate<Item> sounds(SoundType... types) {
        Set<SoundType> grouped = Set.copyOf(Arrays.asList(types));
        return item -> item instanceof BlockItem block && grouped.contains(block.getBlock().defaultBlockState().getSoundType());
    }
}
