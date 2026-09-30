# Changelog

Written for Consumers: what a mod building against Wireworks can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- Electric poles in three tiers (small, medium, substation) plus a creative pole. A pole powers every FE machine inside its supply area, and wires join poles within wire reach into one network that shares its generators' output, charging and drawing on accumulators (#1).
- `wireworks-server.toml` sets each tier's supply size and wire reach. The defaults are Factorio's.
- A Consumer tags its generators `wireworks:generators`, its accumulators `wireworks:accumulators`, and its wiring items `wireworks:wire_tools` (default: copper ingot). A multiblock part names its anchor through `EnergyOwner` or `EnergyOwnerBlock`.
- Poles place, extend and preview through Groundworks, which is nested. The pole's network shows in Jade when Jade is installed.
