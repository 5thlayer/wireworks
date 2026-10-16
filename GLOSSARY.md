# Wireworks

Electric poles after Factorio's: a pole powers every machine inside its supply area, and wires join poles into networks. The FactoryWorks Pack is its first Consumer, and it plays in a game with no Pack.

## Language

### Parties

**Wireworks**:
This Module.
_Avoid_: Library, Binding

**Consumer**:
A mod that builds against this Module: the FactoryWorks Pack, or another Module. It tags its generators, accumulators and wire tools, and its multiblocks name their energy owner.
_Avoid_: client (collides with the game's client side), dependent, integration

### Poles

**Distribution Pole**:
A pole of one tier, small, medium or large, or the creative pole. It has no energy face of its own: it reaches every FE block in its **Supply Area** and joins its **District** by **Wire**s. It never wires to a **Transmission Pole**.
_Avoid_: Supply Area Pole (the code's name), power pole, node, DC pole

**Transmission Pole**:
A pole with no **Supply Area** and no tier, which carries **Wire**s far: it powers nothing itself, and reaches **Distribution Pole**s only through a **Transformer**.
_Avoid_: AC pole, high-voltage pole, pylon, large pole (a Distribution Pole)

**Transformer**:
The only block that wires to both **Transmission Pole**s and **Distribution Pole**s, and the only joint between the two. It has no **Supply Area**, and energy crosses it either way.
_Avoid_: substation (Factorio's large pole), converter, step-down

**Tier**:
A pole's geometry, small, medium or large: its supply size and its **Wire Reach**, set in `wireworks-server.toml` and Factorio's by default. A tier grants no power of its own. A pack may name the poles its own way.
_Avoid_: level, voltage, substation (Factorio's name for the large pole)

**Supply Area**:
The square a pole supplies, its supply size on a side and two blocks up and down, measured at the base of its **Pole Column**. An even side is offset half a block.
_Avoid_: range, radius, coverage

**Pole Column**:
A pole stacked up to five blocks tall, which is still one pole: raising it costs nothing, breaking any segment drops the column above it, and only the base pays back its item. Its **Wire**s and area are the base's.
_Avoid_: tower, stack

**Creative Pole**:
A pole with the large pole's area that generates without limit, for trying a machine without a power chain. It has no recipe.
_Avoid_: infinite source, debug pole

### Networks

**Electric Network**:
Every **District** and **Transmission Line** joined through **Transformer**s. Each District settles its own book first; the network's book then meets the Districts' shortfalls from their surplus, generator output first and accumulators second, shares a shortfall among Districts in proportion to each one's, and only then charges accumulators from generator surplus, each District's own first. A world with no Transformer has one District per network.
_Avoid_: grid, power net, FE network, AC/DC

**District**:
Every **Distribution Pole** joined to another by a **Wire**, directly or through other Distribution Poles, plus every generator, accumulator and machine standing in any of their areas. Its own demand is met from its generators first and its accumulators second, and a shortfall is shared out. What it has left over or lacks crosses its Transformers, and its accumulators charge only once every District on its **Electric Network** is fed.
_Avoid_: zone, subnet, area (the **Supply Area**), substation

**Transmission Line**:
Every **Transmission Pole** joined to another by a **Wire**, plus the **Transformer**s wired to them. It holds no energy and keeps no book of its own: its **Electric Network** does.
_Avoid_: HV line, AC line, power line

**Wire**:
A stored connection between two poles' bases, and the only thing that joins poles: two poles in reach but not wired are not connected. A wire joins two poles of one system, or either kind of pole to a **Transformer**. A wire can only exist within **Wire Reach**. A placed pole wires itself to up to five nearby poles, and a **Wire Tool** adds or cuts one by hand.
_Avoid_: link, cable, connection

**Wire Reach**:
How far a wire can run from a pole, set per **Tier**.
_Avoid_: range, span

**Wire Tool**:
Any item in `wireworks:wire_tools`, the copper ingot by default. A click on one pole holds a **Pending End**; a click on a second wires the pair or cuts their wire.
_Avoid_: wrench, wire cutter

**Pending End**:
The first pole of a wire being made by hand, held on the **Wire Tool**. It lets go when its pole breaks, the tool leaves the main hand, the player changes dimension or walks out of reach.
_Avoid_: selection, anchor (the code's name for the pole it holds)

**Generator**:
A block tagged `wireworks:generators`, which a network draws FE from first. The tag, not the face, decides the role, so a battery that allows extraction is not drained.
_Avoid_: source, producer

**Accumulator**:
A block tagged `wireworks:accumulators`, which a network charges from a generator surplus and draws on to cover a shortfall. Wireworks ships one of its own, and any mod's block can play the part.
_Avoid_: battery (the **Battery** is the item it is crafted from), storage

**Solar Panel**:
The **Generator** Wireworks ships: its output follows **Daylight**, and it makes nothing while a roof hides the sky above it.
_Avoid_: solar generator

**Boiler**:
The machine that burns fuel to turn water into **Steam**, at Factorio's 60 mB/s. A 3x2 **Groundworks** footprint: water goes in at the two ends of its front row, steam leaves its back middle, and fuel goes in through any block. Its two tanks hold 2,000 mB each. A water bucket slot empties into the water tank and an empty bucket slot fills from the steam tank; the buckets, and what a burnt fuel leaves, a lava bucket's bucket, go to its output slot, which pipes and hoppers empty. It burns no fuel while its steam, or what the fuel would leave, has nowhere to go.
_Avoid_: furnace, kettle

**Steam Engine**:
The **Generator** that burns **Steam**, 30 mB/s of it for up to 450 FE/t. A 2x1x2 footprint with its own steam tank, filled by a pipe or by a **Boiler** standing against it. Steam that comes in at one end of its row passes on out of the other, so Engines placed end to end share it.
_Avoid_: turbine (the **Steam Turbine**)

**Steam**:
The fluid the **Boiler** makes and the **Steam Engine** burns, Factorio's 165 degree steam. Its bucket, which draws upside down as a gas does, is filled by the **Boiler**. It crosses between blocks through NeoForge's fluid capability, so Wireworks needs no pipe mod. The **Steam Turbine** refuses it.
_Avoid_: vapour

**Daylight**:
How much of a **Solar Panel**'s peak output the time of day allows, all of it through the day, none at night, ramping between them at dusk and dawn.
_Avoid_: sunlight, light level (the game's block light, which a roof or torch changes)

**Energy Owner**:
The block whose energy a multiblock's part stands for, named through `EnergyOwner` or `EnergyOwnerBlock`; a Groundworks footprint's part stands for its origin, whatever mod declared it. A pole resolves each block to its owner first, so it counts and feeds each machine once. A face a pole reaches journals its buffer, since a pole measures room with an insert it then aborts.
_Avoid_: controller, master (Oritech's terms for its own engines)

### Nuclear

**Reactor**:
The machine that burns **Fission Fuel** to turn water into **Superheated Steam**, a 3x3x3 footprint. It has no temperature: while it has fuel, water and room for its steam it runs at its one rate, and it never melts down.
_Avoid_: nuclear reactor, fission reactor, core

**Steam Turbine**:
The **Generator** that burns **Superheated Steam** and nothing else, a 3x1x2 footprint whose steam passes on out of the far end of its row, as the **Steam Engine**'s does.
_Avoid_: turbine alone, generator (the role, which it plays)

**Superheated Steam**:
The fluid the **Reactor** makes and only the **Steam Turbine** burns, Factorio's 500 degree steam. It is a second fluid rather than a temperature, so nuclear power adds to the **Boiler** chain instead of retiring it.
_Avoid_: hot steam, high-pressure steam

**Fission Fuel**:
Any item in `wireworks:fission_fuel`, which the **Reactor** burns: the **Fuel Cell** by default.
_Avoid_: nuclear fuel, uranium (a **Fissile**)

**Fuel Cell**:
The **Fission Fuel** Wireworks ships, crafted from a **Fissile**. A burnt one leaves a **Depleted Fuel Cell**.
_Avoid_: uranium fuel cell, fuel rod

**Depleted Fuel Cell**:
What a **Fuel Cell** leaves in the **Reactor** once burnt, which a **Fissile** refills into a Fuel Cell.
_Avoid_: used-up fuel cell (Factorio's name), spent cell, nuclear waste

**Fissile**:
Any item in `wireworks:fissile`, which a **Fuel Cell** is crafted from: a uranium ingot or glowstone dust by default, so Wireworks plays alone.
_Avoid_: fission fuel (what the **Reactor** burns), enriched uranium

### Resources

**Tar**:
Any item in `c:tar`, a **Plastic Resin**. Wireworks makes none: Fieldworks does, where its crude oil meets lava, and without it the tag is empty.
_Avoid_: bitumen, asphalt

**Bioresin**:
Wireworks' own **Plastic Resin**, made from plants: brewed from a water bottle and sugar cane, or crafted from honeycomb and kelp. No route is a pairing vanilla already brews.
_Avoid_: bioplastic (what plastic made from it would be)

**Plastic Resin**:
An item in `wireworks:plastic_resins`, which plastic is made from: vanilla's resin clump, **Bioresin**, and **Tar** when a mod fills `c:tar`.
_Avoid_: resin (vanilla's resin clump alone)

**Plastic**:
What a blast furnace makes from any **Plastic Resin**, one for one. The Solar Panel, the Transformer and the large and transmission poles take it.
_Avoid_: plastic bar (Factorio's name)

**Battery**:
Crafted from one **Cathode**, one **Anode** and one **Electrolyte**. The Accumulator takes it.

**Cathode**:
Any item in `wireworks:cathodes`, one of a **Battery**'s three parts: a copper ingot by default.

**Anode**:
Any item in `wireworks:anodes`, one of a **Battery**'s three parts: an iron ingot by default.

**Electrolyte**:
Any item in `wireworks:electrolytes`, one of a **Battery**'s three parts: redstone by default, and sulfur when a mod fills `c:dusts/sulfur`.
_Avoid_: acid (Factorio's sulfuric acid)

### Drawing

**Supply Area Square**:
The yellow outline of a pole's **Supply Area** on the ground its **Pole Column**'s base stands on, with an outline around every machine it reaches, in its role's colour: orange for a **Generator**, blue for a machine it feeds, purple for an **Accumulator**. Shown while holding a pole or looking at one. It says how much ground the area spreads over, not how far up and down it reaches, which the machine outlines and the item tooltip answer, and never whether anything is fed, which the Jade line answers.
_Avoid_: Supply Area Box (the cube it replaced), overlay, range indicator

### Gallery

**Gallery pack**:
The datapack in this Module's jar, "Wireworks Gallery", off until a player turns it on, whose functions build a working scene of each README feature, each with a sign naming it. Functions only (libworks ADR 0010).
_Avoid_: demo pack, showcase pack, gallery datapack
