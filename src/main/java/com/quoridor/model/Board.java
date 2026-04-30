package com.quoridor.model;

/**
 * Represents the 9x9 Quoridor board and all placed walls.
 *
 * Wall storage:
 *   hWalls[r][c] == true  →  horizontal wall anchored at (r,c)
 *                             blocks movement between row r and row r+1
 *                             across columns c and c+1
 *
 *   vWalls[r][c] == true  →  vertical wall anchored at (r,c)
 *                             blocks movement between col c and col c+1
 *                             across rows r and r+1
 *
 * Both arrays are 8x8 because walls span 2 cells and can't
 * be anchored at index 8.
 *
 * This class is responsible ONLY for wall storage and edge queries.
 * Move legality and pathfinding live in GameState/MoveValidator.
 */
public class Board {

    // ── Constants ────────────────────────────────────────────

    public static final int SIZE = 9;          // cells per side
    public static final int WALL_SLOTS = 8;    // wall anchor indices per side

    // ── Wall arrays ──────────────────────────────────────────

    // hWalls[row][col]: horizontal wall below row, starting at col
    private final boolean[][] hWalls;

    // vWalls[row][col]: vertical wall right of col, starting at row
    private final boolean[][] vWalls;

    // ── Constructor ──────────────────────────────────────────

    public Board() {
        hWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
        vWalls = new boolean[WALL_SLOTS][WALL_SLOTS];
        // Java initialises boolean arrays to false — no walls placed yet
    }

    // ── Wall placement ───────────────────────────────────────

    /**
     * Places a wall on the board.
     * Does NOT validate legality — call MoveValidator first.
     *
     * @param wall  The wall to place (orientation + anchor)
     */
    public void placeWall(Wall wall) {
        int r = wall.getAnchor().row;
        int c = wall.getAnchor().col;

        if (wall.isHorizontal()) {
            hWalls[r][c] = true;
        } else {
            vWalls[r][c] = true;
        }
    }

    // ── Edge (movement) queries ──────────────────────────────

    /**
     * Returns true if movement from 'from' to 'to' is blocked by a wall.
     * Assumes 'from' and 'to' are orthogonally adjacent.
     *
     * This is the core wall-query method — every move check calls this.
     */
    public boolean isBlockedByWall(Position from, Position to) {
        int r = from.row;
        int c = from.col;

        // Moving UP: row decreases by 1
        // Blocked if there's a horizontal wall directly ABOVE 'from',
        // i.e., anchored at (r-1, c) or (r-1, c-1)
        if (to.row == r - 1) {
            return isHWallAt(r - 1, c) || isHWallAt(r - 1, c - 1);
        }

        // Moving DOWN: row increases by 1
        // Blocked if there's a horizontal wall directly BELOW 'from',
        // i.e., anchored at (r, c) or (r, c-1)
        if (to.row == r + 1) {
            return isHWallAt(r, c) || isHWallAt(r, c - 1);
        }

        // Moving LEFT: col decreases by 1
        // Blocked if there's a vertical wall directly LEFT of 'from',
        // i.e., anchored at (r, c-1) or (r-1, c-1)
        if (to.col == c - 1) {
            return isVWallAt(r, c - 1) || isVWallAt(r - 1, c - 1);
        }

        // Moving RIGHT: col increases by 1
        // Blocked if there's a vertical wall directly RIGHT of 'from',
        // i.e., anchored at (r, c) or (r-1, c)
        if (to.col == c + 1) {
            return isVWallAt(r, c) || isVWallAt(r - 1, c);
        }

        // Not adjacent — should never reach here in normal use
        return false;
    }

    // ── Wall existence checks ────────────────────────────────

    /**
     * Safe bounds-checked lookup for horizontal wall array.
     * Returns false for out-of-bounds indices (no wall there).
     */
    public boolean isHWallAt(int r, int c) {
        if (r < 0 || r >= WALL_SLOTS || c < 0 || c >= WALL_SLOTS) return false;
        return hWalls[r][c];
    }

    /**
     * Safe bounds-checked lookup for vertical wall array.
     * Returns false for out-of-bounds indices (no wall there).
     */
    public boolean isVWallAt(int r, int c) {
        if (r < 0 || r >= WALL_SLOTS || c < 0 || c >= WALL_SLOTS) return false;
        return vWalls[r][c];
    }

    // ── Deep copy ────────────────────────────────────────────

    /**
     * Returns a full copy of the board with the same walls.
     * Used by GameState.copy() for AI simulation.
     */
    public Board copy() {
        Board copy = new Board();
        for (int r = 0; r < WALL_SLOTS; r++) {
            for (int c = 0; c < WALL_SLOTS; c++) {
                copy.hWalls[r][c] = this.hWalls[r][c];
                copy.vWalls[r][c] = this.vWalls[r][c];
            }
        }
        return copy;
    }

    // ── Accessors for rendering ──────────────────────────────

    /** Raw read-only access for the renderer — do not modify. */
    public boolean[][] getHWalls() { return hWalls; }
    public boolean[][] getVWalls() { return vWalls; }
}