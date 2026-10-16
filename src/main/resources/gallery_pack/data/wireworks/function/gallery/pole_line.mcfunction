# A line of small poles, seven blocks apart, inside one another's reach, so each wired itself to the
# last; each a column one segment taller than the last, raised by clicking it with its own pole. A
# held pole previews where it lands, through Groundworks.
function wireworks:gallery/clear
setblock ~5 ~ ~-6 wireworks:small_pole
setblock ~12 ~ ~-6 wireworks:small_pole
setblock ~12 ~1 ~-6 wireworks:small_pole
setblock ~19 ~ ~-6 wireworks:small_pole
setblock ~19 ~1 ~-6 wireworks:small_pole
setblock ~19 ~2 ~-6 wireworks:small_pole
function wireworks:gallery/sign {scene:"pole_line"}
