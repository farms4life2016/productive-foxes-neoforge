#!/usr/bin/env python3
"""Pick temporary Braixen spawn egg colors from a Braixen PNG texture.

The script classifies visible pixels by hue into yellow and orange buckets, then
computes deterministic weighted RGB averages for each bucket.
"""

from __future__ import annotations

import argparse
import colorsys
from collections import Counter
from pathlib import Path

import yaml

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit(
        "Pillow is required to read PNG files. Try running this with the project "
        "venv, or install pillow after confirming with the repo owner."
    ) from exc

from png_to_indexed_yaml import convert_png_to_yaml, image_pixels, rgba_to_hex8


DEFAULT_SOURCE = Path("src/main/resources/assets/productivefoxes/vixen_maid/ccpl/braixen_yellow.png")
DEFAULT_OUT_DIR = Path("texture_editor/analysis/braixen_spawn_egg")


def rgb_to_hsl_degrees(r: int, g: int, b: int) -> tuple[float, float, float]:
    h, l, s = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
    return h * 360.0, s, l


def classify_pixel(rgba: tuple[int, int, int, int]) -> str:
    r, g, b, a = rgba
    if a < 16:
        return "transparent"

    hue, saturation, lightness = rgb_to_hsl_degrees(r, g, b)
    if lightness < 0.14:
        return "dark_outline"
    if saturation < 0.15:
        return "neutral"
    if 0.0 <= hue < 25.0 and saturation >= 0.45 and lightness >= 0.25:
        return "orange"
    if 40.0 <= hue <= 72.0 and saturation >= 0.35 and lightness >= 0.25:
        return "yellow"
    return "other"


def pixel_weight(rgba: tuple[int, int, int, int]) -> float:
    r, g, b, a = rgba
    _, saturation, lightness = rgb_to_hsl_degrees(r, g, b)
    return (a / 255.0) * (0.5 + saturation) * (0.5 + lightness)


def weighted_average(pixels: list[tuple[int, int, int, int]]) -> tuple[int, int, int]:
    if not pixels:
        raise ValueError("cannot average an empty pixel bucket")

    total_weight = 0.0
    totals = [0.0, 0.0, 0.0]
    for rgba in pixels:
        weight = pixel_weight(rgba)
        total_weight += weight
        for i in range(3):
            totals[i] += rgba[i] * weight

    return tuple(round(channel / total_weight) for channel in totals)


def rgb_to_hex6(rgb: tuple[int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*rgb)


def rgb_to_decimal(rgb: tuple[int, int, int]) -> int:
    r, g, b = rgb
    return (r << 16) + (g << 8) + b


def analyze(source_png: Path, out_dir: Path) -> dict:
    image = Image.open(source_png).convert("RGBA")
    width, height = image.size
    pixels = image_pixels(image)

    buckets: dict[str, list[tuple[int, int, int, int]]] = {
        "yellow": [],
        "orange": [],
    }
    class_counts: Counter[str] = Counter()
    bucket_color_counts: dict[str, Counter[str]] = {
        "yellow": Counter(),
        "orange": Counter(),
    }
    classified_rows: list[str] = []

    symbols = {
        "transparent": ".",
        "dark_outline": "D",
        "neutral": "N",
        "yellow": "Y",
        "orange": "O",
        "other": "?",
    }

    for y in range(height):
        row_symbols: list[str] = []
        for x in range(width):
            rgba = pixels[y * width + x]
            classification = classify_pixel(rgba)
            class_counts[classification] += 1
            if classification in buckets:
                buckets[classification].append(rgba)
                bucket_color_counts[classification][rgba_to_hex8(rgba)] += 1
            row_symbols.append(symbols[classification])
        classified_rows.append("".join(row_symbols))

    yellow_rgb = weighted_average(buckets["yellow"])
    orange_rgb = weighted_average(buckets["orange"])

    summary = {
        "version": 1,
        "source_png": str(source_png),
        "method": "HSL hue buckets, alpha/saturation/lightness weighted RGB average",
        "classification_rules": {
            "transparent": "alpha < 16",
            "dark_outline": "lightness < 0.14",
            "neutral": "saturation < 0.15",
            "orange": "0 <= hue < 25 and saturation >= 0.45 and lightness >= 0.25",
            "yellow": "40 <= hue <= 72 and saturation >= 0.35 and lightness >= 0.25",
            "other": "anything else",
            "weight": "alpha/255 * (0.5 + saturation) * (0.5 + lightness)",
        },
        "width": width,
        "height": height,
        "class_counts": dict(sorted(class_counts.items())),
        "bucket_color_counts": {
            bucket: dict(sorted(counts.items(), key=lambda item: (-item[1], item[0])))
            for bucket, counts in bucket_color_counts.items()
        },
        "chosen_colors": {
            "yellow": {
                "hex": rgb_to_hex6(yellow_rgb),
                "decimal": rgb_to_decimal(yellow_rgb),
                "pixel_count": len(buckets["yellow"]),
            },
            "orange": {
                "hex": rgb_to_hex6(orange_rgb),
                "decimal": rgb_to_decimal(orange_rgb),
                "pixel_count": len(buckets["orange"]),
            },
        },
        "java": f"properties -> new DeferredSpawnEggItem(ProductiveFoxesEntities.BRAIXEN, {rgb_to_decimal(yellow_rgb)}, {rgb_to_decimal(orange_rgb)}, properties)",
    }

    out_dir.mkdir(parents=True, exist_ok=True)
    convert_png_to_yaml(source_png, out_dir / f"{source_png.stem}.indexed.yaml")

    classified = {
        "version": 1,
        "format": "classification-grid",
        "source_png": str(source_png),
        "legend": symbols,
        "pixels": "\n".join(classified_rows),
    }
    with (out_dir / f"{source_png.stem}.classified.yaml").open("w", encoding="utf-8") as handle:
        yaml.safe_dump(classified, handle, sort_keys=False, width=160)
    with (out_dir / f"{source_png.stem}.spawn_egg_colors.yaml").open("w", encoding="utf-8") as handle:
        yaml.safe_dump(summary, handle, sort_keys=False, width=160)

    return summary


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR)
    args = parser.parse_args()

    summary = analyze(args.source, args.out_dir)
    print(yaml.safe_dump(summary["chosen_colors"], sort_keys=False), end="")
    print(summary["java"])


if __name__ == "__main__":
    main()
