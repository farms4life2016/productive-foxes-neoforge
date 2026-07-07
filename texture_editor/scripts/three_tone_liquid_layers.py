#!/usr/bin/env python3
"""Analyze a liquid texture as three tintable Minecraft-style layers."""

from __future__ import annotations

import argparse
import json
import math
from collections import Counter
from pathlib import Path

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit(
        "Pillow is required to read PNG files. Try running this with the project "
        "venv, or install pillow after confirming with the repo owner."
    ) from exc


DEFAULT_SOURCE = Path("texture_editor/create_liquid_honey/honey_still.png")
DEFAULT_OUT_DIR = Path("texture_editor/analysis/create_liquid_honey_three_tone")


def clamp_byte(value: float) -> int:
    return max(0, min(255, round(value)))


def rgba_to_hex8(rgba: tuple[int, int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}{:02X}".format(*rgba)


def rgb_to_hex6(rgb: tuple[int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*rgb)


def parse_hex_rgb(value: str) -> tuple[int, int, int]:
    stripped = value.strip().removeprefix("#")
    if len(stripped) != 6:
        raise argparse.ArgumentTypeError(f"expected #RRGGBB, got {value!r}")
    try:
        return tuple(int(stripped[index : index + 2], 16) for index in (0, 2, 4))
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"expected #RRGGBB, got {value!r}") from exc


def luma(rgb: tuple[int, int, int]) -> float:
    r, g, b = rgb
    return (0.2126 * r) + (0.7152 * g) + (0.0722 * b)


def project_segment(
    color: tuple[int, int, int],
    start: tuple[int, int, int],
    end: tuple[int, int, int],
) -> tuple[float, tuple[int, int, int]]:
    delta = tuple(end[index] - start[index] for index in range(3))
    denom = sum(channel * channel for channel in delta)
    if denom == 0:
        return 0.0, start

    raw_t = sum((color[index] - start[index]) * delta[index] for index in range(3)) / denom
    t = max(0.0, min(1.0, raw_t))
    reconstructed = tuple(clamp_byte(start[index] + (delta[index] * t)) for index in range(3))
    return t, reconstructed


def alpha_over(
    bottom: tuple[int, int, int, int],
    top: tuple[int, int, int, int],
) -> tuple[int, int, int, int]:
    br, bg, bb, ba_byte = bottom
    tr, tg, tb, ta_byte = top
    ba = ba_byte / 255.0
    ta = ta_byte / 255.0
    out_a = ta + (ba * (1.0 - ta))
    if out_a == 0.0:
        return (0, 0, 0, 0)

    out_rgb = []
    for bottom_channel, top_channel in ((br, tr), (bg, tg), (bb, tb)):
        value = ((top_channel * ta) + (bottom_channel * ba * (1.0 - ta))) / out_a
        out_rgb.append(clamp_byte(value))
    return (out_rgb[0], out_rgb[1], out_rgb[2], clamp_byte(out_a * 255.0))


def error_stats(
    original: list[tuple[int, int, int, int]],
    reconstructed: list[tuple[int, int, int, int]],
) -> dict[str, float | int]:
    total_abs = 0
    total_sq = 0
    max_abs = 0
    exact = 0
    visible = 0
    channels = 0
    for source, recon in zip(original, reconstructed):
        if source[3] == 0:
            continue
        visible += 1
        if source == recon:
            exact += 1
        for index in range(4):
            diff = abs(source[index] - recon[index])
            total_abs += diff
            total_sq += diff * diff
            max_abs = max(max_abs, diff)
            channels += 1

    return {
        "visible_pixels": visible,
        "mean_abs_channel_error": round(total_abs / channels, 4) if channels else 0.0,
        "rmse_channel_error": round(math.sqrt(total_sq / channels), 4) if channels else 0.0,
        "max_abs_channel_error": max_abs,
        "exact_pixel_percent": round(100.0 * exact / visible, 4) if visible else 0.0,
    }


def save_image(path: Path, size: tuple[int, int], pixels: list[tuple[int, int, int, int]]) -> None:
    image = Image.new("RGBA", size)
    image.putdata(pixels)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def choose_auto_anchors(
    sorted_palette: list[dict],
) -> tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int], str]:
    if not sorted_palette:
        raise ValueError("cannot choose tone anchors from an empty palette")

    dark = tuple(sorted_palette[0]["rgb"])
    medium_index = (len(sorted_palette) - 1) // 2
    medium = tuple(sorted_palette[medium_index]["rgb"])
    light = tuple(sorted_palette[-1]["rgb"])
    note = (
        "automatic selection: dark is the leftmost color by luma, light is the "
        "rightmost color by luma, and medium is the lower median palette entry "
        "so the choice remains an actual source color when the palette count is even"
    )
    return dark, medium, light, note


