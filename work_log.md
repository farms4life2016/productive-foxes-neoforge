# Work Log

This log serves as documentation for all the programming and game design changes
that were made during this mod's development.
Log entries may not be in chronological order or have an associated date.


## Initial Commit

May 9, 2026

I chose Neoforge 1.21.1 because it was the only version of the game and modloader
that supported the ever-so-popular *Create: Aeronautics*.


## Shoulder Fox

May 15, 2026

Ported old behaviour from 1.20 that allowed the player to pick up
baby vanilla Foxes and carry them on the shoulders like Parrots.

Originally, vanilla Foxes were gonna pick our mod's custom berry bushes.
Since transporting vanilla mobs is a hassle (*cough cough Villagers*),
I added this feature to make it easier to transport your Foxes to your berry farms.
Though it's not completely brainless either: like vanilla Parrots,
jumping or taking damage will cause the Foxes to dismount.

This port is fully functional and implemented.
However, we might not need it in our mod... unfortunate.


## Braixen entity port

May 16 to Jun 23, 2026

I decided that I didn't want to fight vanilla Fox hardcode
and I wanted to add my own Fox entity -- so I have full control
over its behaviour.

I'm not an artist, so I will try to "borrow" as much high-quality art as I can.
The good news is that Cobblemon's assets are open-source
and don't come with an overly restrictive license.

### Pokemon Choice

Although I've not been playing Cobblemon consistently,
I'm like one of the OGs that remember what the models used to look like
before many of them were overhauled and redesigned.
As such, I will be referring to "old" and "new" models.

Vaporeon was first ported as a smoke test, to see if GPT5.X
could port over the Kotlin rendering code. Surprisingly, it worked!
Vaporeon was later deleted due to the newer model's lack of animations,
and the old model was kinda lackluster in comparison.

Sylveon was also considered, but again, the new model lacked animations,
and the old model lacked depth. Kinda sucks to say no to my favourite 'mon.
Maybe when Cobblemon adds more animations in future updates?

I inspected Braixen and its evo line next, cuz *some* people fantasize about
this humanoid fox to an "unhealthy" degree. I thought it'd be funny to lean 
into this trope have Braixen "maids" pick berries for your industrial complex.
The new models for Braixen and Delphox make them look too aggressive or sassy,
so I used my elite-ball knowledge of old models to port the older Braixen model,
which looked cuter and passive.

The old Braixen model was basically perfect.
There was a built-in walking animations and an idle animation.
In contrast, Delphox's old model didn't have a complete animation for the lower half of her body;
it's like she would "slide" across the ground while her upper body was animated.
So I just focused on Braixen.

I also ported the Delphox by accident, it is on a separate branch in case I pick it up later.

Fennekin was considered if we ever decide to add a breeding system
for our Braxiens: Fennekin would be the "baby" when two Braixens breed.
However, I'm not considering a breeding system for Braixen right now.

Any other (not-fox-like) Pokemon in the `blockbench` folder are most likely unrelated to the mod.
I just moved them there to browse at my convenience, so I don't need to comb through
the giant Cobblemon Assets repo.

### Porting Goals

- Have an entity like a vanilla Fox that I have full code control over
  - E.g. picks berry bushes, defends trusted players, vanilla death animation
- Remove behaviour from the vanilla Fox that I didn't need or like
  - E.g. sleeping during day, being scared/targeted by wild Wolves, etc.
- Should look decently animated
  - when eating, food particles should appear at the "mouth"
  - visibly holds held item
  - leads should appear attached regardless of animation
  - animations should be smooth, e.g. animation transitions
  - I added a basic arm-raising animation during munching
- Have a spawn egg
- Port the shiny variant, like how vanilla Fox has two colour options

Roughly speaking, May 16 to 31 was working on porting,
and June 20 to 23 was spent polishing the port.

**Merged as PR #1**

### Loose ends

No way to spawn Braixen in Survival mode.
Since Braixen is kinda like the "bottleneck" in this mod
(berry production limits how much processing you can do downstream),
I don't want to make it super easy for players to spawn her.
Yet, I also don't want to make it impossible for players to spawn her in.
We need a system that allows you to spawn Braixens in good quantity
but also make it somewhat punishing if you don't take care of your Braixens.
I don't want players to see them as "disposable."

Breeding code was been intentionally commented out in case we want it in the future.
However, I feel like breeding makes them very disposable.
So the code will probably stay commented out.

## Jam Production

Jul 4 until now.

Long hiatus because Deltarune Ch. 5 released and Palworld 1.0 released.
Plus, my free Codex subscription expired sometime in-between,
and I didn't renew it (busy enjoying video games).

The goal is to add a series of items, blocks, fluids, and recipes
to create a production chain for berry jam.
I am unsure if this branch should also be responsible for processing-related
block entities (like a furnace or crusher).
Otherwise, we can rely on Create's built-in processing options for now.

The basic processing line should look like this:

1. (optional) wash berries. "washed berries" can be a separate item.
2. crush washed berries into pulp. pulp is liquid
3. pulp + sugar + lemon juice + pectin --(heated mixing)--> heated slurry
4. (optional) impurities removed from slurry
5. Hot slurry is poured in glass jars
6. Jars are sealed and inverted.
7. Jars are cooled.

### Fluid Implementation

I first tried to register "tinted water."

Next, I tried to create a tinted version of Create's liquid Honey and Chocolate textures,
but then I found out that they were copyrighted instead of MIT.
Create assets are copyrighted but the code is MIT.
I've left the copyright stuff on a separate branch so we can reference it later.

Not all is lost though, as I devised various ways to convert an existing, coloured texture
into a bunch of generic textures. When provided custom tints, these
generic textures combine to created a tinted version of the original, coloured texture.

The algorithm works well for textures with a low number of unique colours.
We basically take 3 tints as input: if we sorted all the unique colours
in the texture from darkest to brightest, then the input would be
the darkest colour, the median colour, and the brightest colour.
The colours in-between are calculated as a mix between adjacent input colours. 
This way, we can reverse-engineer a coloured texture into a generic one,
then apply three tints to get a similarly-textured texture with a different colour scheme.

I tried to look for other open-source repos with decent colour textures to replace Create.
I found Tinker's Construct, which has some custom textures for fluids.
Modern Industrialization has an animated "bubbling" overlay over tinted water that we could use,
but the mod only provides "still" fluids, no flowing fluids.

This guy also has some textures (CC-BY 4.0):
`https://github.com/malcolmriley/unused-textures`.
Oritech borrows some textures from them. The mod itself also has some great textures.
However, some of those textures just feel *too* animated and a bit disgusting as food.

Maybe I should just use tinted water and lava for simplicity for now...
