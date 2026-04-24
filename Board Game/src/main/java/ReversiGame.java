import java.util.ArrayDeque;
import java.util.Deque;

public class ReversiGame implements GameSession {
    private final Board board;
    private char currentPlayer;
    private int blackCount;
    private int whiteCount;
    private final Deque<GameSnapshot> history;

    private static class GameSnapshot {
        final char[][] boardState;
        final char currentPlayer;

        GameSnapshot(char[][] boardState, char currentPlayer) {
            this.boardState = boardState;
            this.currentPlayer = currentPlayer;
        }
    }

    public ReversiGame(int size) {
        this.board = new Board(size);
        this.history = new ArrayDeque<GameSnapshot>();
        init();
    }

    public void init() {
        board.initialize();
        currentPlayer = Board.BLACK;
        history.clear();
        refreshPieceCounts();
    }

    @Override
    public BoardView getBoard() { return board; }

    @Override
    public char getCurrentPlayer() { return currentPlayer; }

    public int getBlackCount() { return blackCount; }

    public int getWhiteCount() { return whiteCount; }

    @Override
    public TurnResult evaluateTurnState() {
        if (board.isFull()) return TurnResult.GAME_OVER;
        if (!board.hasLegalMove(currentPlayer)) {
            char opponent = (currentPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;
            if (!board.hasLegalMove(opponent)) return TurnResult.GAME_OVER;
            currentPlayer = opponent;
            return TurnResult.PASS_TURN;
        }
        return TurnResult.SUCCESS;
    }
    @Override
    public TurnResult processMove(ProcessInput m) {
        if (m == null) return TurnResult.INVALID_INPUT;
        if (board.isFull()) return TurnResult.GAME_OVER;
        if (board.getCell(m.row, m.col) != Board.EMPTY) return TurnResult.OCCUPIED;

        if (!board.isLegalMove(m.row, m.col, currentPlayer)) {
            return TurnResult.ILLEGAL_MOVE;
        }

        saveSnapshot();
        board.processMove(m.row, m.col, currentPlayer);
        refreshPieceCounts();
        currentPlayer = (currentPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;

        if (board.isFull()) return TurnResult.GAME_OVER;

        if (!board.hasLegalMove(currentPlayer)) {
            currentPlayer = (currentPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;
            if (!board.hasLegalMove(currentPlayer)) return TurnResult.GAME_OVER;
            return TurnResult.PASS_TURN;
        }

        return TurnResult.SUCCESS;
    }

    @Override
    public boolean undoLastMove() {
        if (history.isEmpty()) return false;
        GameSnapshot snapshot = history.pop();
        board.loadCells(snapshot.boardState);
        currentPlayer = snapshot.currentPlayer;
        refreshPieceCounts();
        return true;
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty();
    }

    @Override
    public boolean isFinished() {
        return board.isFull() || (!board.hasLegalMove(Board.BLACK) && !board.hasLegalMove(Board.WHITE));
    }

    @Override
    public boolean shouldShowScore() {
        return true;
    }

    @Override
    public boolean shouldShowLegalMoves() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Reversi";
    }

    @Override
    public String getPlayersSummary() {
        return " P1:Tairitsu\n P2:Hikari";
    }

    @Override
    public String getScoreSummary() {
        return String.format(" Score: B=%d | W=%d", blackCount, whiteCount);
    }

    @Override
    public String getFinishSummary() {
        if (blackCount > whiteCount) return "Winner: Tairitsu";
        if (whiteCount > blackCount) return "Winner: Hikari";
        return "Winner: NOBODY";
    }

    @Override
    public GameSession newGame(int boardSize) {
        return new ReversiGame(boardSize);
    }

    private void saveSnapshot() {
        history.push(new GameSnapshot(board.copyCells(), currentPlayer));
    }

    private void refreshPieceCounts() {
        int black = 0;
        int white = 0;
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                char cell = board.getCell(row, col);
                if (cell == Board.BLACK) black++;
                else if (cell == Board.WHITE) white++;
            }
        }
        blackCount = black;
        whiteCount = white;
    }

    public boolean isBoardFull() { return board.isFull(); }
}
