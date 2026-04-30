package com.quoridor.model;

import java.util.Objects;

/**
 * Immutable (row, col) coordinate on the 9x9 board.
 *
 * Row 0 is the top edge; row 8 is the bottom edge.
 * Col 0 is the left edge; col 8 is the right edge.
 *
 * Using a dedicated class (instead of raw int pairs) means:
 *  - We can use Position as a HashMap key (equals + hashCode).
 *  - Method signatures are self-documenting.
 *  - We catch row/col swaps at compile time.
 */
public class Position {

    public final int row;
    public final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // ── Neighbour helpers ────────────────────────────────────

    /** Returns the position one step in the given direction. */
    public Position step(Direction dir) {
        return switch (dir) {
            case UP    -> new Position(row - 1, col);
            case DOWN  -> new Position(row + 1, col);
            case LEFT  -> new Position(row,     col - 1);
            case RIGHT -> new Position(row,     col + 1);
        };
    }

    /** Returns true if this position is inside the 9x9 board. */
    public boolean isValid() {
        return row >= 0 && row <= 8 && col >= 0 && col <= 8;
    }

    // ── Object overrides ─────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}