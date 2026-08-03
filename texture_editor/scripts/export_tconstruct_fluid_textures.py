#!/usr/bin/env python3
"""Copy TConstruct fluid textures and render tint previews for inspection."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path

try:
    from PIL import Image
except ImportError as exc:  # pragma: no cover - dependency guard
    raise SystemExit("Pillow is required. Run this with the texture-editor Python env.") from exc


DEFAULT_TCONSTRUCT = Path("../TinkersConstruct")
DEFAULT_OUT = Path("texture_editor/analysis/tconstruct_fluids")


def parse_argb(value: str) -> tuple[int, int, int, int]:
    value = value.removeprefix("#")
    if len(value) == 6:
        value = "FF" + value
    if len(value) != 8:
        raise ValueError(f"expected ARGB hex color, got {value!r}")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4, 6))


def texture_path(tconstruct: Path, texture_id: str) -> Path:
    namespace, path = texture_id.split(":", 1)
    if namespace != "tconstruct":
        raise ValueError(f"unexpected namespace in {texture_id!r}")
    return tconstruct / "src/main/resources/assets/tconstruct/textures" / f"{path}.png"


def tint_image(source: Path, dest: Path, argb: tuple[int, int, int, int]) -> None:
    tint_a, tint_r, tint_g, tint_b = argb
    image = Image.open(source).convert("RGBA")
    pixels = []
    source_pixels = image.get_flattened_data() if hasattr(image, "get_flattened_data") else image.getdata()
    for r, g, b, a in source_pixels:
        pixels.append((
            round(r * tint_r / 255),
            round(g * tint_g / 255),
            round(b * tint_b / 255),
            round(a * tint_a / 255),
        ))
    image.putdata(pixels)
    dest.parent.mkdir(parents=True, exist_ok=True)
    image.save(dest)


def copy_tree(source: Path, dest: Path) -> int:
    count = 0
    for path in source.rglob("*"):
        if path.is_file():
            target = dest / path.relative_to(source)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
            count += 1
    return count


def export(tconstruct: Path, out_dir: Path) -> dict:
    textures_dir = tconstruct / "src/main/resources/assets/tconstruct/textures/fluid"
    metadata_dir = tconstruct / "src/generated/resources/assets/tconstruct/mantle/fluid_texture"

    raw_count = copy_tree(textures_dir, out_dir / "raw/assets/tconstruct/textures/fluid")
    metadata_count = copy_tree(metadata_dir, out_dir / "metadata/assets/tconstruct/mantle/fluid_texture")

    summary = {
        "source": str(tconstruct),
        "raw_files": raw_count,
        "fluid_texture_jsons": metadata_count,
        "tinted_previews": {},
        "notes": [
            "TConstruct stores many fluid PNGs as greyscale/base sprites.",
            "Generated mantle/fluid_texture JSON supplies ARGB color, still, flowing, and camera texture ids.",
            "These previews multiply each source RGBA channel by the ARGB tint, matching the baked tint path closely enough for inspection.",
        ],
    }

    for json_path in sorted(metadata_dir.glob("*.json")):
        data = json.loads(json_path.read_text(encoding="utf-8"))
        color = data.get("color", "FFFFFFFF")
        argb = parse_argb(color)
        fluid_name = json_path.stem
        outputs = {}
        for key in ("still", "flowing", "camera"):
            texture_id = data.get(key)
            if not texture_id:
                continue
            source = texture_path(tconstruct, texture_id)
            if source.exists():
                dest = out_dir / "tinted_previews" / fluid_name / f"{key}.png"
                tint_image(source, dest, argb)
                outputs[key] = {
                    "source": str(source.relative_to(tconstruct)),
                    "output": str(dest),
                }
        summary["tinted_previews"][fluid_name] = {
            "color": color,
            "textures": outputs,
        }

    report_path = out_dir / "README.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    (out_dir / "README.md").write_text(
        "\n".join([
            "# TConstruct Fluid Texture Inspection",
            "",
            "Source: `../TinkersConstruct`",
            "License: MIT, copyright SlimeKnights. Keep the MIT notice if these copied assets become more than local inspection material.",
            "",
            "## Folders",
            "",
            "- `raw/assets/tconstruct/textures/fluid/`: copied source fluid PNG/MCMeta files.",
            "- `metadata/assets/tconstruct/mantle/fluid_texture/`: copied generated texture metadata JSON.",
            "- `tinted_previews/<fluid>/`: offline tinted PNG previews for `still`, `flowing`, and `camera` where present.",
            "",
            "## Notes",
            "",
            "- Mushroom stew uses `color: FFCD8C6F` with shared `fluid/food/stew` greyscale sprites.",
            "- TConstruct buckets use generated `models/item/*_bucket.json` with `loader: tconstruct:fluid_container`.",
            "- That model bakes `IClientFluidTypeExtensions#getTintColor(fluid)` into the fluid layer.",
        ]) + "\n",
        encoding="utf-8",
    )
    return summary


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tconstruct", type=Path, default=DEFAULT_TCONSTRUCT)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    summary = export(args.tconstruct, args.out)
    print(f"raw files: {summary['raw_files']}")
    print(f"fluid texture jsons: {summary['fluid_texture_jsons']}")
    print(f"tinted preview fluids: {len(summary['tinted_previews'])}")


if __name__ == "__main__":
    main()
