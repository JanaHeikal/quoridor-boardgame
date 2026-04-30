package com.quoridor.view;

import com.quoridor.controller.GameController;
import com.quoridor.model.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * BoardRenderer builds and manages all visual nodes on the game board.
 *
 * Key design decision: we use two dedicated boolean arrays
 * (hWallPlaced, vWallPlaced) to track which slots have a placed wall.
 * This replaces the unreliable opacity == 1.0 float comparison that
 * caused placed walls to disappear on mouse hover.
 */
public class BoardRenderer {

    // ── Public constants ──────────────────────────────────────
    public static final double CELL_SIZE        = 58;
    public static final double GAP_SIZE         = 12;
    public static final double PADDING          = 30;
    public static final double BOARD_PIXEL_SIZE =
        PADDING * 2 + 9 * CELL_SIZE + 8 * GAP_SIZE;

    public static final String P1_COLOR = "#E84444";
    public static final String P2_COLOR = "#4488EE";

    // ── Colours ───────────────────────────────────────────────
    private static final Color COLOR_CELL_NORMAL  = Color.web("#F0DEB4");
    private static final Color COLOR_CELL_HOVER   = Color.web("#FFEAA0");
    private static final Color COLOR_CELL_VALID   = Color.web("#90EE90");
    private static final Color COLOR_CELL_DARK    = Color.web("#D4B896");
    private static final Color COLOR_WALL_SLOT    = Color.web("#5C3A1E");
    private static final Color COLOR_WALL_PLACED  = Color.web("#8B4513");
    private static final Color COLOR_WALL_PREVIEW = Color.web("#CD853F");

    // ── Visual nodes ──────────────────────────────────────────
    private final Pane          boardPane;
    private GameController      controller;

    private final Rectangle[][] cellNodes;
    private final Rectangle[][] hSlotNodes;   // [8][9]
    private final Rectangle[][] vSlotNodes;   // [9][8]

    private Circle p1Pawn;
    private Circle p2Pawn;

    // ── Placed-wall tracking arrays ───────────────────────────
    // TRUE = this slot currently has a placed wall on it.
    // Used by hover logic to avoid overwriting placed walls.
    // Reset and rebuilt every time redrawWalls() is called.
    private final boolean[][] hPlaced;  // [8][9]
    private final boolean[][] vPlaced;  // [9][8]

    // ── Valid move highlights ─────────────────────────────────
    private final List<Position> highlightedCells = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────

    public BoardRenderer(GameState initialState) {
        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_PIXEL_SIZE, BOARD_PIXEL_SIZE);
        boardPane.setStyle("-fx-background-color: #3D1F0A;");

        cellNodes  = new Rectangle[9][9];
        hSlotNodes = new Rectangle[8][9];
        vSlotNodes = new Rectangle[9][8];
        hPlaced    = new boolean[8][9];
        vPlaced    = new boolean[9][8];

