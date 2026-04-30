package com.quoridor.model;

public class GameState {

    private final Board    board;
    private final Player[] players;
    private int            currentPlayerIndex;
    private boolean        gameOver;
    private Player         winner;

    // Tracks whether this is a vs-computer game (for copy())
    private boolean vsComputer;

    public GameState() {
        this(false);
    }

    public GameState(boolean vsComputer) {
        this.vsComputer = vsComputer;
        board = new Board();

        String p2Name = vsComputer ? "Computer" : "Player 2";

        players = new Player[]{
            new Player(1, "Player 1", new Position(8, 4), 0),
            new Player(2, p2Name,     new Position(0, 4), 8)
        };

        currentPlayerIndex = 0;
        gameOver           = false;
        winner             = null;
    }

    public Player getCurrentPlayer()  { return players[currentPlayerIndex]; }
    public Player getOpponent()       { return players[1 - currentPlayerIndex]; }

    public void endTurn() {
        if (getCurrentPlayer().hasWon()) {
            gameOver = true;
            winner   = getCurrentPlayer();
            return;
        }
        currentPlayerIndex = 1 - currentPlayerIndex;
    }

    public void movePawn(Position target) {
        getCurrentPlayer().setPosition(target);
        endTurn();
    }

    public void placeWall(Wall wall) {
        board.placeWall(wall);
        getCurrentPlayer().useWall();
        endTurn();
    }

    public Board    getBoard()              { return board; }
    public Player   getPlayer1()            { return players[0]; }
    public Player   getPlayer2()            { return players[1]; }
    public Player[] getPlayers()            { return players; }
    public boolean  isGameOver()            { return gameOver; }
    public Player   getWinner()             { return winner; }
    public int      getCurrentPlayerIndex() { return currentPlayerIndex; }
    public boolean  isVsComputer()          { return vsComputer; }

    /**
     * Deep copy — preserves vsComputer flag so Player 2's
     * name ("Computer" or "Player 2") is correctly restored.
     */
    public GameState copy() {
        // Use a private copy constructor to avoid re-initialising players
        GameState copy = new GameState();
        copy.vsComputer = this.vsComputer;

        for (int r = 0; r < Board.WALL_SLOTS; r++) {
            for (int c = 0; c < Board.WALL_SLOTS; c++) {
                if (this.board.isHWallAt(r, c))
                    copy.board.placeWall(new Wall(
                        Wall.Orientation.HORIZONTAL, new Position(r, c)));
                if (this.board.isVWallAt(r, c))
                    copy.board.placeWall(new Wall(
                        Wall.Orientation.VERTICAL, new Position(r, c)));
            }
        }

        copy.players[0]         = this.players[0].copy();
        copy.players[1]         = this.players[1].copy();
        copy.currentPlayerIndex = this.currentPlayerIndex;
        copy.gameOver           = this.gameOver;
        copy.winner             = (this.winner == null) ? null
                                    : copy.players[this.winner.getId() - 1];
        return copy;
    }

    @Override
    public String toString() {
        return "Turn: " + getCurrentPlayer().getName()
             + " | " + players[0] + " | " + players[1];
    }
}