package common.ui;

import common.GameDefinition;

import javax.swing.JOptionPane;
import java.util.List;

public final class JavaFxUiPlugin implements GameUiPlugin {
    public static final String ID = "javafx";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "JavaFX (stub)";
    }

    @Override
    public void start(GameUiBridge bridge) throws Exception {
        GameUiRenderer renderer = new GameUiRenderer() {
            @Override
            public void render(GameUiState state) {
            }

            @Override
            public GameDefinition promptGameType(List<GameDefinition> availableGames, GameDefinition defaultType, String promptTitle) {
                return defaultType;
            }

            @Override
            public void showMessage(String title, String message) {
                // JavaFX might not be available at runtime; use simple dialog for stub
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void close() {
            }
        };

        bridge.attach(renderer);
        renderer.render(bridge.snapshot());
        renderer.showMessage("Info", "JavaFX UI plugin is a stub — implement later.");
    }
}
