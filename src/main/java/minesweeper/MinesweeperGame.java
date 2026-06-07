package minesweeper;

import common.BoardView;
import common.GameAction;
import common.GameSession;
import common.TurnResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class MinesweeperGame implements GameSession {
    private static final int TOTAL_MINES_HINT = 10;
    private static final long DEMO_SEED = 1778639585876L;

    private MinesweeperBoard board;
    private BoardView boardView;
    private final Deque<String[][]> history;
    private long seed;
    private final MinesweeperDemoDebugger demoDebugger;
    private int demoIndex;
    private boolean demoResetDone;

    public MinesweeperGame(int boardSize) {
        this(System.currentTimeMillis());
    }

    private MinesweeperGame(long seed) {
        this.seed = seed;
        this.board = new MinesweeperBoard(seed);
        this.boardView = new MinesweeperBoardAdapter(board);
        this.history = new ArrayDeque<String[][]>();
        this.demoDebugger = new MinesweeperDemoDebugger(seed, board.getSize());
    }

    @Override
    public String getDisplayName() {
        return "Minesweeper";
    }

    @Override
    public BoardView getBoard() {
        return boardView;
    }

    public String getCellState(int row, int col) {
        return board.getCell(row, col);
    }

    public int getFlipCount() {
        int count = 0;
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                String cell = board.getCell(row, col);
                if (MinesweeperBoardView.UNFLIPPED.equals(cell)) continue;
                if (MinesweeperBoardView.FLAG.equals(cell)) continue;
                count++;
            }
        }
        return count;
    }

    public int getFlagCount() {
        int count = 0;
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (MinesweeperBoardView.FLAG.equals(board.getCell(row, col))) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getRemainingMineHintCount() {
        return TOTAL_MINES_HINT - getFlagCount();
    }

    public boolean canPlaceMoreFlags() {
        return getFlagCount() < TOTAL_MINES_HINT;
    }

    @Override
    public TurnResult handleRawInput(String rawInput) {
        if (rawInput == null) return TurnResult.INVALID_INPUT;
        String input = rawInput.trim().toUpperCase();
        if ("AUTO".equals(input)) {
            return applyAutoMove();
        }
        if ("H".equals(input)) {
            return useHint();
        }
        if (input.startsWith("FLAG ")) {
            int[] position = parseCoordinate(input.substring(5));
            if (position == null) return TurnResult.INVALID_INPUT;
            return toggleFlagAt(position[0], position[1]);
        }
        int[] position = parseCoordinate(input);
        if (position == null) return TurnResult.INVALID_INPUT;
        return revealAt(position[0], position[1]);
    }

    private TurnResult applyAutoMove() {
        if (!demoResetDone) {
            resetForDemo();
        }

        List<String> demoInputs = getDemoInputs();
        if (demoInputs.isEmpty() || demoIndex >= demoInputs.size()) {
            return TurnResult.GAME_OVER;
        }

        String next = demoInputs.get(demoIndex);
        String input = translateDisplayInput(next).trim().toUpperCase();
        demoIndex++;

        if (input.startsWith("FLAG ")) {
            int[] position = parseCoordinate(input.substring(5));
            if (position == null) return TurnResult.INVALID_INPUT;
            return toggleFlagAt(position[0], position[1]);
        }
        int[] position = parseCoordinate(input);
        if (position == null) return TurnResult.INVALID_INPUT;
        return revealAt(position[0], position[1]);
    }

    private String translateDisplayInput(String rawInput) {
        if (rawInput == null) return rawInput;
        String input = rawInput.trim().toUpperCase();
        if (input.length() == 1) return input;
        if (input.startsWith("FLAG ")) {
            return "FLAG " + translateDisplayInput(input.substring(5));
        }

        char file = input.charAt(0);
        if (!Character.isLetter(file)) return input;

        String rankPart = input.substring(1);
        try {
            int rank = Integer.parseInt(rankPart);
            int size = board.getSize();
            if (rank < 1 || rank > size) return input;
            int internalRank = size - rank + 1;
            return String.valueOf(file) + internalRank;
        } catch (NumberFormatException e) {
            return input;
        }
    }

    private void resetForDemo() {
        this.seed = DEMO_SEED;
        this.board = new MinesweeperBoard(DEMO_SEED);
        this.boardView = new MinesweeperBoardAdapter(board);
        this.history.clear();
        this.demoIndex = 0;
        this.demoResetDone = true;
    }

    @Override
    public String getStatusSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append(" Flipped: ").append(getFlipCount()).append('\n');
        builder.append(" Flags: ").append(getFlagCount()).append('\n');
        builder.append(" Remaining mines: ").append(getRemainingMineHintCount());
        return builder.toString();
    }

    @Override
    public List<GameAction> getActionButtons() {
        return List.of(
            new GameAction("flip", "Flip"),
            new GameAction("flag", "Flag")
        );
    }

    @Override
    public TurnResult handleAction(GameAction action, String rawInput) {
        if (action == null || rawInput == null) return TurnResult.INVALID_INPUT;
        int[] position = parseCoordinate(rawInput);
        if (position == null) return TurnResult.INVALID_INPUT;
        if ("flip".equals(action.id)) {
            return revealAt(position[0], position[1]);
        }
        if ("flag".equals(action.id)) {
            return toggleFlagAt(position[0], position[1]);
        }
        if ("hint".equals(action.id)) {
            // hint ignores coordinate input; just reveal a random safe cell
            return useHint();
        }
        return TurnResult.INVALID_INPUT;
    }

    @Override
    public TurnResult evaluateTurnState() {
        if (isFinished()) return TurnResult.GAME_OVER;
        return TurnResult.SUCCESS;
    }

    public TurnResult revealAt(int row, int col) {
        return applyBoardAction(row, col, false);
    }

    public TurnResult toggleFlagAt(int row, int col) {
        return applyBoardAction(row, col, true);
    }

    public TurnResult useHint() {
        if (isFinished()) return TurnResult.GAME_OVER;
        history.push(board.copyCells());
        boolean revealed = board.revealRandomSafeCell();
        if (!revealed) {
            history.pop();
            return TurnResult.SUCCESS;
        }
        if (isFinished()) return TurnResult.GAME_OVER;
        return TurnResult.SUCCESS;
    }

    private TurnResult applyBoardAction(int row, int col, boolean toggleFlag) {
        if (isFinished()) return TurnResult.GAME_OVER;
        if (row < 0 || row >= board.getSize() || col < 0 || col >= board.getSize()) {
            return TurnResult.INVALID_INPUT;
        }

        String before = board.getCell(row, col);
        if (toggleFlag) {
            if (MinesweeperBoardView.MINE.equals(before) || before != null && before.length() == 1 && Character.isDigit(before.charAt(0))) {
                return TurnResult.OCCUPIED;
            }
            if (MinesweeperBoardView.FLAG.equals(before)) {
                history.push(board.copyCells());
                board.toggleFlag(row, col);
                return TurnResult.SUCCESS;
            }
            if (!MinesweeperBoardView.UNFLIPPED.equals(before)) return TurnResult.OCCUPIED;
            history.push(board.copyCells());
            board.toggleFlag(row, col);
            return TurnResult.SUCCESS;
        }

        if (!MinesweeperBoardView.UNFLIPPED.equals(before)) return TurnResult.OCCUPIED;

        history.push(board.copyCells());
        board.reveal(row, col);
        if (isFinished()) return TurnResult.GAME_OVER;
        return TurnResult.SUCCESS;
    }

    @Override
    public boolean undoLastMove() {
        if (history.isEmpty()) return false;
        board.loadCells(history.pop());
        return true;
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty();
    }

    @Override
    public boolean isFinished() {
        return board.isMineRevealed() || board.isAllSafeCellsRevealed();
    }

    @Override
    public String getFinishSummary() {
        if (board.isMineRevealed()) return "Result: BOOM";
        if (board.isAllSafeCellsRevealed()) return "Result: CLEARED";
        return "";
    }

    @Override
    public boolean isDemoDebuggerAvailable() {
        return MinesweeperDemoDebugger.isEnabled();
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
        return demoDebugger.finishIfCleared(board.isAllSafeCellsRevealed());
    }

    @Override
    public List<String> getDemoInputs() {
        // Recorded by MinesweeperDemoDebugger with seed 1778639585876.
        return List.of(
            "A3", "A2", "A1", "B1", "B2", "B3", "B4", "A4",
            "C1", "D1", "D2", "C2", "FLAG C3", "D3", "C4", "D4",
            "B5", "C5", "D5", "B6", "C6", "D6", "FLAG A5", "A6",
            "A7", "B7", "FLAG C7", "C8", "FLAG D7", "E3", "F2", "F3",
            "F4", "E5", "E6", "E7", "FLAG E4", "E2", "FLAG E1", "F1",
            "G1", "G2", "G3", "H1", "H2", "H3", "H4", "G4",
            "H5", "H6", "H7", "H8", "G5", "G6", "G7", "G8",
            "FLAG F5", "F6", "F7", "FLAG F8", "FLAG E8", "D8", "FLAG B8", "A8"
        );
    }

    @Override
    public String getDemoSummary() {
        return "Minesweeper demo uses a recorded seed and a human-played route with flags.";
    }

    @Override
    public GameSession newGame(int boardSize) {
        return new MinesweeperGame(boardSize);
    }

    @Override
    public GameSession newDemoGame(int boardSize) {
        return new MinesweeperGame(DEMO_SEED);
    }

    private int[] parseCoordinate(String rawInput) {
        String input = rawInput.trim().toUpperCase();
        if (input.length() != 2) return null;
        char colChar = input.charAt(0);
        char rowChar = input.charAt(1);
        int size = board.getSize();
        if (colChar < 'A' || colChar >= 'A' + size) return null;
        if (rowChar < '1' || rowChar >= '1' + size) return null;
        return new int[] {rowChar - '1', colChar - 'A'};
    }
}
