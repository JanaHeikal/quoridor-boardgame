package com.quoridor.view;

import com.quoridor.ai.AIPlayer;
import com.quoridor.controller.GameController;
import com.quoridor.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * GameView builds and owns the entire JavaFX scene.
 *
 * Layout (game screen):
 *   ┌─────────────────────────────────────────┐
 *   │  [Player 2 info bar]                    │
 *   │  [BoardRenderer — the 9x9 board]        │
 *   │  [Player 1 info bar]                    │
 *   │  [Status message label]                 │
 *   │  [Buttons: Place Wall | New Game | Mode]│
 *   └─────────────────────────────────────────┘
 *
 * GameView never contains game logic.
 * It reads GameState (via GameController) to know what to display,
 * and forwards user input events to GameController.
 */
public class GameView {

    // ── Constants ─────────────────────────────────────────────
    private static final double WINDOW_WIDTH  = 780;
    private static final double WINDOW_HEIGHT = 900;

    // ── Core references ───────────────────────────────────────
    private final Stage   stage;
    private GameController controller;
    private BoardRenderer  boardRenderer;

    // ── UI nodes updated dynamically ─────────────────────────
    private Label  statusLabel;
    private Label  p1WallsLabel;
    private Label  p2WallsLabel;
    private Label  p1NameLabel;
    private Label  p2NameLabel;
    private Button modeToggleButton;
    private Button newGameButton;
    private Button undoButton;
    private Button redoButton;

    // ── Constructor ───────────────────────────────────────────

    public GameView(Stage stage) {
        this.stage = stage;
        configureStage();
    }
    // ── Stage setup ───────────────────────────────────────────


    // ════════════════════════════════════════════════════════
    //  MODE SELECTION SCREEN
    // ════════════════════════════════════════════════════════

    /**
     * Builds the opening screen where the player chooses
     * Human vs Human or Human vs Computer.
     */
    private VBox buildModeSelectionPane() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2C1810;");

        Label title = new Label("QUORIDOR");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 52));
        title.setTextFill(Color.web("#F5E6C8"));

        Label subtitle = new Label("Abstract Strategy Board Game");
        subtitle.setFont(Font.font("Georgia", 18));
        subtitle.setTextFill(Color.web("#C4A882"));

        Separator sep = new Separator();
        sep.setMaxWidth(300);
        sep.setStyle("-fx-background-color: #C4A882;");

        Label chooseLabel = new Label("Choose Game Mode");
        chooseLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        chooseLabel.setTextFill(Color.web("#F5E6C8"));

        Button hvhButton = createMenuButton("👤 vs 👤   Human vs Human");
        Button hvcButton = createMenuButton("👤 vs 🤖   Human vs Computer");
        
        // Difficulty selector (only relevant for Human vs Computer)
Label diffLabel = new Label("AI Difficulty:");
diffLabel.setFont(Font.font("Georgia", 16));
diffLabel.setTextFill(Color.web("#C4A882"));

javafx.scene.control.ToggleGroup diffGroup =
    new javafx.scene.control.ToggleGroup();

javafx.scene.control.RadioButton easyBtn   =
    createDifficultyButton("Easy",   diffGroup);
javafx.scene.control.RadioButton mediumBtn =
    createDifficultyButton("Medium", diffGroup);
javafx.scene.control.RadioButton hardBtn   =
    createDifficultyButton("Hard",   diffGroup);

mediumBtn.setSelected(true); // default

HBox diffBox = new HBox(16, easyBtn, mediumBtn, hardBtn);
diffBox.setAlignment(Pos.CENTER);

// Wire difficulty buttons to controller
easyBtn.setOnAction(e -> {
    if (controller != null)
        controller.setAIDifficulty(AIPlayer.Difficulty.EASY);
});
mediumBtn.setOnAction(e -> {
    if (controller != null)
        controller.setAIDifficulty(AIPlayer.Difficulty.MEDIUM);
});
hardBtn.setOnAction(e -> {
    if (controller != null)
        controller.setAIDifficulty(AIPlayer.Difficulty.HARD);
});



        hvhButton.setOnAction(e -> {
            if (controller != null) controller.startGame(false);
        });
        hvcButton.setOnAction(e -> {
            if (controller != null) controller.startGame(true);
        });

        // Update root.getChildren().addAll() to include these:
