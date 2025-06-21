# BetterClaim - Advanced Minecraft Land Claiming Plugin
![version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![MC version](https://img.shields.io/badge/MC-1.20+-brightgreen.svg)

A comprehensive Paper 1.20+ plugin for chunk-based land claiming with advanced GUI management, hierarchical permissions, and database persistence.

## Features

### Core Functionality
- **Chunk-based claiming system** - Players can claim and manage chunks
- **Two-step claiming process** - Create initial claim with `/claim create`, then expand with `/claim expand`
- **Adjacent expansion only** - New chunks must be adjacent to existing claims
- **SQLite database storage** - Persistent claim data
- **Multi-world support** - Works across all server worlds

### How Claiming Works
1. **Initial Claim**: Use `/claim create` to create your first claim in the current chunk
2. **Expansion**: Use `/claim expand` while standing in a chunk adjacent to your existing claim
3. **Adjacent Rule**: You can only expand to chunks that share a side (not diagonal) with your current claim
4. **Size Limits**: Each claim can expand up to the configured maximum chunks (default: 100)

### Permission System
- **Hierarchical trust levels**:
  - `trusted` - Basic permissions (break/place blocks, use containers)
  - `moderator` - Extended permissions (redstone, pressure plates, entities)
  - `admin` - Full permissions (animal damage, all interactions)

### GUI Management
- **Claim Management GUI** - Visual interface for claim owners
- **Flag Configuration GUI** - Toggle protection settings
- **Trust Management GUI** - Add/remove trusted players with levels
- **Admin Panel GUI** - Server administration tools

### Protection Features
- **Comprehensive block protection** - Prevent unauthorized building
- **Interaction protection** - Doors, buttons, levers, pressure plates
- **Container protection** - Chests, furnaces, dispensers
- **Entity protection** - Animal damage, entity interaction
- **PvP control** - Enable/disable combat per claim
- **Environmental protection** - Fire spread, explosions, lava/water flow

## Commands

### Player Commands (`/claim`)
- `/claim create` - Claim the current chunk (creates a new claim)
- `/claim expand` - Expand existing claim to current chunk (must be adjacent)
- `/claim delete` - Delete your claim
- `/claim info` - Show claim information
- `/claim list` - List your claims
- `/claim trust <player> [level]` - Trust a player (trusted/moderator/admin)
- `/claim untrust <player>` - Remove player trust
- `/claim flag <flag> <true|false>` - Set claim flags
- `/claim gui` - Open claim management GUI

### Admin Commands (`/claimadmin`)
- `/claimadmin list [player]` - List all claims or player's claims
- `/claimadmin delete <player>` - Delete a player's claims
- `/claimadmin info <player>` - Show detailed claim information
- `/claimadmin teleport <player>` - Teleport to player's claim
- `/claimadmin gui` - Open admin management GUI
- `/claimadmin reload` - Reload configuration
- `/claimadmin stats` - Show plugin statistics

## Permissions

### Basic Permissions
- `betterclaim.claim` - Use basic claim commands (default: true)
- `betterclaim.admin` - Use administrative commands (default: op)
- `betterclaim.bypass` - Bypass all claim protections (default: op)
- `betterclaim.*` - All permissions

## Configuration Files

<details>
<summary><strong>config.yml</strong> - Main Configuration</summary>

```yaml
# BetterClaim Configuration File
# For support, visit: https://github.com/Fl1uxxNoob/BetterClaim

# Database settings
database:
  type: sqlite
  filename: claims.db
  
# Claim settings
claim:
  # Maximum number of chunks per claim
  max-chunks-per-claim: 100
  # Maximum number of claims per player
  max-claims-per-player: 1
  # Require claims to be adjacent (expandable only)
  require-adjacent: true
  # Show claim borders when entering/leaving
  show-borders: true
  # Border particle effect
  border-particle: REDSTONE
  # Auto-save interval in minutes
  auto-save-interval: 5
  
# Default claim flags
default-flags:
  pvp: false
  mob-spawning: true
  mob-damage: true
  explosions: false
  fire-spread: false
  lava-flow: false
  water-flow: true
  item-pickup: false
  block-break: false
  block-place: false
  container-access: false
  door-access: false
  button-access: false
  lever-access: false
  pressure-plate-access: false
  redstone-access: false
  entity-interact: false
  animal-damage: false
  
# Trust levels and their permissions
trust-levels:
  trusted:
    - block-break
    - block-place
    - container-access
    - door-access
    - button-access
    - lever-access
    - item-pickup
  moderator:
    - block-break
    - block-place
    - container-access
    - door-access
    - button-access
    - lever-access
    - pressure-plate-access
    - redstone-access
    - entity-interact
    - item-pickup
  admin:
    - block-break
    - block-place
    - container-access
    - door-access
    - button-access
    - lever-access
    - pressure-plate-access
    - redstone-access
    - entity-interact
    - animal-damage
    - item-pickup
    
# GUI settings
gui:
  # GUI title prefixes
  titles:
    claim-main: "&6&lClaim Management"
    claim-flags: "&6&lClaim Flags"
    claim-trust: "&6&lClaim Trust"
    admin-main: "&c&lAdmin Panel"
    
# Performance settings
performance:
  # Cache size for claims
  cache-size: 1000
  # Cache expiration time in minutes
  cache-expiration: 30
  # Async operations
  async-saves: true
```

</details>

<details>
<summary><strong>messages.yml</strong> - Customizable Messages</summary>

```yaml
# BetterClaim Messages File
# Use & for color codes
# Placeholders: {player}, {claim}, {chunk}, {flag}, {trust_level}, {x}, {z}

# General messages
prefix: "&8[&6BetterClaim&8]&r "
no-permission: "&cYou don't have permission to use this command."
player-only: "&cThis command can only be used by players."
invalid-usage: "&cInvalid usage. Use &e{usage}&c for help."
reload-success: "&aConfiguration reloaded successfully."

# Claim messages
claim:
  created: "&aSuccessfully claimed chunk at &e{x}, {z}&a!"
  deleted: "&aSuccessfully deleted your claim."
  already-claimed: "&cThis chunk is already claimed by &e{player}&c."
  already-own-chunk: "&cYou already own this chunk."
  not-claimed: "&cThis chunk is not claimed."
  not-your-claim: "&cThis chunk is not claimed by you."
  no-claims: "&cYou don't have any claims."
  max-claims-reached: "&cYou have reached the maximum number of claims ({max})."
  max-chunks-reached: "&cYou have reached the maximum number of chunks per claim ({max})."
  must-be-adjacent: "&cNew chunks must be adjacent to your existing claim."
  cannot-claim-here: "&cYou cannot claim this chunk."
  enter: "&aEntering claim: &e{claim} &aowned by &e{player}"
  leave: "&7Leaving claim: &e{claim}"
  expand:
    no-claims: "&cYou must have at least one claim to expand."
    not-adjacent: "&cThis chunk is not adjacent to any of your claims."
    max-chunks: "&cThis claim has reached the maximum size ({max} chunks)."
    success: "&aSuccessfully expanded claim &e{claim} &ato include chunk &e{x}, {z}&a!"
    failed: "&cFailed to expand claim. Please try again."
  
# Trust messages
trust:
  added: "&aSuccessfully added &e{player} &aas &e{trust_level} &ato your claim."
  removed: "&aSuccessfully removed &e{player} &afrom your claim."
  already-trusted: "&c{player} is already trusted in your claim."
  not-trusted: "&c{player} is not trusted in your claim."
  cannot-trust-self: "&cYou cannot trust yourself."
  trust-level-updated: "&aUpdated &e{player}&a's trust level to &e{trust_level}&a."
  
# Protection messages
protection:
  block-break: "&cYou cannot break blocks in this claim."
  block-place: "&cYou cannot place blocks in this claim."
  container-access: "&cYou cannot access containers in this claim."
  door-access: "&cYou cannot use doors in this claim."
  button-access: "&cYou cannot use buttons in this claim."
  lever-access: "&cYou cannot use levers in this claim."
  pressure-plate-access: "&cYou cannot use pressure plates in this claim."
  redstone-access: "&cYou cannot interact with redstone in this claim."
  entity-interact: "&cYou cannot interact with entities in this claim."
  animal-damage: "&cYou cannot damage animals in this claim."
  item-pickup: "&cYou cannot pick up items in this claim."
  pvp: "&cPvP is disabled in this claim."
  
# Flag messages
flag:
  updated: "&aFlag &e{flag} &ahas been set to &e{value}&a."
  invalid: "&cInvalid flag: &e{flag}&c."
  
# Admin messages
admin:
  claim-deleted: "&aSuccessfully deleted claim owned by &e{player}&a."
  claim-info: "&6Claim Info:\n&7Owner: &e{player}\n&7Chunks: &e{chunks}\n&7Created: &e{created}"
  teleported: "&aTeleported to claim owned by &e{player}&a."
  no-claims-found: "&cNo claims found for player &e{player}&c."
  
# Error messages
error:
  database: "&cDatabase error occurred. Please contact an administrator."
  player-not-found: "&cPlayer &e{player} &cnot found."
  claim-not-found: "&cClaim not found."
  internal: "&cAn internal error occurred. Please try again."
  
# Command help
help:
  claim:
    - "&6&lBetterClaim Commands:"
    - "&e/claim create &7- Claim the current chunk"
    - "&e/claim expand &7- Expand claim to current chunk (must be adjacent)"
    - "&e/claim delete &7- Delete your claim"
    - "&e/claim info &7- Show claim information"
    - "&e/claim list &7- List your claims"
    - "&e/claim trust <player> [level] &7- Trust a player"
    - "&e/claim untrust <player> &7- Untrust a player"
    - "&e/claim flag <flag> <value> &7- Set a claim flag"
    - "&e/claim gui &7- Open claim management GUI"
  admin:
    - "&c&lBetterClaim Admin Commands:"
    - "&e/claimadmin list [player] &7- List claims"
    - "&e/claimadmin delete <player> &7- Delete a claim"
    - "&e/claimadmin info <player> &7- Show claim info"
    - "&e/claimadmin teleport <player> &7- Teleport to claim"
    - "&e/claimadmin gui &7- Open admin GUI"
```

</details>

### Customizable Flags
- `pvp` - Player vs Player combat
- `mob-spawning` - Mob spawning
- `explosions` - TNT and creeper explosions
- `fire-spread` - Fire spreading
- `lava-flow` / `water-flow` - Liquid flow
- `block-break` / `block-place` - Block modifications
- `container-access` - Chest/furnace access
- `door-access` - Door usage
- `button-access` / `lever-access` - Redstone components
- `entity-interact` - Entity interactions
- `animal-damage` - Damage to animals
- `item-pickup` - Item collection

### Trust Level Permissions
Configure what each trust level can do in `config.yml`:
```yaml
trust-levels:
  trusted:
    - block-break
    - block-place
    - container-access
    - door-access
  moderator:
    - (all trusted permissions)
    - redstone-access
    - entity-interact
  admin:
    - (all moderator permissions)
    - animal-damage
```

## Installation

1. Download the `BetterClaim-1.0.0.jar` file
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Configure the plugin in `plugins/BetterClaim/config.yml`
5. Customize messages in `plugins/BetterClaim/messages.yml`

## Database

The plugin uses SQLite by default, creating a `claims.db` file in the plugin folder. All claim data, including:
- Claim ownership and metadata
- Chunk locations
- Flag settings
- Trust relationships
- Member permissions

## API Compatibility

- **Paper 1.20.1+** - Primary target
- **Spigot 1.20+** - Compatible
- **Java 17+** - Required

## Support

The plugin includes comprehensive error handling and logging. Check your server console for any issues and verify configuration files are properly formatted.

## Technical Details

- **Database**: SQLite with async operations
- **Threading**: Async saves and non-blocking operations
- **Performance**: Efficient chunk lookup with caching
- **Memory**: Optimized for large servers
- **Storage**: Minimal database footprint

## Version Compatibility

Built for Paper 1.20.1 but compatible with newer versions. The plugin uses stable APIs that should work with future Minecraft updates.

## Credits

**BetterClaim** was developed by **Fl1uxxNoob**.

## License

AFKGuard is licensed under the **GNU General Public License v3.0** (GPL-3.0).  
You are free to use, modify, and distribute this software under the terms of the license.  
A copy of the license is available in the [LICENSE](./LICENSE) file.
