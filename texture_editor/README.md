# MC Texture Editor

A standalone, single-file HTML tool for editing 16×16 Minecraft textures by hand **or** via an AI model using a human-readable YAML pixel format.

No build step, no server, no dependencies beyond one CDN script (`js-yaml`). Open `mc-texture-editor.html` directly in a browser.

---

## Why this exists

Minecraft textures are tiny (16×16px) and use few unique colours, which makes them a poor fit for normal AI image generation (diffusion models default to ~1024×1024, are token/compute expensive, and rarely produce clean pixel-art edges).

Instead, this tool encodes a texture as an **indexed colour grid in YAML** — a format an LLM can read, reason about, and edit directly as text, then hand back to this tool to render as a PNG.

**Typical workflow:**

1. Open `mc-texture-editor.html` in a browser.
2. Drag a vanilla texture PNG onto the canvas (e.g. `sweet_berries.png` from the Minecraft jar/resource pack).
3. Click **Export YAML**, copy the contents.
4. Paste the YAML to an AI assistant with an instruction like *"replace the red berries with blue ones."*
5. Paste the AI's returned YAML into the **YAML** textarea and click **Import YAML**.
6. Touch up individual pixels with the Pencil tool, or recolor every instance of a shade with **Fill All**.
7. Click **Export PNG** and drop the result into your resource pack / mod's `textures/` folder.

---

## File

- `mc-texture-editor.html` — the entire app (HTML + CSS + JS in one file). Pure vanilla JS, Canvas 2D API, no build tooling.
- Single external dependency, loaded via CDN: [`js-yaml`](https://github.com/nodeca/js-yaml) (YAML parse/serialize).

---

## YAML format (v1)

```yaml
version: 1
format: indexed
width: 16
height: 16
palette:
  T: "#00000000"     # T is reserved for transparent
  A: "#6B8E23FF"
  B: "#556B2FFF"
  C: "#8B0000FF"
pixels: |
  TTTTTTTTTTTTTTTT
  TTAABBCCAABBCCAT
  TAABBCCCCAABBCCT
  ...              # 16 rows × 16 chars total
```

### Rules

- **`version`** — schema version. Currently `1`. A parser/consumer should check this and reject or migrate unknown versions. Future versions may add `format: rgba` (uncompressed per-pixel colour, for textures with too many unique colours to index usefully) or other layout changes.
- **`format`** — `indexed` is the only format `v1` supports. Each pixel is a single character key into `palette`.
- **`width` / `height`** — currently always `16` / `16`. The tool is built for 16×16 but the schema isn't hardcoded to that size — wider/taller grids are valid YAML, just not yet supported by the editor UI.
- **`palette`** — maps a single character to an 8-character hex colour string in `#RRGGBBAA` form (always includes alpha, even if fully opaque `FF`). The character `T` is a soft convention for fully-transparent (`#00000000`) but is not enforced — any character may map to any colour.
- **`pixels`** — a YAML block literal (`|`) containing exactly `height` rows of exactly `width` characters each, top row first, top-left pixel first. Every character must exist as a key in `palette`.
- Palette characters are assigned from `A–Z`, `a–z`, `0–9`, then a small set of symbols (`! @ $ % ^ & * + = ~`) — roughly 70 available before running out. A texture needing more unique colours than that should be re-encoded as `format: rgba` instead (not yet implemented in the editor's import logic — flag this if you hit it).

### Notes for an AI editing this YAML

- Preserve `width`/`height` and don't change the number of rows/columns in `pixels`.
- Reuse existing palette characters where the colour is already present; only add new palette entries for genuinely new colours.
- Keep `pixels` as a single contiguous block — no blank lines, no trailing whitespace differences within rows that would change row length.
- It's fine to leave unused colours in the palette (the tool's "Prune" button cleans those up), but don't reuse a character for two different colours.
- Output the full YAML document back (not a diff/patch) — the tool's importer replaces the entire canvas state on import.

---

## Features

- **Pencil** — click/drag to paint individual pixels with the selected colour.
- **Fill All** — replaces every pixel matching the colour under the cursor with the currently selected colour (not a flood-fill/bucket — it's global, matching the "replace all pixels of colour X" requirement, not contiguous regions).
- **Pick (eyedropper)** — click a pixel to load its colour into the active colour slot and palette.
- **Undo / Redo** — up to 80 steps, also bound to `Ctrl+Z` / `Ctrl+Shift+Z`.
- **Colour controls** — native OS colour picker, raw `#RRGGBBAA` hex entry, separate alpha slider.
- **Palette panel** — swatches for all colours in use; click to select, **+ Add** for a new slot, **Prune** to drop palette entries not present on the canvas.
- **Import** — PNG (drag-and-drop or file picker, auto-extracts palette) and YAML (paste into the textarea or drag-and-drop a `.yaml`/`.yml` file).
- **Export** — PNG (native 16×16, no upscaling) and YAML (downloads a file and also fills the textarea for easy copy).
- **Zoom slider** — 8–48px per cell for the on-screen grid (cosmetic only, doesn't affect exported PNG resolution).

---

## Known limitations / not yet built

- **No colour quantization/merging on PNG import.** A texture with anti-aliased edges or near-duplicate colours will get a separate palette entry per unique RGBA value, which can bloat the palette well beyond what a hand-authored Minecraft texture would have. A tolerance-based colour-merge pass on import would fix this.
- **Indexed format only.** No `format: rgba` (uncompressed) implementation yet, even though the schema anticipates it. Needed once a texture exceeds ~70 unique colours.
- **16×16 only.** Width/height are constants in the code (`const W = 16, H = 16`). Scaling to 32×32 means parameterizing canvas sizing, the palette character budget, and the new-canvas dialog.
- **No multi-frame/animation support** (e.g. animated water/lava textures, `.mcmeta` files).
- **Fill All is global, not flood-fill.** There's no contiguous-region bucket tool currently.

---

## Tech stack

Vanilla HTML/CSS/JS, Canvas 2D API, `OffscreenCanvas` for pixel buffer → PNG conversion. One CDN dependency (`js-yaml`). No npm, no bundler, no framework — intentionally, to keep this a single shareable file.
