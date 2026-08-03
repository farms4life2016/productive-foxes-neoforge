# Productive Foxes

This is a Minecraft mod written in Java for Neoforge 1.21.1

## Folder structure

- `../`: contains source code for other mods such as Mekanism, Create, and Cobblemon
  - those mods should be open-source and OK to copy from (typically MIT)
  - there is also a `productive-foxes` folder (without `-neoforge`).
    this is old code for 1.21 that we can port over, written by me.
- `1.20.4`: contains textures used by Minecraft 1.20.4.
  If I ever ask you to pull up a texture from vanilla, look in here.
- `texture_editor`: small one-file HTML app to convert Minecraft textures into YAML and back.
  This allows text-based LLMs to generate or edit Minecraft textures without diffusion-based image-generation tools.

### Cobblemon-specific Notes

Cobblemon is mainly MPL 2.0 (code) and CC-PL (modified CC-NC-BY-SA?) so we need NOTICES where appropriate.

Cobblemon is a huge repository. It may take a long time to switch branches, so *don't do it yourself*.
If you need to switch branches, ask the user to switch for you. Provide the command.

Cobblemon has two repos: a main repo with all the code and assets, and a secondary repo with only assets.

## Licensing

The licensing for this project is undecided. However, as much code as possible should be kept
under my control so I can freely control the license in the future.
For example, don't label the entire folder MPL 2.0 just because there is Cobblemon code in one file.

I'm not a lawyer. License enforcement doesn't need to be strict
and it should not decelerate or clutter development.
However, we still need to respect the copyrights of others when we borrow their code. 

## Python

(instructions for venv usage go here)
