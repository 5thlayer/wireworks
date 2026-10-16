# A creative pole and four Accumulators in its area, each facing another way, all charging from it with
# no wire or cable to any. Wireworks has no machine of its own that only takes FE, and the gallery
# requires no other mod; an Accumulator takes FE as any machine would.
function wireworks:gallery/clear
function wireworks:gallery/column {x:16,z:-6,pole:"creative_pole"}
setblock ~9 ~ ~-6 wireworks:accumulator[facing=north] strict
setblock ~9 ~ ~-5 wireworks:accumulator_part[facing=north,part=1] strict
setblock ~10 ~ ~-6 wireworks:accumulator_part[facing=north,part=2] strict
setblock ~10 ~ ~-5 wireworks:accumulator_part[facing=north,part=3] strict
setblock ~21 ~ ~-6 wireworks:accumulator[facing=east] strict
setblock ~20 ~ ~-6 wireworks:accumulator_part[facing=east,part=1] strict
setblock ~21 ~ ~-5 wireworks:accumulator_part[facing=east,part=2] strict
setblock ~20 ~ ~-5 wireworks:accumulator_part[facing=east,part=3] strict
setblock ~16 ~ ~-3 wireworks:accumulator[facing=south] strict
setblock ~16 ~ ~-4 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~15 ~ ~-3 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~15 ~ ~-4 wireworks:accumulator_part[facing=south,part=3] strict
setblock ~15 ~ ~-10 wireworks:accumulator[facing=west] strict
setblock ~16 ~ ~-10 wireworks:accumulator_part[facing=west,part=1] strict
setblock ~15 ~ ~-11 wireworks:accumulator_part[facing=west,part=2] strict
setblock ~16 ~ ~-11 wireworks:accumulator_part[facing=west,part=3] strict
function wireworks:gallery/sign {scene:"by_area"}
