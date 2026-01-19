# EnchantLimiter — Changelog

Release: 3.0.2 — 2026-01-19

## Summary
This release introduces four crystal items that modify enchantment point capacity, tightens and fixes anvil/enchantment behavior (including partial application of enchanted books to avoid exceeding enchantment-point caps), stabilizes mixins and event handling, introduces shaped recipes for crystals, and improves tooltip and creative tab behavior. The goal is predictable, robust handling of enchantment points and clearer UX.

## Highlights
- New Crystal items: Common, Uncommon, Rare, Legendary (+1 / +3 / +5 / +10 enchantment points).
- Anvil behavior: enchanted books are now partially applied when only limited capacity remains (the rest is discarded).
- New custom creative tab (Enchantment Table icon) with crystal items visible.
- Tooltip improvements: enchant points visible only on relevant items (swords, tools, armor, shield, bow, crossbow, trident).

## Added
- Items
  - `enchantlimiter:common_crystal` — +1 enchantment point, stack 64
  - `enchantlimiter:uncommon_crystal` — +3 enchantment points, stack 64
  - `enchantlimiter:rare_crystal` — +5 enchantment points, stack 64
  - `enchantlimiter:legendary_crystal` — +10 enchantment points, stack 64
- Config
  - New `crystals` section in server TOML: enable toggle, values per crystal, crystal blacklist.

## Fixed
- Amboss accepted unlimited books and allowed over-cap stacking — fixed by:
  - `AnvilEnchantHandler`: trims book-added levels to remaining capacity (partial application).
  - `EnchantmentHelperMixin`: ensures final writes respect caps.
  - `ItemStackMixin`: prevents direct `enchant(...)` calls from writing levels that exceed capacity.
- Tooltip shown on all items — fixed to show only for relevant item types.

## Removed
- Problematic or incorrect Mixin injections that caused build/mapping errors; functionality replaced/covered by event handlers and `EnchantmentHelper` overwrite.

## Application
Partial book application:
    - Sword 0/20 + Book(cost 20) ⇒ applies fully.
    - Sword 0/20 + Book(cost 30) ⇒ result is trimmed to fit 20; extra levels lost.
    - Sword 19/20 + Book(Unbreaking V) ⇒ only 1 point applied (e.g. Unbreaking I).
