package com.quoridor.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * MoveValidator contains all move-legality logic for Quoridor.
 *
 * It is stateless — all methods are static and operate on
 * a GameState passed as a parameter.  This keeps GameState
 * clean and makes the validator easy to test in isolation.
 *
 * Responsibilities:
 *  1. Determine legal pawn moves (normal, jump, diagonal jump).
 *  2. Determine legal wall placements (overlap, cross, path-blocking).
 *  3. Provide BFS to verify a path exists after wall placement.
 */
public class MoveValidator {

    // ── Private constructor ───────────────────────────────────
    // This class is never instantiated — all methods are static.
    private MoveValidator() {}

    // ════════════════════════════════════════════════════════
    //  PAWN MOVEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Returns all positions the current player's pawn can legally
     * move to, given the current game state.
     *
     * This is the method the UI calls to highlight valid cells,
     * and the AI calls to enumerate possible pawn moves.
     *
     * @param state  The current game state.
     * @return       List of legal destination positions.
     */
    public static List<Position> getLegalPawnMoves(GameState state) {
        List<Position> moves    = new ArrayList<>();
        Position       current  = state.getCurrentPlayer().getPosition();
        Position       opponent = state.getOpponent().getPosition();
        Board          board    = state.getBoard();

        // Try all four orthogonal directions
        for (Direction dir : Direction.values()) {
            Position neighbor = current.step(dir);

            // Out of bounds — skip
            if (!neighbor.isValid()) continue;

            // Wall between current and neighbor — skip
            if (board.isBlockedByWall(current, neighbor)) continue;

            // Neighbor is occupied by opponent — jump logic
            if (neighbor.equals(opponent)) {
                addJumpMoves(moves, board, current, neighbor, dir);
                continue;
            }

            // Normal move — unoccupied, unblocked
            moves.add(neighbor);
        }

        return moves;
    }

    /**
     * Returns true if moving the current player's pawn to 'target'
     * is legal in the given state.
     */
    public static boolean isLegalPawnMove(GameState state, Position target) {
        return getLegalPawnMoves(state).contains(target);
    }

    // ── Jump logic ───────────────────────────────────────────

    /**
     * Handles the jump and diagonal-jump cases.
     *
     * Called when the neighbor in direction 'dir' is occupied
     * by the opponent.
     *
     * @param moves     The list to add legal jump destinations to.
     * @param board     The current board (for wall checks).
     * @param current   The moving player's current position.
     * @param opponent  The opponent's position (= current.step(dir)).
     * @param dir       The direction from current toward the opponent.
     */
    private static void addJumpMoves(
            List<Position> moves,
            Board board,
            Position current,
            Position opponent,
            Direction dir) {

        // Straight jump: land on the far side of the opponent
        Position straightLanding = opponent.step(dir);

        boolean straightBlocked =
            !straightLanding.isValid()                          // board edge
            || board.isBlockedByWall(opponent, straightLanding); // wall behind opponent

        if (!straightBlocked) {
            // Straight jump is available
            moves.add(straightLanding);
        } else {
            // Straight jump is blocked → offer diagonal jumps instead
            List<Direction> sideDirections = getSideDirections(dir);

            for (Direction sideDir : sideDirections) {
                Position diagonalLanding = opponent.step(sideDir);

                if (!diagonalLanding.isValid()) continue;
                if (board.isBlockedByWall(opponent, diagonalLanding)) continue;

                // Diagonal landing must not be occupied by the moving player
                // (In a 2-player game this can't happen, but good to be safe)
                moves.add(diagonalLanding);
            }
        }
    }

    /**
     * Returns the two directions perpendicular to the given direction.
     *
     * If you're moving UP, the sides are LEFT and RIGHT.
     * If you're moving LEFT, the sides are UP and DOWN.
     *
     * Used to compute diagonal jump candidates.
     */
    private static List<Direction> getSideDirections(Direction dir) {
        return switch (dir) {
            case UP, DOWN  -> List.of(Direction.LEFT, Direction.RIGHT);
            case LEFT, RIGHT -> List.of(Direction.UP, Direction.DOWN);
        };
    }

