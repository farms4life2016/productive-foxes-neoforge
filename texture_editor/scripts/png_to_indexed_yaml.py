#!/usr/bin/env python3
"""Convert a PNG into an indexed YAML texture representation.

This mirrors the texture_editor HTML format where practical, but supports model
textures with more unique colors than the editor's one-character palette can
represent by using space-separated palette tokens.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import yaml

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit(
        "Pillow is required to read PNG files. Try running this with the project "
        "venv, or install pillow after confirming with the repo owner."
    ) from exc


EDITOR_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@$%^&*+=~"


def rgba_to_hex8(rgba: tuple[int, int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}{:02X}".format(*rgba)


def token_for_index(index: int) -> str:
    if index < len(EDITOR_CHARS):
        return EDITOR_CHARS[index]
    return f"C{index:03d}"


def image_pixels(image: Image.Image) -> list[tuple[int, int, int, int]]:
    if hasattr(image, "get_flattened_data"):
        return list(image.get_flattened_data())
    return list(image.getdata())


def convert_png_to_yaml(png_path: Path, yaml_path: Path) -> None:
    image = Image.open(png_path).convert("RGBA")
    width, height = image.size
    pixels = image_pixels(image)

    color_to_token: dict[str, str] = {}
    color_to_token["#00000000"] = "T"
    next_index = 0

    rows: list[str] = []
    for y in range(height):
        row_tokens: list[str] = []
        for x in range(width):
            color = rgba_to_hex8(pixels[y * width + x])
            if color not in color_to_token:
                while token_for_index(next_index) == "T":
                    next_index += 1
                color_to_token[color] = token_for_index(next_index)
                next_index += 1
            row_tokens.append(color_to_token[color])
        rows.append(" ".join(row_tokens))

    palette = {token: color for color, token in color_to_token.items()}
    data = {
        "version": 1,
        "format": "indexed-tokenized",
        "source_png": str(png_path),
        "width": width,
        "height": height,
        "palette": palette,
        "pixels": "\n".join(rows),
    }

    yaml_path.parent.mkdir(parents=True, exist_ok=True)
    with yaml_path.open("w", encoding="utf-8") as handle:
        yaml.safe_dump(data, handle, sort_keys=False, width=120)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("png", type=Path)
    parser.add_argument("yaml", type=Path)
    args = parser.parse_args()
    convert_png_to_yaml(args.png, args.yaml)


if __name__ == "__main__":
    main()
