package common;

import java.util.Collections;
import java.util.List;

public interface GameSession {
    String getDisplayName();

    BoardView getBoard();

    default char getCurrentPlayer() {
        return '\0';
    }

    TurnResult evaluateTurnState();

    default TurnResult handleRawInput(String rawInput) {
        return TurnResult.INVALID_INPUT;
    }

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

    default String getStatusSummary() {
        return "";
    }

    default List<String> getDemoInputs() {
        return Collections.emptyList();
    }

    default String getDemoSummary() {
        return "";
    }

    default List<GameAction> getActionButtons() {
        return Collections.emptyList();
    }

    default TurnResult handleAction(GameAction action, String rawInput) {
        return TurnResult.INVALID_INPUT;
    }

    GameSession newGame(int boardSize);
}