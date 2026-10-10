![Wireworks](https://raw.githubusercontent.com/5thlayer/wireworks/main/publish/wireworks-cover.png)

Wireworks brings Factorio's electric poles to NeoForge. A pole powers every machine inside its supply area, with no cable run to each machine and no face to connect. Wire poles together and they form one network that shares its generators' output among every machine it reaches.

**NeoForge, Minecraft 26.1.2 only.** Wireworks is early work, so please report any issues you find on [GitHub](https://github.com/5thlayer/wireworks/issues). From 0.2.0 the poles are named by size, with new ids, so a world made with 0.1.0 loses its poles.

## Features

- **Poles in three tiers.** Small, medium and large poles, each with its own supply area and wire reach, plus a creative pole that generates without limit, for trying a machine without a power chain.
- **Power by area.** Every Forge Energy machine inside a pole's supply area is powered, whichever way it faces.
- **Generation and storage.** A Solar Panel makes power by the time of day and nothing under a roof, and an Accumulator stores it and gives it back when generators fall short. Each is placed whole from one item and sits on the network with no cable. Both wear stand-in art for now.
- **Steam.** A Boiler burns fuel to turn water into steam at Factorio's 60 mB/s, and a Steam Engine burns that steam for up to 450 FE/t. Both are placed whole from one item, move fluid through NeoForge's fluid capability, so any pipe mod that speaks it connects, and a Boiler sitting against an Engine's steam port feeds it with no pipe at all. Engines placed end to end pass steam along the row. The Engine is a generator on the network, like the Solar Panel.
- **Wire a network.** A placed pole wires itself to up to five nearby poles. To add or cut a wire by hand, click one pole with a copper ingot, then another within wire reach. A network shares every generator's output, and charges and draws on its accumulators.
- **Place a line of poles.** Poles place, extend and preview through [Groundworks](https://github.com/5thlayer/groundworks), which Wireworks requires.
- **See the network.** With [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) installed, looking at a pole shows its network.

## For pack developers

Each tier's supply area and wire reach, the Solar Panel's peak and the Accumulator's capacity and flow are set in `wireworks-server.toml`, with Factorio's numbers as defaults. Which blocks generate, store and wire is data: tag generators `wireworks:generators`, accumulators `wireworks:accumulators`, and wiring items `wireworks:wire_tools`. A pack names the poles its own way in its own language file, over Wireworks' `block.wireworks.*` and `item.wireworks.*` keys.

## For mod developers

Wireworks is also a library, carved out of the [FactoryWorks](https://github.com/5thlayer/factoryworks) pack. A multiblock built as a Groundworks footprint is counted once by its origin with nothing more; any other names its anchor through `EnergyOwner` or `EnergyOwnerBlock`, and `PoleColumnReplace.BUILDER` swaps a pole column for another tier through Groundworks' Fast Replace, keeping its wires.

## Dependencies

[Groundworks](https://github.com/5thlayer/groundworks) `0.5.4` or later in the 0.5 series, which is a separate download. Wireworks is needed on both the client and the server.

## License

MIT, under REUSE (`REUSE.toml`). The cover art uses Minecraft's dirt texture, which remains Mojang's. Source is on [GitHub](https://github.com/5thlayer/wireworks).
