# TConstruct Fluid Texture Inspection

Source: `../TinkersConstruct`
License: MIT, copyright SlimeKnights. Keep the MIT notice if these copied assets become more than local inspection material.

## Folders

- `raw/assets/tconstruct/textures/fluid/`: copied source fluid PNG/MCMeta files.
- `metadata/assets/tconstruct/mantle/fluid_texture/`: copied generated texture metadata JSON.
- `tinted_previews/<fluid>/`: offline tinted PNG previews for `still`, `flowing`, and `camera` where present.

## Notes

- Mushroom stew uses `color: FFCD8C6F` with shared `fluid/food/stew` greyscale sprites.
- TConstruct buckets use generated `models/item/*_bucket.json` with `loader: tconstruct:fluid_container`.
- That model bakes `IClientFluidTypeExtensions#getTintColor(fluid)` into the fluid layer.