root.getChildren().addAll(
    title, subtitle, sep, chooseLabel,
    hvhButton, hvcButton,
    diffLabel, diffBox
);
        return root;
    }

    /** Creates a styled menu button with hover effects. */
    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(300);
        btn.setPrefHeight(55);
        btn.setFont(Font.font("Georgia", 16));
        applyMenuButtonStyle(btn, false);

        btn.setOnMouseEntered(e -> applyMenuButtonStyle(btn, true));
        btn.setOnMouseExited(e  -> applyMenuButtonStyle(btn, false));
        return btn;
    }

    private void applyMenuButtonStyle(Button btn, boolean hovered) {
        String bg     = hovered ? "#A0522D" : "#8B4513";
        String text   = hovered ? "#FFFFFF"  : "#F5E6C8";
        String border = hovered ? "#F5E6C8"  : "#C4A882";
        btn.setStyle(
            "-fx-background-color: " + bg     + ";" +
            "-fx-text-fill: "        + text   + ";" +
            "-fx-border-color: "     + border + ";" +
            "-fx-border-width: 2;"   +
            "-fx-border-radius: 6;"  +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
    }

    // ════════════════════════════════════════════════════════
    //  GAME SCREEN
    // ════════════════════════════════════════════════════════

    /**
     * Builds and displays the main game screen.
     * Called by GameController.startGame() once a mode is chosen.
     *
     * @param state  The freshly initialised GameState to display.
     */
    public void showGameScreen(GameState state) {
    boardRenderer = new BoardRenderer(state);
    boardRenderer.setController(controller);

    VBox root = new VBox(8);
    root.setAlignment(Pos.CENTER);
    root.setPadding(new Insets(16));
    root.setStyle("-fx-background-color: #2C1810;");

    // ── Player 2 bar (top) ────────────────────────────────
    HBox p2Bar = buildPlayerBar(2, state);

    // ── The board ─────────────────────────────────────────
    Pane boardPane = boardRenderer.getBoardPane();

    // ── Player 1 bar (bottom) ─────────────────────────────
    HBox p1Bar = buildPlayerBar(1, state);

    // ── Status label ──────────────────────────────────────
    statusLabel = new Label(
        state.getCurrentPlayer().getName() + "'s turn.");
    statusLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
    statusLabel.setTextFill(Color.web("#F5E6C8"));
    statusLabel.setWrapText(true);

    // ── Buttons ───────────────────────────────────────────

    // Toggle between pawn move and wall placement mode
    modeToggleButton = new Button("🧱  Place Wall");
    styleControlButton(modeToggleButton);
    modeToggleButton.setOnAction(e -> {
        if (controller == null) return;
        GameController.InputMode current = controller.getInputMode();
        if (current == GameController.InputMode.PAWN) {
            controller.setInputMode(GameController.InputMode.WALL);
        } else {
            controller.setInputMode(GameController.InputMode.PAWN);
        }
    });

    // Undo last human move
    undoButton = new Button("↩  Undo");
    styleControlButton(undoButton);
    undoButton.setDisable(true); // disabled until a move is made
    undoButton.setOnAction(e -> {
        if (controller != null) controller.undo();
    });

    // Redo last undone move
    redoButton = new Button("↪  Redo");
    styleControlButton(redoButton);
    redoButton.setDisable(true); // disabled until an undo is made
    redoButton.setOnAction(e -> {
        if (controller != null) controller.redo();
    });

    // Reset the game with the same mode
    newGameButton = new Button("⟳  New Game");
    styleControlButton(newGameButton);
    newGameButton.setOnAction(e -> {
        if (controller != null) controller.resetGame();
    });

    // Return to mode selection screen
    Button changeModeButton = new Button("⬅  Change Mode");
    styleControlButton(changeModeButton);
    changeModeButton.setOnAction(e -> showModeSelection());

    // Exit the application cleanly
    Button exitButton = new Button("✕  Exit");
    styleControlButton(exitButton);
    exitButton.setOnAction(e -> {
        javafx.application.Platform.exit();
        System.exit(0);
    });

    // ── Button bar ────────────────────────────────────────
    HBox buttonBar = new HBox(10,
        modeToggleButton, undoButton, redoButton,
        newGameButton, changeModeButton, exitButton);
    buttonBar.setAlignment(Pos.CENTER);

    // ── Assemble the scene ────────────────────────────────
    root.getChildren().addAll(
        p2Bar, boardPane, p1Bar, statusLabel, buttonBar
    );

    stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
}

    // ── Player info bar ───────────────────────────────────────

    /**
     * Builds the player info bar showing name and remaining wall count.
     *
     * @param playerNum  1 or 2
     * @param state      Current game state (for initial values)
     */
    private HBox buildPlayerBar(int playerNum, GameState state) {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle(
            "-fx-background-color: #3D2010;" +
            "-fx-border-color: #8B4513;"     +
            "-fx-border-width: 1;"           +
            "-fx-border-radius: 6;"          +
            "-fx-background-radius: 6;"
        );
        bar.setMaxWidth(BoardRenderer.BOARD_PIXEL_SIZE);

        boolean isP1   = (playerNum == 1);
        String  name   = isP1
            ? state.getPlayer1().getName()
            : state.getPlayer2().getName();
        String  color  = isP1
            ? BoardRenderer.P1_COLOR
            : BoardRenderer.P2_COLOR;

        Label nameLabel = new Label("● " + name);
        nameLabel.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web(color));

        Label wallsLabel = new Label("Walls: " + "█".repeat(10));
        wallsLabel.setFont(Font.font("Monospaced", 14));
        wallsLabel.setTextFill(Color.web("#C4A882"));

        if (isP1) {
            p1NameLabel  = nameLabel;
            p1WallsLabel = wallsLabel;
        } else {
            p2NameLabel  = nameLabel;
            p2WallsLabel = wallsLabel;
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(nameLabel, spacer, wallsLabel);
        return bar;
    }

    // ── Button styling ────────────────────────────────────────

    private void styleControlButton(Button btn) {
        btn.setPrefWidth(160);
        btn.setPrefHeight(38);
        btn.setFont(Font.font("Georgia", 14));
        btn.setStyle(
            "-fx-background-color: #5C3010;" +
            "-fx-text-fill: #F5E6C8;"        +
            "-fx-border-color: #8B6040;"     +
            "-fx-border-width: 1;"           +
            "-fx-border-radius: 4;"          +
            "-fx-background-radius: 4;"      +
            "-fx-cursor: hand;"
        );
    }

    // ════════════════════════════════════════════════════════
    //  UPDATE METHODS  (called by GameController)
    // ════════════════════════════════════════════════════════

    /**
     * Full refresh: redraws the board, updates wall count labels,
     * and updates the status message.
     * Called by GameController after every move.
     *
     * @param state  The updated game state.
     */
    public void refresh(GameState state) {
        if (boardRenderer == null) return;
        boardRenderer.redraw(state);
        updateWallLabels(state);
        updateStatusLabel(state);
    }

    /** Updates both players' wall-count display. */
    private void updateWallLabels(GameState state) {
        int w1 = state.getPlayer1().getWallsRemaining();
        int w2 = state.getPlayer2().getWallsRemaining();

        if (p1WallsLabel != null)
            p1WallsLabel.setText(
                "Walls: " + "█".repeat(w1) + "░".repeat(10 - w1));
        if (p2WallsLabel != null)
            p2WallsLabel.setText(
                "Walls: " + "█".repeat(w2) + "░".repeat(10 - w2));
    }

    /** Updates the status message below the board. */
    private void updateStatusLabel(GameState state) {
        if (statusLabel == null) return;
        if (state.isGameOver()) {
            statusLabel.setText(
                "🏆  " + state.getWinner().getName()
                + " wins! Congratulations!");
            statusLabel.setTextFill(Color.web("#FFD700"));
        } else {
            statusLabel.setText(
                state.getCurrentPlayer().getName() + "'s turn.");
            statusLabel.setTextFill(Color.web("#F5E6C8"));
        }
    }

    /**
     * Displays a short status message (e.g. "Invalid move!").
     * Called by GameController for feedback.
     */
    public void showMessage(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.web("#F5E6C8"));
        }
    }

    /**
     * Updates the mode-toggle button label to match the current
     * input mode. Called by GameController.setInputMode().
     *
     * @param mode  The new input mode.
     */
    public void updateModeButton(GameController.InputMode mode) {
        if (modeToggleButton == null) return;
        if (mode == GameController.InputMode.PAWN) {
            modeToggleButton.setText("🧱  Place Wall");
        } else {
            modeToggleButton.setText("♟  Move Pawn");
        }
    }

    /** Returns to the mode selection screen. */
    public void showModeSelection() {
        stage.setScene(new Scene(
            buildModeSelectionPane(), WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    // ── Getters / Setters ─────────────────────────────────────

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public BoardRenderer getBoardRenderer() { return boardRenderer; }

    public void show() { stage.show(); }
    
    private void configureStage() {
    stage.setTitle("Quoridor");
    stage.setWidth(WINDOW_WIDTH);
    stage.setHeight(WINDOW_HEIGHT);
    stage.setResizable(false);

    // Cleanly shut down the JVM (including any AI background threads)
    // when the user closes the window via the OS close button.
    stage.setOnCloseRequest(e -> {
        javafx.application.Platform.exit();
        System.exit(0);
    });

    stage.setScene(new Scene(
        buildModeSelectionPane(), WINDOW_WIDTH, WINDOW_HEIGHT));
}
    
    private javafx.scene.control.RadioButton createDifficultyButton(
        String text,
        javafx.scene.control.ToggleGroup group) {

    javafx.scene.control.RadioButton btn =
        new javafx.scene.control.RadioButton(text);
    btn.setToggleGroup(group);
    btn.setFont(Font.font("Georgia", 14));
    btn.setTextFill(Color.web("#F5E6C8"));
    btn.setStyle("-fx-cursor: hand;");
    return btn;
}
    public void updateUndoRedoButtons(boolean canUndo, boolean canRedo) {
    if (undoButton != null) undoButton.setDisable(!canUndo);
    if (redoButton != null) redoButton.setDisable(!canRedo);
}
}
