# Placeholder art

The Solar Panel, the Accumulator, the Transmission Pole and the Transformer wear stand-in textures until real art is drawn. They are Wireworks' own, MIT. The machines' models draw the whole footprint from the anchor's block model.

`scripts/build-placeholder-textures.py` draws every file below, and `--check` fails when a committed one is stale. Redraw a texture in that script and re-run it; never edit the PNGs.

All files are under `src/main/resources/assets/wireworks/textures/`. A texture is as large as the model face it covers, in pixels to the block unit, so none is stretched.

| Texture | Size | Where it is used |
|---|---|---|
| `block/solar_panel_frame.png` | 16x16 | The Solar Panel's pillar, on every face but the top, which the top layer covers. |
| `block/solar_panel_edge.png` | 48x16 | The sides of the Solar Panel's 3x3 top layer. |
| `block/solar_panel_underside.png` | 48x48 | The underside of the top layer. |
| `block/solar_panel_top.png` | 48x48 | The top layer's dark blue panel face. |
| `block/accumulator_casing.png` | 32x32 | The Accumulator's top and bottom. |
| `block/accumulator_side.png` | 32x16 | The Accumulator's four sides, a battery-cell pattern. |
| `block/transmission_pole.png` | 16x16 | The Transmission Pole's post and crossarm, a braced grey plate. |
| `block/transformer.png` | 16x16 | Every face of the Transformer's cube, a grey casing with two gold coil windings. |

The models are `models/block/solar_panel.json` and `models/block/accumulator.json`, with an item model of each recentred for the inventory slot. The Solar Panel's spans x and z -16 to 32 and y 0 to 32 from its pillar. The Accumulator's spans x and z 0 to 32 from a corner anchor and is turned by `facing`.

## Carried over from FactoryWorks Core

The Boiler and the Steam Engine arrive with art drawn for FactoryWorks Core, 5thlayer's own and unmodified, under `textures/block/`: `steam_engine/steam_engine.png`, the model's texture, `kit/status_light_working.png`, its green light, `fluid_port_input.png` and `fluid_port_output.png`, the rings on the Boiler's port blocks, and `fluid/steam.png` with its animation. They are hand-drawn, so `build-placeholder-textures.py` does not draw them and leaves them alone. The Boiler's body is vanilla's furnace and iron block textures, and its model a stand-in until a 3x2 model is drawn: the anchor is a furnace-faced cube, and each part block draws itself (`DrawnPartBlock`), iron with the port rings on the water and steam ports, picked by part and facing in `blockstates/boiler_part.json`. The Engine's model was exported from a Blockbench file that stays in the FactoryWorks Showcase. The Engine's `credit` line naming that export is dropped.

## Vanilla stand-ins

The Steam Turbine has no art of its own yet. Its anchor wears vanilla's piston top on its front and copper block on every other face, and each part block draws itself as a plain copper block (`DrawnPartBlock`), so the 3x1x2 shows block by block. Superheated Steam draws with `fluid/steam.png`, tinted warm in `client/SteamClient.java`, and its liquid block with vanilla's water model.

The Reactor has no art of its own yet either. A block model reaches only one block past its own and the Reactor's anchor is the bottom centre of a 3x3x3, so until a model is drawn each part block draws itself (`DrawnPartBlock`): vanilla's iron block for the casing, prismarine bricks for the four water ports, and the blast furnace's top on the steam port. `blockstates/reactor_part.json` picks the model by part number, water at parts 2, 4, 5 and 7 and steam at 22, the order `FootprintShape.standing(3, 3)` numbers them. Its item is a blast furnace face on iron. The Fuel Cell wears vanilla's bottle o' enchanting and the Depleted Fuel Cell the glass bottle.

## From unused-textures

Three item textures are Malcolm Riley's, from [unused-textures](https://github.com/malcolmriley/unused-textures), under [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) (`LICENSES/CC-BY-4.0.txt`). Each is copied unmodified, and `REUSE.toml` annotates it. `build-placeholder-textures.py` does not draw them.

| Texture | Source file | Item |
|---|---|---|
| `item/bioresin.png` | `items/blob_green_resin_spheres.png` | Bioresin |
| `item/plastic.png` | `items/material_plastic_sheet.png` | Plastic |
| `item/battery.png` | `items/part_electronic_battery_galvanic.png` | The battery |

The credit also stands in the README's License section, which the mod pages carry, and in `neoforge.mods.toml`'s `credits`, which the game's mod list shows.
