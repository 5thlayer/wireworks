#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT
"""Builds textures adapted from Malcolm Riley's unused textures in data/art/ (docs/placeholder-art.md).

The Boiler's front, cold and lit, is the smelter's let into the Boiler's plating, as Craftworks' Refiner front is: the brick
showing round the smelter's rim, in its top two rows and the outer two columns of its upper half, is
painted over with the plain plating, so the iron firebox and grate sit in a plated casing like the
Boiler's body. Which pixels are brick is read from the cold front: the lit one's glow warms its iron to a
brick's red. Every frame of the lit smelter's animation is built, and its animation copied beside the
sprite.

The Accumulator's sides are the battery block's with its left column, a light edge, painted over with
the column beside it. The Accumulator's model mirrors one block of each side, so that column only ever
shows where two blocks meet, and there it drew a seam down the middle of the side.

The Solar Panel's cell is blue_cement_center set in a frame of the plain plating, its outer ring, with a
dark line inside the frame, so each block of the 3x3 top reads as a panel of its own.

Run from the repository root; it needs Pillow. `--check` builds and diffs, and fails when a committed
texture is stale.
"""

import io
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
BLOCK = ROOT / "src" / "main" / "resources" / "assets" / "wireworks" / "textures" / "block"

# A sprite's side, and the height of each frame of its animation.
SIZE = 16
# The smelter's rim, where its brick shows: rows down to RIM_TOP, and the outer RIM_SIDE columns on each
# side down to RIM_UPPER, the bottom of the firebox; below it the grate's iron reaches the edge.
RIM_TOP = 1
RIM_SIDE = 2
RIM_UPPER = 7
# How much redder than blue a pixel is to be the smelter's brick, against the grey of its cold iron.
BRICK_RED = 18
# The line inside a solar cell's frame, a navy darker than the cell.
CELL_LINE = (30, 30, 58, 255)


def frames(name):
    image = Image.open(ART / f"{name}.png").convert("RGBA")
    # An animated sprite's frames stack downwards.
    return [image.crop((0, y, SIZE, y + SIZE)) for y in range(0, image.height, SIZE)]


def on_rim(x, y):
    return y <= RIM_TOP or (y <= RIM_UPPER and (x < RIM_SIDE or x >= SIZE - RIM_SIDE))


def brick(pixel):
    r, g, b, _ = pixel
    return r > g and r - b >= BRICK_RED


def brick_on_rim():
    cold = frames("device_smelter_front")[0].load()
    return [(x, y) for y in range(SIZE) for x in range(SIZE) if on_rim(x, y) and brick(cold[x, y])]


def boiler_front(smelter):
    plating = frames("space-plating-plain")[0].load()
    rim = brick_on_rim()
    built = frames(smelter)
    for frame in built:
        pixels = frame.load()
        for x, y in rim:
            pixels[x, y] = plating[x, y]
    strip = Image.new("RGBA", (SIZE, SIZE * len(built)))
    for i, frame in enumerate(built):
        strip.paste(frame, (0, SIZE * i))
    return strip


def accumulator_sides():
    sides = frames("battery_sides")[0]
    pixels = sides.load()
    for y in range(SIZE):
        pixels[0, y] = pixels[1, y]
    return sides


def ring(inset):
    """The pixels of the square ring inset this far from a sprite's edge."""
    last = SIZE - 1 - inset
    return {(x, y) for i in range(inset, last + 1) for x, y in ((i, inset), (i, last), (inset, i), (last, i))}


def solar_cell():
    cell = frames("blue_cement_center")[0]
    pixels = cell.load()
    plating = frames("space-plating-plain")[0].load()
    for x, y in ring(0):
        pixels[x, y] = plating[x, y]
    for x, y in ring(1):
        pixels[x, y] = CELL_LINE
    return cell


def png(image):
    out = io.BytesIO()
    image.save(out, format="PNG")
    return out.getvalue()


# Each built texture, its builder, and the animation copied beside it, if any.
TEXTURES = {
    BLOCK / "boiler" / "front_off.png": (lambda: boiler_front("device_smelter_front"), None),
    BLOCK / "boiler" / "front_on.png": (lambda: boiler_front("device_smelter_front_lit"), "device_smelter_front_lit.png.mcmeta"),
    BLOCK / "accumulator" / "sides.png": (accumulator_sides, None),
    BLOCK / "solar_panel" / "cell.png": (solar_cell, None),
}


def main():
    check = "--check" in sys.argv
    stale = False
    for path, (build, animation) in TEXTURES.items():
        image = png(build())
        meta = path.with_name(path.name + ".mcmeta")
        if check:
            current = path.is_file() and path.read_bytes() == image
            if animation:
                current = current and meta.is_file() and meta.read_bytes() == (ART / animation).read_bytes()
            print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-textures.py")
                  % path.relative_to(ROOT))
            stale = stale or not current
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(image)
        if animation:
            shutil.copyfile(ART / animation, meta)
        print(f"wrote {path.relative_to(ROOT)}")
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
