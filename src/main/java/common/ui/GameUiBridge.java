package common.ui;

import common.GameAction;

public interface GameUiBridge {
    void attach(GameUiRenderer renderer);

    GameUiState snapshot();

    void onRawInput(String rawInput);

    void onAction(GameAction action, String rawInput);

    void onResetRequested();

    void onNewGameRequested();

    void onSwitchGameRequested(int index);

    void onQuitRequested();

    default void setDemoRunning(boolean running) {
    }

    default boolean isDemoRunning() {
        return false;
    }
}