package common.ui;

import common.GameDefinition;
import java.util.List;

public interface GameUiRenderer {
    void render(GameUiState state);

    GameDefinition promptGameType(List<GameDefinition> availableGames, GameDefinition defaultType, String promptTitle);

    void showMessage(String title, String message);

    void close();
}