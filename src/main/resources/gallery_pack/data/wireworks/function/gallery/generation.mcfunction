# A Solar Panel and an Accumulator in a medium pole's area: the panel makes power by day, the
# Accumulator stores what nothing draws and gives it back at night.
function wireworks:gallery/clear
function wireworks:gallery/column {x:14,z:-6,pole:"medium_pole"}
setblock ~11 ~ ~-6 wireworks:solar_panel[facing=north] strict
setblock ~10 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=1] strict
setblock ~11 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=2] strict
setblock ~12 ~1 ~-7 wireworks:solar_panel_part[facing=north,part=3] strict
setblock ~10 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=4] strict
setblock ~11 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=5] strict
setblock ~12 ~1 ~-6 wireworks:solar_panel_part[facing=north,part=6] strict
setblock ~10 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=7] strict
setblock ~11 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=8] strict
setblock ~12 ~1 ~-5 wireworks:solar_panel_part[facing=north,part=9] strict
setblock ~16 ~ ~-6 wireworks:accumulator[facing=north] strict
setblock ~16 ~ ~-5 wireworks:accumulator_part[facing=north,part=1] strict
setblock ~17 ~ ~-6 wireworks:accumulator_part[facing=north,part=2] strict
setblock ~17 ~ ~-5 wireworks:accumulator_part[facing=north,part=3] strict
function wireworks:gallery/sign {scene:"generation"}
