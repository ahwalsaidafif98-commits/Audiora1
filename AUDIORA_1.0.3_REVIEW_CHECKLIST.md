# Audiora Java 1.0.3 — Review Checklist

This document records the complete feature request list for the next Audiora Java update. A checklist item is not considered implemented merely because it is listed here.

## Existing code / needs verification
- [x] Sound settings screen has Master and category ON/OFF controls and volume sliders.
- [x] Categories include inventory, player, items, birds, insects, nature, and UI.
- [x] Target HUD currently shows the targeted living entity name, health, and entity preview.
- [x] Pickup notification currently detects inventory count increases and displays item name + amount.
- [x] Critical combo counter exists.
- [ ] Verify all existing features in Minecraft 26.2 on device.

## Sound controls — priority bug fixes
- [x] Add a client-tick enforcement pass to stop registered sounds when Master/category is turned OFF or volume reaches 0.
- [ ] Verify that water/ambient sounds and cricket sounds actually stop when Nature/Crickets are OFF or 0%.
- [ ] Ensure all Audiora ambience playback paths consult the matching category setting.
- [ ] Ensure sounds already playing are stopped immediately when the setting changes.
- [ ] Ensure Enable All / Disable All work on the actual audio playback, not only the UI/config values.
- [ ] Add a Preview button beside each sound/category in settings so a sample can be heard before enabling/disabling it. Preview should still work when that category is OFF and should not change its saved state.

## HUD and information requested
- [ ] Held Item HUD: show the held item's actual icon, display name, durability percentage, and durability bar.
- [ ] Hide durability percentage/bar for items that do not have durability.
- [ ] Coordinate HUD: show X, Y, Z.
- [ ] Armor durability HUD: show durability percentage for equipped armor pieces.
- [ ] Biome HUD: show current biome name.
- [ ] Potion effect sounds: each potion effect should have its own distinct sound when applied to the player.
- [ ] Ore Tracker: track collected ore types and counts; show percentage with a clearly defined denominator (e.g. share of tracked ore total).
- [ ] Target HUD should support both mobs and players.
- [ ] Pickup HUD should show the item icon/name and amount, e.g. “Diamond ×1”.
- [ ] Damage Numbers: show damage dealt above the mob/player that was hit.
- [ ] Critical Hit indicator/number should appear when a critical hit is confirmed.

## HUD Editor requested
- [ ] Add an on-screen Edit HUD mode with direct drag-and-drop positioning; do not require X+/X−/Y+/Y− buttons.
- [ ] Each element must have its own saved position: held item, target mob/player, pickup notification, critical hit/combo, damage numbers, coordinates, armor durability, biome, and ore tracker.
- [ ] Provide visible preview/selection handles while editing.
- [ ] Add Reset Position and Save/Apply controls.
- [ ] Persist positions after leaving and re-entering a world.

## Review notes
- Current HUD position controls in the settings screen are arrow buttons, not the requested drag editor.
- Current pickup notification is text-only; it does not yet render the picked-up item icon.
- Current target HUD does not yet provide damage numbers.
- This checklist is a scope/verification document, not a claim that every feature above is already implemented.
