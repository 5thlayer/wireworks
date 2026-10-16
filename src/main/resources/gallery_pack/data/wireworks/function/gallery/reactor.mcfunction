# A Reactor, loaded with a Fuel Cell and holding a tank and a bucket of water, against the row end of
# a Steam Turbine behind it, which its superheated steam port feeds with no pipe; the Turbine charges
# two Accumulators through a medium pole whose area reaches both. Both machines face the sign, and the
# Reactor's front shows what goes in. The Reactor stops once its steam has nowhere to go, the
# Accumulators full: the sign builds the scene afresh.
function wireworks:gallery/clear
setblock ~13 ~ ~-4 wireworks:reactor[facing=south]{Water:10000,Items:[{Slot:0b,id:"wireworks:fuel_cell",count:1},{Slot:2b,id:"minecraft:water_bucket",count:1}]} strict
setblock ~14 ~ ~-3 wireworks:reactor_part[facing=south,part=1] strict
setblock ~13 ~ ~-3 wireworks:reactor_part[facing=south,part=2] strict
setblock ~12 ~ ~-3 wireworks:reactor_part[facing=south,part=3] strict
setblock ~14 ~ ~-4 wireworks:reactor_part[facing=south,part=4] strict
setblock ~12 ~ ~-4 wireworks:reactor_part[facing=south,part=5] strict
setblock ~14 ~ ~-5 wireworks:reactor_part[facing=south,part=6] strict
setblock ~13 ~ ~-5 wireworks:reactor_part[facing=south,part=7] strict
setblock ~12 ~ ~-5 wireworks:reactor_part[facing=south,part=8] strict
setblock ~14 ~1 ~-3 wireworks:reactor_part[facing=south,part=9] strict
setblock ~13 ~1 ~-3 wireworks:reactor_part[facing=south,part=10] strict
setblock ~12 ~1 ~-3 wireworks:reactor_part[facing=south,part=11] strict
setblock ~14 ~1 ~-4 wireworks:reactor_part[facing=south,part=12] strict
setblock ~13 ~1 ~-4 wireworks:reactor_part[facing=south,part=13] strict
setblock ~12 ~1 ~-4 wireworks:reactor_part[facing=south,part=14] strict
setblock ~14 ~1 ~-5 wireworks:reactor_part[facing=south,part=15] strict
setblock ~13 ~1 ~-5 wireworks:reactor_part[facing=south,part=16] strict
setblock ~12 ~1 ~-5 wireworks:reactor_part[facing=south,part=17] strict
setblock ~14 ~2 ~-3 wireworks:reactor_part[facing=south,part=18] strict
setblock ~13 ~2 ~-3 wireworks:reactor_part[facing=south,part=19] strict
setblock ~12 ~2 ~-3 wireworks:reactor_part[facing=south,part=20] strict
setblock ~14 ~2 ~-4 wireworks:reactor_part[facing=south,part=21] strict
setblock ~13 ~2 ~-4 wireworks:reactor_part[facing=south,part=22] strict
setblock ~12 ~2 ~-4 wireworks:reactor_part[facing=south,part=23] strict
setblock ~14 ~2 ~-5 wireworks:reactor_part[facing=south,part=24] strict
setblock ~13 ~2 ~-5 wireworks:reactor_part[facing=south,part=25] strict
setblock ~12 ~2 ~-5 wireworks:reactor_part[facing=south,part=26] strict
setblock ~13 ~ ~-6 wireworks:steam_turbine[facing=south,running=false] strict
setblock ~14 ~ ~-6 wireworks:steam_turbine_part[facing=south,part=1] strict
setblock ~12 ~ ~-6 wireworks:steam_turbine_part[facing=south,part=2] strict
setblock ~14 ~1 ~-6 wireworks:steam_turbine_part[facing=south,part=3] strict
setblock ~13 ~1 ~-6 wireworks:steam_turbine_part[facing=south,part=4] strict
setblock ~12 ~1 ~-6 wireworks:steam_turbine_part[facing=south,part=5] strict
function wireworks:gallery/column {x:17,z:-8,pole:"medium_pole"}
setblock ~19 ~ ~-6 wireworks:accumulator[facing=south] strict
setblock ~19 ~ ~-7 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~18 ~ ~-6 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~18 ~ ~-7 wireworks:accumulator_part[facing=south,part=3] strict
setblock ~19 ~ ~-10 wireworks:accumulator[facing=south] strict
setblock ~19 ~ ~-11 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~18 ~ ~-10 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~18 ~ ~-11 wireworks:accumulator_part[facing=south,part=3] strict
# What goes in, framed on the Reactor's front: the Fuel Cell, and water at its front water port. No
# block_pos, which takes absolute coordinates only: summon logs that the frame has none, then sets it here.
summon minecraft:item_frame ~13 ~1 ~-2 {Facing:3b,Fixed:1b,Invulnerable:1b,Item:{id:"wireworks:fuel_cell",count:1}}
summon minecraft:item_frame ~13 ~ ~-2 {Facing:3b,Fixed:1b,Invulnerable:1b,Item:{id:"minecraft:water_bucket",count:1}}
function wireworks:gallery/sign {scene:"reactor"}
