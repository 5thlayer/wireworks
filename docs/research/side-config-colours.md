# Side-config colours for a face that is both input and output

Researched 2026-10-06 against the mods' own source on GitHub. Every hex value comes from a constant
or from the pixels of the committed texture, sampled locally; the links are permalinks to the commit
read.

## Answer

No single colour stands for "both". The mods that colour input blue and output orange show a face
that does both by **putting the two colours together** on the face, half blue and half orange.
Thermal 1.16+ does this with `SIDE_BOTH`, Thermal Expansion 1.12 with Omni, and Ender IO with
Push/Pull. **Purple** for "Input/Output" is real, but it comes from **Mekanism**, where input is
*red* and output *blue*, so purple is the blend of those two. Mekanism's Energy Cube offers it, and
it is the likeliest source of a remembered "purple for both". Old Thermal Expansion (1.7.10–1.12)
also had a purple face, but there it meant *secondary input*, not mixed. Titanium (Industrial
Foregoing) marks its passive two-way mode "Enabled" with a green tick. GregTech and Modern
Industrialization use blue and orange too, but for *fluid* and *item* auto-output, not for input and
output. So under blue = input and orange = output, the convention for a mixed face is a
blue-and-orange two-tone. Where one flat colour is needed, purple is the most recognisable choice,
though it is borrowed from Mekanism's different scheme.

## Table

