# The Wireworks Gallery: a scene of each README feature, in README order, each building and clearing a
# plot of its own, 24 blocks wide and 13 deep (libworks ADR 0010). The scenes stand in rows of three,
# read west to east and then north, each row 16 blocks north of the one before, so the whole gallery
# stays near enough to where it is run for its machines to keep working. Each scene's poles stand far
# enough from the next scene's that no wire joins two scenes.
function wireworks:gallery/tiers
execute positioned ~24 ~ ~ run function wireworks:gallery/by_area
execute positioned ~48 ~ ~ run function wireworks:gallery/generation
execute positioned ~ ~ ~-16 run function wireworks:gallery/steam
execute positioned ~24 ~ ~-16 run function wireworks:gallery/reactor
execute positioned ~48 ~ ~-16 run function wireworks:gallery/wiring
execute positioned ~ ~ ~-32 run function wireworks:gallery/transmission
execute positioned ~24 ~ ~-32 run function wireworks:gallery/pole_line
execute positioned ~48 ~ ~-32 run function wireworks:gallery/jade
