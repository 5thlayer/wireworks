# A scene's sign at ~14 ~ ~0, facing south, waxed. Its name is in bold, and clicking it runs the
# scene's function again from the plot's corner, 14 blocks west of the sign, which builds it afresh.
$setblock ~14 ~ ~ minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{translate:"wireworks.gallery.$(scene)",bold:true,click_event:{action:"run_command",command:"execute positioned ~-14 ~ ~ run function wireworks:gallery/$(scene)"}},{translate:"wireworks.gallery.$(scene).1"},{translate:"wireworks.gallery.$(scene).2"},{translate:"wireworks.gallery.$(scene).3"}]}}
