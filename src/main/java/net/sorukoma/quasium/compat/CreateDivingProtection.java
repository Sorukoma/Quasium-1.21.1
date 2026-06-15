package net.sorukoma.quasium.compat;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Soft dependency integration with Create mod's diving gear.
 * <p>
 * When toxic smoke (drown damage) hits a target wearing Create's diving helmet + backtank,
 * air is consumed from the backtank instead of dealing damage.
 * <p>
 * No hard dependency — all checks use registry names and DataComponent lookups.
 * If Create is not installed, all methods return the original damage (no protection).
 */
public class CreateDivingProtection {

    // Create mod registry names (note: Create has a typo — "banktank" not "backtank")
    private static final ResourceLocation RL_COPPER_DIVING_HELMET =
            ResourceLocation.fromNamespaceAndPath("create", "copper_diving_helmet");
    private static final ResourceLocation RL_NETHERITE_DIVING_HELMET =
            ResourceLocation.fromNamespaceAndPath("create", "netherite_diving_helmet");
    private static final ResourceLocation RL_COPPER_BACKTANK =
            ResourceLocation.fromNamespaceAndPath("create", "copper_backtank");
    private static final ResourceLocation RL_NETHERITE_BACKTANK =
            ResourceLocation.fromNamespaceAndPath("create", "netherite_backtank");
    private static final ResourceLocation RL_BACKTANK_AIR =
            ResourceLocation.fromNamespaceAndPath("create", "banktank_air");

    // 8 damage (max strength 1.0) = full vanilla air supply (300). Not the entire backtank (900).
    // This means: 1 damage = 37.5 backtank air consumed (300 / 8).
    private static final int VANILLA_MAX_AIR = 300;
    private static final float AIR_PER_DAMAGE = VANILLA_MAX_AIR / 8.0f; // 37.5

    private static DataComponentType<Integer> cachedAirComponent = null;
    private static boolean airComponentLookedUp = false;

    /**
     * Try to absorb drown damage using Create's diving gear.
     * <p>
     * If the target has a diving helmet (head) + backtank with air (chest/Curios),
     * consumes backtank air proportional to damage and returns remaining unabsorbed damage.
     * <p>
     * Conversion: 8 damage (strength 1.0) = full vanilla air supply (300 air).
     * The backtank is drained proportionally (1/3 of a 900-air default tank for max damage).
     *
     * @return remaining damage (0 = fully absorbed by backtank air)
     */
    public static float absorbDamage(LivingEntity entity, float damage) {
        if (damage <= 0) return 0;
        if (!hasDivingHelmet(entity)) return damage;

        ItemStack backtank = findBacktank(entity);
        if (backtank.isEmpty()) return damage;

        DataComponentType<Integer> airComponent = getBacktankAirComponent();
        if (airComponent == null) return damage; // Create not installed

        int currentAir = backtank.getOrDefault(airComponent, 0);
        if (currentAir <= 0) return damage; // Backtank empty

        int airToConsume = Math.round(damage * AIR_PER_DAMAGE);
        int actualConsumed = Math.min(airToConsume, currentAir);

        backtank.set(airComponent, currentAir - actualConsumed);

        // Proportional remaining damage
        float absorbedFraction = (float) actualConsumed / airToConsume;
        return damage * (1.0f - absorbedFraction);
    }

    private static boolean hasDivingHelmet(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return false;
        ResourceLocation id = helmet.getItem().builtInRegistryHolder().key().location();
        return id.equals(RL_COPPER_DIVING_HELMET) || id.equals(RL_NETHERITE_DIVING_HELMET);
    }

    private static ItemStack findBacktank(LivingEntity entity) {
        // Primary: chest armor slot (Create's backtank is tagged as chest armor)
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && isBacktank(chest)) return chest;

        // Fallback: Curios slots (if entity is a Player and Curios is available)
        if (entity instanceof Player player) {
            try {
                var handlerOpt = CuriosApi.getCuriosInventory(player);
                if (handlerOpt.isPresent()) {
                    var resultOpt = handlerOpt.get().findFirstCurio(CreateDivingProtection::isBacktank);
                    if (resultOpt.isPresent()) return resultOpt.get().stack();
                }
            } catch (Exception ignored) {
                // Curios might not be fully initialized
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isBacktank(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        return id.equals(RL_COPPER_BACKTANK) || id.equals(RL_NETHERITE_BACKTANK);
    }

    /**
     * Lazy lookup of Create's BACKTANK_AIR DataComponentType from the global registry.
     * Returns null if Create is not installed (component not registered).
     */
    private static DataComponentType<Integer> getBacktankAirComponent() {
        if (!airComponentLookedUp) {
            airComponentLookedUp = true;
            try {
                DataComponentType<?> raw = BuiltInRegistries.DATA_COMPONENT_TYPE.get(RL_BACKTANK_AIR);
                if (raw != null) {
                    // Safe unchecked cast: Create registers BACKTANK_AIR as Integer
                    @SuppressWarnings("unchecked")
                    DataComponentType<Integer> typed = (DataComponentType<Integer>) raw;
                    cachedAirComponent = typed;
                }
            } catch (Exception ignored) {
                // Create not installed or registry access issue
            }
        }
        return cachedAirComponent;
    }
}