![Wireworks](https://raw.githubusercontent.com/5thlayer/wireworks/main/publish/wireworks-cover.png)

Wireworks brings Factorio's electric poles to NeoForge. A pole powers every machine inside its supply area without wires or cables from the pole to the machine. Poles can be wired together to form one network that shares its generators' output among every machine it reaches.

## Why?

Because I always loved the simplicity of Factorio's power mechanics and I couldn't find any mod to reproduce it exactly.

**NeoForge, Minecraft 26.1.2 only.** Wireworks is early work, so please report any issues you find on [GitHub](https://github.com/5thlayer/wireworks/issues). From 0.2.0 the poles are named by size, with new ids, so a world made with 0.1.0 loses its poles.

To play Wireworks beside every other FactoryWorks mod, install the [FactoryWorks Showcase](https://www.curseforge.com/minecraft/modpacks/factoryworks-showcase) modpack from CurseForge.

## Features

- **Poles in three tiers.** Small, medium and large poles, each with its own supply area and wire reach, plus a creative pole that generates without limit, for trying a machine without a power chain.
- **Power by area.** Every Forge Energy machine inside a pole's supply area is powered, whichever way it faces.
- **Generation and storage.** A Solar Panel makes power by the time of day and nothing under a roof, and an Accumulator stores it and gives it back when generators fall short. Each is placed whole from one item and sits on the network with no cable. Both wear stand-in art for now.
- **Steam.** A Boiler burns fuel to turn water into steam at Factorio's 60 mB/s, and a Steam Engine burns that steam for up to 450 FE/t. Both are placed whole from one item, move fluid through NeoForge's fluid capability, so any pipe mod that speaks it connects, and a Boiler sitting against an Engine's steam port feeds it with no pipe at all. Engines placed end to end pass steam along the row. The Engine is a generator on the network, like the Solar Panel.
- **Wire a network.** A placed pole wires itself to up to five nearby poles. To add or cut a wire by hand, click one pole with a copper ingot, then another within wire reach. A network shares every generator's output, and charges and draws on its accumulators.
- **Place a line of poles.** Poles place, extend and preview through [Groundworks](https://github.com/5thlayer/groundworks), which Wireworks requires.
- **See the network.** With [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) installed, looking at a pole shows its network.

## Gallery

Wireworks carries a gallery of its features: one working scene each, with a sign naming it, built in rows of three north and east of where you stand. It is a datapack in the jar, **Wireworks Gallery**, off until you turn it on in a world's Data Packs screen or with `/datapack enable "mod/wireworks:gallery_pack"`. Then `/function wireworks:gallery/build` builds every scene, and `/function wireworks:gallery/<scene>` one of them, such as `steam`; clicking a scene's sign builds it again, which puts back the coal and water the Steam scene burns. A function needs cheats in single player (on at world creation, or Open to LAN with cheats) or operator rights on a server. Wireworks has no machine of its own that only takes power, so its scenes power Accumulators. The gallery clears the ground it builds on and changes nothing else; it adds no recipes, loot or worldgen.

## For pack developers

Each tier's supply area and wire reach, the Solar Panel's peak and the Accumulator's capacity and flow are set in `wireworks-server.toml`, with Factorio's numbers as defaults. Which blocks generate, store and wire is data: tag generators `wireworks:generators`, accumulators `wireworks:accumulators`, and wiring items `wireworks:wire_tools`. A pack names the poles its own way in its own language file, over Wireworks' `block.wireworks.*` and `item.wireworks.*` keys.

## For mod developers

Wireworks is also a library, carved out of the [FactoryWorks](https://github.com/5thlayer/factoryworks) pack. A multiblock built as a Groundworks footprint is counted once by its origin with nothing more; any other names its anchor through `EnergyOwner` or `EnergyOwnerBlock`, and `PoleColumnReplace.BUILDER` swaps a pole column for another tier through Groundworks' Fast Replace, keeping its wires.

## Dependencies

[Groundworks](https://github.com/5thlayer/groundworks) `0.5.11` or later in the 0.5 series, which is a separate download. Wireworks is needed on both the client and the server.

## License

MIT, under REUSE (`REUSE.toml`). The cover art uses Minecraft's dirt texture, which remains Mojang's. The bioresin, plastic, battery and fuel cell item textures, and the Accumulator's, the Solar Panel's, the Boiler's, the Steam Engine's, the Steam Turbine's and the Reactor's textures, are by Malcolm Riley (the Boiler's front adapted from his smelter, the Accumulator's sides from his battery block, the Solar Panel's cell from his blue cement, the Steam Turbine's still fan and coil side from his duct fan and copper coil, and the Steam Engine's front from his duct panels and copper tank), from [unused-textures](https://github.com/malcolmriley/unused-textures), under [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/). Source is on [GitHub](https://github.com/5thlayer/wireworks).
