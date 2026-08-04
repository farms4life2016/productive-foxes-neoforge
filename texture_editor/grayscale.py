#!/usr/bin/env python3
"""
Grayscale Minecraft texture PNGs in place so a BlockColor/ItemColor tint
shows up as a clean, saturated colour instead of muddy multiply blending.

Operates on RGBA PNGs. Fully-transparent pixels are left alone; every
opaque/translucent pixel is converted to grayscale (luminance), keeping
the original alpha so cutout shapes are preserved.

Usage:
    python3.11 grayscale.py <png> [<png> ...]
    python3.11 grayscale.py <dir>       # grayscales every *.png in the dir

Requires Pillow:  pip3.11 install Pillow
"""
import sys
from pathlib import Path

from PIL import Image


def grayscale_png(path: Path) -> None:
    img = Image.open(path)
    if img.mode != "RGBA":
        img = img.convert("RGBA")

    r, g, b, a = img.split()
    # ponytail: ITU-R BT.601 luma weights — close enough to perceived brightness
    # for 16x16 game art; a fancier perceptual luma isn't worth the bytes here.
    gray = Image.merge("RGB", (r, g, b)).convert("L")
    recolored = Image.merge("RGBA", (gray, gray, gray, a))
    recolored.save(path, optimize=True)
    print(f"grayscaled: {path}")


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 1

    files: list[Path] = []
    for arg in argv[1:]:
        p = Path(arg)
        if p.is_dir():
            files.extend(sorted(p.glob("*.png")))
        elif p.is_file() and p.suffix.lower() == ".png":
            files.append(p)
        else:
            print(f"skip (not a png/dir): {p}", file=sys.stderr)

    if not files:
        print("no png files found", file=sys.stderr)
        return 1

    for f in files:
        grayscale_png(f)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
