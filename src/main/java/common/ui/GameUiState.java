package common.ui;

import common.GameAction;
import common.GameDefinition;
import common.GameSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameUiState {
    private final String title;
    private final GameSession activeGame;
    private final List<GameDefinition> availableGames;
    private final List<GameAction> actionButtons;
    private final int activeGameIndex;
    private final int gameCount;
    private final boolean demoRunning;
    private final boolean demoPaused;
    private final String gameInfoText;
    private final String currentPlayerText;
    private final String scoreText;
    private final String boardIndexText;
    private final String messageText;

    public GameUiState(
        String title,
        GameSession activeGame,
        List<GameDefinition> availableGames,
        List<GameAction> actionButtons,
        int activeGameIndex,
        int gameCount,
        boolean demoRunning,
        boolean demoPaused,
        String gameInfoText,
        String currentPlayerText,
        String scoreText,
        String boardIndexText,
        String messageText
    ) {
        this.title = safeText(title);
        this.activeGame = activeGame;
        this.availableGames = copyList(availableGames);
        this.actionButtons = copyList(actionButtons);
        this.activeGameIndex = activeGameIndex;
        this.gameCount = gameCount;
        this.demoRunning = demoRunning;
        this.demoPaused = demoPaused;
        this.gameInfoText = safeText(gameInfoText);
        this.currentPlayerText = safeText(currentPlayerText);
        this.scoreText = safeText(scoreText);
        this.boardIndexText = safeText(boardIndexText);
        this.messageText = safeText(messageText);
    }

    public String getTitle() {
        return title;
    }

    public GameSession getActiveGame() {
        return activeGame;
    }

    public List<GameDefinition> getAvailableGames() {
        return availableGames;
    }

    public List<GameAction> getActionButtons() {
        return actionButtons;
    }

    public int getActiveGameIndex() {
        return activeGameIndex;
    }

    public int getGameCount() {
        return gameCount;
    }

    public boolean isDemoRunning() {
        return demoRunning;
    }

    public boolean isDemoPaused() {
        return demoPaused;
    }

    public String getGameInfoText() {
        return gameInfoText;
    }

    public String getCurrentPlayerText() {
        return currentPlayerText;
    }

    public String getScoreText() {
        return scoreText;
    }

    public String getBoardIndexText() {
        return boardIndexText;
    }

    public String getMessageText() {
        return messageText;
    }

    private static String safeText(String text) {
        return text == null ? "" : text;
    }

    private static <T> List<T> copyList(List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}