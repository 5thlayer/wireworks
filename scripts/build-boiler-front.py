#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT
"""Builds the Boiler's front from Malcolm Riley's unused textures in data/art/ (docs/placeholder-art.md).

The front is the lit smelter's let into the Boiler's plating, as Craftworks' Refiner front is: the brick
showing round the smelter's rim, in its top two rows and the outer two columns of its upper half, is
painted over with the plain plating, so the iron firebox and grate sit in a plated casing like the
Boiler's body. Which pixels are brick is read from the cold front: the lit one's glow warms its iron to a
brick's red. Every frame of the lit smelter's animation is built, and its animation copied beside the
sprite.

Run from the repository root; it needs Pillow. `--check` builds and diffs, and fails when the committed
front is stale.
"""

import io
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
FRONT = ROOT / "src" / "main" / "resources" / "assets" / "wireworks" / "textures" / "block" / "boiler" / "front.png"

# A sprite's side, and the height of each frame of its animation.
SIZE = 16
# The smelter's rim, where its brick shows: rows down to RIM_TOP, and the outer RIM_SIDE columns on each
# side down to RIM_UPPER, the bottom of the firebox; below it the grate's iron reaches the edge.
RIM_TOP = 1
RIM_SIDE = 2
RIM_UPPER = 7
# How much redder than blue a pixel is to be the smelter's brick, against the grey of its cold iron.
BRICK_RED = 18


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


def boiler_front():
    plating = frames("space-plating-plain")[0].load()
    rim = brick_on_rim()
    built = frames("device_smelter_front_lit")
    for frame in built:
        pixels = frame.load()
        for x, y in rim:
            pixels[x, y] = plating[x, y]
    strip = Image.new("RGBA", (SIZE, SIZE * len(built)))
    for i, frame in enumerate(built):
        strip.paste(frame, (0, SIZE * i))
    out = io.BytesIO()
    strip.save(out, format="PNG")
    return out.getvalue()


def main():
    image = boiler_front()
    animation = ART / "device_smelter_front_lit.png.mcmeta"
    meta = FRONT.with_name(FRONT.name + ".mcmeta")
    if "--check" in sys.argv:
        current = FRONT.is_file() and FRONT.read_bytes() == image
        current = current and meta.is_file() and meta.read_bytes() == animation.read_bytes()
        print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-boiler-front.py")
              % FRONT.relative_to(ROOT))
        return 0 if current else 1
    FRONT.parent.mkdir(parents=True, exist_ok=True)
    FRONT.write_bytes(image)
    shutil.copyfile(animation, meta)
    print(f"wrote {FRONT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
