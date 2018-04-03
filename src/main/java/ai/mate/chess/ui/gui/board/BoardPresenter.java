package ai.mate.chess.ui.gui.board;

import ai.mate.chess.algorithm.AI;
import ai.mate.chess.controller.Game;
import ai.mate.chess.controller.GameController;
import ai.mate.chess.model.board.Tile;
import ai.mate.chess.model.move.Move;
import ai.mate.chess.model.piece.Pawn;
import ai.mate.chess.model.piece.Piece;
import ai.mate.chess.model.piece.Queen;
import ai.mate.chess.model.player.Player;

import java.awt.*;

import static ai.mate.chess.model.move.Move.MoveType.ATTACK;
import static ai.mate.chess.model.move.Move.MoveType.PAWN_PROMOTION;
import static ai.mate.chess.model.piece.Piece.PieceType.KING;

public class BoardPresenter implements BoardGUIContract.Presenter {
    private final BoardGUIContract.View view;
    private final GameController gameController;
    private final Game game;
    private final AI ai;

    public BoardPresenter(BoardGUIContract.View view, GameController gameController, Game game, AI ai) {
        this.gameController = gameController;
        this.view = view;
        this.game = game;
        this.ai = ai;
        view.setPresenter(this);
    }

    @Override
    public void start() {
        loadBoard();
    }

    @Override
    public void loadBoard() {
        view.setBoard(gameController.getBoard());
        view.showView();
    }

    @Override
    public void pause() {
        view.setEnabled(false);
        view.showOverlay();
    }

    @Override
    public void unpause() {
        view.setEnabled(true);
        view.hideOverlay();
        view.updateBoard(gameController.getBoard());
    }

    @Override
    public void handleClickedTile(Tile tile) {
        if (gameController.isGameOver()) {
            return;
        }

        if (!tile.isEmpty() && isClickablePiece(tile)) {
            // It means they clicked a tile with a piece and it's not highlighted
            // Or they clicked the king, who is in check
            boolean inCheck = tile.getHighlight() == Tile.TILE_HIGHLIGHT.ORANGE;
            gameController.unhighlightBoard();
            Player currentPlayer = gameController.getCurrentPlayer();
            Piece piece = tile.getPiece();

            if (inCheck) {
                if (piece.getPieceType() != KING) {
                    // If they clicked a piece thats not a king while in check, don't show the moves
                    System.out.println("You can't updatePiece other pieces except your King while in check!");
                    return;
                } else if (piece.getPieceType() == KING && piece.getAvailableMoves(gameController.getBoard()).isEmpty()) {
                    handleGameOver(Piece.PlayerColor.BLACK);
                    return;
                }
            }

            if (currentPlayer.getPlayerColor() == piece.getPlayerColor()) {
                // Only show the current user their moves
                showAvailableMoves(piece);
                gameController.setSelectedPiece(piece);
            }

            view.updateBoard(gameController.getBoard());
        } else if (tile.isHighlighted() && isMove(tile)) {
            // It means they clicked a tile with a piece that is highlighted, i.e an attacking updatePiece.
            // Or they clicked a tile without a piece that is highlighted
            Move move = tile.getMove();

            if (handleKingCaptureGameOver(move)) {
                handleGameOver(Piece.PlayerColor.WHITE);
                move.handleMove(gameController.getBoard());
                return;
            }

            move.handleMove(gameController.getBoard());

            // Check for pawn promotion
            if (move.getType() == PAWN_PROMOTION) {
                Pawn pawn = (Pawn) gameController.getSelectedPiece();
                if (pawn.promotePawn()) {
                    game.showPawnPromotionView();
                }
            }

            gameController.unhighlightBoard();
            handleAIMove();

            if (gameController.isGameOver()) {
                // Check if the AI ended the game already
                return;
            }

            gameController.nextTurn();

            // Check if the AI put the user in check.
            if (gameController.inCheck(Piece.PlayerColor.WHITE)) {
                // If so, highlight the tile orange
                // Orange is a special color, only used to signify in check
                Tile kingTile = gameController.getTile(gameController.whiteKingPosition);
                kingTile.setHighlight(Tile.TILE_HIGHLIGHT.ORANGE);
            }

            view.updateBoard(gameController.getBoard());
        } else {
            gameController.unhighlightBoard();
            view.updateBoard(gameController.getBoard());
        }

    }

    private boolean handleKingCaptureGameOver(Move move) {
        if (move.getType() == ATTACK) {
            if (gameController.getTile(move.getEnd()).getPiece().getPieceType() == KING) {
                return true;
            }
        }

        return false;
    }

    private void handleGameOver(Piece.PlayerColor winner) {
        gameController.endGame(winner);
        gameController.printWinner();
        view.showOverlay();
    }

    private boolean isClickablePiece(Tile tile) {
        return !tile.isHighlighted() || tile.getHighlight() == Tile.TILE_HIGHLIGHT.ORANGE;
    }

    private void handleAIMove() {
        Move aiMove = ai.bestMove(gameController.getBoard());
        if (handleKingCaptureGameOver(aiMove)) {
            // The AI has captured the king
            handleGameOver(Piece.PlayerColor.BLACK);
            aiMove.handleMove(gameController.getBoard());
            return;
        } else {
            aiMove.handleMove(gameController.getBoard());
        }

        if (aiMove.getType() == PAWN_PROMOTION) {
            Piece pawnToPromote = gameController.getTile(aiMove.end).getPiece();
            Piece queen = new Queen(Piece.PlayerColor.BLACK, pawnToPromote.getPosition());
            queen.setPosition(pawnToPromote.getPosition());
            gameController.getTile(pawnToPromote.getPosition()).setPiece(queen);
        }

        gameController.nextTurn();
    }

    private void showAvailableMoves(Piece piece) {
        for (Move move : piece.getAvailableMoves(gameController.getBoard())) {
            Point movePoint = new Point(move.getEnd().x, move.getEnd().y);
            Tile startTile = gameController.getTile(piece.getPosition());
            startTile.setHighlight(Tile.TILE_HIGHLIGHT.GREEN);
            Tile target = gameController.getTile(movePoint);
            target.setHighlight(move.getTileHighlight());
            target.setMove(move);
            gameController.setTile(movePoint, target);
        }
    }

    public boolean isMove(Tile tile) {
        return tile.getHighlight() != Tile.TILE_HIGHLIGHT.GREEN && tile.getHighlight() != Tile.TILE_HIGHLIGHT.ORANGE;
    }
}