package ai.mate.chess.controller;

import ai.mate.chess.controller.interfaces.IGameController;
import ai.mate.chess.model.Board;
import ai.mate.chess.ui.ITui;
import ai.mate.chess.ui.Tui;

public class GameController implements IGameController {

    private final ITui tui = Tui.getInstance();

    private Board board;

    private static IGameController instance;

    static {
        try {
            instance = new GameController();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate Singleton GameController instance!");
        }
    }

    private GameController() {

    }

    public static synchronized IGameController getInstance() {
        return instance;
    }

    @Override
    public void start() {
        reset();
        tui.printBoard(board);
        System.out.println(tui.getBoardPositionInput());
    }

    @Override
    public void reset() {
        board = new Board();
    }

}