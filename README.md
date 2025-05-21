Latest version: v1.5.1

The latest version can currently only be found on Modrinth https://modrinth.com/plugin/raycasted-entity-occlusions/

This is a simple server-side plugin for PaperMC (May also work with Spigot, this is untested) that hides/culls entities from players if they do not have line-of-sight. All raycasts are run async to reduce performance impact.
Use cases:

- To prevent mob nametags from being visible through walls
  - If the mobs don't exist, you can't see them
- Act as an "anticheat" to block players from using mods such as freecam to find entities through walls
  - This is not an actual anticheat and will not detect freecam, instead just making freecam less useful
  - Freecam will still be useful for locating structures and features such as caves
- Reduce client lag
  - In servers with large megabases full of item frames, armour stands and other entities, RaycastedEntityOcclusion will prevent these entities from lagging the client unless they are within line-of-sight

EVERYTHING BELOW IS OUT OF DATE, READ THE CHANGELOG FOR v1.4.1 https://modrinth.com/plugin/raycasted-entity-occlusions/version/1.4.1

Default Config:

- AlwaysShowRadius: 8
- RaycastRadius: 48
- SearchRadius: 52
- MoreChecks: false
- OccludePlayers: false
- RecheckInterval: 20 

Entities inside the AlwaysShowRadius are always shown, even if occluded. This is to prevent nasty surprises when turning a corner quickly. 

The SearchRadius is a cubic bounding box around the player within which entities will be checked. If an entity is within the SearchRadius but not the RaycastRadius, it will be automatically hidden.

The RaycastRadius is the radius where entities will be checked. If an entity is currently hidden it will run a raycast every tick. This is to quickly detect when the entity becomes visible. If the entity is currently visible, it will only run the raycast every {RecheckInterval} ticks to reduce performance impact.

MoreChecks toggles between two raycasting modes. By default (MoreChecks = false) the plugin runs one raycast from the center of each mob to the player. This means that if the center of the mob is hidden but some parts should be visible, the player won't be able to see the mob at all. Enabling MoreChecks runs a raycast from the top, bottom, left and right of the entity to the player.

OccludePlayers toggles whether players will be checked. Enabling this prevents ESP mods from revealing players.


Known issues:
- Due to the nature of the plugin, there will be a short delay once an entity should be visible before it appears, causing it to appear like it "popped" into view. This issue is partially resolved by turning MoreChecks on
- If OccludePlayers is set to true any occluded players will be removed from tablist
- When updating the plugin to a version with more config versions, no checks are performed to detect this and add the new configs to config.yml. This means that the config file will be missing several options. (This is an issue when updating to v1.1.0 or v1.2.0). To fix, just copy the new config options from the Default Config section above.
