package com.quoridor;

import com.quoridor.controller.GameController;
import com.quoridor.view.GameView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point. Creates the Controller and View,
 * wires them together, then shows the window.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        // 1. Create controller (no dependencies yet)
        GameController controller = new GameController();

        // 2. Create view — passes the stage to configure the window
        GameView gameView = new GameView(primaryStage);

        // 3. Wire them together (mutual references)
        controller.setGameView(gameView);
        gameView.setController(controller);

        // 4. Show the mode selection screen
        gameView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}