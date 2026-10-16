# Clears a scene's plot, from ~2 to ~25 east and from ~-12 to ~0 south, 16 high, onto a stone floor:
# the item frames in it, then its blocks, then the items dropped, both those left lying and those the
# fills knock loose, such as the sign, whose floor goes before it does, and a Boiler's fuel. Each
# block goes as it would in a player's world, so a pole's wires go with it. structure_void first,
# which no scene places, so each fill changes a block.
execute align xyz positioned ~2 ~-1 ~-12 as @e[type=minecraft:item_frame,dx=23,dy=16,dz=12] run kill @s
fill ~2 ~-1 ~-12 ~25 ~15 ~ minecraft:structure_void
fill ~2 ~-1 ~-12 ~25 ~-1 ~ minecraft:stone
fill ~2 ~ ~-12 ~25 ~15 ~ minecraft:air
execute align xyz positioned ~2 ~-1 ~-12 as @e[type=minecraft:item,dx=23,dy=16,dz=12] run kill @s
