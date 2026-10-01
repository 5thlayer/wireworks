# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Wireworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

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
