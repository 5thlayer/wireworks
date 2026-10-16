# Two Districts on one line: a creative pole's in the west, and a small pole's in the east with an
# Accumulator beyond the creative pole's area, which charges from it across three Transmission Poles,
# each District joined to the line by a Transformer. The Distribution Poles go first and the
# Transformers last, so each Transformer wires itself to the nearest pole of each system: the
# creative pole stands within a Transformer's reach of both.
function wireworks:gallery/clear
setblock ~11 ~ ~-6 wireworks:creative_pole
setblock ~23 ~ ~-4 wireworks:small_pole
setblock ~22 ~ ~-3 wireworks:accumulator[facing=south] strict
setblock ~22 ~ ~-4 wireworks:accumulator_part[facing=south,part=1] strict
setblock ~21 ~ ~-3 wireworks:accumulator_part[facing=south,part=2] strict
setblock ~21 ~ ~-4 wireworks:accumulator_part[facing=south,part=3] strict
setblock ~6 ~ ~-10 wireworks:transmission_pole
setblock ~12 ~ ~-10 wireworks:transmission_pole
setblock ~18 ~ ~-10 wireworks:transmission_pole
setblock ~5 ~ ~-6 wireworks:transformer
setblock ~19 ~ ~-6 wireworks:transformer
function wireworks:gallery/sign {scene:"transmission"}
