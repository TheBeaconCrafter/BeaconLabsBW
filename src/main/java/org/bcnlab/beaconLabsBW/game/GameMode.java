package org.bcnlab.beaconLabsBW.game;

/**
 * Represents the different game modes available in BedWars
 */
public enum GameMode {
    NORMAL("Normal"),
    ULTIMATES("Ultimates");
    
    private final String displayName;
    
    GameMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
