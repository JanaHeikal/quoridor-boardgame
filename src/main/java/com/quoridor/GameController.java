package com.quoridor;

/**
 * GameController is the bridge between the View and the Model.
 *
 * Responsibility:
 *   - Receives user-action events from GameView (e.g. "user clicked cell 3,4").
 *   - Asks GameState (the model) whether the action is legal.
 *   - Tells GameState to apply the action if legal.
 *   - Notifies GameView to redraw.
 *
 * This class will grow significantly in Steps 6 and 7.
 * The skeleton is here now so the package structure is complete
 * and GameView can reference it without compilation errors.
 */
public class GameController {

    /**
     * Default constructor.
     * Dependencies (GameState, GameView) will be injected in later steps
     * once those classes have meaningful content.
     */
    public GameController() {
        // intentionally empty for now
    }
}