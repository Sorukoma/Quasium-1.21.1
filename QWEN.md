# QWEN.md — Project Context for Quasium Mod

## Project Overview

**Quasium** is a NeoForge 1.21.1 Minecraft mod that implements a realistic **hookah (калян)** system with:
- Coal heating mechanics (charcoal sizes, heat decay, idle cooling, overheat burn)
- Tobacco bowl system (regular flavors, self-mixed tobaccos with makhorka/tobacco bases)
- "Kaif" (кайф) mechanic — a cumulative effect resource that scales with puff efficiency
- Backpack hookah (Curios back slot) — portable hookah with hotkeys
- Progression book, drying shelves, fermentation, molasses/glycerin/tallow chain
- Quasium armor with dodge chance, custom ores, netherite hammer + anvil crafting

**Key Stats:** NeoForge 21.1.60, Java 21, Minecraft 1.21.1, mod_id=`quasium`, author=Sorukoma

## Build & Run

```bash
# Build
./gradlew build

# Run client
./gradlew runClient

# Run server
./gradlew runServer

# Data generators
./gradlew runData

# Clean + refresh dependencies
./gradlew clean --refresh-dependencies
```

Build output is in `build/libs/`. No automated tests exist (test source set is empty).

## Architecture & Key Packages

| Package | Purpose |
|---------|---------|
| `block/` | Block registrations (Hookah, Heater, DryingShelf, SoakingBowl, FermentationBarrel, QuasiumAnvil, ores, crops) |
| `block/entity/` | BlockEntities — core tick logic lives here (HookahBlockEntity, HeaterBlockEntity, DryingShelfBlockEntity, etc.) |
| `item/` | All items — CharcoalItem (3 sizes), CalloudItem, HookahBowlItem, HookahPipeItem, BackpackHookahItem, KaifometerItem, tobacco items, etc. |
| `component/` | **ModDataComponents.java** — single file containing ALL DataComponent records: CharcoalData, CalloudData, HookahBowlContents, BowlTobaccoGroup, TobaccoMixData, CharcoalSize enum |
| `screen/` | Menu + Screen pairs for all containers (Hookah, Heater, Calloud, HookahBowl, MixingBowl, BackpackHookah) |
| `network/` | ~30 packet classes for client-server sync (puff data, pipe attach/detach, smoke spawn, kaif updates) |
| `data/` | BackpackHookahState (portable hookah tick logic), PlayerKaifData, PlayerTobaccoData, PlayerOreLuckData |
| `client/` | Client-side handlers (input, kaif HUD, renderers, key bindings) |
| `curios/` | Curios API integration for backpack hookah back slot |
| `recipe/` | Custom recipe types: DryingRecipes (drying shelf), AnvilRecipes (hammer + anvil) |
| `datagen/` | Data generators (advancements, recipes, worldgen, loot modifiers) |
| `entity/` | HookahConnectionEntity (pipe visual link), ToxicSmokeEntity |
| `progression/` | Progression book system with dynamic per-player pages |

## Core Mechanics Reference

### Charcoal & Heat System
- **CharcoalSize enum**: MM_22(0.75 maxHeat, 1200 maxTicks, vol=3), MM_25(1.2, 1800, vol=4), MM_27(1.5, 2400, vol=6)
- **CalloudData**: `charcoals` (base list) + `stackedCharcoals` (layered list, 50% heat via STACKED_HEAT_MULTIPLIER)
- **getCurrentHeat()** = `maxHeat * (heatingProgress / 100)` — timeRatio REMOVED, only heatingProgress matters
- **Idle cooling**: `heatingProgress` decreases at rate = `100 / (maxTicks * CALLOUD_IDLE_COOLING_FACTOR)` where factor=5; remainingTicks unchanged during idle
- **Puff decay**: remainingTicks decreases by 1 per tick; heatingProgress stays during puff
- **Calloud volume**: total ≤ 12 (sum of all charcoal volumes, base + stacked)
- **Stacked slots**: CalloudMenu has 8 slots (4 base + 4 stacked); stacked requires base occupied; orphaned stacked auto-returned to player

### Heat Thresholds & Overheat
- REGULAR tobacco threshold: 1.0, SELF_MIXED_TOBACCO: 1.5, MAKHORKA: 2.0
- **Overheat**: totalHeat > threshold × 1.8 → tobacco burns faster (rate = excessHeat × 0.4/tick)
- **Charcoal limit overheat**: extra coals beyond limit → ×3 multiplier per extra coal

### Puff Efficiency & Effects
- `puffEfficiency`: heatRatio ≥ 0.6 → 1.0; below → linear `(ratio/0.6) * 0.8`
- `effectivePuffDuration` = totalPuffDuration × avgPuffEfficiency — scales effect duration
- Kaif accumulation per second = `Config.KAIF_PER_SECOND_SMOKING` × puffEfficiency

### Bowl System
- `HookahBowlContents`: groups + slotAssignments + burnedSlots + heated flag
- `BowlTobaccoGroup`: type (regular/self_mixed), flavor, base, strength, effects
- Compatibility check: all regular OR all self_mixed with same base → compatible → kaif gained
- Incompatible → kaif subtracted, nausea applied

### Backpack Hookah (Curios)
- `BackpackHookahState`: mirrors HookahBlockEntity logic but for wearable item
- Same constants (heat thresholds, cooling factor, overheat)
- State persisted in player NBT under key `QuasiumBackpackHookah`

## DataComponent Pattern

All DataComponents are in **one file** (`ModDataComponents.java`) as Java records with:
- `Codec` via `RecordCodecBuilder` for NBT serialization
- `StreamCodec` for network sync (RegistryFriendlyByteBuf where needed)
- Custom manual encode/decode in `FriendlyByteBuf` for menu open packets (CalloudItem, HookahBowlItem)
- Collections made unmodifiable in record compact constructors

## Development Conventions

- **Language**: Russian comments are common; UI strings use `Component.translatable()` with `quasium.*` keys
- **No tests**: No test suite exists; verify via `./gradlew build` compilation
- **Client-server split**: All game logic runs server-side; client only renders/syncs; `level.isClientSide()` guards everywhere
- **ItemStackHandler**: Used for all container menus; ephemeral handlers created per menu open (not persisted via NBT in menus)
- **Networking**: Custom packets registered via `PayloadRegistrar`; each packet has ID + STREAM_CODEC + handler class
- **Curios dependency**: Runtime + compile dependency for backpack hookah; uses `findFirstCurio()` API
- **JEI dependency**: Compile-only API + localRuntime for testing; no hard runtime dependency

## Important Gotchas

- **HeatingProgress vs remainingTicks**: During idle, only heatingProgress decays (remainingTicks stays). During puff, remainingTicks decays (heatingProgress stays). These are separate axes.
- **CalloudMenu orphaned stacked**: When a base slot is emptied, stacked coal is auto-returned to player inventory via `updateCalloudItem()` to prevent data loss.
- **ItemStackHandler slot bounds**: `ItemStackHandler.deserializeNBT` restores original slot count from saved NBT — hardcoded slot indices can crash if slot count changed after world save.
- **BackpackHookahState double-decrement**: puff tick must NOT also run idle decay — idle decay is inside `if (!isPuffing)` block.
- **puffEfficiency lambda**: Must use `final float` (ternary expression) for lambdas, not mutable local variable.
