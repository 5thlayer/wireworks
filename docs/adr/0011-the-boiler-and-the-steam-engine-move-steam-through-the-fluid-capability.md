---
status: accepted
---

# The Boiler and the Steam Engine move steam through the fluid capability

FactoryWorks Core's Boiler and Steam Engine joined Pipeworks segments through a Pipeworks API, took
their numbers from a corpus the Pack extracted from Factorio's data, valued fuel from a table the Pack
generated, and drew the engine on Oritech's model. ADR-0127 and ADR-0128 give both machines to
Wireworks and let no Module require another but Groundworks.

## Decision

**The two machines own their tanks and meet pipes only through `Capabilities.Fluid.BLOCK`.** The
Boiler keeps a 600 mB water tank and a 200 mB steam tank, the Engine a 200 mB steam tank, and a port
block hands out a face on the sides its port opens onto. Wireworks names no pipe mod. Two things the
segments gave for free are rebuilt by hand:

- The Boiler offers its steam to whatever stands against its steam port each tick, so a Boiler beside
  an Engine needs no pipe.
- The Boiler's water faces draw from and fill one tank, so water passes through the front row.
- The Engine's steam passes through. Its row runs along its local x, and a face at either end
  remembers which end it took steam in at (saved with the block). Each tick, after burning, the
  Engine offers what its tank holds to whatever stands against the other end, an Engine or a pipe.
  Engines in a row share the steam in the order it reaches them, each burning its rate before it
  passes the rest on, so a starved row feeds its first Engine fully. Steam put in at a side, from
  above, or with no side named only fills that Engine and is never passed on, so no pair of Engines
  hands steam back and forth. Core got this from Pipeworks' segment; here it needs only the fluid
  capability.

The Boiler's water faces accept extraction as well as insertion, so a pipe can draw water back out of
a Boiler; that is accepted.

**Harvest is by pickaxe.** The Boiler and the Engine are mineable with a pickaxe and drop nothing
without one, whichever block of the footprint is broken.

**Parts answer from a listener ahead of Groundworks'.** Groundworks forwards a part's lookup to its
origin with the side unchanged, so the origin cannot tell which port was reached. The Boiler's part
block registers its fluid faces at the highest priority, and the anchor opens onto nothing.

**Factorio's numbers are plain constants** (ADR-0115): 1.8 MW, 165 degrees, 15 degrees, steam's
0.2 kJ, 200-unit boxes, 0.5 steam per tick for 900 kW. `BoilerSpec` and `SteamEngineSpec` derive the
per-tick rates from them and refuse a rate that does not land on a whole millibucket.

**Fuel is vanilla's burn time at 2,500 J per tick by default**, which makes coal's 1,600 ticks
Factorio's 4 MJ. ADR-0127 has burners read vanilla burn time. The conversion is
`boiler.joules_per_burn_tick` in the server config, since no table of fuel values ships with the
Library.

## Consequences

- `GuardedResourceHandler`, which stops a pipe that names no slot from walking past a per-slot
  refusal, is a local copy under `wireworks.internal` until libworks-runtime ships (5thlayer/libworks#8).
  The swap is one import in `BoilerItemHandler` and `FluidFace`.
- The art is Core's, unmodified, with the Boiler on vanilla textures until its 3x2 model is drawn.
