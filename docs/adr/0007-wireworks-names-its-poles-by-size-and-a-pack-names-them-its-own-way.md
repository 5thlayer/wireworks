---
status: accepted
amends: [6]
---

# Wireworks names its poles by size, and a pack names them its own way

Wireworks carried Factorio's names out of the Pack: the Small Electric Pole, the Medium Electric
Pole and the Substation, with the ids `small_electric_pole`, `medium_electric_pole`,
`substation_electric_pole` and `creative_electric_pole`. Those are the Pack's names. A mod any pack
can use ships names that say what a pole is and nothing a pack would have to contradict.

**The tiers are small, medium and large.** One word per tier names the pole and everything behind
it: the Small Pole, the Medium Pole and the Large Pole, with the ids `small_pole`, `medium_pole` and
`large_pole`, the `PoleTier` constants, and the sections of `wireworks-server.toml`. The creative
pole is the Creative Pole, `creative_pole`. "Electric" leaves the names; the **Electric Network**
stays a domain term, and Jade's toggle reads "Pole network".

**A pack names the poles its own way**, in its own language file, over Wireworks' keys. The
FactoryWorks Pack names them after Factorio again.

**The ids change with no remap.** 0.1.0 had one download on CurseForge and was still in review on
Modrinth, so no world worth a remap holds the old ids. Under ADR 0001 the change is breaking, and
its release is 0.2.0.

- *Considered: Factorio's names as Wireworks' default.* Rejected: the Library is after Factorio's
  poles, not its catalogue, and a pack that plays something else would have to rename every pole.
- *Considered: new names over the old ids.* Rejected: an id that says `substation` behind a Large
  Pole is two names for one tier, and the ids are cheapest to change before anyone holds them.
- *Considered: names by material (Wooden, Iron, Steel) or by role (Transmission Tower).* Rejected:
  a material promises a recipe a pack may not use, and a role name says less than a size.

## Consequences

- A world made with 0.1.0 loses its poles. A `wireworks-server.toml` that sets `[substation]` no
  longer reaches the large pole, which takes its defaults until the pack renames the section
  `[large]`.
- The FactoryWorks Pack moves to the new ids and adds its Factorio names in the same release-train
  step: its recipes, the `buildings` tag, `replace_groups.json`, `item-map.json`, the EMI index,
  its config and the tests that hold them.
