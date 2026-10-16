# Four small poles in a ring, each wired, as it was placed, to the nearest poles in reach that are not
# already wired to each other: four wires, and none across the ring. The chest holds a copper ingot,
# which adds or cuts a wire by hand.
function wireworks:gallery/clear
setblock ~6 ~ ~-6 wireworks:small_pole
setblock ~11 ~ ~-3 wireworks:small_pole
setblock ~11 ~ ~-9 wireworks:small_pole
setblock ~16 ~ ~-6 wireworks:small_pole
setblock ~20 ~ ~-3 minecraft:chest[facing=south]{Items:[{Slot:13b,id:"minecraft:copper_ingot",count:1}]}
function wireworks:gallery/sign {scene:"wiring"}