    // ════════════════════════════════════════════════════════
    //  WALL PLACEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Returns true if placing 'wall' is legal in the given state.
     *
     * Checks (in order):
     *  1. The current player has walls remaining.
     *  2. The anchor is within the valid 8x8 placement grid.
     *  3. The wall does not overlap an existing wall.
     *  4. The wall does not cross an existing wall.
     *  5. After placement, both players still have a path to their goal.
     *
     * @param state  The current game state.
     * @param wall   The wall the current player wants to place.
     */
    public static boolean isLegalWallPlacement(GameState state, Wall wall) {

        // 1. Player must have walls left
        if (state.getCurrentPlayer().getWallsRemaining() <= 0) return false;

        // 2. Anchor must be in the 8x8 placement grid
        int r = wall.getAnchor().row;
        int c = wall.getAnchor().col;
        if (r < 0 || r >= Board.WALL_SLOTS) return false;
        if (c < 0 || c >= Board.WALL_SLOTS) return false;

        Board board = state.getBoard();

        // 3 & 4. Check overlap and crossing
        if (wall.isHorizontal()) {
            // Overlap: another horizontal wall at the same or adjacent position
            if (board.isHWallAt(r, c))     return false; // exact same spot
            if (board.isHWallAt(r, c - 1)) return false; // overlaps to the left
            if (board.isHWallAt(r, c + 1)) return false; // overlaps to the right

            // Cross: a vertical wall at the same anchor crosses this horizontal one
            if (board.isVWallAt(r, c)) return false;

        } else { // VERTICAL
            // Overlap: another vertical wall at the same or adjacent position
            if (board.isVWallAt(r, c))     return false;
            if (board.isVWallAt(r - 1, c)) return false;
            if (board.isVWallAt(r + 1, c)) return false;

            // Cross: a horizontal wall at the same anchor crosses this vertical one
            if (board.isHWallAt(r, c)) return false;
        }

        // 5. Path check: simulate placing the wall, then run BFS for both players
        //    We use GameState.copy() so the real state is never modified.
        GameState simulated = state.copy();
        simulated.getBoard().placeWall(wall);

        boolean p1HasPath = hasPathToGoal(
            simulated.getBoard(),
            simulated.getPlayer1().getPosition(),
            simulated.getPlayer1().getGoalRow()
        );

        boolean p2HasPath = hasPathToGoal(
            simulated.getBoard(),
            simulated.getPlayer2().getPosition(),
            simulated.getPlayer2().getGoalRow()
        );

        return p1HasPath && p2HasPath;
    }

    // ════════════════════════════════════════════════════════
    //  PATHFINDING (BFS)
    // ════════════════════════════════════════════════════════

    /**
     * Returns true if there is at least one unblocked path from
     * 'start' to any cell in 'goalRow', given the current board walls.
     *
     * This is a standard Breadth-First Search (BFS) over the 9x9 grid.
     *
     * BFS guarantees we find A path if one exists — we don't need the
     * shortest path here, just confirmation that one exists.
     *
     * Time complexity: O(81) in the worst case — the board is tiny,
     * so this is extremely fast even when called many times by the AI.
     *
     * @param board    The board to search (with walls).
     * @param start    The starting position (player's pawn).
     * @param goalRow  The row the player needs to reach.
     */
    public static boolean hasPathToGoal(Board board, Position start, int goalRow) {

        // visited array prevents revisiting cells
        boolean[][] visited = new boolean[Board.SIZE][Board.SIZE];

        Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        visited[start.row][start.col] = true;

        while (!queue.isEmpty()) {
            Position current = queue.poll();

            // Reached the goal row — path exists
            if (current.row == goalRow) return true;

            // Explore all four orthogonal neighbours
            for (Direction dir : Direction.values()) {
                Position next = current.step(dir);

                if (!next.isValid()) continue;
                if (visited[next.row][next.col]) continue;
                if (board.isBlockedByWall(current, next)) continue;

                visited[next.row][next.col] = true;
                queue.add(next);
            }
        }

        // Exhausted all reachable cells — no path to goal
        return false;
    }

    // ════════════════════════════════════════════════════════
    //  WALL ENUMERATION (used by AI)
    // ════════════════════════════════════════════════════════

    /**
     * Returns all legal wall placements for the current player.
     *
     * The AI uses this to enumerate every possible wall move.
     * There are at most 128 candidate walls (64 horizontal + 64 vertical),
     * and many will be filtered by overlap/cross/path checks.
     *
     * @param state  The current game state.
     * @return       List of all legal walls the current player can place.
     */
    public static List<Wall> getLegalWallPlacements(GameState state) {
        List<Wall> walls = new ArrayList<>();

        if (state.getCurrentPlayer().getWallsRemaining() <= 0) return walls;

        for (int r = 0; r < Board.WALL_SLOTS; r++) {
            for (int c = 0; c < Board.WALL_SLOTS; c++) {
                Wall hWall = new Wall(
                    Wall.Orientation.HORIZONTAL, new Position(r, c));
                Wall vWall = new Wall(
                    Wall.Orientation.VERTICAL,   new Position(r, c));

                if (isLegalWallPlacement(state, hWall)) walls.add(hWall);
                if (isLegalWallPlacement(state, vWall)) walls.add(vWall);
            }
        }

        return walls;
    }
}