package com.quoridor.ai;

import com.quoridor.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AIPlayer implements the computer opponent for Quoridor.
 *
 * Difficulty levels:
 *  EASY   — depth 1, pawn moves only, picks the move that
 *            most advances toward the goal
 *  MEDIUM — depth 2, Minimax, occasionally places walls
 *  HARD   — depth 3, Minimax + Alpha-Beta pruning,
 *            considers both pawn moves and wall placements
 *
 * The AI always plays as Player 2 (index 1).
 */
public class AIPlayer {

    // ── Difficulty enum ───────────────────────────────────────

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    // ── Inner class: any AI-chosen move ───────────────────────

    public static class AIMove {

        private final Position targetPosition; // null if wall move
        private final Wall     wall;           // null if pawn move

        public AIMove(Position target) {
            this.targetPosition = target;
            this.wall           = null;
        }

        public AIMove(Wall wall) {
            this.targetPosition = null;
            this.wall           = wall;
        }

        public boolean  isPawnMove()        { return wall == null; }
        public Position getTargetPosition() { return targetPosition; }
        public Wall     getWall()           { return wall; }
    }

    // ── Constants ─────────────────────────────────────────────

    private static final int MAX_DEPTH_EASY   = 1;
    private static final int MAX_DEPTH_MEDIUM = 2;
    private static final int MAX_DEPTH_HARD   = 3;

    private static final int  INF = 1_000_000;

    // ── Fields ────────────────────────────────────────────────

    private Difficulty difficulty;
    private final Random random = new Random();

    // ── Constructor ───────────────────────────────────────────

    public AIPlayer() {
        this.difficulty = Difficulty.MEDIUM; // default
    }

