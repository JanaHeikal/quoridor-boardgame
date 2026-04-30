package com.quoridor.controller;

import com.quoridor.ai.AIPlayer;
import com.quoridor.model.*;
import com.quoridor.view.GameView;
import javafx.application.Platform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * GameController bridges the View and the Model.
 *
 * Responsibilities:
 *  - Receives user input events from GameView
 *  - Validates moves using MoveValidator
 *  - Applies moves to GameState
 *  - Tells GameView to refresh after every move
 *  - Manages undo/redo history
 *  - Triggers AI moves on a background thread
 */
public class GameController {

    // ── Input mode enum ───────────────────────────────────────
    public enum InputMode { PAWN, WALL }

    // ── Dependencies ──────────────────────────────────────────
    private GameView  gameView;
    private GameState gameState;
    private AIPlayer  aiPlayer;

    // ── Game settings ─────────────────────────────────────────
    private InputMode           inputMode    = InputMode.PAWN;
    private boolean             vsComputer   = false;
    private boolean             inputLocked  = false;
    private AIPlayer.Difficulty aiDifficulty = AIPlayer.Difficulty.MEDIUM;

    // ── Undo / Redo stacks ────────────────────────────────────
    // Each entry is a full deep copy of the GameState taken
    // immediately BEFORE a move was applied.
    private final Deque<GameState> undoStack = new ArrayDeque<>();
    private final Deque<GameState> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO_HISTORY = 50;

    // ── Constructor ───────────────────────────────────────────

    public GameController() {}

    // ── Dependency injection ──────────────────────────────────

    public void setGameView(GameView view) {
        this.gameView = view;
    }

    // ════════════════════════════════════════════════════════
    //  GAME LIFECYCLE
    // ════════════════════════════════════════════════════════

    /**
     * Starts a new game with the chosen mode.
     * Called by GameView when the player picks Human vs Human
     * or Human vs Computer on the mode selection screen.
     *
     * @param vsComputer  true = Human vs AI, false = Human vs Human
     */
    public void startGame(boolean vsComputer) {
        this.vsComputer  = vsComputer;
        this.gameState   = new GameState(vsComputer);
        this.inputMode   = InputMode.PAWN;
        this.inputLocked = false;
        this.aiPlayer    = vsComputer
            ? new AIPlayer(aiDifficulty) : null;

        // Clear undo/redo history on every new game
        undoStack.clear();
        redoStack.clear();

        // Build the game screen and start the first turn
        gameView.showGameScreen(gameState);
        beginTurn();
    }

    /**
     * Resets the game with the same mode (Human vs Human
     * or Human vs Computer) that was previously selected.
     */
    public void resetGame() {
        startGame(vsComputer);
    }

    // ════════════════════════════════════════════════════════
    //  TURN MANAGEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Prepares the UI for the current player's turn.
     *
     * Always redraws the full board first (pawns + walls) so
     * that walls placed on the previous turn are always visible.
     *
     * For a human player:
     *   - Redraws the board
     *   - Switches to PAWN mode
     *   - Highlights all legal pawn moves in green
     *   - Unlocks input
     *   - Updates undo/redo button states
     *
     * For the AI player:
     *   - Redraws the board
     *   - Locks input
     *   - Clears highlights
     *   - Triggers AI move on a background thread
     */
    private void beginTurn() {
        if (gameState.isGameOver()) return;

        // Always redraw the full board at the start of every turn.
        // This is the single place that guarantees walls placed by
        // the previous player are shown before the next player acts.
        gameView.refresh(gameState);

        setInputMode(InputMode.PAWN);

        boolean currentIsAI = vsComputer
            && gameState.getCurrentPlayerIndex() == 1;

        if (!currentIsAI) {
            // Human turn — highlight legal pawn moves
            List<Position> legalMoves =
                MoveValidator.getLegalPawnMoves(gameState);
            gameView.getBoardRenderer().highlightValidMoves(legalMoves);
            inputLocked = false;
            gameView.updateUndoRedoButtons(
                !undoStack.isEmpty(), !redoStack.isEmpty());
        } else {
            // AI turn — lock input and compute move on background thread
            inputLocked = true;
            gameView.getBoardRenderer().clearHighlights();
            triggerAIMove();
        }
    }

    /**
     * Runs the AI move computation on a background thread
     * so the JavaFX UI thread is never blocked.
     *
     * The result is applied back on the JavaFX Application Thread
     * via Platform.runLater().
     */
    private void triggerAIMove() {
        gameView.showMessage("Computer is thinking...");

        Thread aiThread = new Thread(() -> {
            // Short pause so the UI shows "thinking" message
            try { Thread.sleep(600); }
            catch (InterruptedException ignored) {}

            // Compute best move on background thread
            AIPlayer.AIMove move = aiPlayer.chooseMove(gameState);

            // Apply on JavaFX thread
            Platform.runLater(() -> applyAIMove(move));
        });

        // Daemon thread: auto-terminates when the app closes
        aiThread.setDaemon(true);
        aiThread.start();
    }

    /**
     * Applies a move chosen by the AI to the game state.
     * AI moves are NOT pushed to the undo stack.
     */
    private void applyAIMove(AIPlayer.AIMove move) {
        if (move.isPawnMove()) {
            gameState.movePawn(move.getTargetPosition());
        } else {
            gameState.placeWall(move.getWall());
        }
        // beginTurn() will call refresh() — no need to call it here
        afterMove();
    }

