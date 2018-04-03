package ai.mate.chess.model.board;

import ai.mate.chess.model.move.Move;
import ai.mate.chess.model.piece.*;
import ai.mate.chess.model.player.Player;

import java.awt.*;
import java.util.List;

import static ai.mate.chess.model.piece.Piece.PieceType.KING;
import static ai.mate.chess.model.piece.Piece.PieceType.PAWN;

public final class Board {

    private Tile[][] board;

    public Board(Player white, Player black) {
        board = new Tile[8][8];
        createEmptyBoard();
        initBoard();
        givePiecesToPlayers(white, black);
    }

    public Board(Board board) {
        this.board = new Tile[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                this.board[i][j] = board.getBoard()[i][j].copy();
            }
        }
    }

    private void initBoard() {
        for (int i = 0; i < 8; i += 7) {
            Piece.PlayerColor playerColor = i == 0 ? Piece.PlayerColor.BLACK : Piece.PlayerColor.WHITE;
            board[i][0] = new Tile(new Rook(playerColor, new Point(0, i)));
            board[i][1] = new Tile(new Knight(playerColor, new Point(1, i)));
            board[i][2] = new Tile(new Bishop(playerColor, new Point(2, i)));
            board[i][3] = new Tile(new Queen(playerColor, new Point(3, i)));
            board[i][4] = new Tile(new King(playerColor, new Point(4, i)));
            board[i][5] = new Tile(new Bishop(playerColor, new Point(5, i)));
            board[i][6] = new Tile(new Knight(playerColor, new Point(6, i)));
            board[i][7] = new Tile(new Rook(playerColor, new Point(7, i)));
        }

        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new Tile(new Point(j, i));
            }
        }

        for (int k = 0; k < 8; k++) {
            board[1][k] = new Tile(new Pawn(Piece.PlayerColor.BLACK, new Point(k, 1)));
            board[6][k] = new Tile(new Pawn(Piece.PlayerColor.WHITE, new Point(k, 6)));
        }
    }

    private void createEmptyBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new Tile(new Point(i, j));
            }
        }
    }

    private void givePiecesToPlayers(Player white, Player black) {
        int[] rows = {0, 1, 6, 7};

        for (int row : rows) {
            for (int i = 0; i < 8; i++) {
                Piece piece = board[row][i].getPiece();
                if (piece != null) {
                    if (piece.getPlayerColor() == Piece.PlayerColor.BLACK) {
                        black.addPiece(piece);
                    } else {
                        white.addPiece(piece);
                    }
                }
            }
        }
    }

    public Tile[][] getBoard() {
        return this.board;
    }

    public Tile getTile(Point point) {
        return this.board[point.y][point.x];
    }

    public void setTile(Point position, Tile tile) {
        this.board[position.y][position.x] = tile;
    }

    public void unhighlightBoard() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j].getHighlight() != Tile.TILE_HIGHLIGHT.NONE) {
                    board[i][j].setHighlight(Tile.TILE_HIGHLIGHT.NONE);
                }
            }
        }
    }

    public void handleMove(Move move) {
        Tile start = getTile(move.getStart());
        Tile end = getTile(move.getEnd());
        Piece pieceToMove = start.getPiece();
        pieceToMove.updatePiece(move);
        start.setPiece(null);
        end.setPiece(pieceToMove);
    }

    public boolean isValidPosition(Point position) {
        return (position.x >= 0 && position.x <= 7 && position.y >= 0 && position.y <= 7);
    }

    public Tile getTile(int x, int y) {
        return this.board[y][x];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            sb.append("");
            sb.append("-----------------------------------------------------------------\n");
            for (int j = 0; j < board[i].length; j++) {
                sb.append("| " + board[j][i] + " ");
            }
            sb.append("|\n");
        }
        sb.append("-----------------------------------------------------------------\n");
        return sb.toString();
    }

    public void clearTile(Point start) {
        getTile(start).setPiece(null);
    }

    public boolean tileIsThreatened(Piece.PlayerColor playerColor, Tile tile) {
        return tileAtPointIsThreatened(playerColor, tile.getPosition());
    }

    public boolean tileAtPointIsThreatened(Piece.PlayerColor goodPlayerColor, Point tilePos) {
        int threatenedRow = tilePos.x;
        int threatenedCol = tilePos.y;

        int[] rowDirections = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] colDirections = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int direction = 0; direction < 8; direction++) {
            int row = threatenedRow;
            int col = threatenedCol;


            int rowIncrement = rowDirections[direction];
            int colIncrement = colDirections[direction];

            for (int step = 0; step < 8; step++) {
                row = row + rowIncrement;
                col = col + colIncrement;

                if (outOfBounds(row, col)) {
                    break;
                } else {
                    Tile t = getTile(row, col);
                    if (!t.isEmpty()) {
                        Piece piece = t.getPiece();
                        if (piece.getPlayerColor() != goodPlayerColor) {
                            if (piece instanceof Knight) {
                                // Handle knights differently. Just compute the moves and check if the tile is there
                                List<Move> moves = piece.getAvailableMoves(this);
                                for (Move move : moves) {
                                    if (move.getType() == Move.MoveType.ATTACK) {
                                        Piece potentialKing = getTile(move.end).getPiece();
                                        if (potentialKing.getPieceType() == KING && potentialKing.getPlayerColor() == goodPlayerColor) {
                                            System.out.println("Knight can attack your king!");
                                            return true;
                                        }
                                    }
                                }
                            } else if (step > 0 && (piece.getPieceType() != PAWN && piece.getPieceType() != KING)) {
                                if (piece.getPositionThreats()[direction]) return true;
                            } else {
                                if (step == 0) {
                                    if (piece.getPositionThreats()[direction]) return true;
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }

        return false;
    }

    private boolean outOfBounds(int row, int col) {
        return row < 0 || row > 7 || col < 0 || col > 7;
    }
}
