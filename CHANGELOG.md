# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Wireworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

### Players

- The Boiler: a 3x2 machine that burns a fuel item for its vanilla burn time and turns water into steam at Factorio's 60 mB/s, 3 mB a tick. Water goes in at either end of its front row, 600 mB in all; steam leaves its back middle, 200 mB. A Boiler with no room for steam makes none and burns no fuel. Right-click it for its fuel slot and gauges. Pipes can put fuel in but never take it out.
- The Steam Engine: a 2x1x2 generator that burns 30 mB/s of steam for 450 FE/t, up to Factorio's 900 kW, from a 200 mB steam tank. A pole draws it once, whichever of its blocks the supply area reaches. Jade names "No steam" and "No pole".
- Steam, a fluid with no bucket. A Boiler standing against an Engine's steam port feeds it with no pipe between.
- The Boiler and the Steam Engine are mined with a pickaxe, and drop nothing from a bare hand.
- Both craft from vanilla materials: the Boiler from a furnace, a bucket and seven iron ingots; the Engine from a piston, an iron block, two copper ingots and five iron ingots.

### Consumers

- `wireworks:steam_engine` is in `wireworks:generators`. Its fluid face is on every block of its footprint, insert-only, and the Boiler's water and steam faces are on the port blocks, through NeoForge's `Capabilities.Fluid.BLOCK`. Wireworks names no pipe mod.
- The Boiler and the Steam Engine ported from FactoryWorks Core, without Oritech or Pipeworks. The Core's `SteamEngineBlock` and `BoilerBlock` ids move from `factoryworks:` to `wireworks:`; a world made with them loses them. (#22)

## 0.5.0

### Players

