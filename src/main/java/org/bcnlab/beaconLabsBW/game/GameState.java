package org.bcnlab.beaconLabsBW.game;

/**
 * Represents the different states of a BedWars game
 */
public enum GameState {
    WAITING,     // Waiting for players
    STARTING,    // Countdown to start
    RUNNING,     // Game in progress
    ENDING       // Game over, showing winner
}
