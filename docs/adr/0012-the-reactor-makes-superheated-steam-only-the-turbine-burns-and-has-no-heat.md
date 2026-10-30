---
status: accepted
---

# The Reactor makes superheated steam only the Turbine burns, and has no heat

ADR-0127 gave nuclear power to Wireworks: a Reactor, a Steam Turbine, and a fuel cell crafted from
`#wireworks:fissile`. FactoryWorks' ADR-0033 had already flattened Factorio's heat layer into a
fluid, but it planned the machines on a GregTech chassis beside a Centrifuge, and ADR-0127 removed
both. This ADR settles the machines as Wireworks builds them (#29).

## Decision

**Superheated steam is a second fluid, and only the Steam Turbine burns it.** The Turbine refuses
ordinary Steam, and the Steam Engine never sees superheated steam. A Turbine that took any steam
would make the Boiler chain's Engines pointless the moment it was built. With two fluids, nuclear
power adds to that chain instead. Factorio already puts the value in the steam's temperature (165
against 500 degrees), and Wireworks keeps those two points as two fluids rather than a temperature
axis.

**The Reactor has no heat.** It has no temperature, no warm-up, no neighbour bonus and no meltdown.
While it has fuel, water and room for its steam, it turns water into superheated steam at one rate.
Otherwise it pauses, burning nothing, as the Boiler does (ADR 0011). Factorio's heat pipes and heat
exchangers are not built, so a Reactor takes water itself.

**Factorio's ratio is kept whole.** The Turbine burns 60 units a second of 500 degree steam for
5.82 MW, which is 3 mB/t for 2,910 FE/t at the Steam Engine's 100 J per FE. Factorio's 40 MW reactor
would make 20.6 mB/t, which is not a whole millibucket, so the Reactor makes 21 mB/t: exactly seven
Turbines, about 40.7 MW. A cell lasts Factorio's 200 seconds, 4,000 ticks of running. These are
constants in `ReactorSpec` and `SteamTurbineSpec`, not config (ADR-0115).

**A burnt Fuel Cell leaves a Depleted Fuel Cell, which a fissile refills.** Factorio's reprocessing
gave uranium back through the Centrifuge, which ADR-0127 removed. A Wireworks recipe can output only
vanilla items or Wireworks' own, and `c:ingots/uranium` is a tag Wireworks does not own, so no
recipe can hand uranium back. Instead, one Depleted Fuel Cell and one `#wireworks:fissile` craft one
Fuel Cell, which saves the casing's eight iron. The depleted cell stays a product the player has to
route somewhere, and the Reactor stops when its depleted slot is full.

**The Reactor is built from plastic, netherite and a blast furnace, not batteries.** ADR-0127 said
the Reactor takes plastic and batteries, but a battery is storage, not part of a reactor. The
Reactor is the mod's high-end generator, and two netherite blocks price it that way. The battery
stays an ingredient of the Accumulator.

## Considered Options

- **One Steam for both chains.** This is simpler, but a Turbine fed by Boilers would replace the
  Engines.
- **A real temperature, with a meltdown.** This would be a Minecraft addition Factorio does not
  have, and ADR-0033 already declined to build a heat layer.
- **No depleted cell, or one that blasts back into iron nuggets.** With no depleted cell, nothing
  comes out of the Reactor for the player to route. Nuggets would only be a chore. The refill turns
  it into a loop the player automates, as reprocessing does.

## Consequences

- #29 is blocked by #27, which adds plastic and the battery.
- `#wireworks:fission_fuel`, which the Reactor burns, and `#wireworks:fissile`, which a Fuel Cell is
  crafted from, are two different tags.

## Amended, on pipes that push nothing

The Reactor draws water each tick from whatever stands against its water Ports, as the Boiler does
(ADR 0011), and leaves another machine's Port alone. The Steam Turbine draws its
superheated steam from whatever stands against either end of its row, so a pipe between the Reactor and
a Turbine moves steam, while the Reactor's own push still serves a Turbine against its port.
