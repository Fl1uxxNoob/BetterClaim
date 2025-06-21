package net.fliuxx.betterClaim.models;

public enum ClaimFlag {
    PVP(false, "PvP", "Allow player vs player combat"),
    MOB_SPAWNING(true, "Mob Spawning", "Allow mobs to spawn"),
    MOB_DAMAGE(true, "Mob Damage", "Allow mobs to take damage"),
    EXPLOSIONS(false, "Explosions", "Allow explosions"),
    FIRE_SPREAD(false, "Fire Spread", "Allow fire to spread"),
    LAVA_FLOW(false, "Lava Flow", "Allow lava to flow"),
    WATER_FLOW(true, "Water Flow", "Allow water to flow"),
    ITEM_PICKUP(false, "Item Pickup", "Allow non-trusted players to pick up items"),
    BLOCK_BREAK(false, "Block Break", "Allow non-trusted players to break blocks"),
    BLOCK_PLACE(false, "Block Place", "Allow non-trusted players to place blocks"),
    CONTAINER_ACCESS(false, "Container Access", "Allow non-trusted players to access containers"),
    DOOR_ACCESS(false, "Door Access", "Allow non-trusted players to use doors"),
    BUTTON_ACCESS(false, "Button Access", "Allow non-trusted players to use buttons"),
    LEVER_ACCESS(false, "Lever Access", "Allow non-trusted players to use levers"),
    PRESSURE_PLATE_ACCESS(false, "Pressure Plate Access", "Allow non-trusted players to use pressure plates"),
    REDSTONE_ACCESS(false, "Redstone Access", "Allow non-trusted players to interact with redstone"),
    ENTITY_INTERACT(false, "Entity Interact", "Allow non-trusted players to interact with entities"),
    ANIMAL_DAMAGE(false, "Animal Damage", "Allow non-trusted players to damage animals");
    
    private final boolean defaultValue;
    private final String displayName;
    private final String description;
    
    ClaimFlag(boolean defaultValue, String displayName, String description) {
        this.defaultValue = defaultValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    public boolean getDefaultValue() {
        return defaultValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ClaimFlag fromString(String name) {
        try {
            return valueOf(name.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
