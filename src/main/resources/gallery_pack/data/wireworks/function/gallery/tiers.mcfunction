# A pole of each tier, small to large, and the creative pole, four blocks apart, which wire themselves
# into one network as they are placed: each a shorter reach than the next, the last generating
# without limit.
function wireworks:gallery/clear
setblock ~5 ~ ~-6 wireworks:small_pole
setblock ~9 ~ ~-6 wireworks:medium_pole
setblock ~13 ~ ~-6 wireworks:large_pole
setblock ~17 ~ ~-6 wireworks:creative_pole
function wireworks:gallery/sign {scene:"tiers"}
