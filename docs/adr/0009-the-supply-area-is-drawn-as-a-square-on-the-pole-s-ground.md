---
status: accepted
amends: [5]
---

# The supply area is drawn as a square on the pole's ground

ADR 0005 drew a **Distribution Pole**'s **Supply Area** as its whole volume: one wireframe box, the
tier's square by the base's two blocks up and down. Its amendment added an outline around every
machine the pole reaches, and left open whether the box was still worth drawing. In play the box
reads as a cage: its vertical edges and its top face, at base+3, close in on a player who almost
always stands inside it. The machine outlines already answer which machines are in the band, so the
box's height bought nothing that the cage did not cost.

## Decision

**The area stays a volume; only the drawing changes.** The pole still supplies its tier's square two
blocks up and down from the base of its **Pole Column** (ADR 0002's band), and the scan, `covers` and
`bounds` are unchanged. This is a change to what a player sees, not to what is fed.

**The drawing is the square's outline, on the plane the base stands on.** That is the top of the
block under the base: the ground the player stands on and most machines sit on. It is the four
bottom edges of the old box, raised two blocks, so it keeps everything else ADR 0005 decided:
anchored at the base, the large pole's even side offset half a block, edges only, drawn through
terrain, one unchanging yellow for the square, the machine outlines and their cache, a placed pole
drawn only while looked at, and no square from the hand for a pole extending a column. The term is
now the **Supply Area Square**.

**Nothing draws the band.** With the cage gone, the area's reach above and below the ground shows
only once a machine there is outlined. The item tooltip already states it ("Reaches 2 blocks up and
down"). This closes ADR 0005's open question: the box is not dropped, it is flattened, since the
square is still the only thing that shows reach over ground with no machines on it yet.

**The machines are outlined in their role's colour.** This reverses ADR 0005's one colour for all
three roles. At placement the question is what feeds this area and what draws on it, and aiming at
each machine for its Jade line is too slow to answer it. The colours are the side-configuration
convention tech mods share, read from their source (`docs/research/side-config-colours.md`):
**Generator**s orange for output and the machines it feeds blue for input, in Thermal's shades,
`#EB6B00` and `#1EBEE7`; **Accumulator**s, which are both, purple, Mekanism's input/output colour
`#A460D9`. The square stays yellow, the area's own colour, so four colours each mean one thing. The
outlines stay edges only. The output orange is Thermal's rather than the wire tool's cut orange
(`#FF8C00`, ADR 0004), redder so the two read apart when both are on screen.

**A machine is outlined whole.** A Groundworks footprint is counted once, by its origin, so it is
outlined once, as the box around all its blocks, the size of the machine. Outlined by its origin's
block alone, a large machine looks small and a part reaching into the area points at nothing;
outlined block by block, it reads as several machines. A multiblock that names its owner through
`EnergyOwner` or `EnergyOwnerBlock` has no shape Wireworks can read, and is outlined by its owner's
block. This amends ADR 0005's rule that an owner is outlined where it stands, for footprints only.

## Considered options

- **The area becomes a surface**, a blanket following each column's ground. The drawing would then be
  the area exactly, which is ADR 0005's objection to surfaces removed. Rejected: it makes the area
  depend on terrain rather than on the tier alone, so laying a floor block can move a machine between
  **District**s; every column needs a rule for which ground it follows, and a layered build (a floor,
  something sunk into it, a platform above, a roof) loses whichever level the rule does not pick. And
  it reverses ADR 0002's band, a breaking change for every Consumer, to fix a drawing problem.
- **A tinted fill.** Drawn through terrain it washes over every machine standing in the square, which
  is ADR 0005's objection to filling the box. Depth-tested it z-fights the ground on flat terrain,
  sinks into a rising slope and floats over a falling one.
- **The square at the band's floor, base−2.** The true bottom of the area, but two blocks into the
  ground on flat terrain, where nothing the player builds stands.
- **Corner ticks for the band.** They start to rebuild the cage this ADR removes.
- **The accumulator in blue and orange together**, the way Thermal and Ender IO draw a face that is
  both. It is the stricter convention, but a two-tone outline reads as two machines on one block,
  and a single colour per role keeps one meaning per colour.
- **The accumulator in the square's yellow**, three colours rather than four. Yellow would then mean
  both the area's edge and an accumulator.
- **Green for the accumulator**, Titanium's "both ways". The wire tool's slack already means "wire"
  in green.
