package minesweeper;

import common.BoardView;
import common.GameAction;
import common.GameSession;
import common.TurnResult;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Deque;

public class MinesweeperGame implements GameSession {
    private static final int TOTAL_MINES_HINT = 10;

    private final MinesweeperBoard board;
    private final BoardView boardView;
    private final Deque<String[][]> history;

    public MinesweeperGame(int boardSize) {
        this.board = new MinesweeperBoard();
        this.boardView = new MinesweeperBoardAdapter(board);
        this.history = new ArrayDeque<String[][]>();
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
        if ("H".equals(input)) {
            return useHint();
        }
        int[] position = parseCoordinate(input);
        if (position == null) return TurnResult.INVALID_INPUT;
        return revealAt(position[0], position[1]);
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
    public List<String> getDemoInputs() {
        List<String> steps = new ArrayList<String>();
        int safeCellCount = board.getSize() * board.getSize() - TOTAL_MINES_HINT;
        for (int i = 0; i < safeCellCount; i++) {
            steps.add("H");
        }
        return steps;
    }

    @Override
    public String getDemoSummary() {
        return "Minesweeper demo uses hint steps to reveal safe cells.";
    }

    @Override
    public GameSession newGame(int boardSize) {
        return new MinesweeperGame(boardSize);
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