    /**
     * Called after every move (human or AI).
     * Checks for win condition and either ends the game
     * or begins the next turn.
     *
     * Note: beginTurn() handles the board refresh for continuing games.
     * Only the game-over branch needs an explicit refresh here.
     */
    private void afterMove() {
        if (gameState.isGameOver()) {
            // Final refresh to show the winning state
            gameView.refresh(gameState);
            gameView.getBoardRenderer().clearHighlights();
            inputLocked = true;
            gameView.updateUndoRedoButtons(false, false);
            return;
        }
        // beginTurn() redraws the board + highlights for the next player
        beginTurn();
    }

    // ════════════════════════════════════════════════════════
    //  HUMAN INPUT HANDLERS
    // ════════════════════════════════════════════════════════

    /**
     * Called by BoardRenderer when the user clicks a board cell.
     *
     * In PAWN mode: attempts to move the pawn to the clicked cell.
     * In WALL mode: ignores cell clicks.
     *
     * Note: no explicit refresh here — afterMove() → beginTurn()
     * handles the full board redraw after the move is applied.
     *
     * @param target  The board position that was clicked.
     */
    public void handleCellClick(Position target) {
        if (inputLocked) return;
        if (inputMode != InputMode.PAWN) return;

        if (MoveValidator.isLegalPawnMove(gameState, target)) {
            pushUndoSnapshot();
            gameState.movePawn(target);
            afterMove();
        } else {
            gameView.showMessage("Invalid move! Click a green cell.");
        }
    }

    /**
     * Called by BoardRenderer when the user clicks a wall slot.
     *
     * Automatically switches to WALL mode if currently in PAWN mode.
     * Attempts to place the wall if the placement is legal.
     *
     * Note: no explicit refresh here — afterMove() → beginTurn()
     * handles the full board redraw after the move is applied.
     *
     * @param wall  The wall the user wants to place.
     */
    public void handleWallClick(Wall wall) {
        if (inputLocked) return;

        // Auto-switch to wall mode when a wall slot is clicked
        if (inputMode == InputMode.PAWN) {
            setInputMode(InputMode.WALL);
        }

        if (gameState.getCurrentPlayer().getWallsRemaining() <= 0) {
            gameView.showMessage("No walls remaining!");
            return;
        }

        if (MoveValidator.isLegalWallPlacement(gameState, wall)) {
            pushUndoSnapshot();
            gameState.placeWall(wall);
            afterMove();
        } else {
            gameView.showMessage(
                "Invalid wall! Would block a path or overlaps.");
        }
    }

    // ════════════════════════════════════════════════════════
    //  UNDO / REDO
    // ════════════════════════════════════════════════════════

    /**
     * Saves a deep copy of the current GameState onto the undo stack
     * immediately before a human move is applied.
     *
     * Also clears the redo stack — making a new move invalidates
     * any previously undone states.
     *
     * Caps the stack at MAX_UNDO_HISTORY to bound memory usage.
     */
    private void pushUndoSnapshot() {
        if (undoStack.size() >= MAX_UNDO_HISTORY) {
            // Remove the oldest entry (bottom of the deque)
            ((ArrayDeque<GameState>) undoStack).removeLast();
        }
        undoStack.push(gameState.copy());
        redoStack.clear();
    }

    /**
     * Undoes the last human move.
     *
     * Pushes the current state onto the redo stack,
     * then restores the top of the undo stack.
     * beginTurn() redraws the board with the restored wall state.
     */
    public void undo() {
        if (undoStack.isEmpty() || inputLocked) return;

        // Save current state so it can be redone
        redoStack.push(gameState.copy());

        // Restore the previous state
        gameState = undoStack.pop();

        // beginTurn() redraws everything including walls
        beginTurn();
    }

    /**
     * Redoes the last undone move.
     *
     * Pushes the current state onto the undo stack,
     * then restores the top of the redo stack.
     * beginTurn() redraws the board with the restored wall state.
     */
    public void redo() {
        if (redoStack.isEmpty() || inputLocked) return;

        // Save current state so it can be undone again
        undoStack.push(gameState.copy());

        // Restore the redo state
        gameState = redoStack.pop();

        // beginTurn() redraws everything including walls
        beginTurn();
    }

    // ════════════════════════════════════════════════════════
    //  INPUT MODE MANAGEMENT
    // ════════════════════════════════════════════════════════

    /**
     * Switches between PAWN move mode and WALL placement mode.
     *
     * In PAWN mode: green highlights show legal move destinations.
     * In WALL mode: highlights are cleared; wall slots are hoverable.
     *
     * @param mode  The new input mode to activate.
     */
    public void setInputMode(InputMode mode) {
        this.inputMode = mode;

        if (mode == InputMode.PAWN) {
            List<Position> legalMoves =
                MoveValidator.getLegalPawnMoves(gameState);
            gameView.getBoardRenderer().highlightValidMoves(legalMoves);
            gameView.showMessage(
                gameState.getCurrentPlayer().getName()
                + "'s turn — click a green cell to move.");
        } else {
            gameView.getBoardRenderer().clearHighlights();
            gameView.showMessage(
                gameState.getCurrentPlayer().getName()
                + " — click a wall slot to place a wall.");
        }

        // Update the toggle button label in the View
        gameView.updateModeButton(mode);
    }

    // ── Setters / Getters ─────────────────────────────────────

    /**
     * Sets the AI difficulty level.
     * Can be called before or during a game.
     */
    public void setAIDifficulty(AIPlayer.Difficulty difficulty) {
        this.aiDifficulty = difficulty;
        if (aiPlayer != null) aiPlayer.setDifficulty(difficulty);
    }

    public InputMode           getInputMode()  { return inputMode;    }
    public AIPlayer.Difficulty getDifficulty() { return aiDifficulty; }
}