        buildGrid();
        buildPawns(initialState);
        buildCoordinateLabels();
    }

    // ════════════════════════════════════════════════════════
    //  BOARD CONSTRUCTION
    // ════════════════════════════════════════════════════════

    private void buildGrid() {

        // ── Cells ─────────────────────────────────────────
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                double x = toPixelX(col);
                double y = toPixelY(row);

                Rectangle cell = new Rectangle(x, y, CELL_SIZE, CELL_SIZE);
                boolean dark = (row + col) % 2 == 1;
                cell.setFill(dark ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
                cell.setArcWidth(4);
                cell.setArcHeight(4);

                final int r = row, c = col;
                cell.setOnMouseEntered(e -> onCellHover(r, c));
                cell.setOnMouseExited(e  -> onCellExit(r, c));
                cell.setOnMouseClicked(e -> onCellClick(r, c));

                cellNodes[row][col] = cell;
                boardPane.getChildren().add(cell);
            }
        }

        // ── Horizontal wall slots (8 rows × 9 cols) ───────
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 9; c++) {
                double x = toPixelX(c);
                double y = toPixelY(r) + CELL_SIZE;

                Rectangle slot = new Rectangle(x, y, CELL_SIZE, GAP_SIZE);
                slot.setFill(COLOR_WALL_SLOT);
                slot.setOpacity(0.3);

                final int fr = r, fc = c;
                slot.setOnMouseEntered(e ->
                    onHSlotHover(fr, fc, true));
                slot.setOnMouseExited(e ->
                    onHSlotHover(fr, fc, false));
                slot.setOnMouseClicked(e ->
                    onWallSlotClick(fr, fc, Wall.Orientation.HORIZONTAL));

                hSlotNodes[r][c] = slot;
                boardPane.getChildren().add(slot);
            }
        }

        // ── Vertical wall slots (9 rows × 8 cols) ─────────
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 8; c++) {
                double x = toPixelX(c) + CELL_SIZE;
                double y = toPixelY(r);

                Rectangle slot = new Rectangle(x, y, GAP_SIZE, CELL_SIZE);
                slot.setFill(COLOR_WALL_SLOT);
                slot.setOpacity(0.3);

                final int fr = r, fc = c;
                slot.setOnMouseEntered(e ->
                    onVSlotHover(fr, fc, true));
                slot.setOnMouseExited(e ->
                    onVSlotHover(fr, fc, false));
                slot.setOnMouseClicked(e ->
                    onWallSlotClick(fr, fc, Wall.Orientation.VERTICAL));

                vSlotNodes[r][c] = slot;
                boardPane.getChildren().add(slot);
            }
        }
    }

    private void buildCoordinateLabels() {
        String[] colLabels = {"A","B","C","D","E","F","G","H","I"};

        for (int c = 0; c < 9; c++) {
            Text t = new Text(
                toPixelX(c) + CELL_SIZE / 2 - 5,
                BOARD_PIXEL_SIZE - 4,
                colLabels[c]);
            t.setFill(Color.web("#C4A882"));
            t.setFont(Font.font("Georgia", 11));
            boardPane.getChildren().add(t);
        }

        for (int r = 0; r < 9; r++) {
            Text t = new Text(
                6,
                toPixelY(r) + CELL_SIZE / 2 + 5,
                String.valueOf(9 - r));
            t.setFill(Color.web("#C4A882"));
            t.setFont(Font.font("Georgia", 11));
            boardPane.getChildren().add(t);
        }
    }

    private void buildPawns(GameState state) {
        p1Pawn = createPawn(P1_COLOR);
        p2Pawn = createPawn(P2_COLOR);
        updatePawnPosition(p1Pawn, state.getPlayer1().getPosition());
        updatePawnPosition(p2Pawn, state.getPlayer2().getPosition());
        boardPane.getChildren().addAll(p1Pawn, p2Pawn);
    }

    private Circle createPawn(String colorHex) {
        double radius = CELL_SIZE / 2 - 6;
        Circle pawn = new Circle(radius);
        pawn.setFill(Color.web(colorHex));
        pawn.setStroke(Color.WHITE);
        pawn.setStrokeWidth(2.5);
        pawn.setEffect(new javafx.scene.effect.DropShadow(6, Color.BLACK));
        pawn.setMouseTransparent(true);
        return pawn;
    }

    // ════════════════════════════════════════════════════════
    //  EVENT HANDLERS — CELLS
    // ════════════════════════════════════════════════════════

    private void onCellHover(int row, int col) {
        if (highlightedCells.contains(new Position(row, col))) {
            cellNodes[row][col].setFill(COLOR_CELL_HOVER);
        }
    }

    private void onCellExit(int row, int col) {
        if (highlightedCells.contains(new Position(row, col))) {
            cellNodes[row][col].setFill(COLOR_CELL_VALID);
        } else {
            cellNodes[row][col].setFill(
                (row + col) % 2 == 1 ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
        }
    }

    private void onCellClick(int row, int col) {
        if (controller != null)
            controller.handleCellClick(new Position(row, col));
    }

    // ════════════════════════════════════════════════════════
    //  EVENT HANDLERS — WALL SLOTS
    // ════════════════════════════════════════════════════════

    /**
     * Hover handler for horizontal wall slots.
     *
     * When hovering slot (r, c), a horizontal wall would span
     * slots (r, c) and (r, c+1) — but ONLY if c is a valid anchor
     * (0–7). If c == 8 (the last slot), the anchor is c-1.
     *
     * Placed walls are NEVER overwritten — checked via hPlaced[][].
     */
    private void onHSlotHover(int r, int c, boolean entering) {
        // Determine the anchor column for this hover
        // A horizontal wall anchored at col 'anchor' paints slots
        // anchor and anchor+1. So hovering slot c could be part of
        // a wall anchored at c (if c <= 6) or c-1 (if c == 7 or 8).
        // We show the preview for the wall that WOULD be placed on click.
        int anchor = (c < Board.WALL_SLOTS) ? c : Board.WALL_SLOTS - 1;

        applyHSlotStyle(r, anchor,     entering);
        applyHSlotStyle(r, anchor + 1, entering);
    }

    /**
     * Hover handler for vertical wall slots.
     */
    private void onVSlotHover(int r, int c, boolean entering) {
        int anchor = (r < Board.WALL_SLOTS) ? r : Board.WALL_SLOTS - 1;

        applyVSlotStyle(anchor,     c, entering);
        applyVSlotStyle(anchor + 1, c, entering);
    }

    /**
     * Applies preview or restore style to a single horizontal slot.
     * NEVER modifies a slot that has a placed wall on it.
     */
    private void applyHSlotStyle(int r, int c, boolean preview) {
        if (r < 0 || r >= 8 || c < 0 || c >= 9) return;
        // If this slot has a placed wall, never touch it
        if (hPlaced[r][c]) return;

        Rectangle slot = hSlotNodes[r][c];
        if (preview) {
            slot.setFill(COLOR_WALL_PREVIEW);
            slot.setOpacity(0.85);
        } else {
            slot.setFill(COLOR_WALL_SLOT);
            slot.setOpacity(0.3);
        }
    }

    /**
     * Applies preview or restore style to a single vertical slot.
     * NEVER modifies a slot that has a placed wall on it.
     */
    private void applyVSlotStyle(int r, int c, boolean preview) {
        if (r < 0 || r >= 9 || c < 0 || c >= 8) return;
        if (vPlaced[r][c]) return;

        Rectangle slot = vSlotNodes[r][c];
        if (preview) {
            slot.setFill(COLOR_WALL_PREVIEW);
            slot.setOpacity(0.85);
        } else {
            slot.setFill(COLOR_WALL_SLOT);
            slot.setOpacity(0.3);
        }
    }

    /**
     * Click handler for wall slots.
     * Normalises the anchor so it's always in the valid 0–7 range.
     */
    private void onWallSlotClick(int row, int col,
                                  Wall.Orientation orientation) {
        if (controller == null) return;

        int anchorRow = row;
        int anchorCol = col;

        if (orientation == Wall.Orientation.HORIZONTAL) {
            if (anchorCol >= Board.WALL_SLOTS)
                anchorCol = Board.WALL_SLOTS - 1;
        } else {
            if (anchorRow >= Board.WALL_SLOTS)
                anchorRow = Board.WALL_SLOTS - 1;
        }

        controller.handleWallClick(
            new Wall(orientation, new Position(anchorRow, anchorCol)));
    }

    // ════════════════════════════════════════════════════════
    //  VALID MOVE HIGHLIGHTS
    // ════════════════════════════════════════════════════════

    public void highlightValidMoves(List<Position> validMoves) {
        clearHighlights();
        highlightedCells.addAll(validMoves);
        for (Position p : validMoves) {
            cellNodes[p.row][p.col].setFill(COLOR_CELL_VALID);
        }
    }

    public void clearHighlights() {
        for (Position p : highlightedCells) {
            cellNodes[p.row][p.col].setFill(
                (p.row + p.col) % 2 == 1
                    ? COLOR_CELL_DARK : COLOR_CELL_NORMAL);
        }
        highlightedCells.clear();
    }

    // ════════════════════════════════════════════════════════
    //  REDRAW
    // ════════════════════════════════════════════════════════

    public void redraw(GameState state) {
        updatePawnPosition(p1Pawn, state.getPlayer1().getPosition());
        updatePawnPosition(p2Pawn, state.getPlayer2().getPosition());
        redrawWalls(state.getBoard());
    }

    private void updatePawnPosition(Circle pawn, Position pos) {
        double cx = toPixelX(pos.col) + CELL_SIZE / 2;
        double cy = toPixelY(pos.row) + CELL_SIZE / 2;
        pawn.setCenterX(cx);
        pawn.setCenterY(cy);
    }

    /**
     * Full wall redraw:
     *   1. Reset all slots visually and clear the hPlaced/vPlaced arrays.
     *   2. Paint every wall currently on the board and mark hPlaced/vPlaced.
     *
     * This is the ONLY method that writes to hPlaced/vPlaced.
     * Hover logic reads these arrays to know which slots are protected.
     */
    private void redrawWalls(Board board) {

        // ── Step 1: Reset everything ──────────────────────
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 9; c++) {
                hSlotNodes[r][c].setFill(COLOR_WALL_SLOT);
                hSlotNodes[r][c].setOpacity(0.3);
                hPlaced[r][c] = false;
            }
        }
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 8; c++) {
                vSlotNodes[r][c].setFill(COLOR_WALL_SLOT);
                vSlotNodes[r][c].setOpacity(0.3);
                vPlaced[r][c] = false;
            }
        }

        // ── Step 2: Paint walls from the board model ──────
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.isHWallAt(r, c)) paintHWall(r, c);
                if (board.isVWallAt(r, c)) paintVWall(r, c);
            }
        }
    }

    /**
     * Paints a horizontal wall at anchor (r, c) — covers slots
     * (r, c) and (r, c+1) — and marks both as placed.
     */
    private void paintHWall(int r, int c) {
        for (int dc = 0; dc <= 1; dc++) {
            int col = c + dc;
            if (col < 9) {
                hSlotNodes[r][col].setFill(COLOR_WALL_PLACED);
                hSlotNodes[r][col].setOpacity(1.0);
                hPlaced[r][col] = true;   // protect from hover overwrite
            }
        }
    }

    /**
     * Paints a vertical wall at anchor (r, c) — covers slots
     * (r, c) and (r+1, c) — and marks both as placed.
     */
    private void paintVWall(int r, int c) {
        for (int dr = 0; dr <= 1; dr++) {
            int row = r + dr;
            if (row < 9) {
                vSlotNodes[row][c].setFill(COLOR_WALL_PLACED);
                vSlotNodes[row][c].setOpacity(1.0);
                vPlaced[row][c] = true;   // protect from hover overwrite
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  COORDINATE HELPERS
    // ════════════════════════════════════════════════════════

    public static double toPixelX(int col) {
        return PADDING + col * (CELL_SIZE + GAP_SIZE);
    }

    public static double toPixelY(int row) {
        return PADDING + row * (CELL_SIZE + GAP_SIZE);
    }

    public Pane getBoardPane()  { return boardPane; }

    public void setController(GameController controller) {
        this.controller = controller;
    }
}