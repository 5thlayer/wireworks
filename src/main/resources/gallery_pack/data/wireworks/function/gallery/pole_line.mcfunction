# A line of small poles, six blocks apart, inside one another's reach, so each wired itself to the
# last; each a column one segment taller than the last, raised by clicking it with its own pole. A
# held pole previews where it lands, through Groundworks. The line starts seven blocks in, out of
# reach of the Transmission scene's pole to the west.
function wireworks:gallery/clear
setblock ~7 ~ ~-6 wireworks:small_pole
setblock ~13 ~ ~-6 wireworks:small_pole
setblock ~13 ~1 ~-6 wireworks:small_pole
setblock ~19 ~ ~-6 wireworks:small_pole
setblock ~19 ~1 ~-6 wireworks:small_pole
setblock ~19 ~2 ~-6 wireworks:small_pole
function wireworks:gallery/sign {scene:"pole_line"}
