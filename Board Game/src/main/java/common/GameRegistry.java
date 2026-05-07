package common;

import java.util.List;
import chess.ChessGame;
import minesweeper.MinesweeperGame;
import peace.PeaceGame;
import reversi.ReversiGame;

public final class GameRegistry {
    private static final List<GameDefinition> DEFAULT_GAMES = List.of(
        new GameDefinition("Peace", () -> new PeaceGame(8)),
        new GameDefinition("Reversi", () -> new ReversiGame(8)),
        new GameDefinition("Minesweeper", () -> new MinesweeperGame(8)),
        new GameDefinition("Chess", () -> new ChessGame(8))
    );

    private GameRegistry() {
    }

    public static List<GameDefinition> defaultGames() {
        return DEFAULT_GAMES;
    }
}