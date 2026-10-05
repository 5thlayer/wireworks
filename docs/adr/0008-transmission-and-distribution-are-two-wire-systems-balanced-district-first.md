---
status: accepted
amends: [3, 7]
---

# Transmission and distribution are two wire systems, balanced district-first

Until now one copper wire chain carried any power any distance, losslessly, and every pole joined by
wires kept one book (ADR 0003). Nothing told a line across the map from the poles inside a base, and
a factory's brownout fell on every machine the wires reached. Wireworks splits the poles into two
systems that meet only at a **Transformer**, and settles them in two levels, without modelling
voltage, current or loss.

## Decision

**Two systems, joined by one block.** The three tiers and the creative pole are **Distribution
Poles**, unchanged. One new **Transmission Pole** has no tier and no **Supply Area**: it powers
nothing and carries **Wire**s far, 32 blocks by default and set in `wireworks-server.toml`. It
stacks as a **Pole Column**, since height exists for wire clearance. A wire joins two poles of one
system, or either kind of pole to a **Transformer**, and nothing else: a **Wire Tool** click across
systems is refused, and the refusal names the Transformer. A Transformer is one block, not a column,
with no area and no GUI. It has a Wire Reach on each side, the Transmission Pole's toward the line
and the Medium Pole's toward the district, both configured. Energy crosses it either way, so a
remote solar field exports up the line.

**Distribution Poles joined by wires are a District, and a District settles first.** Each District
runs ADR 0003's balance on its own book: its generators, then its accumulators, with only a
generator surplus charging its accumulators. Then the **Electric Network**, every District and
**Transmission Line** joined through Transformers, settles the Districts' leftovers in the same
order: their generator surplus meets the other Districts' shortfalls, then their accumulators
discharge to cover what remains, then generator surplus charges accumulators anywhere. A shortfall
across Districts is shared in proportion to each District's shortfall, so every importing machine
ends at the same fraction. Within a District it is still water-filled. The model has two levels and
no more: a District is always a leaf, two lines joined only through a District are one network, and
a second Transformer between the same District and line adds nothing.

A District that makes its own power keeps it. That is the one thing the Transformer means, and the
reason to build one.

**This departs from Factorio.** A Factorio network is flat, substations included, and a player who
knows Factorio will expect a self-sufficient district to brown out with its neighbours. Wireworks is
after Factorio's poles, not its every rule. Here a District protected from its neighbours is what
makes the split more than a wiring rule, and the balance is two copies of Factorio's own,
stacked.

**This is not the power ladder ADR 0002 refused.** A tier still grants no power, and no system
changes what a machine receives: there is no voltage, no rating and no throughput cap. Transmission
is a topology, and the Transmission Pole sits beside the tiers, not on top of them.

**Role names are allowed for the systems, not for the tiers.** ADR 0007 refused a role name for a
tier, because a size says more than a role. Transmission and distribution are not tiers but two
kinds of wire system, and a role is what tells them apart. Its reasoning for the tiers stands. "AC"
and "DC" are kept out: they promise physics the mod does not model. A pack names the blocks its own
way, in its own language file, as ADR 0007 allows.

**Auto-wiring stays inside a system.** A placed pole wires itself only to poles of its own system and
to Transformers in reach. A placed Transformer wires itself to the nearest pole of each system, one
each, so that it never stitches two Districts together by accident. One tag, `wireworks:wire_tools`,
wires both. The poles at a wire's ends decide its system, not the tool.

**Transmission wires look different**: thicker, darker and deeper in their sag. A Transmission Pole
shows no **Supply Area Box**. Its Jade line gives its network's surplus or shortfall, and a
Transformer's gives the power crossing it this tick, imported or exported.

**Always on.** There is no config switch. A world with no Transformer has one District per network,
which is exactly the network before this ADR. A pack that does not want transmission removes the two
recipes.

## Considered Options

- *One flat balance, with the Transformer only a wiring rule.* This is Factorio's own model and costs
  nothing in the balance. Rejected: it gives a constraint but no partition, so a Transformer is only
  a socket, and a self-sufficient District still browns out for its neighbours. It remains the
  fallback, since moving between the two changes who browns out and never the wiring.
- *A buffered Transformer*, holding energy that both sides push into and pull from. Rejected: it is
  an accumulator by another name, with block entity energy, save data, loss on break and one tick of
  lag. Its capacity and rate are a throughput cap by the back door, which this design refuses.
- *A throughput cap per Transformer, or a hop or distance limit on distribution chains.* Rejected as
  the first step into an electricity sim. A long chain of Distribution Poles stays legal. It is only
  tedious, and the tedium is the reason to build a line.
- *The Large Pole becomes the Transmission Pole, or the Transformer.* Rejected: it changes what an
  existing id means, a second breaking rename after ADR 0007, and the Pack configures the Large Pole
  as Factorio's substation.
- *A ladder of Transmission Poles.* Rejected: one pole keeps transmission a topology, not a second
  tier ladder.
- *Water-fill across Districts.* Rejected: it hands an equal cut to each District, so a small
  District is fed for being small. Proportional sharing lands a shortfall evenly on every importing
  machine. It is also the rule the balance already uses between generators.
- *Districts export only generator surplus, not their accumulators.* Rejected: a central battery
  farm backing the whole network is how players build, and refusing it would demand storage in every
  District.

## Consequences

- `NetworkBalance` stays pure and composes: each District settles, the network settles the
  Districts' surpluses and shortfalls, and each District hands its share on to its machines, in one
  tick, unit-tested on a plain JVM.
- The change only adds blocks. Under ADR 0001 its release bumps the patch, and old worlds load
  unchanged. The FactoryWorks Pack receives two craftable blocks. It ships them, renames them or
  removes their recipes, a choice its ADR 0002 has to answer.
- The glossary's **Electric Network** no longer means one book. **District** carries ADR 0003's
  balance, and the network carries the second level above it.
