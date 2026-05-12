package common;

public class ProcessInput {
    public enum Action {
        SWITCH_BOARD,
        UNDO,
        QUIT,
        INVALID
    }

    public final Action action;
    public final int value;

    private ProcessInput(Action action, int value) {
        this.action = action;
        this.value = value;
    }

    public static ProcessInput parse(String s) {
        if (s == null) return invalid();
        String input = s.trim().toUpperCase();
        if (input.isEmpty()) return invalid();

        if ("U".equals(input)) return undo();
        if ("Q".equals(input)) return quit();

        if (input.length() == 1) {
            char c = input.charAt(0);
            if (c >= '1' && c <= '9') {
                int boardIndex = c - '0';
                return switchBoard(boardIndex);
            }
            return invalid();
        }

        return invalid();
    }


    public static ProcessInput switchBoard(int boardIndex) {
        return new ProcessInput(Action.SWITCH_BOARD, boardIndex);
    }

    public static ProcessInput undo() {
        return new ProcessInput(Action.UNDO, -1);
    }

    public static ProcessInput quit() {
        return new ProcessInput(Action.QUIT, -1);
    }

    public static ProcessInput hint() {
        return invalid();
    }

    public static ProcessInput invalid() {
        return new ProcessInput(Action.INVALID, -1);
    }
}