#!/usr/bin/env python3
"""Split colored liquid textures into a tint and grayscale generic texture."""

from __future__ import annotations

import argparse
import colorsys
import json
import math
from dataclasses import dataclass
from pathlib import Path

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit(
        "Pillow is required to read PNG files. Try running this with the project "
        "venv, or install pillow after confirming with the repo owner."
    ) from exc


DEFAULT_SOURCES = [
    Path("texture_editor/create_liquid_honey/honey_still.png"),
    Path("texture_editor/create_liquid_honey/honey_flow.png"),
    Path("texture_editor/create_liquid_honey/honey_bucket.png"),
]
DEFAULT_OUT_DIR = Path("texture_editor/analysis/create_liquid_honey_tint")


@dataclass(frozen=True)
class PixelInfo:
    rgba: tuple[int, int, int, int]
    hue: float
    saturation: float
    lightness: float
    luma: float
    is_tint_candidate: bool


def clamp_byte(value: float) -> int:
    return max(0, min(255, round(value)))


def rgb_to_hex6(rgb: tuple[int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*rgb)


def rgba_to_hex8(rgba: tuple[int, int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}{:02X}".format(*rgba)


def analyze_pixel(
    rgba: tuple[int, int, int, int],
    alpha_min: int,
    saturation_min: float,
    luma_min: float,
    luma_max: float,
) -> PixelInfo:
    r, g, b, a = rgba
    hue, lightness, saturation = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
    luma = (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
    is_candidate = (
        a >= alpha_min
        and saturation >= saturation_min
        and luma_min <= luma <= luma_max
    )
    return PixelInfo(
        rgba=rgba,
        hue=hue * 360.0,
        saturation=saturation,
        lightness=lightness,
        luma=luma,
        is_tint_candidate=is_candidate,
    )


def estimate_tint(candidates: list[PixelInfo]) -> tuple[tuple[int, int, int], tuple[int, int, int]]:
    """Return (weighted_average_rgb, normalized_tint_rgb)."""
    if not candidates:
        raise ValueError("cannot estimate a tint without candidate pixels")

    totals = [0.0, 0.0, 0.0]
    total_weight = 0.0
    for info in candidates:
        r, g, b, a = info.rgba
        weight = (a / 255.0) * (0.35 + info.saturation) * (0.35 + info.lightness)
        total_weight += weight
        totals[0] += r * weight
        totals[1] += g * weight
        totals[2] += b * weight

    weighted = tuple(clamp_byte(channel / total_weight) for channel in totals)
    peak = max(weighted)
    if peak <= 0:
        return weighted, weighted

    normalized = tuple(clamp_byte(channel * 255.0 / peak) for channel in weighted)
    return weighted, normalized


def project_to_gray(rgb: tuple[int, int, int], tint: tuple[int, int, int]) -> int:
    """Least-squares grayscale value for rgb ~= gray * tint / 255."""
    denom = sum(channel * channel for channel in tint)
    if denom <= 0:
        return 0
    scale = sum(rgb_channel * tint_channel for rgb_channel, tint_channel in zip(rgb, tint)) / denom
    return clamp_byte(scale * 255.0)


def apply_tint(gray: int, alpha: int, tint: tuple[int, int, int]) -> tuple[int, int, int, int]:
    return (
        clamp_byte(gray * tint[0] / 255.0),
        clamp_byte(gray * tint[1] / 255.0),
        clamp_byte(gray * tint[2] / 255.0),
        alpha,
    )


def error_stats(
    original: list[tuple[int, int, int, int]],
    reconstructed: list[tuple[int, int, int, int]],
    include_mask: list[bool],
) -> dict[str, float | int]:
    total_abs = 0
    total_sq = 0
    max_abs = 0
    exact_pixels = 0
    compared_pixels = 0
    channel_count = 0

    for orig, recon, include in zip(original, reconstructed, include_mask):
        if not include:
            continue
        compared_pixels += 1
        if orig == recon:
            exact_pixels += 1
        for channel_index in range(3):
            diff = abs(orig[channel_index] - recon[channel_index])
            total_abs += diff
            total_sq += diff * diff
            max_abs = max(max_abs, diff)
            channel_count += 1

    if channel_count == 0:
        return {
            "compared_pixels": 0,
            "mean_abs_channel_error": 0.0,
            "rmse_channel_error": 0.0,
            "max_abs_channel_error": 0,
            "exact_pixel_percent": 0.0,
        }

    return {
        "compared_pixels": compared_pixels,
        "mean_abs_channel_error": round(total_abs / channel_count, 4),
        "rmse_channel_error": round(math.sqrt(total_sq / channel_count), 4),
        "max_abs_channel_error": max_abs,
        "exact_pixel_percent": round(100.0 * exact_pixels / compared_pixels, 4),
    }


def save_image(path: Path, size: tuple[int, int], pixels: list[tuple[int, int, int, int]]) -> None:
    image = Image.new("RGBA", size)
    image.putdata(pixels)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def split_one(
    source: Path,
    out_dir: Path,
    alpha_min: int,
    saturation_min: float,
    luma_min: float,
    luma_max: float,
) -> dict:
    image = Image.open(source).convert("RGBA")
    size = image.size
    pixels = list(image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata())
    infos = [
        analyze_pixel(
            rgba,
            alpha_min=alpha_min,
            saturation_min=saturation_min,
            luma_min=luma_min,
            luma_max=luma_max,
        )
        for rgba in pixels
    ]
    visible_mask = [rgba[3] >= alpha_min for rgba in pixels]
    candidate_mask = [info.is_tint_candidate for info in infos]
    candidates = [info for info in infos if info.is_tint_candidate]
    if not candidates:
        candidates = [info for info, visible in zip(infos, visible_mask) if visible]

    weighted_rgb, tint = estimate_tint(candidates)
    generic_all: list[tuple[int, int, int, int]] = []
    recombined_all: list[tuple[int, int, int, int]] = []
    generic_colored: list[tuple[int, int, int, int]] = []
    base_passthrough: list[tuple[int, int, int, int]] = []
    recombined_with_base: list[tuple[int, int, int, int]] = []

    for rgba, is_candidate in zip(pixels, candidate_mask):
        r, g, b, a = rgba
        gray = project_to_gray((r, g, b), tint)

        generic_all.append((gray, gray, gray, a))
        recombined_all.append(apply_tint(gray, a, tint))

        if is_candidate:
            generic_colored.append((gray, gray, gray, a))
            base_passthrough.append((0, 0, 0, 0))
            recombined_with_base.append(apply_tint(gray, a, tint))
        else:
            generic_colored.append((0, 0, 0, 0))
            base_passthrough.append(rgba)
            recombined_with_base.append(rgba)

    stem = source.stem
    image_dir = out_dir / stem
    save_image(image_dir / f"{stem}.generic_all.png", size, generic_all)
    save_image(image_dir / f"{stem}.recombined_all.png", size, recombined_all)
    save_image(image_dir / f"{stem}.generic_colored.png", size, generic_colored)
    save_image(image_dir / f"{stem}.base_passthrough.png", size, base_passthrough)
    save_image(image_dir / f"{stem}.recombined_with_base.png", size, recombined_with_base)

    unique_original = len({rgba_to_hex8(rgba) for rgba in pixels})
    unique_generic_all = len({rgba_to_hex8(rgba) for rgba in generic_all})
    unique_recombined_all = len({rgba_to_hex8(rgba) for rgba in recombined_all})
    report = {
        "source": str(source),
        "size": {"width": size[0], "height": size[1]},
        "pixels": len(pixels),
        "visible_pixels": sum(visible_mask),
        "tint_candidate_pixels": sum(candidate_mask),
        "candidate_rules": {
            "alpha_min": alpha_min,
            "saturation_min": saturation_min,
            "luma_min": luma_min,
            "luma_max": luma_max,
        },
        "weighted_average_rgb": {
            "rgb": weighted_rgb,
            "hex": rgb_to_hex6(weighted_rgb),
        },
        "normalized_tint_rgb": {
            "rgb": tint,
            "hex": rgb_to_hex6(tint),
            "note": "max channel normalized to 255 so bright grayscale pixels can recover source highlights",
        },
        "unique_colors": {
            "original": unique_original,
            "generic_all": unique_generic_all,
            "recombined_all": unique_recombined_all,
        },
        "single_tinted_layer_error": error_stats(pixels, recombined_all, visible_mask),
        "colored_pixels_only_error": error_stats(pixels, recombined_with_base, candidate_mask),
        "base_plus_tinted_layer_error": error_stats(pixels, recombined_with_base, visible_mask),
        "outputs": {
            "generic_all": str(image_dir / f"{stem}.generic_all.png"),
            "recombined_all": str(image_dir / f"{stem}.recombined_all.png"),
            "generic_colored": str(image_dir / f"{stem}.generic_colored.png"),
            "base_passthrough": str(image_dir / f"{stem}.base_passthrough.png"),
            "recombined_with_base": str(image_dir / f"{stem}.recombined_with_base.png"),
        },
    }

    with (image_dir / f"{stem}.tint_report.json").open("w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
        handle.write("\n")
    return report


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sources", nargs="*", type=Path, default=DEFAULT_SOURCES)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR)
    parser.add_argument("--alpha-min", type=int, default=16)
    parser.add_argument("--saturation-min", type=float, default=0.18)
    parser.add_argument("--luma-min", type=float, default=16.0)
    parser.add_argument("--luma-max", type=float, default=248.0)
    args = parser.parse_args()

    reports = [
        split_one(
            source=source,
            out_dir=args.out_dir,
            alpha_min=args.alpha_min,
            saturation_min=args.saturation_min,
            luma_min=args.luma_min,
            luma_max=args.luma_max,
        )
        for source in args.sources
    ]
    args.out_dir.mkdir(parents=True, exist_ok=True)
    with (args.out_dir / "summary.json").open("w", encoding="utf-8") as handle:
        json.dump(reports, handle, indent=2)
        handle.write("\n")

    for report in reports:
        source = Path(report["source"]).name
        tint = report["normalized_tint_rgb"]["hex"]
        one_layer = report["single_tinted_layer_error"]
        with_base = report["base_plus_tinted_layer_error"]
        print(
            f"{source}: tint {tint}; "
            f"single-layer MAE {one_layer['mean_abs_channel_error']} "
            f"RMSE {one_layer['rmse_channel_error']} max {one_layer['max_abs_channel_error']}; "
            f"base+layer MAE {with_base['mean_abs_channel_error']} "
            f"RMSE {with_base['rmse_channel_error']} max {with_base['max_abs_channel_error']}"
        )


if __name__ == "__main__":
    main()
