#!/usr/bin/env python3
"""Generates the Assist Bank launcher icon.

The adaptive icon (API 26+) is hand-written vector XML under `res/drawable`;
this script produces the legacy PNG mipmaps that API 23–25 still uses, from
the SAME normalised geometry, so the two never drift apart.

    python3 tools/generate_launcher_icon.py

No third-party dependencies: the rasteriser is a supersampled point-in-shape
test and the PNG writer is zlib + struct, so the script runs on a stock
Python 3.

Glyph: a classical bank facade — pediment, entablature, four columns, plinth —
in white on the brand-blue tile.
"""

import os
import struct
import zlib

# Brand blue, top-left to bottom-right. The mid-tone is md_primary (#1F5BB7).
GRADIENT_LIGHT = (0x2E, 0x6C, 0xCB)
GRADIENT_DARK = (0x13, 0x40, 0x8C)
GLYPH_RGB = (0xFF, 0xFF, 0xFF)

# Fraction of the tile the glyph box occupies. The round icon is cropped to a
# circle, so its glyph sits a little smaller.
GLYPH_SCALE = {"square": 0.60, "round": 0.56}

CORNER_RADIUS = 0.2237  # of the tile edge — the Android squircle-ish rounding

# Glyph geometry, in the glyph box's own 0..1 coordinates. Kept in one place
# because res/drawable/ic_launcher_foreground.xml maps these same numbers into
# its 108-unit viewport.
PEDIMENT = ((0.5, 0.00), (0.0, 0.24), (1.0, 0.24))         # apex, base l, base r
ENTABLATURE = (0.04, 0.26, 0.96, 0.34)                      # x0, y0, x1, y1
COLUMNS_Y = (0.38, 0.80)
COLUMN_X = ((0.080, 0.215), (0.315, 0.450), (0.550, 0.685), (0.785, 0.920))
PLINTH = (0.00, 0.84, 1.00, 0.96)

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

SUPERSAMPLE = 4  # 4x4 samples per pixel

RES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")


def in_rect(x, y, x0, y0, x1, y1):
    return x0 <= x <= x1 and y0 <= y <= y1


def in_triangle(x, y, a, b, c):
    def side(p, q):
        return (q[0] - p[0]) * (y - p[1]) - (q[1] - p[1]) * (x - p[0])

    d1, d2, d3 = side(a, b), side(b, c), side(c, a)
    has_neg = d1 < 0 or d2 < 0 or d3 < 0
    has_pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (has_neg and has_pos)


def in_glyph(u, v):
    """`u`, `v` are coordinates inside the glyph box (0..1)."""
    if in_triangle(u, v, *PEDIMENT):
        return True
    if in_rect(u, v, *ENTABLATURE):
        return True
    if in_rect(u, v, *PLINTH):
        return True
    y0, y1 = COLUMNS_Y
    if y0 <= v <= y1:
        for x0, x1 in COLUMN_X:
            if x0 <= u <= x1:
                return True
    return False


def in_tile(x, y, shape):
    """Silhouette of the icon itself, in 0..1 tile coordinates."""
    if shape == "round":
        dx, dy = x - 0.5, y - 0.5
        return dx * dx + dy * dy <= 0.25
    r = CORNER_RADIUS
    if not (0.0 <= x <= 1.0 and 0.0 <= y <= 1.0):
        return False
    # Inside the cross of the rounded rect, or inside a corner disc.
    if r <= x <= 1 - r or r <= y <= 1 - r:
        return True
    cx = r if x < r else 1 - r
    cy = r if y < r else 1 - r
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def render(size, shape):
    """Returns RGBA rows for one icon."""
    scale = GLYPH_SCALE[shape]
    origin = (1.0 - scale) / 2.0
    ss = SUPERSAMPLE
    weight = 1.0 / (ss * ss)
    rows = []
    for py in range(size):
        row = bytearray()
        for px in range(size):
            tile_cov = 0.0
            glyph_cov = 0.0
            for sy in range(ss):
                y = (py + (sy + 0.5) / ss) / size
                for sx in range(ss):
                    x = (px + (sx + 0.5) / ss) / size
                    if in_tile(x, y, shape):
                        tile_cov += weight
                        u = (x - origin) / scale
                        v = (y - origin) / scale
                        if 0.0 <= u <= 1.0 and 0.0 <= v <= 1.0 and in_glyph(u, v):
                            glyph_cov += weight
            if tile_cov == 0.0:
                row += b"\x00\x00\x00\x00"
                continue
            # Background gradient, sampled at the pixel centre.
            t = ((px + 0.5) * 0.35 + (py + 0.5) * 0.65) / size
            bg = tuple(
                GRADIENT_LIGHT[i] + (GRADIENT_DARK[i] - GRADIENT_LIGHT[i]) * t
                for i in range(3)
            )
            # Glyph over background, then the whole tile against transparency.
            g = min(glyph_cov / tile_cov, 1.0)
            rgb = tuple(int(round(bg[i] + (GLYPH_RGB[i] - bg[i]) * g)) for i in range(3))
            row += bytes(rgb) + bytes((int(round(tile_cov * 255)),))
        rows.append(bytes(row))
    return rows


def write_png(path, rows):
    size = len(rows)
    raw = b"".join(b"\x00" + r for r in rows)

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    with open(path, "wb") as f:
        f.write(png)


def main():
    for density, size in DENSITIES.items():
        out = os.path.normpath(os.path.join(RES_DIR, "mipmap-" + density))
        os.makedirs(out, exist_ok=True)
        for name, shape in (("ic_launcher", "square"), ("ic_launcher_round", "round")):
            path = os.path.join(out, name + ".png")
            write_png(path, render(size, shape))
            print("wrote %s (%dpx)" % (path, size))


if __name__ == "__main__":
    main()
