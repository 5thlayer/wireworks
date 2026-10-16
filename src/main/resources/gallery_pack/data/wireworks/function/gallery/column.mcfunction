# A pole column at its full height, PoleColumn.MAX_SEGMENTS: its base at ~$(x) ~ ~$(z), placed first so it
# wires itself, then four segments, bottom up, each extending it as clicking it with its own pole
# would. Only the Pole Line scene, which shows a column raised, builds shorter ones.
$setblock ~$(x) ~ ~$(z) wireworks:$(pole)
$setblock ~$(x) ~1 ~$(z) wireworks:$(pole)
$setblock ~$(x) ~2 ~$(z) wireworks:$(pole)
$setblock ~$(x) ~3 ~$(z) wireworks:$(pole)
$setblock ~$(x) ~4 ~$(z) wireworks:$(pole)
