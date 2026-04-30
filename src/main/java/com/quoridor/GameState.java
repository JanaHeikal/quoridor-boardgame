package com.quoridor;

/**
 * GameState is the single source of truth for everything that describes
 * a game in progress:
 *   - The board (cells and wall slots)
 *   - Both players (positions, wall counts)
 *   - Whose turn it is
 *   - Whether the game is over and who won
 *
 * The View only reads GameState; it never writes it.
 * The Controller writes GameState by calling well-defined methods.
 *
 * This skeleton will be fully implemented in Step 2.
 */
public class GameState {

    public GameState() {
        // Step 2 will initialise the board, players, and turn here.
    }
}