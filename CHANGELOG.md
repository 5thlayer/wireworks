# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Wireworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

### Consumers

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
