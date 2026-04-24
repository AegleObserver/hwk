public class ProcessInput {
    public enum Action {
        MOVE,
        FLIP,
        FLAG,
        HINT,
        SWITCH_BOARD,
        UNDO,
        QUIT,
        INVALID
    }

    public final Action action;
    public final int row;
    public final int col;

    private ProcessInput(Action action, int row, int col) {
        this.action = action;
        this.row = row;
        this.col = col;
    }

    public static ProcessInput parse(String s) {
        if (s == null) return invalid();
        String input = s.trim().toUpperCase();
        if (input.isEmpty()) return invalid();

        if ("U".equals(input)) return undo();
        if ("H".equals(input)) return hint();
        if ("Q".equals(input)) return quit();

        if (input.length() == 1) {
            char c = input.charAt(0);
            if (c >= '1' && c <= '9') {
                int boardIndex = c - '0';
                return switchBoard(boardIndex);
            }
            return invalid();
        }

        if (input.length() != 2) return invalid();

        char c = input.charAt(0);
        char r = input.charAt(1);
        char maxCol = (char) ('A' + Board.FIXED_SIZE - 1);
        char maxRow = (char) ('0' + Board.FIXED_SIZE);
        if (c < 'A' || c > maxCol || r < '1' || r > maxRow) return invalid();

        int col = c - 'A';
        int row = r - '1';
        return move(row, col);
    }

    public static ProcessInput move(int row, int col) {
        return new ProcessInput(Action.MOVE, row, col);
    }

    public static ProcessInput flip() {
        return new ProcessInput(Action.FLIP, -1, -1);
    }

    public static ProcessInput flag() {
        return new ProcessInput(Action.FLAG, -1, -1);
    }

    public static ProcessInput hint() {
        return new ProcessInput(Action.HINT, -1, -1);
    }

    public static ProcessInput switchBoard(int boardIndex) {
        return new ProcessInput(Action.SWITCH_BOARD, -1, boardIndex);
    }

    public static ProcessInput undo() {
        return new ProcessInput(Action.UNDO, -1, -1);
    }

    public static ProcessInput quit() {
        return new ProcessInput(Action.QUIT, -1, -1);
    }

    public static ProcessInput invalid() {
        return new ProcessInput(Action.INVALID, -1, -1);
    }
}
