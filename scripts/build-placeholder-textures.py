#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

"""Draws the stand-in textures of the Transmission Pole and the Transformer.

Each wears this placeholder art until real art is drawn (docs/placeholder-art.md). Each
texture is sized to the model face it covers, so none is stretched.

Run it after changing a drawing here:

    scripts/build-placeholder-textures.py

`--check` redraws and diffs, and fails when a committed texture is stale. No PIL: the writer below
is a minimal RGBA encoder.
"""

import pathlib
import struct
import sys
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/wireworks/textures"

FRAME = (118, 123, 130, 255)
FRAME_EDGE = (66, 70, 77, 255)
FRAME_LIGHT = (156, 161, 168, 255)
FRAME_RIVET = (190, 194, 199, 255)

CASING_CELL = (52, 58, 66, 255)
CASING_TERMINAL = (200, 168, 70, 255)



def blank(width, height, colour):
    return [[colour] * width for _ in range(height)]


def put_rect(image, x0, y0, x1, y1, colour):
    for y in range(y0, y1):
        for x in range(x0, x1):
            image[y][x] = colour


def frame_tile(image, x0, y0, size=16):
    """A grey plate with a dark rim, a light inner edge and a rivet in each corner."""
    put_rect(image, x0, y0, x0 + size, y0 + size, FRAME)
    for i in range(size):
        for edge in (0, size - 1):
            image[y0 + edge][x0 + i] = FRAME_EDGE
            image[y0 + i][x0 + edge] = FRAME_EDGE
    for i in range(1, size - 1):
        image[y0 + 1][x0 + i] = FRAME_LIGHT
        image[y0 + i][x0 + 1] = FRAME_LIGHT
    for cx, cy in ((3, 3), (size - 4, 3), (3, size - 4), (size - 4, size - 4)):
        image[y0 + cy][x0 + cx] = FRAME_RIVET


def framed(width, height):
    image = blank(width, height, FRAME)
    for ty in range(height // 16):
        for tx in range(width // 16):
            frame_tile(image, tx * 16, ty * 16)
    return image


def transmission_pole():
    """A grey steel lattice: a plate with a dark rim and a diagonal brace, for the post and its crossarm."""
    image = framed(16, 16)
    for i in range(2, 14):
        image[i][i] = FRAME_EDGE
        image[i][15 - i] = FRAME_EDGE
    return image


def transformer():
    """A grey casing with two gold coil windings side by side, for the Transformer's body."""
    image = framed(16, 16)
    for x0 in (3, 9):
        put_rect(image, x0, 3, x0 + 4, 13, CASING_CELL)
        for y in range(4, 13, 2):
            put_rect(image, x0, y, x0 + 4, y + 1, CASING_TERMINAL)
    return image


TEXTURE_NAMES = {
    "block/transformer.png": transformer,
    "block/transmission_pole.png": transmission_pole,
}


def png_bytes(rows):
    height, width = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)

    def chunk(tag, body):
        block = tag + body
        return struct.pack(">I", len(body)) + block + struct.pack(">I", zlib.crc32(block))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def main():
    check = "--check" in sys.argv
    stale = False
    for name, draw in TEXTURE_NAMES.items():
        path = TEXTURES / name
        path.parent.mkdir(parents=True, exist_ok=True)
        image = png_bytes(draw())
        if check:
            if image != (path.read_bytes() if path.is_file() else b""):
                print("FAIL %s is stale -- re-run scripts/build-placeholder-textures.py"
                      % path.relative_to(ROOT))
                stale = True
            else:
                print("ok   %s is current" % path.relative_to(ROOT))
        else:
            path.write_bytes(image)
            print("wrote %s" % path.relative_to(ROOT))
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