def analyze(
    source: Path,
    out_dir: Path,
    manual_dark: tuple[int, int, int] | None,
    manual_medium: tuple[int, int, int] | None,
    manual_light: tuple[int, int, int] | None,
) -> dict:
    image = Image.open(source).convert("RGBA")
    size = image.size
    pixels = list(image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata())
    counts = Counter(rgba for rgba in pixels if rgba[3] > 0)

    sorted_palette = []
    for rgba, count in sorted(counts.items(), key=lambda item: (luma(item[0][:3]), item[0])):
        rgb = rgba[:3]
        sorted_palette.append(
            {
                "hex": rgb_to_hex6(rgb),
                "rgba": rgba_to_hex8(rgba),
                "rgb": list(rgb),
                "count": count,
                "luma": round(luma(rgb), 4),
            }
        )

    if manual_dark or manual_medium or manual_light:
        if not (manual_dark and manual_medium and manual_light):
            raise ValueError("--dark, --medium, and --light must be supplied together")
        dark, medium, light = manual_dark, manual_medium, manual_light
        anchor_note = "manual selection from --dark, --medium, and --light"
    else:
        dark, medium, light, anchor_note = choose_auto_anchors(sorted_palette)

    dark_layer: list[tuple[int, int, int, int]] = []
    medium_layer: list[tuple[int, int, int, int]] = []
    light_layer: list[tuple[int, int, int, int]] = []
    recombined: list[tuple[int, int, int, int]] = []
    pixel_class_grid: list[str] = []
    per_color: dict[str, dict] = {}

    medium_luma = luma(medium)
    for y in range(size[1]):
        row_symbols: list[str] = []
        for x in range(size[0]):
            rgba = pixels[y * size[0] + x]
            r, g, b, a = rgba
            if a == 0:
                dark_layer.append((255, 255, 255, 0))
                medium_layer.append((255, 255, 255, 0))
                light_layer.append((255, 255, 255, 0))
                recombined.append((0, 0, 0, 0))
                row_symbols.append(".")
                continue

            source_rgb = (r, g, b)
            if luma(source_rgb) <= medium_luma:
                t, approx_rgb = project_segment(source_rgb, dark, medium)
                dark_alpha = a
                medium_alpha = clamp_byte(t * a)
                light_alpha = 0
                symbol = "D" if t < 0.20 else "d" if t < 0.80 else "M"
                blend = {
                    "segment": "dark_to_medium",
                    "dark_weight": round(1.0 - t, 6),
                    "medium_weight": round(t, 6),
                    "light_weight": 0.0,
                }
            else:
                t, approx_rgb = project_segment(source_rgb, medium, light)
                dark_alpha = a
                medium_alpha = a
                light_alpha = clamp_byte(t * a)
                symbol = "M" if t < 0.20 else "l" if t < 0.80 else "L"
                blend = {
                    "segment": "medium_to_light",
                    "dark_weight": 0.0,
                    "medium_weight": round(1.0 - t, 6),
                    "light_weight": round(t, 6),
                }

            dark_pixel = (*dark, dark_alpha)
            medium_pixel = (*medium, medium_alpha)
            light_pixel = (*light, light_alpha)
            composed = alpha_over(alpha_over(dark_pixel, medium_pixel), light_pixel)
            dark_layer.append((255, 255, 255, dark_alpha))
            medium_layer.append((255, 255, 255, medium_alpha))
            light_layer.append((255, 255, 255, light_alpha))
            recombined.append(composed)
            row_symbols.append(symbol)

            color_key = rgba_to_hex8(rgba)
            if color_key not in per_color:
                per_color[color_key] = {
                    "source_rgb": list(source_rgb),
                    "source_hex": rgb_to_hex6(source_rgb),
                    "count": counts[rgba],
                    "source_luma": round(luma(source_rgb), 4),
                    "approx_rgb": list(approx_rgb),
                    "approx_hex": rgb_to_hex6(approx_rgb),
                    "composited_rgb": list(composed[:3]),
                    "composited_hex": rgb_to_hex6(composed[:3]),
                    "max_channel_error": max(abs(source_rgb[index] - composed[index]) for index in range(3)),
                    **blend,
                }
        pixel_class_grid.append("".join(row_symbols))

    stem = source.stem
    image_dir = out_dir / stem
    save_image(image_dir / f"{stem}.layer_dark_mask.png", size, dark_layer)
    save_image(image_dir / f"{stem}.layer_medium_mask.png", size, medium_layer)
    save_image(image_dir / f"{stem}.layer_light_mask.png", size, light_layer)
    save_image(image_dir / f"{stem}.recombined_three_tone.png", size, recombined)

    report = {
        "source": str(source),
        "size": {"width": size[0], "height": size[1]},
        "visible_pixels": sum(counts.values()),
        "unique_visible_colors": len(counts),
        "sorted_palette_by_luma": sorted_palette,
        "selected_anchors": {
            "dark": {"rgb": list(dark), "hex": rgb_to_hex6(dark), "luma": round(luma(dark), 4)},
            "medium": {"rgb": list(medium), "hex": rgb_to_hex6(medium), "luma": round(luma(medium), 4)},
            "light": {"rgb": list(light), "hex": rgb_to_hex6(light), "luma": round(luma(light), 4)},
            "note": anchor_note,
        },
        "minecraft_layer_model": {
            "intent": "layer0 dark mask tinted dark, layer1 medium mask tinted medium, layer2 light mask tinted light",
            "blend_order": "dark bottom, medium over dark, light over medium",
            "compatibility_note": (
                "This matches the standard alpha-over behavior used by layered item models. "
                "A normal fluid sprite has one tint; using three fluid-sprite colors would need "
                "a custom renderer, generated baked texture, or another modded rendering hook."
            ),
        },
        "class_grid_legend": {
            ".": "transparent",
            "D": "mostly dark",
            "d": "dark/medium blend",
            "M": "mostly medium",
            "l": "medium/light blend",
            "L": "mostly light",
        },
        "class_grid": "\n".join(pixel_class_grid),
        "per_source_color_blends": [
            per_color[key] for key in sorted(per_color, key=lambda color: luma(tuple(per_color[color]["source_rgb"])))
        ],
        "recombined_error": error_stats(pixels, recombined),
        "outputs": {
            "dark_layer_mask": str(image_dir / f"{stem}.layer_dark_mask.png"),
            "medium_layer_mask": str(image_dir / f"{stem}.layer_medium_mask.png"),
            "light_layer_mask": str(image_dir / f"{stem}.layer_light_mask.png"),
            "recombined": str(image_dir / f"{stem}.recombined_three_tone.png"),
        },
    }

    image_dir.mkdir(parents=True, exist_ok=True)
    with (image_dir / f"{stem}.three_tone_report.json").open("w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
        handle.write("\n")
    with (out_dir / "summary.json").open("w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
        handle.write("\n")
    return report


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR)
    parser.add_argument("--dark", type=parse_hex_rgb)
    parser.add_argument("--medium", type=parse_hex_rgb)
    parser.add_argument("--light", type=parse_hex_rgb)
    args = parser.parse_args()

    report = analyze(
        source=args.source,
        out_dir=args.out_dir,
        manual_dark=args.dark,
        manual_medium=args.medium,
        manual_light=args.light,
    )
    anchors = report["selected_anchors"]
    error = report["recombined_error"]
    print(
        "anchors: "
        f"dark {anchors['dark']['hex']}, "
        f"medium {anchors['medium']['hex']}, "
        f"light {anchors['light']['hex']}"
    )
    print(
        "recombined error: "
        f"MAE {error['mean_abs_channel_error']}, "
        f"RMSE {error['rmse_channel_error']}, "
        f"max {error['max_abs_channel_error']}, "
        f"exact {error['exact_pixel_percent']}%"
    )


if __name__ == "__main__":
    main()
