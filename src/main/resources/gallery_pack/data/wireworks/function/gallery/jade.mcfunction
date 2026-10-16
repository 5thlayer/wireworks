# Two small poles wired into one network, a Solar Panel in one's area and an Accumulator in the
# other's. Jade, which the gallery must not require, shows the network on a pole: its poles, production,
# consumption and accumulators.
function wireworks:gallery/clear
function wireworks:gallery/column {x:8,z:-6,pole:"small_pole"}
function wireworks:gallery/column {x:14,z:-6,pole:"small_pole"}
setblock ~6 ~ ~-6 wireworks:solar_panel[facing=north] strict
setblock ~5 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=1] strict
setblock ~6 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=2] strict
setblock ~7 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=3] strict
setblock ~5 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=4] strict
setblock ~6 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=5] strict
setblock ~7 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=6] strict
setblock ~5 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=7] strict
setblock ~6 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=8] strict
setblock ~7 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=9] strict
setblock ~16 ~ ~-4 wireworks:accumulator[facing=south] strict
setblock ~16 ~ ~-5 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~15 ~ ~-4 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~15 ~ ~-5 wireworks:accumulator_part[facing=south,part=3] strict
function wireworks:gallery/sign {scene:"jade"}
