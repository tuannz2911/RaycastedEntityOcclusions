This is a simple server-side plugin for PaperMC (May also work with Spigot, this is untested) that hides entities from players if they do not have line-of-sight. All raycasts are run async to reduce performance impact.
Use cases:

- To prevent mob nametags from being visible through walls
  - If the mobs don't exist, you can't see them
- Act as an "anticheat" to block players from using mods such as freecam to find entities through walls
  - This is not an actual anticheat and will not detect freecam, instead just making freecam less useful
  - Freecam will still be useful for locating structures and features such as caves
- Reduce client lag
  - In servers with large megabases full of item frames, armour stands and other entities, RaycastedEntityOcclusion will prevent these entities from lagging the client unless they are within line-of-sight

Config info:

The config has three settings:

AlwaysShowRadius: 4
RaycastRadius: 64
SearchRadius: 72

Entities inside the AlwaysShowRadius are always shown, even if occluded. This is to prevent nasty surprises when turning a corner quickly. 
The SearchRadius is a misnomer, it's actually a cubic bounding box around the player within which entities will be checked. If an entity is within the SearchRadius but not the RaycastRadius, it will be automatically hidden.
The RaycastRadius is the radius where entities will be checked. If an entity is currently hidden it will run a raycast every tick. This is to quickly detect when the entity becomes visible. If the entity is currently visible, it will only ruun the raycast every 10 ticks to reduce performance impact.

Known issues:
- Players are also occluded. This itself is not an issue, however hiding players also removes them from the tab list and command autocomplete.
- Due to the nature of the plugin, there will be a short delay once an entity should be visible before it appears, causing it to appear like it "popped" into view
