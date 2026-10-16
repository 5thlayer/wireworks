# Four small poles in a ring, each wired, as it was placed, to the nearest poles in reach that are not
# already wired to each other: four wires, and none across the ring. The chest holds a copper ingot,
# which adds or cuts a wire by hand.
function wireworks:gallery/clear
function wireworks:gallery/column {x:6,z:-6,pole:"small_pole"}
function wireworks:gallery/column {x:11,z:-3,pole:"small_pole"}
function wireworks:gallery/column {x:11,z:-9,pole:"small_pole"}
function wireworks:gallery/column {x:16,z:-6,pole:"small_pole"}
setblock ~20 ~ ~-3 minecraft:chest[facing=south]{Items:[{Slot:13b,id:"minecraft:copper_ingot",count:1}]}
function wireworks:gallery/sign {scene:"wiring"}
