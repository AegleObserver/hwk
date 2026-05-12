package peace;

import common.BoardView;
import common.GameSession;
import common.TurnResult;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import reversi.Board;

public class PeaceGame implements GameSession {
    private final Board board;
    private char currentPlayer;
    private final Deque<GameSnapshot> history;

    private static class GameSnapshot {
        final char[][] boardState;
        final char currentPlayer;

        GameSnapshot(char[][] boardState, char currentPlayer) {
            this.boardState = boardState;
            this.currentPlayer = currentPlayer;
        }
    }

    public PeaceGame(int boardSize) {
        this.board = new Board(boardSize);
        this.history = new ArrayDeque<GameSnapshot>();
        initBlankBoard();
    }

    private void initBlankBoard() {
        char[][] blank = new char[board.getSize()][board.getSize()];
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                blank[row][col] = Board.EMPTY;
            }
        }
        board.loadCells(blank);
        currentPlayer = Board.BLACK;
        history.clear();
    }

    @Override
    public String getDisplayName() {
        return "Peace";
    }

    @Override
    public BoardView getBoard() {
        return board;
    }

    @Override
    public char getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public TurnResult handleRawInput(String rawInput) {
        int[] position = parseCoordinate(rawInput);
        if (position == null) return TurnResult.INVALID_INPUT;
        return applyMove(position[0], position[1]);
    }

    @Override
    public TurnResult evaluateTurnState() {
        return board.isFull() ? TurnResult.GAME_OVER : TurnResult.SUCCESS;
    }

    @Override
    public boolean undoLastMove() {
        if (history.isEmpty()) return false;
        GameSnapshot snapshot = history.pop();
        board.loadCells(snapshot.boardState);
        currentPlayer = snapshot.currentPlayer;
        return true;
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty();
    }

    @Override
    public boolean isFinished() {
        return board.isFull();
    }

    @Override
    public boolean shouldShowScore() {
        return false;
    }

    @Override
    public boolean shouldShowLegalMoves() {
        return false;
    }

    @Override
    public String getPlayersSummary() {
        return " P1:Tairitsu\n P2:Hikari";
    }


    @Override
    public String getFinishSummary() {
        return "";
    }

    @Override
    public List<String> getDemoInputs() {
        List<String> steps = new ArrayList<String>();
        int size = board.getSize();
        for (int row = size; row >= 1; row--) {
            for (int col = 0; col < size; col++) {
                char file = (char) ('A' + col);
                steps.add(String.valueOf(file) + row);
            }
        }
        return steps;
    }

    @Override
    public String getDemoSummary() {
        return "Peace demo fills all cells in order.";
    }

    @Override
    public GameSession newGame(int boardSize) {
        return new PeaceGame(boardSize);
    }

    private int[] parseCoordinate(String rawInput) {
        if (rawInput == null) return null;
        String input = rawInput.trim().toUpperCase();
        if (input.length() != 2) return null;
        char colChar = input.charAt(0);
        char rowChar = input.charAt(1);
        int size = board.getSize();
        if (colChar < 'A' || colChar >= 'A' + size) return null;
        if (rowChar < '1' || rowChar >= '1' + size) return null;
        return new int[] {rowChar - '1', colChar - 'A'};
    }

    private TurnResult applyMove(int row, int col) {
        if (board.isFull()) return TurnResult.GAME_OVER;
        if (board.getCell(row, col) != Board.EMPTY) return TurnResult.OCCUPIED;

        history.push(new GameSnapshot(board.copyCells(), currentPlayer));
        board.setCell(row, col, currentPlayer);
        currentPlayer = (currentPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;
        if (board.isFull()) return TurnResult.GAME_OVER;
        return TurnResult.SUCCESS;
    }
}