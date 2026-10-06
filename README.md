# Enchants II

This mod overhauls the minecraft's enchanting system based on Minecraft Dungeons II.

## Target

- Minecraft: 1.21.1
- Mod loader: NeoForge 21.1.252
- Java: 21
- Mod ID: `enchantsii`
- Base package: `com.redondoguibi.enchantsii`

## Enchanting system

Right-clicking a vanilla enchanting table opens the Enchants II interface instead of the random vanilla enchanting interface.

### Reusable enchanted books

Vanilla enchanted books are reusable enchantment sources.

1. Put the item to enchant in the item slot.
2. Put a vanilla enchanted book in the book slot.
3. Select one enchantment stored in the book.
4. Pay the resource cost.
5. The enchantment is applied or upgraded by one level.
6. The enchanted book stays in the table and is not consumed.

A book only unlocks levels up to the level stored on that book. For example, a Sharpness III book can take an item from no Sharpness to Sharpness I, then II, then III, but no further.

Books containing multiple enchantments are supported. The interface lists their enchantments separately and paginates them when necessary.

### Costs

Every application or upgrade costs a fixed **150 raw XP points**.

The lapis lazuli cost depends on the level being applied:

- Level I: 2 lapis
- Level II: 3 lapis
- Level III: 4 lapis
- Level IV: 5 lapis
- Level V: 6 lapis
- In general: `lapis cost = target enchantment level + 1`

Lapis lazuli is consumed automatically from the player's main inventory and hotbar; it does not need a dedicated table slot.

Creative-mode players do not consume XP or lapis.

### Compatibility

Enchants II respects the vanilla enchantment supported-item rules and vanilla enchantment conflicts.

Bookshelves do not change the cost or available enchantments. Progression comes from obtaining enchanted books and paying XP/lapis.

### Anvils

Anvils can still repair and rename items and can still combine enchanted books with enchanted books.

Applying an enchanted book directly to equipment in an anvil is disabled so that the reusable enchanting-table system cannot be bypassed by consuming the book.

## Status

Core enchanting overhaul implemented.
