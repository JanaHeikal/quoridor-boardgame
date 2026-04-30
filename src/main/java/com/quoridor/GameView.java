package com.quoridor;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * GameView owns the JavaFX Stage and builds the application's Scene.
 *
 * Responsibility: everything the user sees.
 *   - Creates and organises UI nodes (board, panels, buttons).
 *   - Forwards user input events to the GameController.
 *   - Refreshes the display when the game state changes.
 *
 * What it does NOT do:
 *   - Contains no game rules.
 *   - Makes no decisions about legal moves.
 *
 * Right now this is a skeleton — a labelled placeholder that
 * proves JavaFX launches correctly.  Step 5 replaces the
 * placeholder with the real board canvas.
 */
public class GameView {

    // ── Constants ────────────────────────────────────────────
    private static final String TITLE   = "Quoridor";
    private static final double WIDTH   = 700;
    private static final double HEIGHT  = 750;

    // ── Fields ───────────────────────────────────────────────
    private final Stage stage;   // the OS window — we hold a reference to
                                  // set title, size, and later swap scenes

    // ── Constructor ──────────────────────────────────────────

    /**
     * @param stage  The primary Stage provided by JavaFX via Main.start().
     */
    public GameView(Stage stage) {
        this.stage = stage;
        configureStage();
    }

    // ── Private helpers ──────────────────────────────────────

    /** Sets window title, size, and builds the initial scene. */
    private void configureStage() {
        stage.setTitle(TITLE);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setResizable(false);  // fixed size keeps the board layout simple

        // --- Placeholder scene (replaced in Step 5) ----------
        Label placeholder = new Label(
            "Quoridor – board will appear here in Step 5"
        );
        placeholder.setStyle(
            "-fx-font-size: 18px; -fx-text-fill: #444444;"
        );

        StackPane root = new StackPane(placeholder);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F5ECD7;"); // warm parchment tone

        stage.setScene(new Scene(root, WIDTH, HEIGHT));
    }

    // ── Public API ───────────────────────────────────────────

    /** Makes the window visible. Called by Main after construction. */
    public void show() {
        stage.show();
    }
}