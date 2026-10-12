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

The Steam Turbine's still fan is the first frame of the duct fan's animation, which the running fan
wears whole. Its coil side is the copper coil's turned half its dark band over, so the band's darker
half runs down the left edge, and then its left half mirrored onto its right: each block has the same
dark half band down either edge, which meet whole where two blocks do, and the coil's shine sits in the
middle, so a face three blocks wide reads the same from either end.

The Steam Engine's front and back draw one engine side-on across its four blocks: a copper cylinder
with its steam chest and pipe, the piston rod into a crosshead in its guides, the connecting rod to a
crank on a spoked flywheel, all on a bed plate, in a recess let into the iron duct's plain panel. It is
drawn here, in shades taken from the duct and from the copper tank's side, and cut into each block's
quarter. The running engine turns its flywheel once over sixteen frames, the crosshead sliding with
the crank; the still one is the first frame.

Run from the repository root; it needs Pillow. `--check` builds and diffs, and fails when a committed
texture is stale.
"""

import io
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance

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
# Half the width of the dark band down the copper coil side's left edge.
COIL_BAND_HALF = 2
# The line inside a solar cell's frame, a navy darker than the cell.
CELL_LINE = (30, 30, 58, 255)
# The Steam Engine's front, two blocks square, and how many frames its flywheel turns once in.
ENGINE = 2 * SIZE
ENGINE_FRAMES = 16
# The crank's angle on the still engine, a little past level so the rods read as two.
ENGINE_STILL = 0.6
# The width of the casing round the engine's recess, and how dark the recess is against it.
ENGINE_FRAME = 2
ENGINE_RECESS = 0.35
# The cylinder's box, and the flywheel's centre, radius, crank and connecting rod.
CYLINDER = (3, 12, 11, 21)
WHEEL = (24, 16)
WHEEL_RADIUS = 7
CRANK = 3.0
CONNECTING_ROD = 8.0
# The piston and connecting rods, bright steel against the dark recess.
PISTON_ROD = (222, 222, 214, 255)
ROD = (200, 200, 192, 255)
# The running engine's animation: a frame a tick, so it turns about 75 times a minute.
ENGINE_ANIMATION = b'{\n\t"animation": {\n\t\t"frametime": 1\n\t}\n}\n'


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


def still_fan():
    return frames("industrial_metal_duct_fan")[0]


def coil_side():
    coil = frames("copper_coil_side")[0]
    turned = Image.new("RGBA", (SIZE, SIZE))
    turned.paste(coil.crop((COIL_BAND_HALF, 0, SIZE, SIZE)), (0, 0))
    turned.paste(coil.crop((0, 0, COIL_BAND_HALF, SIZE)), (SIZE - COIL_BAND_HALF, 0))
    left = turned.crop((0, 0, SIZE // 2, SIZE))
    turned.paste(left.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (SIZE // 2, 0))
    return turned


def shades(name, count):
    """Count of the sprite's colours, darkest to lightest, evenly picked."""
    sprite = frames(name)[0]
    pixels = sprite.load()
    seen = {pixels[x, y] for y in range(SIZE) for x in range(SIZE) if pixels[x, y][3] > 0}
    ordered = sorted(seen, key=lambda p: (0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2], p))
    return [ordered[i * (len(ordered) - 1) // (count - 1)] for i in range(count)]


def engine_front(theta):
    """The Steam Engine's front at this crank angle: the cylinder on the left, the flywheel on the right."""
    iron = shades("industrial_metal_duct_flat_1", 6)
    copper = shades("metal_tank_copper_sides", 6)
    panel = frames("industrial_metal_duct_flat_2")[0]
    image = Image.new("RGBA", (ENGINE, ENGINE))
    for x in (0, SIZE):
        for y in (0, SIZE):
            image.paste(panel, (x, y))
    inner = (ENGINE_FRAME, ENGINE_FRAME, ENGINE - ENGINE_FRAME, ENGINE - ENGINE_FRAME)
    image.paste(ImageEnhance.Brightness(image.crop(inner)).enhance(ENGINE_RECESS), inner[:2])
    draw = ImageDraw.Draw(image)
    pixels = image.load()
    last = ENGINE - ENGINE_FRAME - 1

    # The bed plate.
    draw.rectangle((ENGINE_FRAME, 25, last, last), fill=iron[2])
    draw.line((ENGINE_FRAME, 25, last, 25), fill=iron[4])
    draw.line((ENGINE_FRAME, last, last, last), fill=iron[1])

    # The cylinder, banded light to dark down its round body, with an iron flange at each end, its
    # rivets, its feet, and the steam chest on top with its pipe up out of the casing.
    x0, y0, x1, y1 = CYLINDER
    bands = (0.15, 0.35, 0.6, 0.85)
    for y in range(y0, y1 + 1):
        t = (y - y0) / (y1 - y0)
        draw.line((x0, y, x1, y), fill=copper[5 - sum(t >= b for b in bands)])
    for x in (x0, x1):
        draw.line((x, y0 - 1, x, y1 + 1), fill=iron[3])
        pixels[x, y0 - 1] = iron[5]
    for x in (x0 + 4, x0 + 7):
        for y in range(y0 + 1, y1, 3):
            pixels[x, y] = copper[0]
    draw.rectangle((5, y1 + 1, 6, 24), fill=iron[2])
    draw.rectangle((9, y1 + 1, 10, 24), fill=iron[2])
    draw.rectangle((5, y0 - 3, 9, y0 - 1), fill=copper[2])
    draw.line((5, y0 - 3, 9, y0 - 3), fill=copper[4])
    draw.rectangle((6, ENGINE_FRAME, 7, y0 - 4), fill=copper[3])
    draw.line((6, ENGINE_FRAME, 6, y0 - 4), fill=copper[5])

    # The crosshead's guides, and the flywheel's stand behind it.
    cx, cy = WHEEL
    draw.line((x1 + 1, cy - 1, 18, cy - 1), fill=iron[1])
    draw.line((x1 + 1, cy + 2, 18, cy + 2), fill=iron[1])
    draw.polygon([(20, 24), (26, 24), (cx, cy), (cx - 2, cy)], fill=iron[2])
    draw.line((cx - 2, cy, 20, 24), fill=iron[3])

    # The flywheel: a rim lit from the top left, six spokes turned to theta, and the hub.
    rim = WHEEL_RADIUS - 1.8
    for y in range(ENGINE):
        for x in range(ENGINE):
            r = math.hypot(x - cx, y - cy)
            if rim < r <= WHEEL_RADIUS + 0.5:
                lit = (y - cy) < -(x - cx) * 0.5 and r > WHEEL_RADIUS - 0.8
                pixels[x, y] = iron[5] if lit else iron[4] if r > WHEEL_RADIUS - 0.9 else iron[3]
            elif r < 1.6:
                pixels[x, y] = iron[5]
            elif r <= rim:
                off = (math.atan2(y - cy, x - cx) - theta + math.pi / 6) % (math.pi / 3) - math.pi / 6
                if r * abs(math.sin(off)) < 0.75:
                    pixels[x, y] = iron[4]

    # The crank pin, the connecting rod back to the crosshead, and the piston rod into the cylinder.
    pin = (round(cx + CRANK * math.cos(theta)), round(cy + CRANK * math.sin(theta)))
    head = round(cx + CRANK * math.cos(theta) - math.sqrt(CONNECTING_ROD ** 2 - (CRANK * math.sin(theta)) ** 2))
    draw.line((x1 + 1, cy, head, cy), fill=PISTON_ROD)
    draw.rectangle((head - 1, cy - 1, head, cy + 1), fill=copper[4])
    draw.line((head, cy, *pin), fill=ROD)
    pixels[pin] = copper[5]
    return image


def engine_quarter(column, row, running):
    """One block's quarter of the engine's front, column 0 the cylinder's and row 0 the top; running, every frame."""
    turns = range(ENGINE_FRAMES) if running else range(1)
    box = (column * SIZE, row * SIZE, column * SIZE + SIZE, row * SIZE + SIZE)
    built = [engine_front(ENGINE_STILL + 2 * math.pi * i / ENGINE_FRAMES).crop(box) for i in turns]
    strip = Image.new("RGBA", (SIZE, SIZE * len(built)))
    for i, frame in enumerate(built):
        strip.paste(frame, (0, SIZE * i))
    return strip


def png(image):
    out = io.BytesIO()
    image.save(out, format="PNG")
    return out.getvalue()


# Each built texture, its builder, and the animation beside it, if any: copied from data/art/, or written.
TEXTURES = {
    BLOCK / "boiler" / "front_off.png": (lambda: boiler_front("device_smelter_front"), None),
    BLOCK / "boiler" / "front_on.png": (lambda: boiler_front("device_smelter_front_lit"), "device_smelter_front_lit.png.mcmeta"),
    BLOCK / "accumulator" / "sides.png": (accumulator_sides, None),
    BLOCK / "solar_panel" / "cell.png": (solar_cell, None),
    BLOCK / "steam_turbine" / "fan_off.png": (still_fan, None),
    BLOCK / "steam_turbine" / "coil_side.png": (coil_side, None),
}
for name, column, row in (("cylinder_top", 0, 0), ("cylinder_bottom", 0, 1), ("wheel_top", 1, 0), ("wheel_bottom", 1, 1)):
    TEXTURES[BLOCK / "steam_engine" / f"{name}.png"] = (lambda c=column, r=row: engine_quarter(c, r, False), None)
    TEXTURES[BLOCK / "steam_engine" / f"{name}_on.png"] = (lambda c=column, r=row: engine_quarter(c, r, True), ENGINE_ANIMATION)


def main():
    check = "--check" in sys.argv
    stale = False
    for path, (build, animation) in TEXTURES.items():
        image = png(build())
        meta = path.with_name(path.name + ".mcmeta")
        animation = animation if animation is None or isinstance(animation, bytes) else (ART / animation).read_bytes()
        if check:
            current = path.is_file() and path.read_bytes() == image
            if animation:
                current = current and meta.is_file() and meta.read_bytes() == animation
            print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-textures.py")
                  % path.relative_to(ROOT))
            stale = stale or not current
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(image)
        if animation:
            meta.write_bytes(animation)
        print(f"wrote {path.relative_to(ROOT)}")
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
