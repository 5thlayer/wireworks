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
pack that wants Factorio's figures pinned states them in its own config and tests that file.

- *Considered: tiers as datapack entries.* Rejected: a block needs its tier at registration, before a
  datapack loads, so only the figures could be data, and a config already carries figures. Beltworks
  and Craftworks configure through server configs too.

**Any item in `wireworks:wire_tools` wires poles.** ADR-0004 gave the gesture to the Engineer's Pick.
Here it runs on the right-click event for any item in the tag, so a tool needs no code of its own,
and the held end lets go on the player tick. The event fires before
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
and states a default group, `wireworks:poles`: small, medium and large, without the creative pole.
A block belongs to the first group stated that claims it, so Wireworks states its default at common
setup, once every mod is constructed. A pack that passes the
builder to `FastReplace.group` at its own construction claims first, and its group replaces the
default for each tier it holds; a tier it leaves out stays in the default, alone among the tiers the
pack took. The FactoryWorks Pack groups small and medium and so keeps the large pole apart. A pole
aimed at a column no group shares with it, the creative pole's or another group's, is refused.

- *Considered: a tag for the default's members, as `wireworks:wire_tools` is.* Not needed: a pack's
  group already takes the tiers it names, and a pack that wants a tier in no group can state it
  alone.
- *Considered: a way in Groundworks for a later group to supersede an earlier one.* Not needed while
  stating last is enough. A pack that states its group after construction races the default, and so
  does a pole put in one of Groundworks' `replace_group` tags, which Groundworks states at common
  setup too: a pack groups its poles in code.

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