    public AIPlayer(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() { return difficulty; }

    // ════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ════════════════════════════════════════════════════════

    /**
     * Chooses the best move for the current player in the given state.
     * Routes to the appropriate algorithm based on difficulty.
     *
     * @param state  The current game state (AI is current player).
     * @return       The chosen AIMove.
     */
    public AIMove chooseMove(GameState state) {
        return switch (difficulty) {
            case EASY   -> chooseMoveEasy(state);
            case MEDIUM -> chooseMoveMinimax(state, MAX_DEPTH_MEDIUM);
            case HARD   -> chooseMoveMinimax(state, MAX_DEPTH_HARD);
        };
    }

    // ════════════════════════════════════════════════════════
    //  EASY: GREEDY PAWN MOVE
    // ════════════════════════════════════════════════════════

    /**
     * Easy AI: always moves the pawn greedily toward the goal.
     * Never places walls.
     * Among moves that advance equally, picks randomly for variety.
     */
    private AIMove chooseMoveEasy(GameState state) {
        List<Position> moves = MoveValidator.getLegalPawnMoves(state);
        if (moves.isEmpty()) {
            return new AIMove(state.getCurrentPlayer().getPosition());
        }

        int    aiGoalRow = state.getCurrentPlayer().getGoalRow();
        int    bestDist  = Integer.MAX_VALUE;
        List<Position> bestMoves = new ArrayList<>();

        for (Position move : moves) {
            // Distance to goal = absolute row difference
            int dist = Math.abs(move.row - aiGoalRow);
            if (dist < bestDist) {
                bestDist = dist;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (dist == bestDist) {
                bestMoves.add(move);
            }
        }

        // Pick randomly among equally good moves
        return new AIMove(
            bestMoves.get(random.nextInt(bestMoves.size())));
    }

    // ════════════════════════════════════════════════════════
    //  MEDIUM / HARD: MINIMAX WITH ALPHA-BETA PRUNING
    // ════════════════════════════════════════════════════════

    /**
     * Runs Minimax with Alpha-Beta pruning to the given depth.
     * Returns the best move found.
     */
    private AIMove chooseMoveMinimax(GameState state, int maxDepth) {
        int    bestScore = Integer.MIN_VALUE;
        AIMove bestMove  = null;

        // The AI is the maximising player (currentPlayerIndex == 1)
        int aiIndex = state.getCurrentPlayerIndex();

        List<AIMove> allMoves = generateMoves(state, maxDepth, true);

        for (AIMove move : allMoves) {
            GameState simulated = applyMove(state.copy(), move);

            int score = minimax(
                simulated,
                maxDepth - 1,
                Integer.MIN_VALUE,  // alpha
                Integer.MAX_VALUE,  // beta
                false,              // next is minimising (human)
                aiIndex
            );

            if (score > bestScore || bestMove == null) {
                bestScore = score;
                bestMove  = move;
            }
        }

        // Fallback — should never be null in a valid game
        if (bestMove == null) {
            List<Position> legalPawnMoves =
                MoveValidator.getLegalPawnMoves(state);
            bestMove = new AIMove(legalPawnMoves.get(0));
        }

        return bestMove;
    }

    /**
     * Recursive Minimax with Alpha-Beta pruning.
     *
     * @param state        Current simulated game state
     * @param depth        Remaining search depth
     * @param alpha        Best score the maximiser can guarantee
     * @param beta         Best score the minimiser can guarantee
     * @param maximising   True if this is the maximising player's turn
     * @param aiIndex      Index of the AI player (0 or 1) — stays constant
     * @return             The evaluated score of this state
     */
    private int minimax(GameState state, int depth,
                        int alpha, int beta,
                        boolean maximising, int aiIndex) {

        // Base cases
        if (state.isGameOver()) {
            // If AI won, return very high score; if human won, very low
            boolean aiWon = state.getWinner() != null
                && state.getWinner().getId() - 1 == aiIndex;
            return aiWon ? INF : -INF;
        }

        if (depth == 0) {
            return evaluate(state, aiIndex);
        }

        List<AIMove> moves = generateMoves(state, depth, maximising);

        if (maximising) {
            int maxScore = Integer.MIN_VALUE;
            for (AIMove move : moves) {
                GameState sim   = applyMove(state.copy(), move);
                int       score = minimax(sim, depth - 1,
                                          alpha, beta, false, aiIndex);
                maxScore = Math.max(maxScore, score);
                alpha    = Math.max(alpha, score);
                if (alpha >= beta) break; // Beta cutoff — prune
            }
            return maxScore;

        } else {
            int minScore = Integer.MAX_VALUE;
            for (AIMove move : moves) {
                GameState sim   = applyMove(state.copy(), move);
                int       score = minimax(sim, depth - 1,
                                          alpha, beta, true, aiIndex);
                minScore = Math.min(minScore, score);
                beta     = Math.min(beta, score);
                if (alpha >= beta) break; // Alpha cutoff — prune
            }
            return minScore;
        }
    }

    // ════════════════════════════════════════════════════════
    //  MOVE GENERATION
    // ════════════════════════════════════════════════════════

    /**
     * Generates candidate moves for the current player.
     *
     * Always includes all legal pawn moves.
     * Wall moves are included based on difficulty and depth:
     *  - EASY:   never
     *  - MEDIUM: only at root (depth == maxDepth), limited to 6 best walls
     *  - HARD:   all depths, limited to 10 best walls
     *
     * Limiting wall candidates is crucial for performance —
     * there can be up to 128 legal walls, which would make
     * the search tree enormous.
     *
     * @param state       Current game state
     * @param depth       Remaining depth (used to limit wall moves)
     * @param isRootCall  True when called from chooseMoveMinimax directly
     */
    private List<AIMove> generateMoves(GameState state,
                                        int depth,
                                        boolean isRootCall) {
        List<AIMove> moves = new ArrayList<>();

        // Always add pawn moves
        for (Position p : MoveValidator.getLegalPawnMoves(state)) {
            moves.add(new AIMove(p));
        }

        // Add wall moves based on difficulty
        if (difficulty == Difficulty.EASY) return moves;

        int wallLimit = (difficulty == Difficulty.MEDIUM) ? 6 : 10;

        // Only add walls if the player has some remaining
        if (state.getCurrentPlayer().getWallsRemaining() > 0) {
            List<Wall> wallMoves = getBestWallCandidates(state, wallLimit);
            for (Wall w : wallMoves) {
                moves.add(new AIMove(w));
            }
        }

        return moves;
    }

    /**
     * Returns the best wall candidates for the current player.
     *
     * Strategy: a good wall slows down the opponent (increases their
     * BFS path length) without significantly slowing the AI.
     * We score each legal wall and return the top N.
     *
     * @param state  Current game state
     * @param limit  Maximum number of wall candidates to return
     */
    private List<Wall> getBestWallCandidates(GameState state, int limit) {
        List<Wall> allLegal = MoveValidator.getLegalWallPlacements(state);

        int aiIndex       = state.getCurrentPlayerIndex();
        int opponentIndex = 1 - aiIndex;

        // Score each wall: how much does it slow the opponent?
        List<int[]> scored = new ArrayList<>(); // [wallIndex, score]

        for (int i = 0; i < allLegal.size(); i++) {
            Wall      wall = allLegal.get(i);
            GameState sim  = state.copy();
            sim.getBoard().placeWall(wall);

            // BFS path lengths after placing this wall
            int opponentPath = bfsDistance(
                sim.getBoard(),
                sim.getPlayers()[opponentIndex].getPosition(),
                sim.getPlayers()[opponentIndex].getGoalRow()
            );
            int aiPath = bfsDistance(
                sim.getBoard(),
                sim.getPlayers()[aiIndex].getPosition(),
                sim.getPlayers()[aiIndex].getGoalRow()
            );

            // A wall is good if it lengthens the opponent's path
            // more than it lengthens the AI's path
            int wallScore = opponentPath - aiPath;
            scored.add(new int[]{i, wallScore});
        }

        // Sort by score descending
        scored.sort((a, b) -> b[1] - a[1]);

        // Return top N walls
        List<Wall> best = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            best.add(allLegal.get(scored.get(i)[0]));
        }
        return best;
    }

    // ════════════════════════════════════════════════════════
    //  EVALUATION FUNCTION
    // ════════════════════════════════════════════════════════

    /**
     * Scores a game state from the AI's perspective.
     *
     * Higher score = better for the AI.
     *
     * Components:
     *  1. Path difference: opponent's BFS distance minus AI's BFS distance
     *     (positive = AI is closer to winning)
     *  2. Wall advantage: AI walls remaining minus opponent walls remaining
     *     (having more walls = more options)
     *  3. Progress bonus: how many rows the AI has advanced
     *
     * @param state    The state to evaluate
     * @param aiIndex  Index of the AI player (0 or 1)
     */
    private int evaluate(GameState state, int aiIndex) {
        int opponentIndex = 1 - aiIndex;

        Player aiPlayer  = state.getPlayers()[aiIndex];
        Player opponent  = state.getPlayers()[opponentIndex];

        // 1. Path length difference (most important factor)
        int aiPathLength = bfsDistance(
            state.getBoard(),
            aiPlayer.getPosition(),
            aiPlayer.getGoalRow()
        );
        int opponentPathLength = bfsDistance(
            state.getBoard(),
            opponent.getPosition(),
            opponent.getGoalRow()
        );

        // If no path exists (shouldn't happen), penalise heavily
        if (aiPathLength < 0)       return -INF;
        if (opponentPathLength < 0) return  INF;

        int pathScore = (opponentPathLength - aiPathLength) * 10;

        // 2. Wall advantage (minor factor)
        int wallScore = (aiPlayer.getWallsRemaining()
                       - opponent.getWallsRemaining()) * 2;

        // 3. Raw progress toward goal
        int progressScore = Math.abs(
            aiPlayer.getPosition().row - aiPlayer.getGoalRow()
        );
        // Invert: smaller distance = better
        progressScore = (Board.SIZE - progressScore);

        return pathScore + wallScore + progressScore;
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    /**
     * Applies a move to a GameState copy and returns it.
     * The copy must already be made before calling this.
     */
    private GameState applyMove(GameState state, AIMove move) {
        if (move.isPawnMove()) {
            state.movePawn(move.getTargetPosition());
        } else {
            state.placeWall(move.getWall());
        }
        return state;
    }

    /**
     * BFS that returns the SHORTEST PATH LENGTH from start to goalRow.
     * Returns -1 if no path exists.
     *
     * Differs from MoveValidator.hasPathToGoal() in that it returns
     * the actual distance, which the evaluation function needs.
     */
    private int bfsDistance(Board board, Position start, int goalRow) {
        boolean[][] visited = new boolean[Board.SIZE][Board.SIZE];
        // Queue entries: [row, col, distance]
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{start.row, start.col, 0});
        visited[start.row][start.col] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int   r       = current[0];
            int   c       = current[1];
            int   dist    = current[2];

            if (r == goalRow) return dist;

            for (Direction dir : Direction.values()) {
                Position from = new Position(r, c);
                Position next = from.step(dir);

                if (!next.isValid()) continue;
                if (visited[next.row][next.col]) continue;
                if (board.isBlockedByWall(from, next)) continue;

                visited[next.row][next.col] = true;
                queue.add(new int[]{next.row, next.col, dist + 1});
            }
        }

        return -1; // no path found
    }
}