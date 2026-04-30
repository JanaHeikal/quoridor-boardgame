package com.quoridor.model;

/**
 * Represents one player in the game.
 *
 * Holds:
 *  - Current pawn position
 *  - Number of walls remaining to place
 *  - The goal row this player is trying to reach
 *  - A display name and numeric ID (1 or 2)
 *
 * Player 1 starts at row 8 (bottom) and heads toward row 0.
 * Player 2 starts at row 0 (top)    and heads toward row 8.
 */
public class Player {

    // ── Fields ───────────────────────────────────────────────

    private final int    id;          // 1 or 2
    private final String name;        // display name
    private final int    goalRow;     // row the player must reach to win

    private Position position;        // current pawn cell
    private int      wallsRemaining;  // walls left to place (starts at 10)

    // ── Constructor ──────────────────────────────────────────

    /**
     * @param id       1 or 2
     * @param name     display name (e.g. "Player 1" or "Computer")
     * @param start    starting pawn position
     * @param goalRow  the row index this player must reach to win
     */
    public Player(int id, String name, Position start, int goalRow) {
        this.id             = id;
        this.name           = name;
        this.goalRow        = goalRow;
        this.position       = start;
        this.wallsRemaining = 10;
    }

    // ── Getters & Setters ────────────────────────────────────

    public int      getId()             { return id; }
    public String   getName()           { return name; }
    public int      getGoalRow()        { return goalRow; }
    public Position getPosition()       { return position; }
    public int      getWallsRemaining() { return wallsRemaining; }

    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Deducts one wall when a player places a wall.
     * Call only after validating the player has walls remaining.
     */
    public void useWall() {
        if (wallsRemaining <= 0) {
            throw new IllegalStateException(
                name + " has no walls remaining to place."
            );
        }
        wallsRemaining--;
    }

    /** Returns true if this player's pawn has reached their goal row. */
    public boolean hasWon() {
        return position.row == goalRow;
    }

    // ── Deep copy ────────────────────────────────────────────

    /**
     * Returns a copy of this player with the same state.
     * Used by GameState.copy() so the AI can simulate moves
     * without modifying the real game state.
     */
    public Player copy() {
        Player copy = new Player(id, name, new Position(position.row, position.col), goalRow);
        copy.wallsRemaining = this.wallsRemaining;
        return copy;
    }

    @Override
    public String toString() {
        return name + " @ " + position + " | walls left: " + wallsRemaining;
    }
}