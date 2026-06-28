# Braixen Model Reference

Here is the Git history for Braixen's Blockbench model:

https://gitlab.com/cable-mc/cobblemon/-/commits/main/common/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0654_braixen?ref_type=heads

Old model = nose is $2 \times 2$. New model = nose is $1 \times 1$.
You can check by importing the `.geo.json` file into Blockbench
or inspect the json for a `nose` bone and its `cubes[0].size` array
(either `[1, 1, 1]` or `[2, 2, 2]`).

I want to use the older model cuz it looks cuter and less aggressive.

| Commit | File stem (in `blockbench/`) | Notes                                                                                                                     |
|---|-----------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `c466d5e0cbd39592fe0d1ed7dfeae5e2a2e76813` | `braixen`                   | Initial commit of Braixen, old model. No battle anim and always holds stick. Stick never in tail.                         |
| `30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc` | `braixen-2`                 | Same old model, added battle anim of holding stick. Idle and walk no longer hold stick. Stick always in tail.             |
| `68923dd95f6a6654b0ef1e44941ea068d77aaf51` | `braixen-3`                 | New model, nose is $1 \times 1$. |
| `ca044e839029472f6a97dbadbff3a410bed4bcb9` | `braixen-4`                 | New model, nose is $1 \times 1$.                                                                                          |
| `34315d9f1dc90f3109e0e21aa2942b18cccbeb3d` | N/A                         | CCPL licenses added to model folder. Previously, MPL2.0 applied to the models. Not sure if CCPL is retroactively applied. |

**Verdict:** use the Braixen assets from commit `30b769db132a81b83dc6b1d61fa6b21e2b8dcdfc`
