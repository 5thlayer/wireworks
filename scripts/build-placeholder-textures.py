#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

"""Draws the Solar Panel's and the Accumulator's stand-in textures.

Both blocks wear this placeholder art until real art is drawn (docs/placeholder-art.md). Each
texture is sized to the model face it covers, so a face of 48 by 16 units is a 48 by 16 pixel
image and is never stretched.

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
TEXTURES = ROOT / "src/main/resources/assets/wireworks/textures/block"

FRAME = (118, 123, 130, 255)
FRAME_EDGE = (66, 70, 77, 255)
FRAME_LIGHT = (156, 161, 168, 255)
FRAME_RIVET = (190, 194, 199, 255)

PANEL = (14, 26, 66, 255)
PANEL_CELL = (22, 42, 96, 255)
PANEL_LINE = (58, 92, 168, 255)

CASING_CELL = (52, 58, 66, 255)
CASING_CELL_LIGHT = (74, 82, 92, 255)
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


def solar_panel_top():
    """Dark blue cells in a 3 by 3 grid of plates, a light line round each cell, inside a frame rim."""
    size = 48
    image = blank(size, size, PANEL)
    for y in range(size):
        for x in range(size):
            if x % 8 == 0 or y % 8 == 0:
                image[y][x] = PANEL_LINE
            elif (x // 8 + y // 8) % 2 == 0:
                image[y][x] = PANEL_CELL
    for i in range(size):
        for width in range(2):
            for edge in (width, size - 1 - width):
                image[edge][i] = FRAME_EDGE
                image[i][edge] = FRAME_EDGE
    return image


def accumulator_casing():
    """Top and bottom: a grey plate over the whole 2 by 2."""
    size = 32
    image = blank(size, size, FRAME)
    for ty in range(2):
        for tx in range(2):
            frame_tile(image, tx * 16, ty * 16)
    return image


def accumulator_side():
    """A 2 by 1 side: grey casing with a row of seven battery cells and their gold terminals."""
    width, height = 32, 16
    image = blank(width, height, FRAME)
    for i in range(width):
        image[0][i] = FRAME_EDGE
        image[height - 1][i] = FRAME_EDGE
        image[1][i] = FRAME_LIGHT
    for i in range(height):
        image[i][0] = FRAME_EDGE
        image[i][width - 1] = FRAME_EDGE
    for cell in range(7):
        x0 = 2 + cell * 4
        put_rect(image, x0, 4, x0 + 2, 13, CASING_CELL)
        put_rect(image, x0, 4, x0 + 1, 13, CASING_CELL_LIGHT)
        put_rect(image, x0, 3, x0 + 2, 4, CASING_TERMINAL)
    return image


TEXTURE_NAMES = {
    "solar_panel_frame.png": lambda: framed(16, 16),
    "solar_panel_edge.png": lambda: framed(48, 16),
    "solar_panel_underside.png": lambda: framed(48, 48),
    "solar_panel_top.png": solar_panel_top,
    "accumulator_casing.png": accumulator_casing,
    "accumulator_side.png": accumulator_side,
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
