# A Boiler, fed coal by the hopper on top and holding a tank and a bucket of water, against the steam
# port of a Steam Engine behind it, which charges two Accumulators through a medium pole whose area
# reaches both. Both machines face the sign, and the Boiler's front shows what goes in. The scene
# burns its coal and water, and the Engine stops once the Accumulators are full: its sign builds it
# afresh.
function wireworks:gallery/clear
setblock ~13 ~ ~-4 wireworks:boiler[facing=south,lit=false]{Water:2000,Items:[{Slot:2b,id:"minecraft:water_bucket",count:1}]} strict
setblock ~14 ~ ~-4 wireworks:boiler_part[facing=south,part=1,lit=false] strict
setblock ~12 ~ ~-4 wireworks:boiler_part[facing=south,part=2,lit=false] strict
setblock ~14 ~ ~-5 wireworks:boiler_part[facing=south,part=3,lit=false] strict
setblock ~13 ~ ~-5 wireworks:boiler_part[facing=south,part=4,lit=false] strict
setblock ~12 ~ ~-5 wireworks:boiler_part[facing=south,part=5,lit=false] strict
setblock ~13 ~1 ~-4 minecraft:hopper[facing=down]{Items:[{Slot:0b,id:"minecraft:coal",count:64}]}
setblock ~13 ~ ~-6 wireworks:steam_engine[facing=south,running=false] strict
setblock ~13 ~1 ~-6 wireworks:steam_engine_part[facing=south,part=1,running=false] strict
setblock ~14 ~ ~-6 wireworks:steam_engine_part[facing=south,part=2,running=false] strict
setblock ~14 ~1 ~-6 wireworks:steam_engine_part[facing=south,part=3,running=false] strict
setblock ~17 ~ ~-7 wireworks:medium_pole
setblock ~19 ~ ~-4 wireworks:accumulator[facing=south] strict
setblock ~19 ~ ~-5 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~18 ~ ~-4 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~18 ~ ~-5 wireworks:accumulator_part[facing=south,part=3] strict
setblock ~19 ~ ~-8 wireworks:accumulator[facing=south] strict
setblock ~19 ~ ~-9 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~18 ~ ~-8 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~18 ~ ~-9 wireworks:accumulator_part[facing=south,part=3] strict
# What goes in, framed on the Boiler's front: the hopper's coal under it, water at a water port. No
# block_pos, which takes absolute coordinates only: summon logs that the frame has none, then sets it here.
summon minecraft:item_frame ~13 ~ ~-3 {Facing:3b,Fixed:1b,Invulnerable:1b,Item:{id:"minecraft:coal",count:1}}
summon minecraft:item_frame ~12 ~ ~-3 {Facing:3b,Fixed:1b,Invulnerable:1b,Item:{id:"minecraft:water_bucket",count:1}}
function wireworks:gallery/sign {scene:"steam"}