| Mod | Version read | Input | Output | Mixed / both | Source |
|---|---|---|---|---|---|
| Thermal Series (CoFH Core + Thermal) | 1.20.1 branch (same textures on 1.16.5) | blue `#1EBEE7` | orange `#EB6B00` | `SIDE_BOTH`: blue and orange on the same face (split texture) | [IReconfigurable](https://github.com/CoFH/CoFHCore/blob/a965c3e0f38b54645b96428d03b9e39b01ea92b2/src/main/java/cofh/lib/api/control/IReconfigurable.java#L30-L36), [machine_config_both.png](https://github.com/CoFH/ThermalCore/blob/5892f3303cbedb19064b364e383531d1351d73f2/src/main/resources/assets/thermal/textures/block/config/machine_config_both.png) |
| Thermal Expansion (legacy) | 1.12 and 1.7.10 branches | blue (`INPUT_ALL`) | orange (`OUTPUT_ALL`) | Omni: blue and orange split (1.12 only); Open: plain grey. Purple = *secondary input* | [TETextures](https://github.com/CoFH/ThermalExpansion-1.12-Legacy/blob/92b52710c19f3923f81ece2f580b044d8d8111fc/src/main/java/cofh/thermalexpansion/init/TETextures.java#L296-L306), [SideConfig](https://github.com/CoFH/CoFHCore-1.12-Legacy/blob/3119c11b853a04a5ff8fa76b97199291f6a40699/src/main/java/cofh/core/util/core/SideConfig.java#L47-L55) |
| Mekanism | 1.21.1 (v10.7.19), 1.16.x | `DARK_RED` `#C9071F` | `DARK_BLUE` `#366BD0` | `INPUT_OUTPUT` = `PURPLE` `#A460D9` | [DataType](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/tile/component/config/DataType.java#L22-L31), [EnumColor](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/api/java/mekanism/api/text/EnumColor.java#L28-L45) |
| Mekanism (legacy) | 1.12 branch | items: red; energy and gas IO: `DARK_GREEN` | items: blue; energy and gas IO: `DARK_RED` | none (no mixed option); purple = "Extra" slot | [TileComponentConfig](https://github.com/mekanism/Mekanism/blob/b032fb98a50a751df7ba6a5530c25c803ea11c4b/src/main/java/mekanism/common/tile/component/TileComponentConfig.java#L117-L122) |
| Ender IO | 1.21.1 branch (same textures on 1.12.2) | Pull: blue `#1C7D9D` (block), `#366696` (GUI) | Push: orange-brown `#895F35` (block), `#995739` (GUI) | Push/Pull: blue and orange on one icon; 1.12 lang `§6Push §f/ §bPull` | [IOMode](https://github.com/Team-EnderIO/EnderIO/blob/18c9377a193b220702461ed1d26ccdd3ca1e7590/enderio/src/main/java/com/enderio/enderio/api/io/IOMode.java), [push_pull.png](https://github.com/Team-EnderIO/EnderIO/blob/18c9377a193b220702461ed1d26ccdd3ca1e7590/enderio/src/main/resources/assets/enderio/textures/block/overlay/push_pull.png) |
| Industrial Foregoing (via Titanium) | Titanium 1.21 branch | Pull: blue dot `#1C94F9` | Push: orange ring `#FD840E` (button); `YELLOW` text | Enabled (passive both ways): green tick `#09A718` | [IFacingComponent](https://github.com/InnovativeOnlineIndustries/Titanium/blob/575af0e676c69cbdd31e14a385fc63a3d1e702e0/src/main/java/com/hrznstudio/titanium/component/sideness/IFacingComponent.java#L43-L47) |
| Immersive Engineering | 1.21.1 branch | blue `#0066AF` | orange `#DD6C00` | none (`IOSideConfig` is NONE/INPUT/OUTPUT) | [IEEnums](https://github.com/BluSunrize/ImmersiveEngineering/blob/75a27f03e4243544243567e8d5c38d336f4f10f4/src/api/java/blusunrize/immersiveengineering/api/IEEnums.java#L21-L25) |
| Modern Industrialization | 1.21.x branch | (no input marking) | grey port; auto-extract outline orange `#FF6A00` = *items*, blue `#0088FF` = *fluids* | none | [MachineModelsToGenerate](https://github.com/AztechMC/Modern-Industrialization/blob/550389695fab55c4328c3645188ed4ba37341878/src/main/java/aztech/modern_industrialization/datagen/model/MachineModelsToGenerate.java#L62-L64) |
| GregTech CEu / Modern | CEu master (1.12.2); GTM 1.20.1 | GTM hatches: green `#83CD77` | GTM hatches: red `#D74545`; auto-output orange = items, blue = fluids | none; GTM "Auto Output/Allow Input" button adds a green mark | [GTMachineUtils](https://github.com/GregTechCEu/GregTech-Modern/blob/2787512d27c701ae52d629bfc6cfc16326ab96ce/src/main/java/com/gregtechceu/gtceu/common/data/machines/GTMachineUtils.java#L229) |
| RFTools Power | 1.16 branch | blue `#2F65B6` | yellow `#BBAA1A` | none | [DimensionalCellTileEntity](https://github.com/McJtyMods/RFToolsPower/blob/36981efbdf28f82de1f319a4e6aa25bd9d6ab487/src/main/java/mcjty/rftoolspower/modules/dimensionalcell/blocks/DimensionalCellTileEntity.java#L145-L148) |
| Powah | Technici4n fork, 26.1 branch | no colour | no colour | `ALL`: no colour (`DARK_GRAY` text, as for every mode) | [Transfer](https://github.com/Technici4n/Powah/blob/42742741fdfde80bfee264d0b154d941f976ae05/src/main/java/owmii/powah/lib/logistics/Transfer.java) |
| Oritech | 26.1 branch | — | — | no side-config UI found | — |

## Per-mod notes

### Thermal Series, 1.16 to 1.20 (CoFH Core, Thermal Core and Foundation)

- CoFH Core's
  [`IReconfigurable.SideConfig`](https://github.com/CoFH/CoFHCore/blob/a965c3e0f38b54645b96428d03b9e39b01ea92b2/src/main/java/cofh/lib/api/control/IReconfigurable.java#L30-L36)
  has five states: `SIDE_NONE`, `SIDE_INPUT`, `SIDE_OUTPUT`, `SIDE_BOTH` and `SIDE_ACCESSIBLE`. King
  Lemming's 2018 announcement of the reworked series lists these options as ["In / Out / Both / Open
  /
  None"](https://github.com/CoFH/cofh.github.io/blob/5e59cd12e1490608bdaed059fd1e01d9bcd56748/_posts/2018-11-02-the-new-thermal-series.md?plain=1#L82).
- The colours exist only as block-face textures, which are drawn on the block and in the GUI's
  side-config panel
  ([`ThermalGuiHelper`](https://github.com/CoFH/ThermalCore/blob/5892f3303cbedb19064b364e383531d1351d73f2/src/main/java/cofh/thermal/core/client/gui/ThermalGuiHelper.java#L40),
  lines 126–128 for the energy cell). The PNGs are in
  [`textures/block/config/`](https://github.com/CoFH/ThermalCore/tree/5892f3303cbedb19064b364e383531d1351d73f2/src/main/resources/assets/thermal/textures/block/config).
  Sampled values:
  - `machine_config_input`: blue frame, `#1EBEE7` and `#0A76CF`
  - `machine_config_output`: orange frame, `#EB6B00` and `#CC3000`
  - `machine_config_both`: the frame alternates blue and orange segments, with a blue/orange centre
  - `machine_config_accessible`: grey only
  - `machine_config_none`: fully transparent
- The energy cell's own set (`cell_config_input`, `_output` and `_both`) works the same way: `_both`
  puts blue bars top and bottom and orange bars left and right. This is the closest analogue to a
  Wireworks accumulator.
- The 1.16.5 branch of
  [ThermalFoundation](https://github.com/CoFH/ThermalFoundation/tree/c72e2fabbe9cbb09f83cff5e1e33f8d0039bbff7/src/main/resources/assets/thermal/textures/block/config)
  has pixel-identical `*_config_both` textures.

### Thermal Expansion 1.7.10–1.12 (legacy)

- The side types are `NONE=0`, `INPUT_ALL=1`, `OUTPUT_PRIMARY=2`, `OUTPUT_SECONDARY=3`,
  `OUTPUT_ALL=4`, `INPUT_PRIMARY=5`, `INPUT_SECONDARY=6`, `OPEN=7` and `OMNI=8`
  ([CoFHCore-1.12-Legacy
  `SideConfig`](https://github.com/CoFH/CoFHCore-1.12-Legacy/blob/3119c11b853a04a5ff8fa76b97199291f6a40699/src/main/java/cofh/core/util/core/SideConfig.java#L47-L55)).
- The texture array
  [`TETextures.CONFIG`](https://github.com/CoFH/ThermalExpansion-1.12-Legacy/blob/92b52710c19f3923f81ece2f580b044d8d8111fc/src/main/java/cofh/thermalexpansion/init/TETextures.java#L296-L306)
  is indexed by that type, which gives:
  - None
  - **Blue** = input
  - **Red** = primary output
  - **Yellow** = secondary output
  - **Orange** = all outputs
  - **Green** = primary input
  - **Purple** = secondary input
  - Open = dark frame, no colour
  - **Omni** = blue/orange split
- 1.7.10 uses the same order.
  [`BlockMachine`](https://github.com/CoFH/ThermalExpansion-1.12-Legacy/blob/5f673af70771b39f60d685c30c44cbd84c2daa2b/src/main/java/cofh/thermalexpansion/block/machine/BlockMachine.java#L202-L207)
  registers Blue, Red, Yellow, Orange, Green and Purple. The Induction Smelter's
  [`slotGroups`](https://github.com/CoFH/ThermalExpansion-1.12-Legacy/blob/5f673af70771b39f60d685c30c44cbd84c2daa2b/src/main/java/cofh/thermalexpansion/block/machine/TileSmelter.java#L27)
  maps green to slot 0 (the primary input) and purple to slot 1 (the secondary input). 1.7.10 has no
  Omni texture; its only all-ways face is the colourless Open.
- Sampled hues:

  | Face | Colour |
  |---|---|
  | blue | `#0A76CF` |
  | red | `#CE230A` |
  | yellow | `#CEA50A` |
  | orange | `#CE640B` |
  | green | `#089D4B` |
  | purple | `#5709B5` |

- **This is the likely source of the "purple" memory, but in Thermal purple never meant mixed.**
- 1.12 energy cells cycle only none, blue (input) and orange (output). A `cell_config_omni.png`
  (blue/orange) is committed but nothing in the code uses it.

### Mekanism

- 1.16 and later:
  [`DataType`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/tile/component/config/DataType.java#L22-L31)
  maps each data type to a colour:

  | `DataType` | `EnumColor` |
  |---|---|
  | `NONE` | `GRAY` |
  | `INPUT`, `INPUT_1` | `DARK_RED` |
  | `INPUT_2` | `ORANGE` |
  | `OUTPUT`, `OUTPUT_1` | `DARK_BLUE` |
  | `OUTPUT_2` | `DARK_AQUA` |
  | **`INPUT_OUTPUT`** | **`PURPLE`** |
  | `ENERGY` | `DARK_GREEN` |
  | `EXTRA` | `YELLOW` |

  The 1.16.x branch
  ([160d59e](https://github.com/mekanism/Mekanism/blob/160d59e8d4b11aec446fc4d7d84b9f01dba5da68/src/main/java/mekanism/common/tile/component/config/DataType.java#L14-L23))
  is identical.
- RGB values from
  [`EnumColor`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/api/java/mekanism/api/text/EnumColor.java#L28-L45):

  | Colour | Hex |
  |---|---|
  | `DARK_RED` | `#C9071F` |
  | `DARK_BLUE` | `#366BD0` |
  | `PURPLE` | `#A460D9` |
  | `ORANGE` | `#FFA160` |
  | `DARK_GREEN` | `#59C15F` |

- The lang label is `"side_data.mekanism.input_output": "Input/Output"`.
- [`TileComponentConfig.setupIOConfig`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/tile/component/TileComponentConfig.java#L187-L194)
  gives INPUT, OUTPUT and INPUT_OUTPUT to every IO config. The Energy Cube calls it for
  `TransmissionType.ENERGY`, so its energy faces cycle through red, blue and purple.
- The same colour is drawn in the world at 60% alpha while a player holds the Configurator
  ([`RenderTickHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/render/RenderTickHandler.java#L491)).
- 1.12: item configs use `DARK_RED` for input, `DARK_BLUE` for output, `DARK_GREEN` for energy and
  `PURPLE` for "Extra". The energy and gas IO config
  ([`setIOConfig`](https://github.com/mekanism/Mekanism/blob/b032fb98a50a751df7ba6a5530c25c803ea11c4b/src/main/java/mekanism/common/tile/component/TileComponentConfig.java#L117-L122))
  used green for input and red for output, with **no mixed option**. Input/Output purple arrived
  with the 1.15/1.16 rewrite.
  - Not verified: exactly which version between 1.12 and 1.16 introduced it. Only these two branches
    were read.

### Ender IO

- [`IOMode`](https://github.com/Team-EnderIO/EnderIO/blob/18c9377a193b220702461ed1d26ccdd3ca1e7590/enderio/src/main/java/com/enderio/enderio/api/io/IOMode.java)
  has five modes: `NONE`, `PUSH`, `PULL`, `BOTH` and `DISABLED`.
- In-world overlays are in
  [`textures/block/overlay/`](https://github.com/Team-EnderIO/EnderIO/tree/18c9377a193b220702461ed1d26ccdd3ca1e7590/enderio/src/main/resources/assets/enderio/textures/block/overlay):
  - `pull`: teal-blue arrows pointing in, `#1C7D9D`
  - `push`: orange-brown arrows pointing out, `#895F35`
  - `push_pull`: blue arrows top and bottom, orange arrows left and right
  - `disabled`: dark red corners, `#673739`
- The GUI icon comes from
  [`IOModeMap`](https://github.com/Team-EnderIO/EnderIO/blob/18c9377a193b220702461ed1d26ccdd3ca1e7590/enderio/src/main/java/com/enderio/enderio/client/foundation/widgets/ioconfig/IOModeMap.java),
  which blits from `io_config_overlay.png`:
  - `PULL` = rect (0,0,16,8), a blue up-arrow, `#366696`
  - `PUSH` = rect (16,0,16,8), an orange down-arrow, `#995739`
  - **`BOTH` = rect (0,0,32,8): the two arrows side by side**
  - `DISABLED` = a dark red square, `#5A4142`
- 1.12.2
  ([7ae0e69](https://github.com/SleepyTrousers/EnderIO/tree/7ae0e6945e5f5dd69ca71d81eed990740e7aa97c/enderio-base/src/main/resources/assets/enderio/textures/blocks/overlays))
  has pixel-identical overlays. Its
  [lang](https://github.com/SleepyTrousers/EnderIO/blob/7ae0e6945e5f5dd69ca71d81eed990740e7aa97c/enderio-base/src/main/resources/assets/enderio/lang/en_us.lang#L1212-L1216)
  colours the names:
  - `pull.colored=§bPull` (aqua)
  - `push.colored=§6Push` (gold)
  - `pullPush.colored=§6Push §f/ §bPull`: two colours, not a third
  - `disabled.colored=§cDisabled` (red)

### Industrial Foregoing (Titanium library)

- Titanium's
  [`IFacingComponent.FaceMode`](https://github.com/InnovativeOnlineIndustries/Titanium/blob/575af0e676c69cbdd31e14a385fc63a3d1e702e0/src/main/java/com/hrznstudio/titanium/component/sideness/IFacingComponent.java#L43-L47)
  defines each mode with a text colour and a button sprite. The sprites are drawn in
  [`DefaultAssetProvider`](https://github.com/InnovativeOnlineIndustries/Titanium/blob/575af0e676c69cbdd31e14a385fc63a3d1e702e0/src/main/java/com/hrznstudio/titanium/client/screen/asset/DefaultAssetProvider.java#L99-L102)
  from `textures/gui/background.png`:

  | Mode | Lang label | Text colour | Button sprite |
  |---|---|---|---|
  | `NONE` | "Disabled" | `RED` | red X, `#C83131` |
  | `ENABLED` | "Enabled" | `GREEN` | green tick, `#09A718` |
  | `PUSH` | "Push" | `YELLOW` | orange ring, `#FD840E` |
  | `PULL` | "Pull" | `BLUE` | blue dot, `#1C94F9` |

- "Enabled" is the passive both-ways mode, so here mixed is **green**.
- Industrial Foregoing also gives each inventory or tank its own `DyeColor`, set per machine with
  `setColor(...)` on the component. Outputs are mostly `ORANGE` and item inputs often
  `BLUE`/`LIGHT_BLUE`, but fluids take a colour of their own (`LIME`, `MAGENTA`, …). That colour
  names the inventory, not its direction. See e.g.
  [`DissolutionChamberTile`](https://github.com/InnovativeOnlineIndustries/Industrial-Foregoing/blob/ce2852db4b34af3ab1b9637cff0b327d4bb561d5/src/main/java/com/buuz135/industrial/block/core/tile/DissolutionChamberTile.java#L71-L91).

### Immersive Engineering

- [`IEEnums.IOSideConfig`](https://github.com/BluSunrize/ImmersiveEngineering/blob/75a27f03e4243544243567e8d5c38d336f4f10f4/src/api/java/blusunrize/immersiveengineering/api/IEEnums.java#L21-L25)
  has only `NONE("none")`, `INPUT("in")` and `OUTPUT("out")`, toggled with the hammer. There is no
  mixed state.
- The capacitor textures are `capacitor_*_{side,up,down}_{in,out,none}.png` in
  [`textures/block/metal_device/`](https://github.com/BluSunrize/ImmersiveEngineering/tree/75a27f03e4243544243567e8d5c38d336f4f10f4/src/main/resources/assets/immersiveengineering/textures/block/metal_device).
  Input faces add blue marks (`#0066AF`) and output faces orange marks (`#DD6C00`).

### Modern Industrialization

- There is no input/output colour coding. The output face gets a dark port overlay, chosen by
  resource: `output_item` is brownish, `output_fluid` bluish-grey and `output_energy` reddish.
- Auto-extract adds an outline:
  [`item_auto.png`](https://github.com/AztechMC/Modern-Industrialization/blob/550389695fab55c4328c3645188ed4ba37341878/src/main/resources/assets/modern_industrialization/textures/block/overlays/item_auto.png)
  is orange `#FF6A00` and
  [`fluid_auto.png`](https://github.com/AztechMC/Modern-Industrialization/blob/550389695fab55c4328c3645188ed4ba37341878/src/main/resources/assets/modern_industrialization/textures/block/overlays/fluid_auto.png)
  is blue `#0088FF`. These are registered in
  [`MachineModelsToGenerate`](https://github.com/AztechMC/Modern-Industrialization/blob/550389695fab55c4328c3645188ed4ba37341878/src/main/java/aztech/modern_industrialization/datagen/model/MachineModelsToGenerate.java#L62-L64).
- So here **orange and blue mean item and fluid**, both on the output face. This clashes with the
  blue-in/orange-out reading.

### GregTech CEu and GregTech Modern

- GT CEu (1.12.2):
  [`overlay_item_output.png`](https://github.com/GregTechCEu/GregTech/blob/52c04cd7bf03e5749a653580c12ef5856cc12e31/src/main/resources/assets/gregtech/textures/blocks/overlay/machine/overlay_item_output.png)
  is orange `#FF6A00` and
  [`overlay_fluid_output.png`](https://github.com/GregTechCEu/GregTech/blob/52c04cd7bf03e5749a653580c12ef5856cc12e31/src/main/resources/assets/gregtech/textures/blocks/overlay/machine/overlay_fluid_output.png)
  blue `#0088FF`. These are the same pixels as Modern Industrialization's, and again mean items and
  fluids. `overlay_pipe_in`/`_out` and `overlay_energy_in`/`_out` are uncoloured.
- GTM (1.20.1):
  - Item and fluid auto-output use orange `#FF651D` and cyan `#1DB7FF`
    ([`MachineModel`](https://github.com/GregTechCEu/GregTech-Modern/blob/2787512d27c701ae52d629bfc6cfc16326ab96ce/src/main/java/com/gregtechceu/gtceu/client/model/machine/MachineModel.java#L66-L67)).
  - Hatches use emissive **green for input** (`#83CD77`) and **red for output** (`#D74545`), via
    [`GTMachineUtils`](https://github.com/GregTechCEu/GregTech-Modern/blob/2787512d27c701ae52d629bfc6cfc16326ab96ce/src/main/java/com/gregtechceu/gtceu/common/data/machines/GTMachineUtils.java#L229).
  - The output-side GUI toggle has an "Auto Output/Allow Input" state (lang `§2…`, dark green). Its
    button sprite adds an orange mark to the green one. This is a toggle, not a face colour.
- Neither has a mixed face colour.

### Create, AE2, Refined Storage

- None of the three has a machine side-config screen with direction colours. Create shows direction
  with arrows and goggles, AE2 and Refined Storage with separate import and export parts.
- Not verified in source: this is from general knowledge, and their repositories were not searched.

### Others checked

- **RFTools Power (1.16)**: the dimensional cell has `MODE_INPUT` and `MODE_OUTPUT`, commented
  "Blue" and "Yellow" in
  [source](https://github.com/McJtyMods/RFToolsPower/blob/36981efbdf28f82de1f319a4e6aa25bd9d6ab487/src/main/java/mcjty/rftoolspower/modules/dimensionalcell/blocks/DimensionalCellTileEntity.java#L145-L148).
  The power cell's `inputmask.png` is `#2F65B6` and its `outputmask.png` `#BBAA1A`. There is no both
  mode.
- **Powah**:
  [`Transfer`](https://github.com/Technici4n/Powah/blob/42742741fdfde80bfee264d0b154d941f976ae05/src/main/java/owmii/powah/lib/logistics/Transfer.java)
  has `ALL`, `EXTRACT`, `RECEIVE` and `NONE`. Every mode but `NONE` is `DARK_GRAY` text, and the
  cable end textures (`energy_cable_*_{all,in,out}.png`) have no saturated colour. Powah tells the
  modes apart by shape.
- **PneumaticCraft: Repressurized**:
  [`SideConfigurator`](https://github.com/TeamPneumatic/pnc-repressurized/blob/93d04cc52714742c4ef747eed82528d0fdbde581/src/main/java/me/desht/pneumaticcraft/common/block/entity/SideConfigurator.java)
  assigns a capability (items, fluids, …) to each face, not a direction, so it has no I/O colours.
- **Cyclic**: no side-config enum found.
- **Oritech**: no side-config UI. Energy storage has a fixed output face, computed from geometry
  ([`ExpandableEnergyStorageBlockEntity.getOutputPosition`](https://github.com/Rearth/Oritech/blob/a88166ca3f6e0b2895709754f5a0607c5f510482/src/main/java/rearth/oritech/block/base/entity/ExpandableEnergyStorageBlockEntity.java#L172)).
  No colour coding was found in code or lang. The GeckoLib model textures were not inspected.

## Implications for Wireworks

Wireworks plans orange for generators (output) and blue for the machines a pole feeds (input).
Thermal, Ender IO and Immersive Engineering use the same scheme, and Thermal and Ender IO both draw
a both-ways face with **both colours at once**. For an accumulator's outline, that means alternating
blue and orange segments along the edge, or blue on two opposite edges and orange on the other two,
the way Thermal's `cell_config_both` does. A player who knows either mod will read it without a
legend. If the line renderer makes a two-tone outline awkward, the next best is **purple**, around
Mekanism's `#A460D9`. It is the one flat "Input/Output" colour a large mod uses. No Wireworks colour
is purple yet: the supply area square is yellow `0xFFFFE04C`, and the slack tints are brown `HELD`,
green `ACCEPTED` (`#33CC33`), orange `CUT` (`#FF8C00`) and red `REFUSED` (`#CC1A1A`). Avoid green,
which Titanium uses for "both" but Wireworks already uses for "wire". Note also that the slack's
`CUT` orange (`1.0, 0.55, 0.0` in `PoleWireRenderer.Tint`) is almost the output orange. The two are
not seen together, since slack shows over a pole and the outlines show on the ground, but choosing a
slightly different output orange (Thermal uses `#EB6B00`) would keep "cut" and "output" apart.
