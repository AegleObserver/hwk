public interface GameSession {
    String getDisplayName();

    BoardView getBoard();

    default char getCurrentPlayer() {
        return '\0';
    }

    TurnResult evaluateTurnState();

    TurnResult processMove(ProcessInput move);

    boolean undoLastMove();

    boolean canUndo();

    boolean isFinished();

    default boolean shouldShowScore() {
        return false;
    }

    default boolean shouldShowLegalMoves() {
        return false;
    }

    default String getPlayersSummary() {
        return "";
    }

    default String getScoreSummary() {
        return "";
    }

    default String getFinishSummary() {
        return "";
    }

    GameSession newGame(int boardSize);
}