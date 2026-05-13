package chess;

import common.StringConstructer;

import common.BoardView;
import common.GameAction;
import common.GameSession;
import common.TurnResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ChessGame implements GameSession {
    private final ChessBoard board;
    private final Deque<ChessSnapshot> history;
    private char currentPlayer;
    private boolean finished;
    private char winner;
    private final ChessDemoDebugger demoDebugger;

    public ChessGame(int boardSize) {
        this.board = new ChessBoard(boardSize);
        this.history = new ArrayDeque<ChessSnapshot>();
        this.demoDebugger = new ChessDemoDebugger();
        this.currentPlayer = 'W';
        this.finished = false;
        this.winner = '\0';
        board.initializeBoard();
    }

    @Override
    public String getDisplayName() {
        return "Chess";
    }

    @Override
    public BoardView getBoard() {
        return board;
    }

    @Override
    public TurnResult handleRawInput(String rawInput) {
        if (finished) return TurnResult.GAME_OVER;

        ProcessInputInChess move = ProcessInputInChess.parse(rawInput);
        if (move == null) return TurnResult.INVALID_INPUT;

        char sourcePiece = board.getCell(move.fromRow, move.fromCol);
        if (sourcePiece == '.' || sourcePiece == 'x') {
            return TurnResult.EMPTY_SOURCE;
        }
        if (!board.hasPieceAt(move.fromRow, move.fromCol, currentPlayer)) {
            return TurnResult.WRONG_SIDE;
        }

        if (!board.isMoveLegalByCache(move, currentPlayer)) {
            return TurnResult.ILLEGAL_MOVE;
        }

        history.push(new ChessSnapshot(board.snapshot(), currentPlayer, finished, winner));
        board.applyMove(move, currentPlayer);

        char opponent = currentPlayer == 'W' ? 'B' : 'W';
        if (!board.isKingAlive(opponent)) {
            finished = true;
            winner = currentPlayer;
            return TurnResult.GAME_OVER;
        }

        currentPlayer = currentPlayer == 'W' ? 'B' : 'W';
        return TurnResult.SUCCESS;
    }

    @Override
    public TurnResult evaluateTurnState() {
        return finished ? TurnResult.GAME_OVER : TurnResult.SUCCESS;
    }

    @Override
    public boolean undoLastMove() {
        if (finished) return false;
        if (history.isEmpty()) return false;
        ChessSnapshot snapshot = history.pop();
        board.load(snapshot.boardSnapshot);
        currentPlayer = snapshot.currentPlayer;
        finished = snapshot.finished;
        winner = snapshot.winner;
        return true;
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty() && !finished;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public char getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public String getPlayersSummary() {
        return " P1:Hikari\n P2:Tairitsu";
    }



    @Override
    public String getStatusSummary() {
        if (finished) {
            return " Winner: " + (winner == 'W' ? "Hikari" : "Tairitsu");
        }
        StringConstructer sc = new StringConstructer();
        sc.append(" Current: ").append(currentPlayer == 'W' ? "Hikari" : "Tairitsu");
        boolean whiteInCheck = isKingInCheck('W');
        boolean blackInCheck = isKingInCheck('B');
        if (whiteInCheck) sc.append('\n').append("Check: Hikari");
        if (blackInCheck) sc.append('\n').append("Check: Tairitsu");
        return sc.toString();
    }

    @Override
    public String getFinishSummary() {
        if (!finished) return "";
        return (winner == 'W' ? "Hikari" : "Tairitsu") + " wins by capturing the king.";
    }

    @Override
    public boolean isDemoDebuggerAvailable() {
        return ChessDemoDebugger.isEnabled();
    }

    @Override
    public boolean isDemoDebuggerRecording() {
        return demoDebugger.isRecording();
    }

    @Override
    public void startDemoDebugger() {
        demoDebugger.start();
    }

    @Override
    public void recordDemoDebuggerStep(String rawInput, GameAction action, TurnResult result) {
        demoDebugger.record(rawInput, action, result);
    }

    @Override
    public String finishDemoDebuggerIfSuccessful() {
        return demoDebugger.finishIfSuccessful(isFinished());
    }

   @Override
public List<String> getDemoInputs() {
    // 这是一个经典的“牧羊人将死”(Scholar's Mate)演示
    // 展示了基础发展、皇后进攻以及最终的将死
    return List.of(
        // 1. 双方推中心兵，开启道路
        "m e2 e4",
        "m e7 e5",

        // 2. 白方出主教，瞄准黑方最脆弱的 f7 兵
        "m f1 c4",
        "m b8 c6", // 黑方正常出马开发

        // 3. 白方出皇后，对 f7 形成双重威胁
        "m d1 h5",
        "m g8 f6", // 黑方试图驱逐皇后，但忽略了 f7 的危险

        // 4. 白方皇后吃掉 f7 兵，直接将死 (Checkmate)
        "m h5 f7",
        "m d8 e7",
        "m f7 e8"
    );
}
    @Override
    public String getDemoSummary() {
        return "Chess demo follows a scripted sequence to capture the king.";
    }

    @Override
    public GameSession newGame(int boardSize) {
        return new ChessGame(boardSize);
    }

    private static final class ChessSnapshot {
        final ChessBoard.ChessBoardSnapshot boardSnapshot;
        final char currentPlayer;
        final boolean finished;
        final char winner;

        private ChessSnapshot(ChessBoard.ChessBoardSnapshot boardSnapshot, char currentPlayer, boolean finished, char winner) {
            this.boardSnapshot = boardSnapshot;
            this.currentPlayer = currentPlayer;
            this.finished = finished;
            this.winner = winner;
        }
    }

    private boolean isKingInCheck(char kingPlayer) {
        // find king position
        int size = board.getSize();
        int kingRow = -1, kingCol = -1;
        char kingChar = kingPlayer == 'W' ? 'K' : 'k';
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board.getCell(r, c) == kingChar) {
                    kingRow = r; kingCol = c; break;
                }
            }
            if (kingRow >= 0) break;
        }
        if (kingRow < 0) return false;

        char opponent = kingPlayer == 'W' ? 'B' : 'W';
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!board.hasPieceAt(r, c, opponent)) continue;
                ProcessInputInChess probe = new ProcessInputInChess(r, c, kingRow, kingCol);
                if (board.isMoveLegalByCache(probe, opponent)) return true;
            }
        }
        return false;
    }
}
