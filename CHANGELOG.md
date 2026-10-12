# Changelog

Each version has two sections: Players, what a player or pack developer sees, and Consumers, what a mod building against Wireworks can use or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

### Consumers

- Wireworks nests libworks-runtime 0.1.7, which offers the Boiler's and the Reactor's ghosts to EMI in place of Wireworks' own stack provider: the recipe and usage keys still work on a ghost and a click still reaches its empty slot (libworks#23).

## 0.6.1

### Players

- The Accumulator, the Solar Panel and the Boiler wear textures by Malcolm Riley, from unused-textures, under CC-BY-4.0, in place of their stand-ins: the Accumulator a battery casing, the Solar Panel blue cells on riveted plating, and the Boiler plating round a smelter's firebox. (#45)
- Wireworks requires Groundworks 0.5.11 or later in the 0.5 series, where it accepted 0.5.4: the Groundworks the other 5thlayer mods' current releases load beside, which Wireworks now builds and tests against.
- The Steam Turbine and the Reactor wear textures by Malcolm Riley, from unused-textures, under CC-BY-4.0, in place of their vanilla stand-ins: the Turbine a copper coil with a duct fan front and back, which spins while it burns and for a second after, and the Reactor dark steel with a yellow hazard band round its middle and a vent on top. The Fuel Cell is a full fuel can and the Depleted Fuel Cell an empty one. (#46)
- The Boiler's firebox is lit, and every block of the Boiler gives off a lit furnace's light, while it boils and for a second after, so one feeding a single Engine does not flicker; it shows cold while it waits on water, fuel, room for its steam or its output. (#45)

### Consumers

- `wireworks:boiler` and `wireworks:boiler_part` have a `lit` block state property, true while the Boiler boils and for a second after. (#45)
- `wireworks:steam_turbine` has a `running` block state property, true while the Turbine burns and for a second after; its parts do not carry it. (#46)

## 0.6.0

### Players

- The Boiler: a 3x2 machine that burns a fuel item for its vanilla burn time and turns water into steam at Factorio's 60 mB/s, 3 mB a tick. Water goes in at either end of its front row, into a 2,000 mB tank; steam leaves its back middle, from a 2,000 mB tank. A Boiler with no room for steam makes none and burns no fuel. Right-click it for its fuel slot, a water bucket slot, an empty bucket slot for steam, its output slot and its gauges. A water bucket empties into the tank and an empty bucket fills with steam, each whole and only once the tank has room or steam for all of it, and goes to the output; what a burnt fuel leaves, such as a lava bucket's empty bucket, goes there too. A Boiler whose output has no room for what a slot would leave waits. Pipes and hoppers put fuel and buckets in and take the output out, and nothing the other way. Its screen and Jade say whether it is boiling, and if not, why not: no water, steam backed up, output full or out of fuel. Jade also shows its slots and each tank, and the screen shows faded stand-ins of what goes in each empty input slot, on which EMI's recipe and usage keys work. (#42)
- The Steam Engine: a 2x1x2 generator that burns 30 mB/s of steam for 450 FE/t, up to Factorio's 900 kW, from a 200 mB steam tank. A pole draws it once, whichever of its blocks the supply area reaches. Jade names "No steam" and "No pole".
- Steam, a fluid, and the Steam Bucket, which the Boiler fills and which draws upside down, as a gas does. Placed, it pours steam into the world as any bucket pours its fluid. A Boiler standing against an Engine's steam port feeds it with no pipe between, and Engines placed end to end share the steam: what comes in at one end passes on out of the other. (#42)
- `boiler.joules_per_burn_tick` in `wireworks-server.toml` sets what a tick of a fuel's vanilla burn time is worth to the Boiler: 2500 J by default, which makes coal Factorio's 4 MJ.
- The Boiler and the Steam Engine are mined with a pickaxe, and drop nothing from a bare hand.
- Both craft from vanilla materials: the Boiler from a furnace, a bucket and seven iron ingots; the Engine from a piston, an iron block, two copper ingots and five iron ingots.
- Bioresin, a plastic resin made from plants: a brewing stand turns a water bottle and sugar cane into bioresin, and honeycomb and kelp craft one. No vanilla brew changes. EMI shows the brew with its water bottle. (#28)
- Plastic, blasted one for one from any plastic resin: vanilla's resin clump, bioresin, or tar when a mod makes it. (#27)
- The battery, crafted from a copper ingot, an iron ingot and redstone. (#27)
- Bioresin, plastic and the battery wear textures by Malcolm Riley, from unused-textures, under CC-BY-4.0. (#37, #38)
- Recipes change. The Solar Panel takes plastic where it took redstone; the large pole takes plastic where it took redstone; the Transformer takes one plastic in its bottom row; the Transmission Pole takes two plastic beside its post; and the Accumulator takes four batteries where it took four redstone blocks. The small and medium poles are unchanged. (#27)
- The Steam Turbine: a 3x1x2 generator that burns 60 mB/s of superheated steam for 2,910 FE/t, Factorio's 5.82 MW, from a 200 mB tank. It refuses Steam, and the Steam Engine refuses superheated steam. Turbines placed end to end share their steam as Engines do. Jade shows what it makes each tick, and names "No steam" and "No pole". It is mined with a pickaxe and crafted from four copper blocks, two plastic, two pistons and an iron block. Its art is a vanilla placeholder. (#39)
- Superheated Steam, a second fluid with no bucket, which only the Steam Turbine burns. (#39)
- The Reactor: a 3x3x3 machine that burns a Fuel Cell and turns water into superheated steam at 21 mB a tick, 420 mB/s, enough for seven Steam Turbines at 2,910 FE/t each. It has no heat: it runs at its one rate or not at all, and burns nothing while it lacks water or room for its steam. Water goes in at the middle of each side of its bottom layer, into a 10,000 mB tank; steam leaves the back middle of its middle layer, from a 10,000 mB tank, and is handed to whatever stands against it, so a row of seven Turbines standing there on the same ground runs with no pipe. A cell lasts 4,000 ticks of running, 200 seconds, and leaves a Depleted Fuel Cell; the Reactor stops while its depleted slot is full. Right-click any of its blocks for its screen: the fuel slot beside the loaded cell's bar, a water slot that empties a water bucket into the tank whole and sends the empty bucket to a bucket output of its own, gauges for the water and the superheated steam, the depleted slot, and a line that says whether it is running and if not why not. Empty slots show what goes in them, and EMI's recipe and usage keys work on those. Pipes and hoppers reach every slot from any block: fuel and water buckets go in, depleted cells and empty buckets come out, and nothing the other way. Jade shows the same status line, the cell's ticks left, its slots and each tank. It is mined with a pickaxe and crafted from four iron blocks, two plastic, two netherite blocks and a blast furnace, with no battery. Its art is a vanilla placeholder. (#40, #41, #43)
- The Boiler takes as long to mine as its hardness of 3.5 says, and the Reactor its 5; both had broken as fast as the other machines' 2.
- The Fuel Cell, crafted from eight iron ingots round glowstone dust, or a uranium ingot when a mod adds one; and the Depleted Fuel Cell, which one glowstone dust or uranium ingot refills into a Fuel Cell. (#40)
- Wireworks' items are in the suite's shared Works tab, beside every other -works mod's, and in no other tab: the Wireworks tab is gone, and the blocks leave Functional Blocks. (#23)
- The Boiler's screen looks as it did, but a status line too long for the panel is cut short and shown whole on hover, and an empty tank's gauge reads no amount where it read 0 mB. (#44)
- The Boiler's water and steam gauges say on hover which tank they are, then the fluid in it or "Empty", then how much it holds, in the words every machine's tanks use.
- A Distribution Pole's Jade lines show again. The Transmission Pole's and the Transformer's lines had taken its Jade id, which left the pole's lines empty; each now has a toggle of its own in Jade's settings, "Transmission Pole network" and "Transformer exchange".

### Consumers

- Wireworks nests libworks-runtime 0.1.4, in the range `[0.1.4,0.2)`, and draws the Boiler's screen and Jade view with its shared machine look in place of a copy of it. (#44)
- The new item `wireworks:steam_bucket`, a plain `BucketItem`, so it has NeoForge's fluid item capability; it is `wireworks:steam`'s bucket. The Boiler's item face takes fuel, water containers and empty containers steam can go into, each in its own slot, and gives only the output. (#42)
- `wireworks:steam_engine` is in `wireworks:generators`. Its fluid face is on every block of its footprint, insert-only, and steam taken in at either end of its row is offered on out of the other, and the Boiler's water and steam faces are on the port blocks, through NeoForge's `Capabilities.Fluid.BLOCK`. Wireworks names no pipe mod.
- `wireworks:steam_turbine` is in `wireworks:generators`. Its fluid face is on every block of its footprint, insert-only, takes only `wireworks:superheated_steam`, and passes it along its row as the Steam Engine's does. (#39)
- `wireworks:reactor`'s item face is on every block of its footprint, through `Capabilities.Item.BLOCK`: Fission Fuel and water containers in, depleted cells and emptied containers out. A water container is any item with NeoForge's fluid item capability holding water. Its water faces are on the middle block of each side of its bottom layer, opening outward, and accept extraction too; its superheated steam face is on the back middle block of its middle layer, opening backward, extract-only. Each tick it offers its steam to the fluid capability against that face. It is in neither `wireworks:generators` nor `wireworks:accumulators`. (#40, #43)
- The new item tags `wireworks:fission_fuel`, what the Reactor burns, which holds `wireworks:fuel_cell`, and `wireworks:fissile`, what a Fuel Cell is crafted and refilled from, which holds `minecraft:glowstone_dust` and, optionally, `#c:ingots/uranium`. They are two tags. Only the Fuel Cell leaves a `wireworks:depleted_fuel_cell` when burnt: any other item added to `wireworks:fission_fuel` leaves its crafting remainder in the depleted slot, or nothing. (#40, #43)
- The new item tag `wireworks:plastic_resins`, the items plastic is made from, holds `minecraft:resin_clump` and, optionally, `#c:tar`, which Fieldworks fills. Wireworks makes no tar of its own. (#26, #34)
- The new item `wireworks:bioresin` is in `wireworks:plastic_resins`. Its brewing is a NeoForge brewing recipe, registered on `RegisterBrewingRecipesEvent`, on a water bottle with sugar cane. (#28)
- Breaking: the Solar Panel and the large pole no longer read `#c:dusts/redstone`, and the Accumulator no longer reads `#c:storage_blocks/redstone`: a pack that filled those tags to feed these recipes now feeds them plastic or batteries. (#27)
- The new items `wireworks:plastic` and `wireworks:battery`. Plastic is a `minecraft:blasting` recipe on `#wireworks:plastic_resins`. The battery takes one of each of the new item tags `wireworks:cathodes` (`minecraft:copper_ingot`), `wireworks:anodes` (`minecraft:iron_ingot`) and `wireworks:electrolytes` (`minecraft:redstone` and, optionally, `#c:dusts/sulfur`), which a Consumer may add to. (#27)
- The Boiler and the Steam Engine ported from FactoryWorks Core, without Oritech or Pipeworks. The Core's `SteamEngineBlock` and `BoilerBlock` ids move from `factoryworks:` to `wireworks:`; a world made with them loses them. (#22)
- Wireworks nests libworks-runtime 0.1.1 by jar-in-jar, up to but not including 0.2: its mod id is `libworks`, it rides inside the jar, and players install nothing more. The Boiler's item and fluid faces extend its `GuardedResourceHandler`. (#33)
- Breaking: the creative tab `wireworks:items` and its lang key `itemGroup.wireworks.items` are removed, with `WireworksRegistries.CREATIVE_TAB`: the items are in libworks' `WorksTab.KEY` and in no vanilla tab (FactoryWorks ADR-0128). (#23)

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