- Breaking: Wireworks no longer bundles Groundworks and requires it as a separate mod, `[0.5.4,0.6)`. Install Groundworks beside Wireworks; Modrinth and CurseForge list it as a required dependency. (#607)

## 0.4.0

### Players

- The supply area is drawn as a yellow square on the ground the pole stands on, not a box around it. The machines it reaches are still outlined, and the pole still supplies two blocks up and down. (#19)
- The machines a pole reaches are outlined in their role's colour, after the side-configuration colours other tech mods use: generators orange, the machines it feeds blue, and accumulators purple. The square stays yellow.
- A machine built as a Groundworks footprint, such as the Solar Panel, the Accumulator or Craftworks' Assembler, is outlined as one box around the whole machine, not by its main block alone.
- A machine built as a Groundworks footprint by another mod, such as Craftworks' Assembler, is counted once by a pole, however many of its blocks the supply area reaches. It used to be counted once per block, overstating its demand, its Jade count and its outlines. (#20)

### Consumers

- Breaking: `EnergyPartBlock` is gone, and `WireworksRegistries.SOLAR_PANEL_PART` and `ACCUMULATOR_PART` hold plain Groundworks `FootprintPartBlock`s. It was never named API, and neither the Pack nor Craftworks names it. (#20)
- A Groundworks footprint's part stands for its standing origin as an energy owner, after `EnergyOwner` and `EnergyOwnerBlock`, so a Consumer's footprint is counted once by a pole without implementing either. (#20)

## 0.3.0

### Players

- The Transmission Pole: a pole with no tier and no supply area that powers nothing and carries wires 32 blocks by default, stacks as a pole column, and joins only other Transmission Poles. A wire between it and a Distribution Pole is refused, and the refusal says a Transformer joins the two.

- The Transformer: the only block that wires to both a Transmission Pole and a Distribution Pole, and never to another Transformer. Placed between a District and a Transmission Line it joins them into one Electric Network, and energy crosses either way: a District that makes its own power keeps it, and its surplus feeds the other Districts on the line, so a remote solar field exports up the line. Breaking it leaves its District settling on its own. It is one block, not a column, with no supply area and no GUI. Held, it previews like a pole: the translucent block and the wires placing it would make, to the nearest Transmission Pole and the nearest Distribution Pole in reach. Craft it from iron and copper ingots round an iron block.

- Jade names what the line carries: a Transmission Pole shows its Electric Network's surplus or shortfall this tick, and a Transformer shows the power crossing it, imported to its District or exported from it. Several Transformers on one District share its exchange evenly. Both follow the "Pole network" toggle; Distribution Poles' lines are unchanged.

### Consumers

- Breaking: `PoleNetworks.Pole` carries a `PoleKind` (a Distribution tier, the Transmission Pole or the Transformer) in place of its `PoleTier`, so `tier()` is gone, and `PoleNetworks.Wire` carries its `WireSystem`; its two-argument constructor still makes a Distribution wire. `ElectricNetworks.networkOf` lists the poles of the pole's District. (#11)
- The network tick settles each Electric Network in one transaction through the two-level balance, Districts first, and the Transmission Pole and the Transformer report into it like a Distribution Pole. A world with no Transformer settles as before. A Transformer's block entity and a District's poles keep a `NetworkExchange` (what crossed, and the network's surplus and shortfall) for a Jade line; a Transformer's is its share of each District it joins.
- `wireworks-server.toml` gains `[transformer] lineReach` (default 32, the Transmission Pole's) and `districtReach` (default 9, the Medium Pole's), and the Transformer is a new block, `wireworks:transformer`, outside the `wireworks:poles` Fast Replace group.
- `wireworks-server.toml` gains `[transmission] wireReach`, the Transmission Pole's Wire Reach, and the pole is a new block, `wireworks:transmission_pole`, outside the `wireworks:poles` Fast Replace group.
- Wireworks accepts Groundworks `[0.5.4,0.6)`, from the version it nests, where it accepted `[0.5.2,0.6)`.

## 0.2.2

### Players

- Wireworks now ships Groundworks 0.5.4, so a footprint part left behind by a changed machine shape no longer crashes the game when right-clicked, replaced over or aimed at, and Groundworks' item tags have names in EMI.

### Consumers

- Wireworks nests Groundworks 0.5.4; its range stays `[0.5.2,0.6)`.

## 0.2.1

### Players

- **A Solar Panel and an Accumulator.** A game with no Pack has generation and storage on the pole network. The Solar Panel (`wireworks:solar_panel`, a pillar under a 3x3 top layer) makes 30 FE/t at full **Daylight**, ramping down through dusk and up through dawn, and nothing at night or while a block hides the sky above its top layer's centre. The Accumulator (`wireworks:accumulator`, Factorio's flat 2x2) holds 50,000 FE and charges and discharges at 150 FE/t. Each is placed whole from one item, breaks as one and has its own recipe and a place in the Wireworks creative tab. With Jade installed, the panel shows what it is making and the Accumulator whether it is charging, discharging or outside every pole's area. Both wear **stand-in art** until real art is drawn (`docs/placeholder-art.md`). (#8)
- `wireworks-server.toml` gains `[solar_panel] peak_watts = 60000` and `[accumulator] capacity_joules = 5000000`, `max_watts = 300000`, in Factorio's watts and joules at 100 J per FE. (#8)

### Consumers

- `wireworks:solar_panel` and `wireworks:accumulator` are tagged into `wireworks:generators` and `wireworks:accumulators`, so a pack that tags its own blocks there plays alongside them. Their ids, part blocks (`solar_panel_part`, `accumulator_part`) and registry entries are held on `WireworksRegistries`. (#8)
- Wireworks now needs Groundworks 0.5.2 or later (`[0.5.2,0.6)`), the first with footprints. (#8)

## 0.2.0

### Players

- **The poles are named by size.** The Small Pole, Medium Pole, Large Pole (was the Substation) and Creative Pole, with the ids `wireworks:small_pole`, `wireworks:medium_pole`, `wireworks:large_pole` and `wireworks:creative_pole`. There is no remap: a world made with 0.1.0 loses its poles. `wireworks-server.toml`'s `[substation]` section is now `[large]`, and Jade's toggle reads "Pole network". A pack names the poles its own way in its own language file. (#7)
- **A Wireworks creative tab** holds every pole, the creative pole last. The poles stay in Functional Blocks too.
- **Fast Replace between pole tiers.** A pole clicked on a column of another tier swaps the whole column, keeping its wires, for one pole charged and one handed back. The creative pole stays out. (#6)

### Consumers

- Wireworks states a default Replace group, `PoleColumnReplace.DEFAULT_GROUP`, at common setup. A pack that states its own pole group with `PoleColumnReplace.BUILDER` at mod construction still gets its group for the tiers it names. (#6)
- The creative tab is `wireworks:items`, held as `WireworksRegistries.CREATIVE_TAB`.
- `PoleTier.SUBSTATION` is now `PoleTier.LARGE`, `PoleTier.blockName()` gives `<tier>_pole`, and `CreativeSupplyAreaPoleBlock.BLOCK_NAME` is `creative_pole`. (#7)
- `PoleLinks` is now `PoleNetworks`, and `PoleLinks.linked` is `PoleNetworks.withinReach`. The reach-only `networks(List)` is gone, and `PendingEnd.stillHeld` takes the player as a `PendingEnd.Holder`. None of them is named API, and the Pack calls none of them. (#5)

## 0.1.0

### Players

First release on Modrinth and CurseForge.

- **Electric poles.** Small, medium and substation poles, plus a creative pole that generates without limit. A pole powers every Forge Energy machine inside its supply area, whichever way it faces. (#1)
- **Wire a network.** A placed pole wires itself to nearby poles, and a click on two poles with a copper ingot adds or cuts a wire between them, within wire reach. A network shares its generators' output among every machine it reaches, and charges and draws on its accumulators. A player who may not build, in adventure or spectator mode, cannot wire. (#1)
- Poles place, extend and preview through Groundworks, which Wireworks bundles. With Jade installed, a pole shows its network.
- A pack developer sets each tier's supply size and wire reach in `wireworks-server.toml`, Factorio's by default, and tags generators `wireworks:generators`, accumulators `wireworks:accumulators` and wiring items `wireworks:wire_tools` (default: copper ingot).

### Consumers

- A Consumer's generators, accumulators and wiring items join through the tags above, and a multiblock part names its anchor through `EnergyOwner` or `EnergyOwnerBlock`. (#1)
- `PoleColumnReplace.BUILDER` replaces a whole pole column with another tier through Groundworks' Fast Replace, keeping its wires, for one pole charged and one handed back. Wireworks states no Replace group: pass the builder to `FastReplace.group` with the tiers you group. (#3)
