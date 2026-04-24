import java.util.ArrayDeque;
import java.util.Deque;

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
    public TurnResult evaluateTurnState() {
        return board.isFull() ? TurnResult.GAME_OVER : TurnResult.SUCCESS;
    }

    @Override
    public TurnResult processMove(ProcessInput move) {
        if (move == null) return TurnResult.INVALID_INPUT;
        if (board.isFull()) return TurnResult.GAME_OVER;
        if (move.row < 0 || move.row >= board.getSize() || move.col < 0 || move.col >= board.getSize()) {
            return TurnResult.INVALID_INPUT;
        }
        if (board.getCell(move.row, move.col) != Board.EMPTY) return TurnResult.OCCUPIED;

        history.push(new GameSnapshot(board.copyCells(), currentPlayer));
        board.setCell(move.row, move.col, currentPlayer);
        currentPlayer = (currentPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;
        if (board.isFull()) return TurnResult.GAME_OVER;
        return TurnResult.SUCCESS;
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
    public GameSession newGame(int boardSize) {
        return new PeaceGame(boardSize);
    }
}
