package com.quoridor.model;

/**
 * Represents a single placed wall.
 *
 * A wall is defined by:
 *  - Its orientation (HORIZONTAL or VERTICAL)
 *  - Its anchor position (the top-left corner of the 2-cell span it covers)
 *
 * HORIZONTAL wall at (r, c):
 *   Blocks movement between row r and row r+1,
 *   covering the gap below cells (r,c) and (r, c+1).
 *
 * VERTICAL wall at (r, c):
 *   Blocks movement between col c and col c+1,
 *   covering the gap beside cells (r,c) and (r+1, c).
 *
 * Valid anchor range for both orientations: row 0-7, col 0-7
 * (walls span 2 cells, so they can't be anchored at index 8).
 */
public class Wall {

    // ── Inner enum ───────────────────────────────────────────

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    // ── Fields ───────────────────────────────────────────────

    private final Orientation orientation;
    private final Position anchor;   // top-left cell of the wall's span

    // ── Constructor ──────────────────────────────────────────

    public Wall(Orientation orientation, Position anchor) {
        this.orientation = orientation;
        this.anchor      = anchor;
    }

    // ── Getters ──────────────────────────────────────────────

    public Orientation getOrientation() { return orientation; }
    public Position    getAnchor()      { return anchor; }

    // ── Convenience ──────────────────────────────────────────

    public boolean isHorizontal() { return orientation == Orientation.HORIZONTAL; }
    public boolean isVertical()   { return orientation == Orientation.VERTICAL;   }

    @Override
    public String toString() {
        return orientation + " wall @ " + anchor;
    }
}