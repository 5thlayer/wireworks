# Placeholder art

The Transmission Pole and the Transformer wear stand-in textures until real art is drawn. They are Wireworks' own, MIT.

`scripts/build-placeholder-textures.py` draws every file below, and `--check` fails when a committed one is stale. Redraw a texture in that script and re-run it; never edit the PNGs.

All files are under `src/main/resources/assets/wireworks/textures/`. A texture is as large as the model face it covers, in pixels to the block unit, so none is stretched.

| Texture | Size | Where it is used |
|---|---|---|
| `block/transmission_pole.png` | 16x16 | The Transmission Pole's post and crossarm, a braced grey plate. |
| `block/transformer.png` | 16x16 | Every face of the Transformer's cube, a grey casing with two gold coil windings. |

## Carried over from FactoryWorks Core

The Boiler and the Steam Engine arrive with art drawn for FactoryWorks Core, 5thlayer's own and unmodified, under `textures/block/`: `steam_engine/steam_engine.png`, the model's texture, `kit/status_light_working.png`, its green light, `fluid_port_input.png` and `fluid_port_output.png`, the rings on the Boiler's port blocks, and `fluid/steam.png` with its animation, which Steam draws tinted a cool grey (`STEAM_TINT` in `client/SteamClient.java`) so a gauge's white label reads over it. They are hand-drawn, so `build-placeholder-textures.py` does not draw them and leaves them alone. The Boiler's body wears unused-textures art, below. The Engine's model was exported from a Blockbench file that stays in the FactoryWorks Showcase. The Engine's `credit` line naming that export is dropped.

## Vanilla stand-ins

The Steam Turbine has no art of its own yet. Its anchor wears vanilla's piston top on its front and copper block on every other face, and each part block draws itself as a plain copper block (`DrawnPartBlock`), so the 3x1x2 shows block by block. The Steam Bucket is NeoForge's fluid container model over vanilla's bucket, `items/steam_bucket.json`, its fluid layer `fluid/steam.png` and the whole flipped upside down by `flip_gas`, so it needs no texture of its own. Superheated Steam draws with `fluid/steam.png`, tinted warm in `client/SteamClient.java`, and its liquid block with vanilla's water model.

The Reactor has no art of its own yet either. A block model reaches only one block past its own and the Reactor's anchor is the bottom centre of a 3x3x3, so until a model is drawn each part block draws itself (`DrawnPartBlock`): vanilla's iron block for the casing, prismarine bricks for the four water ports, and the blast furnace's top on the outer face of the steam port, turned with the facing. `blockstates/reactor_part.json` picks the model by part number, water at parts 2, 4, 5 and 7 and steam at 16, the back middle of the middle layer, in the order `FootprintShape.standing(3, 3)` numbers them. Its item is a blast furnace face on iron. The Fuel Cell wears vanilla's bottle o' enchanting and the Depleted Fuel Cell the glass bottle.

## From unused-textures

These textures are Malcolm Riley's, from [unused-textures](https://github.com/malcolmriley/unused-textures) at commit `9218cb8997bf1dc83617bd7748acf44a16c19c42`, under [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) (`LICENSES/CC-BY-4.0.txt`), and `REUSE.toml` annotates them. Every one is copied unmodified but the Boiler's front, the Accumulator's sides and the Solar Panel's cell, which are adapted. The placeholder script draws none of them.

| Texture | Source file | Where it is used |
|---|---|---|
| `item/bioresin.png` | `items/blob_green_resin_spheres.png` | Bioresin |
| `item/plastic.png` | `items/material_plastic_sheet.png` | Plastic |
| `item/battery.png` | `items/part_electronic_battery_galvanic.png` | The battery |
| `block/accumulator/top.png` | `blocks/battery_top.png` | The Accumulator's top |
| `block/accumulator/sides.png` | adapted from `blocks/battery_sides.png` | The Accumulator's four sides |
| `block/accumulator/bottom.png` | `blocks/battery_bottom.png` | The Accumulator's bottom |
| `block/solar_panel/cell.png` | adapted from `blocks/blue_cement_center.png` and `space-plating-plain.png` | The top of the Solar Panel's 3x3 layer |
| `block/plating/brace.png` | `blocks/space-plating-square.png` | The Solar Panel's pillar |
| `block/plating/rivets.png` | `blocks/space-plating-rivets-horizontal.png` | The sides of the Solar Panel's top layer |
| `block/plating/plain.png` | `blocks/space-plating-plain.png` | The underside of the Solar Panel's top layer; the top and bottom of every Boiler block |
| `block/plating/convex_rivets.png` | `blocks/space-plating-convex-rivets-horizontal.png` | The sides of the Boiler's blocks, under the port rings |
| `block/boiler/front_off.png` | adapted from `blocks/device_smelter_front.png` and `space-plating-plain.png` | The Boiler's firebox, on the anchor's front, while it is not boiling |
| `block/boiler/front_on.png` | adapted from `blocks/device_smelter_front_lit.png` with its `.mcmeta`, `device_smelter_front.png` and `space-plating-plain.png` | The Boiler's firebox while it boils (`lit`) |

`scripts/build-textures.py` builds the adapted textures from unmodified copies of their sources kept under `data/art/`. It needs Pillow, and `--check` fails when a committed one is stale. The Boiler's front, cold and lit, is the smelter's let into the plating, as Craftworks' Refiner front is: the script paints the brick showing round the smelter's rim with the plain plating, on every frame of the lit one's animation, and copies the animation beside the sprite. The Accumulator's sides are the battery block's with its light left edge painted over with the column beside it: the model mirrors the left-hand block of each side, so the yellow bands meet in the middle between the two dark lines, and that edge only ever showed as a seam where the two blocks meet. The Solar Panel's cell is the blue cement framed in the plain plating, its outer ring, with a dark navy line inside the frame, so each block of the 3x3 top reads as a panel of its own.

Each texture covers one block's face. The Accumulator's and the Solar Panel's models, `models/block/accumulator.json` and `models/block/solar_panel.json`, draw the whole footprint from the anchor's block model, one element per block with only its outer faces, each face showing the whole 16x16 texture, so nothing is tiled into a larger file. The Solar Panel's spans x and z -16 to 32 and y 0 to 32 from its pillar. The Accumulator's spans x and z 0 to 32 from a corner anchor and is turned by `facing`. Each has an item model recentred for the inventory slot. Both block models turn ambient occlusion off: a face that reaches over a part block samples its shading there, and the part blocks are full blocks to it, so the Solar Panel's top drew near black. The Boiler's model is per block already: the anchor is an `orientable_with_bottom` cube, `boiler.json` with the cold front and `boiler_on.json` with the lit one, which `blockstates/boiler.json` picks by `lit`, and each part block draws itself (`DrawnPartBlock`), plating with the port rings on the water and steam ports, picked by part and facing in `blockstates/boiler_part.json`.

The credit also stands in the README's License section, which the mod pages carry, and in `neoforge.mods.toml`'s `credits`, which the game's mod list shows.
