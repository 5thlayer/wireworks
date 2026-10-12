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

The Steam Bucket is NeoForge's fluid container model over vanilla's bucket, `items/steam_bucket.json`, its fluid layer `fluid/steam.png` and the whole flipped upside down by `flip_gas`, so it needs no texture of its own. Superheated Steam draws with `fluid/steam.png`, tinted warm in `client/SteamClient.java`, and its liquid block with vanilla's water model.

## From unused-textures

These textures are Malcolm Riley's, from [unused-textures](https://github.com/malcolmriley/unused-textures) at commit `9218cb8997bf1dc83617bd7748acf44a16c19c42`, under [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) (`LICENSES/CC-BY-4.0.txt`), and `REUSE.toml` annotates them. Every one is copied unmodified but the Boiler's front, the Accumulator's sides, the Solar Panel's cell and the Steam Turbine's still fan and coil side, which are adapted. The placeholder script draws none of them.

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
| `block/steam_turbine/coil_side.png` | adapted from `blocks/copper_coil_side.png` | The Steam Turbine's front, back, top and bottom, but the anchor's front and back |
| `block/steam_turbine/coil_end.png` | `blocks/copper_coil_end.png` | The Steam Turbine's two ends, the sides of a row of them |
| `block/steam_turbine/fan_off.png` | adapted from `blocks/industrial_metal_duct_fan.png` | The fan on the Turbine's anchor, front and back, while it is not running |
| `block/steam_turbine/fan_on.png` | `blocks/industrial_metal_duct_fan.png` with its `.mcmeta` | The fan while the Turbine runs (`running`) |
| `block/reactor/steel.png` | `blocks/metal_steel_block.png` | The Reactor's casing, its bottom and top layers and every top and bottom face |
| `block/plating/hazard_stripe.png` | `blocks/space-plating-stripe-yellow-horizontal-undamaged.png` | The sides of the Reactor's middle layer, a band round it |
| `block/reactor/vent.png` | `blocks/industrial_metal_duct_vent.png` | The top of the Reactor's top centre block |
| `item/fuel_cell.png` | `items/container_fuelcan_full.png` | The Fuel Cell |
| `item/depleted_fuel_cell.png` | `items/container_fuelcan_empty.png` | The Depleted Fuel Cell |

`scripts/build-textures.py` builds the adapted textures from unmodified copies of their sources kept under `data/art/`. It needs Pillow, and `--check` fails when a committed one is stale. The Boiler's front, cold and lit, is the smelter's let into the plating, as Craftworks' Refiner front is: the script paints the brick showing round the smelter's rim with the plain plating, on every frame of the lit one's animation, and copies the animation beside the sprite. The Accumulator's sides are the battery block's with its light left edge painted over with the column beside it: the model mirrors the left-hand block of each side, so the yellow bands meet in the middle between the two dark lines, and that edge only ever showed as a seam where the two blocks meet. The Solar Panel's cell is the blue cement framed in the plain plating, its outer ring, with a dark navy line inside the frame, so each block of the 3x3 top reads as a panel of its own. The Steam Turbine's still fan is the first frame of the duct fan's animation, which the running fan wears whole. Its coil side is the copper coil's turned two columns, half its dark band, and its left half mirrored onto its right, so each block has the band's darker half down either edge and the coil's shine in the middle: the halves meet where two blocks do, and the Turbine's three-wide faces are symmetric left to right.

Each texture covers one block's face. The Accumulator's and the Solar Panel's models, `models/block/accumulator.json` and `models/block/solar_panel.json`, draw the whole footprint from the anchor's block model, one element per block with only its outer faces, each face showing the whole 16x16 texture, so nothing is tiled into a larger file. The Solar Panel's spans x and z -16 to 32 and y 0 to 32 from its pillar. The Accumulator's spans x and z 0 to 32 from a corner anchor and is turned by `facing`. Each has an item model recentred for the inventory slot. Both block models turn ambient occlusion off: a face that reaches over a part block samples its shading there, and the part blocks are full blocks to it, so the Solar Panel's top drew near black. The Boiler's model is per block already: the anchor is an `orientable_with_bottom` cube, `boiler.json` with the cold front and `boiler_on.json` with the lit one, which `blockstates/boiler.json` picks by `lit`, and each part block draws itself (`BoilerPartBlock`), plating with the port rings on the water and steam ports, picked by part and facing in `blockstates/boiler_part.json`. Every block of the Boiler gives off light while it is lit: a block that gives off light is drawn at its own light level, so the anchor lit alone would draw brighter than the parts round it.

The Steam Turbine and the Reactor are per block too, each part drawing itself (`DrawnPartBlock`), since a block model reaches only one block past its own and the Reactor's anchor is the bottom centre of a 3x3x3. The Turbine's anchor is `steam_turbine.json` with the still fan front and back and `steam_turbine_on.json` with the spinning one, which `blockstates/steam_turbine.json` picks by `running`; its parts are copper coil, the coil's ends on the row's sides, turned with the facing by `blockstates/steam_turbine_part.json`. A turbine's front and back meet its neighbours' in a row, so a row shows the coil ends along its sides. Only the anchor carries `running`, since the fan is on the anchor alone and nothing gives off light. The Reactor's parts are dark steel, the middle layer banded by the hazard stripe and the top centre vented, and `blockstates/reactor_part.json` picks the model by part number: water ports at parts 2, 4, 5 and 7, the steam port at 16, the back middle of the middle layer, turned with the facing, the band at 9 to 17 and the vent at 22, in the order `FootprintShape.standing(3, 3)` numbers them. Each port wears the Boiler's port ring, a water port's on all four sides, since its other three meet the Reactor's own blocks. Its item is a single block of the band under the vent.

The credit also stands in the README's License section, which the mod pages carry, and in `neoforge.mods.toml`'s `credits`, which the game's mod list shows.
