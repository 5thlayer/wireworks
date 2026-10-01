---
status: accepted
amends: [2, 3, 4, 5]
---

# Wireworks is its own mod

ADR-0002 to ADR-0005 were written for FactoryWorks' `core/energy/`, where the poles served one pack:
Factorio's figures, Oritech's generators, the Engineer's Pick. Wireworks takes them out as a mod any
pack can use (5thlayer/factoryworks#476), and FactoryWorks pins its jar and deletes its own copy. This
ADR records where Wireworks departs from those four; everything they decide that is not named here
stands.

**A tier's figures are a server config.** ADR-0002 pinned the supply areas and wire reaches to
Factorio's prototypes. Here the three tiers are fixed, since their blocks register at startup, but
each one's supply size and wire reach are set in `wireworks-server.toml`, and the defaults are
Factorio's. A server config reaches the client, so the preview draws the area the server powers. A
wire only exists within reach, so a reload that shortens a reach cuts the wires it no longer spans. A
pack that wants Factorio's figures pinned states them in its own config and tests that file.

- *Considered: tiers as datapack entries.* Rejected: a block needs its tier at registration, before a
  datapack loads, so only the figures could be data, and a config already carries figures. Beltworks
  and Craftworks configure through server configs too.

**Any item in `wireworks:wire_tools` wires poles.** ADR-0004 gave the gesture to the Engineer's Pick.
Here it runs on the right-click event for any item in the tag, so a tool needs no code of its own,
and the held end lets go on the player tick, or as the tool is dropped. The event fires before
vanilla asks whether the player may build, so the gesture asks itself: a player in adventure or
spectator mode cannot wire. The default tag holds the copper ingot, since vanilla
has no copper cable. A pack replaces the tag to choose its own tools.

**The generators and accumulators are tags a pack fills.** ADR-0003 named Oritech's Steam Engine and
Large Energy Storage and the `factoryworks:` tags. Wireworks ships `wireworks:generators` and
`wireworks:accumulators` empty, and no generator or accumulator of its own beyond the creative pole.
What ADR-0003 says about Oritech's blocks, the mixins and the calibration is FactoryWorks', not
Wireworks'.

**A multiblock's part names its owner through `EnergyOwner` or `EnergyOwnerBlock`.** That is the API
a pack's multiblock implements, so a pole counts and feeds each machine once. A face a pole reaches
must journal its buffer through the transaction, since a pole measures room with an insert it then
aborts.

**Fast Replace is Groundworks'.** ADR-0002 and FactoryWorks ADR-0082 had the Pack swap a pole column
for another tier. Groundworks' ADR 0008 moves the click, the charge and the refund into Groundworks;
Wireworks gives the column builder, `PoleColumnReplace`, and keeps a column's wires across the swap,
but for those the new tier no longer reaches, which are cut. A swap that would join the column to one
of the new tier on or under it is refused, since one of the two would lose its base. A pack states which tiers form a group by passing the builder to `FastReplace.group`. Wireworks
states no group, so a group is never claimed before the pack's. Without one, a pole aimed at a column
of another tier is refused.

**The placement plan and its preview are Groundworks'** (FactoryWorks ADR-0069, Groundworks ADR 0001
and 0002). Wireworks plans the pole column through it and draws the supply area and the wires on its
preview event.

**Wireworks ships crafting recipes.** ADR-0002 had the pack emit the poles' recipes. A mod with no
pack needs its own, so Wireworks ships vanilla crafting recipes for the three tiers. A pack replaces
or removes them; the creative pole has none.

## Consequences

- The ids are `wireworks:`. FactoryWorks moves to them with no remap, since it is pre-release.
- FactoryWorks' Bindings are its config, its tags, the Pick in `wireworks:wire_tools`, its pole
  group, and the GameTests that hold them.
