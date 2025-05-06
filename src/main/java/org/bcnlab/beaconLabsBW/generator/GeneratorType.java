package org.bcnlab.beaconLabsBW.generator;

/**
 * Types of resource generators in BedWars
 */
public enum GeneratorType {
    IRON,
    GOLD,
    TEAM,  // Replaces individual IRON and GOLD generators for teams, upgradeable through forge upgrades
    EMERALD,
    DIAMOND
}
