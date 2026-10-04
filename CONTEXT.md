# Wireworks

Electric poles after Factorio's: a pole powers every machine inside its supply area, and wires join poles into networks. The FactoryWorks Pack is its first Consumer, and it plays in a game with no Pack.

## Language

### Parties

**Wireworks**:
This Library.

**Consumer**:
A mod that builds against this Library: the FactoryWorks Pack, or another Library. It tags its generators, accumulators and wire tools, and its multiblocks name their energy owner.
_Avoid_: client (collides with the game's client side), dependent, integration

### Poles

**Supply Area Pole**:
A pole of one tier, small, medium or large, or the creative pole. It has no energy face of its own: it reaches every FE block in its **Supply Area** and joins its **Electric Network** by **Wire**s.
_Avoid_: power pole, node, transmitter

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
Every **Supply Area Pole** joined to another by a **Wire**, directly or through other poles, plus every generator, accumulator and machine standing in any of their areas. One balance: demand is met from generators first and accumulators second, only a generator surplus charges accumulators, and a shortfall is shared out.
_Avoid_: grid, power net, FE network

**Wire**:
A stored connection between two poles' bases, and the only thing that joins poles: two poles in reach but not wired are not connected. A wire can only exist within **Wire Reach**. A placed pole wires itself to up to five nearby poles, and a **Wire Tool** adds or cuts one by hand.
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
_Avoid_: battery, storage

**Solar Panel**:
The **Generator** Wireworks ships: its output follows **Daylight**, and it makes nothing while a roof hides the sky above it.
_Avoid_: solar generator

**Daylight**:
How much of a **Solar Panel**'s peak output the time of day allows, all of it through the day, none at night, ramping between them at dusk and dawn.
_Avoid_: sunlight, light level (the game's block light, which a roof or torch changes)

**Energy Owner**:
The block whose energy a multiblock's part stands for, named through `EnergyOwner` or `EnergyOwnerBlock`. A pole resolves each block to its owner first, so it counts and feeds each machine once. A face a pole reaches journals its buffer, since a pole measures room with an insert it then aborts.
_Avoid_: controller, master (Oritech's terms for its own engines)

### Drawing

**Supply Area Box**:
The yellow wireframe of a pole's whole area, anchored at the base, with an outline around every machine it reaches. Shown while holding a pole or looking at one. It says where the area lands, never whether anything is fed, which the Jade line answers.
_Avoid_: overlay, range indicator
