# Placeholder art

The Solar Panel, the Accumulator, the Transmission Pole, the Transformer and tar wear stand-in textures until real art is drawn. They are Wireworks' own, MIT, and the models that use them draw the whole footprint from the anchor's block model.

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
| `block/tar.png` | 16x16 | Every face of the tar block, near-black with dark pits and a few glints. |
| `item/tar.png` | 16x16 | The tar item, a black lump on a clear ground. |

The models are `models/block/solar_panel.json` and `models/block/accumulator.json`, with an item model of each recentred for the inventory slot. The Solar Panel's spans x and z -16 to 32 and y 0 to 32 from its pillar. The Accumulator's spans x and z 0 to 32 from a corner anchor and is turned by `facing`.

## Carried over from FactoryWorks Core

The Boiler and the Steam Engine arrive with art drawn for FactoryWorks Core, 5thlayer's own and unmodified, under `textures/block/`: `steam_engine/steam_engine.png`, the model's texture, `kit/status_light_working.png`, its green light, `fluid_port_input.png` and `fluid_port_output.png`, the rings on the Boiler's port blocks, and `fluid/steam.png` with its animation. They are hand-drawn, so `build-placeholder-textures.py` does not draw them and leaves them alone. The Boiler's body is vanilla's furnace and iron block textures, and its model a stand-in until a 3x2 model is drawn. The Engine's model was exported from a Blockbench file that stays in the FactoryWorks Showcase. The Engine's `credit` line naming that export is dropped.
