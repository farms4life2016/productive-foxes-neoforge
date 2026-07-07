#!/usr/bin/env python3
"""Print PNG pixel values for texture inspection."""

from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path
from typing import TextIO

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit(
        "Pillow is required to read PNG files. Try running this with the project "
        "venv, or install pillow after confirming with the repo owner."
    ) from exc


def rgba_to_hex8(rgba: tuple[int, int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}{:02X}".format(*rgba)


def print_pixels(png_path: Path, out: TextIO, max_pixels: int, print_all: bool) -> None:
    image = Image.open(png_path).convert("RGBA")
    width, height = image.size
    pixels = list(image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata())
    counts = Counter(rgba_to_hex8(rgba) for rgba in pixels)
    visible_count = sum(1 for _, _, _, a in pixels if a > 0)

    print(f"source: {png_path}", file=out)
    print(f"size: {width}x{height}", file=out)
    print(f"pixels: {len(pixels)}", file=out)
    print(f"visible_pixels: {visible_count}", file=out)
    print(f"unique_colors: {len(counts)}", file=out)
    print("unique_color_counts:", file=out)
    for color, count in sorted(counts.items(), key=lambda item: (-item[1], item[0])):
        print(f"  {color}: {count}", file=out)

    if not print_all and len(pixels) > max_pixels:
        print(
            f"pixel_grid: skipped because image has {len(pixels)} pixels; "
            f"rerun with --all or raise --max-pixels",
            file=out,
        )
        return

    print("pixel_grid:", file=out)
    for y in range(height):
        row = [rgba_to_hex8(pixels[y * width + x]) for x in range(width)]
        print(f"  y={y:04d}: {' '.join(row)}", file=out)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("png", type=Path)
    parser.add_argument(
        "--max-pixels",
        type=int,
        default=4096,
        help="Skip the pixel grid above this count unless --all is supplied.",
    )
    parser.add_argument("--all", action="store_true", help="Print every pixel even for large textures.")
    args = parser.parse_args()

    print_pixels(args.png, out=__import__("sys").stdout, max_pixels=args.max_pixels, print_all=args.all)


if __name__ == "__main__":
    main()
